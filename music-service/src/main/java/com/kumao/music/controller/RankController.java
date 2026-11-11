package com.kumao.music.controller;

import com.kumao.common.result.R;
import com.kumao.music.dto.RankItemVO;
import com.kumao.music.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/hot")
    public R<List<RankItemVO>> hot(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(rankService.hot(limit));
    }

    @GetMapping("/new")
    public R<List<RankItemVO>> newSongs(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(rankService.newSongs(limit));
    }

    @GetMapping("/rising")
    public R<List<RankItemVO>> rising(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(rankService.rising(limit));
    }
}
