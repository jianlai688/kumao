package com.kumao.file.controller;

import com.kumao.common.dto.PageQuery;
import com.kumao.common.result.PageResult;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.file.entity.FileRecord;
import com.kumao.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public R<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(defaultValue = "common") String bizType) {
        return R.ok(fileService.upload(file, bizType, UserContext.getUserId()));
    }

    /** 多文件上传 */
    @PostMapping("/upload/batch")
    public R<List<FileRecord>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                           @RequestParam(defaultValue = "common") String bizType) {
        List<FileRecord> records = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            records.add(fileService.upload(file, bizType, UserContext.getUserId()));
        }
        return R.ok(records);
    }

    @GetMapping("/{id}/presigned")
    public R<String> presigned(@PathVariable Long id) {
        return R.ok(fileService.presignedUrl(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id, UserContext.getUserId(), UserContext.isAdmin());
        return R.ok();
    }

    @GetMapping("/admin/page")
    public R<PageResult<FileRecord>> page(PageQuery query,
                                         @RequestParam(required = false) String fileType,
                                         @RequestParam(required = false) String bizType) {
        UserContext.requireAdmin();
        return R.ok(fileService.page(query, fileType, bizType));
    }

    /** 上传约束，供前端提前校验 */
    @GetMapping("/constraints")
    public R<Map<String, Object>> constraints() {
        return R.ok(Map.of(
                "image", fileService.allowedImageExt(),
                "video", fileService.allowedVideoExt(),
                "maxImageSize", 10 * 1024 * 1024,
                "maxAudioSize", 100 * 1024 * 1024,
                "maxVideoSize", 500 * 1024 * 1024
        ));
    }
}
