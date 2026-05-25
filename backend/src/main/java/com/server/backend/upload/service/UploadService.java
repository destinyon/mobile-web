package com.server.backend.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.server.backend.common.BusinessException;
import com.server.backend.upload.dto.UploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "video/mp4");
    private final String endpoint;
    private final String bucket;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String publicBaseUrl;

    public UploadService(
            @Value("${app.oss.endpoint}") String endpoint,
            @Value("${app.oss.bucket}") String bucket,
            @Value("${app.oss.access-key-id}") String accessKeyId,
            @Value("${app.oss.access-key-secret}") String accessKeySecret,
            @Value("${app.oss.public-base-url}") String publicBaseUrl) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.publicBaseUrl = publicBaseUrl;
    }

    public UploadResult upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (!ALLOWED.contains(file.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "仅支持 jpg、png、webp 图片或 mp4 视频");
        }
        if (bucket == null || bucket.isBlank() || accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "OSS 配置缺失");
        }
        String suffix = suffix(file.getOriginalFilename(), file.getContentType());
        String key = "uploads/" + UUID.randomUUID() + suffix;
        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            oss.putObject(bucket, key, file.getInputStream());
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "上传 OSS 失败");
        } finally {
            oss.shutdown();
        }
        return new UploadResult(publicBaseUrl.replaceAll("/$", "") + "/" + key, key);
    }

    public UploadResult uploadBytes(String key, byte[] bytes, String contentType) {
        if (bucket == null || bucket.isBlank() || accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "OSS 配置缺失");
        }
        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            oss.putObject(bucket, key, new java.io.ByteArrayInputStream(bytes));
        } finally {
            oss.shutdown();
        }
        return new UploadResult(publicBaseUrl.replaceAll("/$", "") + "/" + key, key);
    }

    private String suffix(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "video/mp4".equals(contentType) ? ".mp4" : ".jpg";
    }
}
