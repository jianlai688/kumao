package com.kumao.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kumao.minio")
public class MinioConfig {

    private String endpoint;
    /** 对外访问地址；为空时使用网关 /static 代理 */
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
