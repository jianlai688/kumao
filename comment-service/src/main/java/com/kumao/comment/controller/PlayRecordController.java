package com.kumao.comment.controller;

import com.kumao.comment.dto.PlayRecordMessage;
import com.kumao.comment.entity.PlayRecord;
import com.kumao.comment.service.PlayRecordService;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/play-records")
@RequiredArgsConstructor
public class PlayRecordController {

    private final PlayRecordService playRecordService;

    /** 前端播放歌曲时投递 MQ，异步落库 */
    @PostMapping
    public R<Void> submit(@RequestBody PlayRecordMessage message) {
        message.setUserId(UserContext.requireUserId());
        if (message.getDevice() == null) {
            var user = UserContext.get();
            message.setDevice(user == null ? null : user.getDevice());
        }
        playRecordService.submit(message);
        return R.ok();
    }

    @GetMapping("/mine")
    public R<PageResult<PlayRecord>> mine(PageQuery query,
                                          @RequestParam(required = false) String device) {
        return R.ok(playRecordService.myRecords(UserContext.requireUserId(), query, device));
    }

    @GetMapping("/recent")
    public R<List<PlayRecord>> recent(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(playRecordService.recent(UserContext.requireUserId(), limit));
    }

    @DeleteMapping("/mine")
    public R<Void> clear() {
        playRecordService.clear(UserContext.requireUserId());
        return R.ok();
    }
}
