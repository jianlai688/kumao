package com.kumao.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.music.dto.LyricLine;
import com.kumao.music.dto.SongRequest;
import com.kumao.music.entity.Mv;
import com.kumao.music.entity.Song;
import com.kumao.music.entity.SongPurchase;
import com.kumao.music.mapper.SongPurchaseMapper;
import com.kumao.music.service.LyricService;
import com.kumao.music.service.MvService;
import com.kumao.music.service.RankService;
import com.kumao.music.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final LyricService lyricService;
    private final RankService rankService;
    private final MvService mvService;
    private final SongPurchaseMapper songPurchaseMapper;

    @GetMapping
    public R<PageResult<Song>> page(PageQuery query,
                                    @RequestParam(required = false) String region,
                                    @RequestParam(required = false) String language,
                                    @RequestParam(required = false) String style,
                                    @RequestParam(required = false) Long singerId,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Long musicianId) {
        return R.ok(songService.page(query, region, language, style, singerId, status, musicianId));
    }

    @GetMapping("/{id}")
    public R<Song> detail(@PathVariable Long id) {
        return R.ok(songService.detail(id));
    }

    /**
     * 单曲购买状态：价格（分）与当前用户是否已购买。未登录时 owned 恒为 false。
     */
    @GetMapping("/{id}/purchase-status")
    public R<Map<String, Object>> purchaseStatus(@PathVariable Long id) {
        Song song = songService.detail(id);
        Long userId = UserContext.getUserId();
        boolean owned = userId != null && songPurchaseMapper.selectCount(
                new LambdaQueryWrapper<SongPurchase>()
                        .eq(SongPurchase::getUserId, userId)
                        .eq(SongPurchase::getSongId, id)
                        .eq(SongPurchase::getStatus, "PAID")) > 0;
        return R.ok(Map.of(
                "price", song.getPrice() == null ? 0 : song.getPrice(),
                "owned", owned,
                "vipOnly", Boolean.TRUE.equals(song.getVipOnly())
        ));
    }

    /**
     * 购买单曲（演示环境 MOCK 支付：创建订单即视为支付成功并解锁）
     */
    @PostMapping("/{id}/purchase")
    public R<SongPurchase> purchase(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        Song song = songService.detail(id);
        int price = song.getPrice() == null ? 0 : song.getPrice();
        if (price <= 0) {
            throw new BizException("该歌曲是免费歌曲，无需购买");
        }
        SongPurchase exists = songPurchaseMapper.selectOne(new LambdaQueryWrapper<SongPurchase>()
                .eq(SongPurchase::getUserId, userId)
                .eq(SongPurchase::getSongId, id)
                .eq(SongPurchase::getStatus, "PAID")
                .last("limit 1"));
        if (exists != null) {
            return R.ok(exists);
        }
        SongPurchase purchase = new SongPurchase();
        purchase.setUserId(userId);
        purchase.setSongId(id);
        purchase.setAmount(price);
        purchase.setStatus("PAID");
        purchase.setOrderNo("SP" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        songPurchaseMapper.insert(purchase);
        return R.ok(purchase);
    }

    @GetMapping("/{id}/lyric")
    public R<List<LyricLine>> lyric(@PathVariable Long id) {
        return R.ok(lyricService.lines(id));
    }

    /** 歌曲关联的 MV（用于播放页切换 MV），无 MV 时 data 为 null */
    @GetMapping("/{id}/mv")
    public R<Mv> mv(@PathVariable Long id) {
        return R.ok(mvService.bySongId(id));
    }

    /** 前端歌词滚动所需：原始 LRC 文本 */
    @GetMapping("/{id}/lyric/raw")
    public R<String> rawLyric(@PathVariable Long id) {
        return R.ok(lyricService.raw(id));
    }

    // 地区分类：国内 / 国外 / 粤语
    @GetMapping("/regions")
    public R<List<String>> regions() {
        return R.ok(List.of("国内", "国外", "粤语"));
    }

    @PostMapping("/admin")
    public R<Long> create(@RequestBody @Valid SongRequest req) {
        UserContext.requireAdmin();
        Long id = songService.create(req);
        rankService.clearCache();
        return R.ok(id);
    }

    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid SongRequest req) {
        UserContext.requireAdmin();
        songService.update(id, req);
        rankService.clearCache();
        return R.ok();
    }

    @PutMapping("/admin/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        UserContext.requireAdmin();
        songService.updateStatus(id, status);
        rankService.clearCache();
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        songService.delete(id);
        rankService.clearCache();
        return R.ok();
    }
}
