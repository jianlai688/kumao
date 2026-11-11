package com.kumao.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.api.client.MusicClient;
import com.kumao.api.dto.SongDTO;
import com.kumao.comment.dto.PlayRecordMessage;
import com.kumao.comment.entity.PlayRecord;
import com.kumao.comment.mapper.PlayRecordMapper;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayRecordService {

    private final PlayRecordMapper playRecordMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MusicClient musicClient;

    @Value("${kumao.mq.play-record-exchange:kumao.play.exchange}")
    private String exchange;
    @Value("${kumao.mq.play-record-routing-key:play.record}")
    private String routingKey;

    /** 异步投递播放记录，立即返回 */
    public void submit(PlayRecordMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.debug("播放记录已投递: {}", message);
    }

    public PageResult<PlayRecord> myRecords(Long userId, PageQuery query, String device) {
        Page<PlayRecord> page = playRecordMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<PlayRecord>()
                        .eq(PlayRecord::getUserId, userId)
                        .eq(device != null && !device.isBlank(), PlayRecord::getDevice, device)
                        .orderByDesc(PlayRecord::getCreateTime));
        return PageResult.of(page);
    }

    /** 落库（由 MQ 消费者调用） */
    public void save(PlayRecordMessage message) {
        PlayRecord record = new PlayRecord();
        record.setUserId(message.getUserId());
        record.setSongId(message.getSongId());
        record.setDevice(message.getDevice());
        record.setPlayedSeconds(message.getPlayedSeconds());
        try {
            SongDTO song = musicClient.getSong(message.getSongId()).getData();
            if (song != null) {
                record.setSongName(song.getName());
                record.setSingerName(song.getSingerName());
                record.setCoverUrl(song.getCoverUrl());
                record.setDuration(song.getDuration());
            }
        } catch (Exception e) {
            log.warn("查询歌曲信息失败，仅保存记录ID: {}", e.getMessage());
        }
        playRecordMapper.insert(record);
        try {
            musicClient.addPlayCount(message.getSongId(), 1);
        } catch (Exception e) {
            log.warn("更新歌曲播放量失败: {}", e.getMessage());
        }
    }

    public List<PlayRecord> recent(Long userId, int limit) {
        return playRecordMapper.selectList(new LambdaQueryWrapper<PlayRecord>()
                .eq(userId != null, PlayRecord::getUserId, userId)
                .orderByDesc(PlayRecord::getCreateTime)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
    }

    public void clear(Long userId) {
        playRecordMapper.delete(new LambdaQueryWrapper<PlayRecord>().eq(PlayRecord::getUserId, userId));
    }
}
