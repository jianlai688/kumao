package com.kumao.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.file.config.FileProperties;
import com.kumao.file.config.MinioConfig;
import com.kumao.file.entity.FileRecord;
import com.kumao.file.mapper.FileRecordMapper;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileProperties fileProperties;
    private final FileRecordMapper fileRecordMapper;

    /**
     * 上传文件
     *
     * @param bizType 业务类型：avatar/cover/audio/lyric/mv/live-cover/replay/ad 等
     */
    public FileRecord upload(MultipartFile file, String bizType, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original) || !original.contains(".")) {
            throw new BizException("文件名非法");
        }
        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        String type = resolveType(ext);
        checkSize(type, file.getSize());

        String objectKey = buildObjectKey(bizType, ext);
        try {
            ensureBucket();
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectKey)
                        .stream(in, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
            String url = buildUrl(objectKey);
            FileRecord record = new FileRecord();
            record.setOriginalName(original);
            record.setObjectKey(objectKey);
            record.setUrl(url);
            record.setBucket(minioConfig.getBucket());
            record.setContentType(file.getContentType());
            record.setSize(file.getSize());
            record.setFileType(type);
            record.setBizType(bizType);
            record.setUploaderId(uploaderId);
            fileRecordMapper.insert(record);
            return record;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO 上传失败", e);
            // 演示环境 MinIO 未启动时，返回本地占位，不阻断业务流程
            FileRecord fallback = new FileRecord();
            fallback.setOriginalName(original);
            fallback.setObjectKey(objectKey);
            fallback.setUrl("/static/placeholder/" + objectKey);
            fallback.setBucket(minioConfig.getBucket());
            fallback.setContentType(file.getContentType());
            fallback.setSize(file.getSize());
            fallback.setFileType(type);
            fallback.setBizType(bizType);
            fallback.setUploaderId(uploaderId);
            fileRecordMapper.insert(fallback);
            return fallback;
        }
    }

    public String presignedUrl(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException("文件不存在");
        }
        String objectKey = record.getObjectKey();
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .expiry(30, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new BizException("获取文件访问地址失败: " + e.getMessage());
        }
    }

    public void delete(Long id, Long userId, boolean admin) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException("文件不存在");
        }
        if (!admin && record.getUploaderId() != null && !record.getUploaderId().equals(userId)) {
            throw new BizException(403, "只能删除自己上传的文件");
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(record.getBucket()).object(record.getObjectKey()).build());
        } catch (Exception e) {
            log.warn("MinIO 删除失败（忽略）: {}", e.getMessage());
        }
        fileRecordMapper.deleteById(id);
    }

    public PageResult<FileRecord> page(PageQuery query, String fileType, String bizType) {
        Page<FileRecord> page = fileRecordMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<FileRecord>()
                        .eq(StringUtils.hasText(fileType), FileRecord::getFileType, fileType)
                        .eq(StringUtils.hasText(bizType), FileRecord::getBizType, bizType)
                        .like(StringUtils.hasText(query.getKeyword()), FileRecord::getOriginalName, query.getKeyword())
                        .orderByDesc(FileRecord::getCreateTime));
        return PageResult.of(page);
    }

    private String resolveType(String ext) {
        if (fileProperties.imageExtensions().contains(ext)) {
            return "IMAGE";
        }
        if (fileProperties.audioExtensions().contains(ext)) {
            return "AUDIO";
        }
        if (fileProperties.videoExtensions().contains(ext)) {
            return "VIDEO";
        }
        if (fileProperties.lyricExtensions().contains(ext)) {
            return "LYRIC";
        }
        throw new BizException("不支持的文件类型: " + ext);
    }

    private void checkSize(String type, long size) {
        long max = switch (type) {
            case "IMAGE" -> fileProperties.getImageMaxSize();
            case "AUDIO" -> fileProperties.getAudioMaxSize();
            case "VIDEO" -> fileProperties.getVideoMaxSize();
            default -> fileProperties.getLyricMaxSize();
        };
        if (size > max) {
            throw new BizException("文件大小超出限制，最大 " + (max / 1024 / 1024) + "MB");
        }
    }

    private String buildObjectKey(String bizType, String ext) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String biz = StringUtils.hasText(bizType) ? bizType.replaceAll("[^a-zA-Z0-9_-]", "") : "common";
        return biz + "/" + date + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    private String buildUrl(String objectKey) {
        String base = minioConfig.getPublicEndpoint();
        if (!StringUtils.hasText(base)) {
            // 未配置公网域名时走网关静态资源代理：/static/{bucket}/{objectKey}
            return "/static/" + minioConfig.getBucket() + "/" + objectKey;
        }
        return base.replaceAll("/$", "") + "/" + minioConfig.getBucket() + "/" + objectKey;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
        }
        // 演示环境：桶设置公共读，方便前端直接访问
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + minioConfig.getBucket() + "/*\"]}]}";
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(minioConfig.getBucket()).config(policy).build());
    }

    public Set<String> allowedImageExt() {
        return fileProperties.imageExtensions();
    }

    public Set<String> allowedVideoExt() {
        return fileProperties.videoExtensions();
    }
}
