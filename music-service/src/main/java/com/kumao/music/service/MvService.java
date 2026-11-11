package com.kumao.music.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicianClient;
import com.kumao.api.client.VipClient;
import com.kumao.api.dto.MusicianDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.music.dto.MvPublishRequest;
import com.kumao.music.dto.MvRequest;
import com.kumao.music.entity.Mv;
import com.kumao.music.entity.Singer;
import com.kumao.music.mapper.MvMapper;
import com.kumao.music.mapper.SingerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MvService {

    private final MvMapper mvMapper;
    private final SingerMapper singerMapper;
    private final VipClient vipClient;
    private final MusicianClient musicianClient;

    public PageResult<Mv> page(PageQuery query, Long singerId, Integer status) {
        Page<Mv> page = mvMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Mv>()
                        .eq(Mv::getStatus, 1)
                        .eq(Mv::getAuditStatus, "APPROVED")
                        .eq(singerId != null, Mv::getSingerId, singerId)
                        .like(StringUtils.hasText(query.getKeyword()), Mv::getName, query.getKeyword())
                        .orderByDesc(Mv::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<Mv> myPage(Long userId, PageQuery query) {
        Page<Mv> page = mvMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Mv>()
                        .eq(Mv::getUploaderUserId, userId)
                        .orderByDesc(Mv::getCreateTime));
        return PageResult.of(page);
    }

    /**
     * 音乐人或认证歌手提交 MV，进入管理员审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, MvPublishRequest req) {
        Singer singer = singerMapper.selectOne(new LambdaQueryWrapper<Singer>()
                .eq(Singer::getUserId, userId)
                .eq(Singer::getStatus, 1)
                .last("limit 1"));
        MusicianDTO musician = null;
        try {
            R<MusicianDTO> result = musicianClient.byUser(userId);
            if (result != null && result.getCode() == 200) musician = result.getData();
        } catch (Exception e) {
            log.warn("查询音乐人身份失败，按歌手身份判断: {}", e.getMessage());
        }
        boolean approvedMusician = musician != null && "APPROVED".equals(musician.getStatus());
        if (!approvedMusician && singer == null) {
            throw new BizException("只有歌手或已认证音乐人可以发布 MV");
        }

        Mv mv = new Mv();
        apply(mv, req);
        if (singer != null) {
            mv.setSingerId(singer.getId());
            mv.setSingerName(singer.getName());
            mv.setMusicianId(singer.getMusicianId() != null ? singer.getMusicianId() : (musician == null ? null : musician.getId()));
        } else {
            mv.setSingerId(null);
            mv.setSingerName(musician.getStageName());
            mv.setMusicianId(musician.getId());
        }
        mv.setUploaderUserId(userId);
        mv.setAuditStatus("PENDING");
        mv.setStatus(0);
        mv.setPlayCount(0L);
        mv.setRejectReason(null);
        mv.setAuditTime(null);
        mvMapper.insert(mv);
        return mv.getId();
    }

    public PageResult<Mv> adminPage(PageQuery query, Integer status, String auditStatus) {
        Page<Mv> page = mvMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Mv>()
                        .eq(status != null, Mv::getStatus, status)
                        .eq(StringUtils.hasText(auditStatus), Mv::getAuditStatus, auditStatus)
                        .like(StringUtils.hasText(query.getKeyword()), Mv::getName, query.getKeyword())
                        .orderByAsc(Mv::getAuditStatus)
                        .orderByDesc(Mv::getCreateTime));
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean approved, String reason) {
        Mv mv = detail(id);
        if (!"PENDING".equals(mv.getAuditStatus())) {
            throw new BizException("该 MV 申请已处理");
        }
        mv.setAuditStatus(approved ? "APPROVED" : "REJECTED");
        mv.setStatus(approved ? 1 : 0);
        mv.setRejectReason(approved ? null : reason);
        mv.setAuditTime(LocalDateTime.now().toString());
        mvMapper.updateById(mv);
    }

    public Mv detail(Long id) {
        Mv mv = mvMapper.selectById(id);
        if (mv == null) {
            throw new BizException("MV 不存在");
        }
        return mv;
    }

    /**
     * 按歌曲查询已上架 MV，没有则返回 null
     */
    public Mv bySongId(Long songId) {
        return mvMapper.selectOne(new LambdaQueryWrapper<Mv>()
                .eq(Mv::getSongId, songId)
                .eq(Mv::getStatus, 1)
                .eq(Mv::getAuditStatus, "APPROVED")
                .last("limit 1"));
    }

    /**
     * 播放鉴权：高清 VIP 专属 MV 校验 vip-service
     */
    public Mv play(Long id, Long userId) {
        Mv mv = detail(id);
        if (mv.getStatus() == null || mv.getStatus() != 1 || !"APPROVED".equals(mv.getAuditStatus())) {
            throw new BizException("MV 已下架");
        }
        if (Boolean.TRUE.equals(mv.getVipOnly())) {
            if (userId == null) {
                throw new BizException(403, "该高清 MV 仅 VIP 会员可观看，请先登录");
            }
            try {
                R<Boolean> r = vipClient.hasPermission(userId, "MV_HD");
                if (r == null || !Boolean.TRUE.equals(r.getData())) {
                    throw new BizException(403, "该高清 MV 仅 VIP 会员可观看，请开通会员");
                }
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("VIP 校验服务不可用，按非会员处理: {}", e.getMessage());
                throw new BizException(503, "会员服务暂不可用，请稍后再试");
            }
        }
        mv.setPlayCount(mv.getPlayCount() == null ? 1L : mv.getPlayCount() + 1);
        mvMapper.updateById(mv);
        return mv;
    }

    public Long create(MvRequest req) {
        Mv mv = new Mv();
        apply(mv, req);
        mv.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        mv.setAuditStatus("APPROVED");
        mv.setPlayCount(0L);
        mvMapper.insert(mv);
        return mv.getId();
    }

    /** 音乐人/歌手：编辑自己的 MV（修改后需重新审核） */
    @Transactional(rollbackFor = Exception.class)
    public void updateOwn(Long userId, Long id, MvPublishRequest req) {
        Mv mv = requireOwn(userId, id);
        apply(mv, req);
        if ("APPROVED".equals(mv.getAuditStatus())) {
            mv.setAuditStatus("PENDING");
        }
        mvMapper.updateById(mv);
    }

    /** 音乐人/歌手：上架 / 下架自己的 MV，status=1 上架，0 下架 */
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnStatus(Long userId, Long id, Integer status) {
        Mv mv = requireOwn(userId, id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不合法");
        }
        mv.setStatus(status);
        mvMapper.updateById(mv);
    }

    /** 删除自己的 MV */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(Long userId, Long id) {
        requireOwn(userId, id);
        mvMapper.deleteById(id);
    }

    private Mv requireOwn(Long userId, Long id) {
        Mv mv = detail(id);
        if (mv.getUploaderUserId() == null || !mv.getUploaderUserId().equals(userId)) {
            throw new BizException("只能管理自己发布的作品");
        }
        return mv;
    }
    public void update(Long id, MvRequest req) {
        Mv mv = detail(id);
        apply(mv, req);
        mvMapper.updateById(mv);
    }

    public void updateStatus(Long id, Integer status) {
        Mv mv = detail(id);
        mv.setStatus(status);
        mvMapper.updateById(mv);
    }

    public void delete(Long id) {
        mvMapper.deleteById(id);
    }

    private void apply(Mv mv, MvPublishRequest req) {
        mv.setSongId(req.getSongId());
        mv.setName(req.getName());
        mv.setCoverUrl(req.getCoverUrl());
        mv.setVideoUrl(req.getVideoUrl());
        mv.setDuration(req.getDuration() == null ? 0 : req.getDuration());
        mv.setQuality(req.getQuality() == null ? "HD" : req.getQuality());
        mv.setVipOnly(Boolean.TRUE.equals(req.getVipOnly()));
        mv.setDescription(req.getDescription());
    }

    private void apply(Mv mv, MvRequest req) {
        mv.setSongId(req.getSongId());
        mv.setName(req.getName());
        mv.setSingerId(req.getSingerId());
        mv.setSingerName(req.getSingerName());
        mv.setCoverUrl(req.getCoverUrl());
        mv.setVideoUrl(req.getVideoUrl());
        mv.setDuration(req.getDuration());
        mv.setQuality(req.getQuality() == null ? "HD" : req.getQuality());
        mv.setVipOnly(Boolean.TRUE.equals(req.getVipOnly()));
        mv.setStatus(req.getStatus());
        mv.setDescription(req.getDescription());
    }
}

