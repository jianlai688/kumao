package com.kumao.musician.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicClient;
import com.kumao.api.client.SearchClient;
import com.kumao.api.client.UserClient;
import com.kumao.api.dto.SearchDocDTO;
import com.kumao.api.dto.SongDTO;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.musician.dto.AlbumRequest;
import com.kumao.musician.dto.MusicianApplyRequest;
import com.kumao.musician.dto.WorkRequest;
import com.kumao.musician.entity.*;
import com.kumao.musician.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicianService {

    private final MusicianMapper musicianMapper;
    private final MusicianWorkMapper workMapper;
    private final MusicianAlbumMapper albumMapper;
    private final MusicianFansMapper fansMapper;
    private final MusicianIncomeMapper incomeMapper;
    private final MusicianWithdrawMapper withdrawMapper;
    private final MusicianPlayStatMapper playStatMapper;
    private final MusicClient musicClient;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final com.kumao.musician.websocket.NewSongWebSocketHandler newSongNotifier;

    // ---------------- 入驻认证 ----------------

    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, MusicianApplyRequest req) {
        Musician exist = musicianMapper.selectOne(new LambdaQueryWrapper<Musician>()
                .eq(Musician::getUserId, userId));
        if (exist != null && ("PENDING".equals(exist.getStatus()) || "APPROVED".equals(exist.getStatus()))) {
            throw new BizException("已提交过音乐人认证，请勿重复提交");
        }
        Musician musician = exist == null ? new Musician() : exist;
        musician.setUserId(userId);
        musician.setStageName(req.getStageName());
        musician.setRealName(req.getRealName());
        musician.setIdCard(req.getIdCard());
        musician.setIdCardFrontUrl(req.getIdCardFrontUrl());
        musician.setIdCardBackUrl(req.getIdCardBackUrl());
        musician.setStyle(req.getStyle());
        musician.setIntro(req.getIntro());
        musician.setAvatar(req.getAvatar());
        musician.setRepresentative(req.getRepresentative());
        musician.setStatus("PENDING");
        musician.setRejectReason(null);
        if (musician.getId() == null) {
            musician.setFansCount(0L);
            musician.setTotalPlayCount(0L);
            musician.setTotalLikeCount(0L);
            musicianMapper.insert(musician);
        } else {
            musicianMapper.updateById(musician);
        }
        return musician.getId();
    }

    public Musician mine(Long userId) {
        return musicianMapper.selectOne(new LambdaQueryWrapper<Musician>().eq(Musician::getUserId, userId));
    }

    public Musician requireApproved(Long userId) {
        Musician musician = mine(userId);
        if (musician == null || !"APPROVED".equals(musician.getStatus())) {
            throw new BizException(403, "仅认证通过的音乐人可访问工作台");
        }
        return musician;
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long musicianId, boolean approved, String reason, Long adminId) {
        Musician musician = musicianMapper.selectById(musicianId);
        if (musician == null) {
            throw new BizException("音乐人申请不存在");
        }
        if (approved && (!StringUtils.hasText(musician.getIdCard())
                || !StringUtils.hasText(musician.getIdCardFrontUrl())
                || !StringUtils.hasText(musician.getIdCardBackUrl()))) {
            throw new BizException("请先核验身份证号及正反面照片，再通过审核");
        }
        musician.setStatus(approved ? "APPROVED" : "REJECTED");
        musician.setRejectReason(approved ? null : reason);
        musician.setAuditBy(adminId);
        musician.setAuditTime(LocalDateTime.now().toString());
        musicianMapper.updateById(musician);
        if (approved) {
            try {
                userClient.grantRole(musician.getUserId(), "MUSICIAN");
            } catch (Exception e) {
                log.warn("授予音乐人角色失败: {}", e.getMessage());
            }
            syncSearch(musician);
        }
    }

    /**
     * 取消资格：下架全部作品
     */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long musicianId, String reason) {
        Musician musician = musicianMapper.selectById(musicianId);
        if (musician == null) {
            throw new BizException("音乐人不存在");
        }
        musician.setStatus("REVOKED");
        musician.setRejectReason(reason);
        musicianMapper.updateById(musician);
        List<MusicianWork> works = workMapper.selectList(new LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getMusicianId, musicianId));
        for (MusicianWork work : works) {
            work.setStatus("OFFLINE");
            workMapper.updateById(work);
            if (work.getSongId() != null) {
                try {
                    musicClient.deleteSong(work.getSongId());
                } catch (Exception e) {
                    log.warn("下架歌曲失败: {}", e.getMessage());
                }
            }
        }
        try {
            userClient.revokeRole(musician.getUserId(), "MUSICIAN");
        } catch (Exception e) {
            log.warn("撤销音乐人角色失败: {}", e.getMessage());
        }
        try {
            searchClient.delete("MUSICIAN", musicianId);
        } catch (Exception e) {
            log.debug("删除音乐人索引失败: {}", e.getMessage());
        }
    }

    public PageResult<Musician> adminPage(PageQuery query, String status) {
        Page<Musician> page = musicianMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Musician>()
                        .eq(StringUtils.hasText(status), Musician::getStatus, status)
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(Musician::getStageName, query.getKeyword())
                                .or().like(Musician::getRealName, query.getKeyword()))
                        .orderByDesc(Musician::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 音乐人主页 ----------------

    public Map<String, Object> homepage(Long musicianId, Long viewerId) {
        Musician musician = musicianMapper.selectById(musicianId);
        if (musician == null) {
            throw new BizException("音乐人不存在");
        }
        maskIdentity(musician);
        List<MusicianWork> works = workMapper.selectList(new LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getMusicianId, musicianId)
                .eq(MusicianWork::getStatus, "APPROVED")
                .orderByDesc(MusicianWork::getCreateTime));
        List<MusicianAlbum> albums = albumMapper.selectList(new LambdaQueryWrapper<MusicianAlbum>()
                .eq(MusicianAlbum::getMusicianId, musicianId));
        boolean followed = viewerId != null && fansMapper.selectCount(new LambdaQueryWrapper<MusicianFans>()
                .eq(MusicianFans::getMusicianId, musicianId).eq(MusicianFans::getUserId, viewerId)) > 0;
        return Map.of("musician", musician, "works", works, "albums", albums, "followed", followed);
    }

    public PageResult<Musician> pageMusicians(PageQuery query, String style) {
        Page<Musician> page = musicianMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<Musician>()
                        .eq(Musician::getStatus, "APPROVED")
                        .eq(StringUtils.hasText(style), Musician::getStyle, style)
                        .like(StringUtils.hasText(query.getKeyword()), Musician::getStageName, query.getKeyword())
                        .orderByDesc(Musician::getFansCount));
        page.getRecords().forEach(this::maskIdentity);
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void follow(Long musicianId, Long userId) {
        Musician musician = musicianMapper.selectById(musicianId);
        if (musician == null) {
            throw new BizException("音乐人不存在");
        }
        Long exist = fansMapper.selectCount(new LambdaQueryWrapper<MusicianFans>()
                .eq(MusicianFans::getMusicianId, musicianId).eq(MusicianFans::getUserId, userId));
        if (exist != null && exist > 0) {
            throw new BizException("已关注该音乐人");
        }
        MusicianFans fans = new MusicianFans();
        fans.setMusicianId(musicianId);
        fans.setUserId(userId);
        fansMapper.insert(fans);
        musician.setFansCount((musician.getFansCount() == null ? 0 : musician.getFansCount()) + 1);
        musicianMapper.updateById(musician);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long musicianId, Long userId) {
        int removed = fansMapper.delete(new LambdaQueryWrapper<MusicianFans>()
                .eq(MusicianFans::getMusicianId, musicianId).eq(MusicianFans::getUserId, userId));
        if (removed > 0) {
            Musician musician = musicianMapper.selectById(musicianId);
            if (musician != null) {
                musician.setFansCount(Math.max(0, (musician.getFansCount() == null ? 0 : musician.getFansCount()) - 1));
                musicianMapper.updateById(musician);
            }
        }
    }

    // ---------------- 作品管理 ----------------

    @Transactional(rollbackFor = Exception.class)
    public Long createWork(Long userId, WorkRequest req) {
        Musician musician = requireApproved(userId);
        MusicianWork work = new MusicianWork();
        work.setMusicianId(musician.getId());
        work.setUserId(userId);
        applyWork(work, req);
        work.setStatus("PENDING");
        work.setPlayCount(0L);
        work.setLikeCount(0L);
        work.setCommentCount(0L);
        work.setShareCount(0L);
        work.setCollectCount(0L);
        workMapper.insert(work);
        return work.getId();
    }

    public void updateWork(Long workId, Long userId, WorkRequest req) {
        MusicianWork work = requireOwnWork(workId, userId);
        applyWork(work, req);
        if ("APPROVED".equals(work.getStatus())) {
            // 已上架作品修改后重新审核
            work.setStatus("PENDING");
        }
        workMapper.updateById(work);
    }

    public void offlineWork(Long workId, Long userId) {
        MusicianWork work = requireOwnWork(workId, userId);
        work.setStatus("OFFLINE");
        workMapper.updateById(work);
    }

    public void deleteWork(Long workId, Long userId) {
        MusicianWork work = requireOwnWork(workId, userId);
        workMapper.deleteById(workId);
        if (work.getSongId() != null) {
            try {
                musicClient.deleteSong(work.getSongId());
            } catch (Exception e) {
                log.warn("删除同步歌曲失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 管理员审核作品：通过后自动同步到 music-service 歌曲库
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditWork(Long workId, boolean approved, String reason) {
        MusicianWork work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException("作品不存在");
        }
        if (approved) {
            work.setStatus("APPROVED");
            work.setRejectReason(null);
            workMapper.updateById(work);
            if (work.getSongId() == null) {
                try {
                    Musician musician = musicianMapper.selectById(work.getMusicianId());
                    SongDTO song = new SongDTO();
                    song.setName(work.getName());
                    song.setSingerName(musician.getStageName());
                    song.setAlbumName(work.getAlbumName());
                    song.setCoverUrl(work.getCoverUrl());
                    song.setAudioUrl(work.getAudioUrl());
                    song.setLyricUrl(work.getLyricUrl());
                    song.setDuration(work.getDuration());
                    song.setLanguage(work.getLanguage());
                    song.setStyle(work.getStyle());
                    song.setRegion("国内");
                    song.setVipOnly(Boolean.TRUE.equals(work.getVipOnly()));
                    song.setPrice(work.getPrice() == null ? 0 : work.getPrice());
                    song.setStatus(1);
                    song.setReleaseDate(work.getReleaseDate() == null ? null : work.getReleaseDate().toString());
                    song.setMusicianId(work.getMusicianId());
                    var r = musicClient.createSong(song);
                    if (r != null && r.getData() != null) {
                        work.setSongId(r.getData());
                        workMapper.updateById(work);
                    }
                } catch (Exception e) {
                    log.error("同步歌曲到 music-service 失败: {}", e.getMessage());
                    throw new BizException("同步歌曲库失败，请稍后重试");
                }
                // 新歌发布通知（WebSocket 推送给关注该音乐人的在线用户），失败不影响审核结果
                try {
                    newSongNotifier.notifyNewSong(work.getMusicianId(), work.getName());
                } catch (Exception e) {
                    log.warn("新歌通知推送失败: {}", e.getMessage());
                }
            }
        } else {
            work.setStatus("REJECTED");
            work.setRejectReason(reason);
            workMapper.updateById(work);
        }
    }

    public PageResult<MusicianWork> works(Long userId, PageQuery query, String status) {
        Musician musician = requireApproved(userId);
        Page<MusicianWork> page = workMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MusicianWork>()
                        .eq(MusicianWork::getMusicianId, musician.getId())
                        .eq(StringUtils.hasText(status), MusicianWork::getStatus, status)
                        .orderByDesc(MusicianWork::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<MusicianWork> adminWorks(PageQuery query, String status) {
        Page<MusicianWork> page = workMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MusicianWork>()
                        .eq(StringUtils.hasText(status), MusicianWork::getStatus, status)
                        .like(StringUtils.hasText(query.getKeyword()), MusicianWork::getName, query.getKeyword())
                        .orderByDesc(MusicianWork::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 专辑 ----------------

    public Long createAlbum(Long userId, AlbumRequest req) {
        Musician musician = requireApproved(userId);
        MusicianAlbum album = new MusicianAlbum();
        album.setMusicianId(musician.getId());
        album.setUserId(userId);
        album.setName(req.getName());
        album.setCoverUrl(req.getCoverUrl());
        album.setIntro(req.getIntro());
        album.setReleaseDate(req.getReleaseDate());
        album.setSongCount(0);
        album.setStatus(1);
        albumMapper.insert(album);
        return album.getId();
    }

    public void updateAlbum(Long albumId, Long userId, AlbumRequest req) {
        MusicianAlbum album = requireOwnAlbum(albumId, userId);
        album.setName(req.getName());
        album.setCoverUrl(req.getCoverUrl());
        album.setIntro(req.getIntro());
        album.setReleaseDate(req.getReleaseDate());
        albumMapper.updateById(album);
    }

    /** 重新上架作品：已下架作品重新提交审核 */
    @Transactional(rollbackFor = Exception.class)
    public void onlineWork(Long workId, Long userId) {
        MusicianWork work = requireOwnWork(workId, userId);
        if (!"OFFLINE".equals(work.getStatus())) {
            throw new BizException("只有已下架作品可以重新上架");
        }
        work.setStatus("PENDING");
        work.setRejectReason(null);
        workMapper.updateById(work);
    }

    /** 上架 / 下架专辑 */
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumStatus(Long albumId, Long userId, Integer status) {
        MusicianAlbum album = requireOwnAlbum(albumId, userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不合法");
        }
        album.setStatus(status);
        albumMapper.updateById(album);
    }
    public void deleteAlbum(Long albumId, Long userId) {
        requireOwnAlbum(albumId, userId);
        albumMapper.deleteById(albumId);
    }

    /**
     * 音乐人修改个人信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMyProfile(Long userId, com.kumao.musician.dto.MusicianProfileRequest req) {
        Musician musician = requireApproved(userId);
        if (req.getStageName() != null) {
            if (req.getStageName().isBlank()) {
                throw new BizException("艺名不能为空");
            }
            musician.setStageName(req.getStageName().trim());
        }
        if (req.getAvatar() != null) {
            musician.setAvatar(req.getAvatar());
        }
        if (req.getStyle() != null) {
            musician.setStyle(req.getStyle());
        }
        if (req.getIntro() != null) {
            musician.setIntro(req.getIntro());
        }
        if (req.getRepresentative() != null) {
            musician.setRepresentative(req.getRepresentative());
        }
        musicianMapper.updateById(musician);
    }
    public List<MusicianAlbum> albums(Long userId) {
        Musician musician = requireApproved(userId);
        return albumMapper.selectList(new LambdaQueryWrapper<MusicianAlbum>()
                .eq(MusicianAlbum::getMusicianId, musician.getId()).orderByDesc(MusicianAlbum::getCreateTime));
    }

    // ---------------- 数据统计 ----------------

    public Map<String, Object> statistics(Long userId, String period) {
        Musician musician = requireApproved(userId);
        List<MusicianWork> works = workMapper.selectList(new LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getMusicianId, musician.getId()));
        long totalPlay = works.stream().mapToLong(w -> w.getPlayCount() == null ? 0 : w.getPlayCount()).sum();
        long totalLike = works.stream().mapToLong(w -> w.getLikeCount() == null ? 0 : w.getLikeCount()).sum();
        long totalComment = works.stream().mapToLong(w -> w.getCommentCount() == null ? 0 : w.getCommentCount()).sum();
        long totalShare = works.stream().mapToLong(w -> w.getShareCount() == null ? 0 : w.getShareCount()).sum();
        long fans = fansMapper.selectCount(new LambdaQueryWrapper<MusicianFans>()
                .eq(MusicianFans::getMusicianId, musician.getId()));
        return Map.of("totalPlay", totalPlay, "totalLike", totalLike, "totalComment", totalComment,
                "totalShare", totalShare, "fans", fans, "works", works.size(), "period", period == null ? "day" : period);
    }

    /**
     * 粉丝地域分布（按粉丝记录 region 聚合，未填写归为“未知”）
     */
    public List<Map<String, Object>> fansRegions(Long userId) {
        Musician musician = requireApproved(userId);
        List<MusicianFans> fans = fansMapper.selectList(new LambdaQueryWrapper<MusicianFans>()
                .eq(MusicianFans::getMusicianId, musician.getId()));
        Map<String, Long> grouped = new java.util.LinkedHashMap<>();
        for (MusicianFans f : fans) {
            String region = StringUtils.hasText(f.getRegion()) ? f.getRegion() : "未知";
            grouped.merge(region, 1L, Long::sum);
        }
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        grouped.forEach((name, value) -> list.add(Map.of("name", name, "value", value)));
        list.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        return list;
    }

    public List<MusicianWork> rankByPlay(Long userId) {
        Musician musician = requireApproved(userId);
        return workMapper.selectList(new LambdaQueryWrapper<MusicianWork>()
                .eq(MusicianWork::getMusicianId, musician.getId())
                .orderByDesc(MusicianWork::getPlayCount)
                .last("limit 10"));
    }

    /**
     * 播放量趋势：日报 / 周报 / 月报
     */
    public List<MusicianPlayStat> trend(Long userId, String period) {
        Musician musician = requireApproved(userId);
        int days = switch (period == null ? "day" : period) {
            case "week" -> 7;
            case "month" -> 30;
            default -> 1;
        };
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return playStatMapper.selectList(new LambdaQueryWrapper<MusicianPlayStat>()
                .eq(MusicianPlayStat::getMusicianId, musician.getId())
                .ge(MusicianPlayStat::getStatDate, from)
                .orderByAsc(MusicianPlayStat::getStatDate));
    }

    // ---------------- 收益 ----------------

    public Map<String, Object> incomeSummary(Long userId) {
        Musician musician = requireApproved(userId);
        List<MusicianIncome> incomes = incomeMapper.selectList(new LambdaQueryWrapper<MusicianIncome>()
                .eq(MusicianIncome::getMusicianId, musician.getId()));
        long total = incomes.stream().mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount()).sum();
        long withdrawn = incomes.stream()
                .filter(i -> "WITHDRAW".equals(i.getType()))
                .mapToLong(i -> Math.abs(i.getAmount() == null ? 0 : i.getAmount())).sum();
        return Map.of("totalIncome", total, "withdrawn", withdrawn, "balance", total - withdrawn,
                "musicianId", musician.getId());
    }

    public PageResult<MusicianIncome> incomes(Long userId, PageQuery query) {
        Musician musician = requireApproved(userId);
        Page<MusicianIncome> page = incomeMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MusicianIncome>()
                        .eq(MusicianIncome::getMusicianId, musician.getId())
                        .orderByDesc(MusicianIncome::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<MusicianWithdraw> withdraws(Long userId, PageQuery query) {
        Musician musician = requireApproved(userId);
        Page<MusicianWithdraw> page = withdrawMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MusicianWithdraw>()
                        .eq(MusicianWithdraw::getMusicianId, musician.getId())
                        .orderByDesc(MusicianWithdraw::getCreateTime));
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long withdraw(Long userId, int amount, String account, String payType) {
        Musician musician = requireApproved(userId);
        String normalizedPayType = normalizeWithdrawType(payType);
        String normalizedAccount = normalizeWithdrawAccount(account);
        Map<String, Object> summary = incomeSummary(userId);
        long balance = ((Number) summary.get("balance")).longValue();
        if (amount <= 0) {
            throw new BizException("提现金额必须大于0");
        }
        if (amount > balance) {
            throw new BizException("可提现余额不足");
        }
        MusicianWithdraw withdraw = new MusicianWithdraw();
        withdraw.setMusicianId(musician.getId());
        withdraw.setUserId(userId);
        withdraw.setAmount(amount);
        withdraw.setAccount(normalizedAccount);
        withdraw.setPayType(normalizedPayType);
        withdraw.setStatus("PENDING");
        withdrawMapper.insert(withdraw);
        return withdraw.getId();
    }

    private String normalizeWithdrawType(String payType) {
        if ("ALIPAY".equalsIgnoreCase(payType)) return "ALIPAY";
        if ("WECHAT".equalsIgnoreCase(payType)) return "WECHAT";
        throw new BizException("提现方式仅支持支付宝或微信");
    }

    private String normalizeWithdrawAccount(String account) {
        String value = account == null ? "" : account.trim();
        if (value.isEmpty()) throw new BizException("请填写收款账号");
        if (value.length() > 80) throw new BizException("收款账号过长");
        return value;
    }

    public void auditWithdraw(Long withdrawId, boolean approved, String remark) {
        MusicianWithdraw withdraw = withdrawMapper.selectById(withdrawId);
        if (withdraw == null) {
            throw new BizException("提现申请不存在");
        }
        if (!"PENDING".equals(withdraw.getStatus())) {
            throw new BizException("该申请已处理");
        }
        withdraw.setStatus(approved ? "APPROVED" : "REJECTED");
        withdraw.setRemark(remark);
        withdraw.setAuditTime(LocalDateTime.now().toString());
        withdrawMapper.updateById(withdraw);
        if (approved) {
            MusicianIncome income = new MusicianIncome();
            income.setMusicianId(withdraw.getMusicianId());
            income.setUserId(withdraw.getUserId());
            income.setType("WITHDRAW");
            income.setAmount(-withdraw.getAmount());
            income.setDescription("提现");
            income.setStatus("SUCCESS");
            income.setBizId(withdrawId);
            incomeMapper.insert(income);
        }
    }

    public Map<String, Object> adminIncomeSummary() {
        List<MusicianIncome> all = incomeMapper.selectList(null);
        List<MusicianWithdraw> withdraws = withdrawMapper.selectList(null);
        long total = all.stream().mapToLong(i -> i.getAmount() == null ? 0 : i.getAmount()).sum();
        long pending = withdraws.stream().filter(w -> "PENDING".equals(w.getStatus())).count();
        return Map.of("totalIncome", total, "pendingWithdraw", pending,
                "musicianCount", musicianMapper.selectCount(null));
    }

    public PageResult<MusicianWithdraw> adminWithdraws(PageQuery query, String status) {
        Page<MusicianWithdraw> page = withdrawMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MusicianWithdraw>()
                        .eq(StringUtils.hasText(status), MusicianWithdraw::getStatus, status)
                        .orderByDesc(MusicianWithdraw::getCreateTime));
        return PageResult.of(page);
    }

    // ---------------- 辅助 ----------------

    private void applyWork(MusicianWork work, WorkRequest req) {
        work.setName(req.getName());
        work.setAlbumId(req.getAlbumId());
        work.setAlbumName(req.getAlbumName());
        work.setCoverUrl(req.getCoverUrl());
        work.setAudioUrl(req.getAudioUrl());
        work.setLyricUrl(req.getLyricUrl());
        work.setDuration(req.getDuration());
        work.setLanguage(req.getLanguage());
        work.setStyle(req.getStyle());
        work.setVipOnly(Boolean.TRUE.equals(req.getVipOnly()));
        work.setPrice(req.getPrice() == null ? 0 : Math.max(0, req.getPrice()));
        work.setReleaseDate(req.getReleaseDate());
    }

    private MusicianWork requireOwnWork(Long workId, Long userId) {
        MusicianWork work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException("作品不存在");
        }
        if (!work.getUserId().equals(userId)) {
            throw new BizException(403, "只能管理自己的作品");
        }
        return work;
    }

    private MusicianAlbum requireOwnAlbum(Long albumId, Long userId) {
        MusicianAlbum album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new BizException("专辑不存在");
        }
        if (!album.getUserId().equals(userId)) {
            throw new BizException(403, "只能管理自己的专辑");
        }
        return album;
    }

    /** 公开主页 / 列表不返回身份证号与证件照片，仅本人和管理端可见。 */
    private void maskIdentity(Musician musician) {
        if (musician == null) return;
        musician.setIdCard(null);
        musician.setIdCardFrontUrl(null);
        musician.setIdCardBackUrl(null);
    }

    private void syncSearch(Musician musician) {
        try {
            SearchDocDTO doc = new SearchDocDTO();
            doc.setBizId(musician.getId());
            doc.setType("MUSICIAN");
            doc.setTitle(musician.getStageName());
            doc.setSubtitle(musician.getStyle());
            doc.setCoverUrl(musician.getAvatar());
            doc.setTags(musician.getStyle());
            doc.setDescription(musician.getIntro());
            doc.setHeat(musician.getFansCount());
            searchClient.sync(doc);
        } catch (Exception e) {
            log.debug("音乐人索引同步失败: {}", e.getMessage());
        }
    }
}


