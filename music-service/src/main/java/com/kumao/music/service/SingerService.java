package com.kumao.music.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicianClient;
import com.kumao.api.dto.MusicianDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.music.dto.SingerApplyRequest;
import com.kumao.music.dto.SingerRequest;
import com.kumao.music.entity.Singer;
import com.kumao.music.mapper.SingerMapper;
import com.kumao.music.mapper.SongMapper;
import com.kumao.music.entity.Song;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SingerService {

    private final SingerMapper singerMapper;
    private final SongMapper songMapper;
    private final MusicianClient musicianClient;

    public PageResult<Singer> page(PageQuery query, String region) {
        Page<Singer> page = singerMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Singer>()
                        .eq(Singer::getStatus, 1)
                        .eq(StringUtils.hasText(region), Singer::getRegion, region)
                        .like(StringUtils.hasText(query.getKeyword()), Singer::getName, query.getKeyword())
                        .orderByDesc(Singer::getFansCount));
        return PageResult.of(page);
    }

    public Singer detail(Long id) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) {
            throw new BizException("歌手不存在");
        }
        return singer;
    }

    /** 当前用户的歌手认证记录（包含待审核、未通过） */
    public Singer mine(Long userId) {
        if (userId == null) return null;
        return singerMapper.selectOne(new LambdaQueryWrapper<Singer>()
                .eq(Singer::getUserId, userId)
                .orderByDesc(Singer::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 音乐人申请歌手认证：必须有已通过的音乐人认证。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, SingerApplyRequest req) {
        R<MusicianDTO> musicianResult = musicianClient.byUser(userId);
        if (musicianResult == null || musicianResult.getCode() != 200 || musicianResult.getData() == null) {
            throw new BizException("请先完成音乐人认证，再申请歌手认证");
        }
        MusicianDTO musician = musicianResult.getData();
        if (!"APPROVED".equals(musician.getStatus())) {
            throw new BizException("只有已认证音乐人才能申请歌手认证");
        }

        Singer singer = singerMapper.selectOne(new LambdaQueryWrapper<Singer>()
                .eq(Singer::getMusicianId, musician.getId())
                .last("limit 1"));
        if (singer != null) {
            if (Integer.valueOf(1).equals(singer.getStatus())) {
                throw new BizException("你已经是认证歌手，无需重复申请");
            }
            if (Integer.valueOf(0).equals(singer.getStatus())) {
                throw new BizException("歌手认证正在审核中，请耐心等待");
            }
        } else {
            singer = new Singer();
            singer.setFansCount(0L);
        }
        singer.setName(musician.getStageName());
        singer.setAvatar(musician.getAvatar());
        singer.setIntro(StringUtils.hasText(req.getIntro()) ? req.getIntro().trim() : musician.getIntro());
        singer.setGender(req.getGender());
        singer.setRegion(req.getRegion());
        singer.setMusicianId(musician.getId());
        singer.setUserId(userId);
        singer.setStatus(0);
        singer.setRejectReason(null);
        singer.setAuditTime(null);
        if (singer.getId() == null) {
            singerMapper.insert(singer);
        } else {
            singerMapper.updateById(singer);
        }
        return singer.getId();
    }

    public PageResult<Singer> adminPage(PageQuery query, Integer status, String region) {
        Page<Singer> page = singerMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Singer>()
                        .eq(status != null, Singer::getStatus, status)
                        .eq(StringUtils.hasText(region), Singer::getRegion, region)
                        .like(StringUtils.hasText(query.getKeyword()), Singer::getName, query.getKeyword())
                        .orderByAsc(Singer::getStatus)
                        .orderByDesc(Singer::getCreateTime));
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean approved, String reason) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) throw new BizException("歌手认证申请不存在");
        if (!Integer.valueOf(0).equals(singer.getStatus())) {
            throw new BizException("该申请已处理");
        }
        singer.setStatus(approved ? 1 : 2);
        singer.setRejectReason(approved ? null : reason);
        singer.setAuditTime(LocalDateTime.now().toString());
        singerMapper.updateById(singer);
    }

    public List<Song> songs(Long singerId) {
        return songMapper.selectList(new LambdaQueryWrapper<Song>()
                .eq(Song::getSingerId, singerId)
                .eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount));
    }

    public Long create(SingerRequest req) {
        Singer singer = new Singer();
        apply(singer, req);
        singer.setFansCount(0L);
        singer.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        singerMapper.insert(singer);
        return singer.getId();
    }

    public void update(Long id, SingerRequest req) {
        Singer singer = detail(id);
        apply(singer, req);
        singerMapper.updateById(singer);
    }

    public void delete(Long id) {
        // 逻辑删除前释放 musician_id，避免唯一键阻止同一音乐人重新申请。
        Singer singer = singerMapper.selectById(id);
        if (singer != null) {
            singerMapper.update(null, new LambdaUpdateWrapper<Singer>()
                    .eq(Singer::getId, id)
                    .set(Singer::getMusicianId, null));
        }
        singerMapper.deleteById(id);
    }

    private void apply(Singer singer, SingerRequest req) {
        singer.setName(req.getName());
        singer.setAvatar(req.getAvatar());
        singer.setGender(req.getGender());
        singer.setRegion(req.getRegion());
        singer.setIntro(req.getIntro());
        if (req.getStatus() != null) {
            singer.setStatus(req.getStatus());
        }
    }
}
