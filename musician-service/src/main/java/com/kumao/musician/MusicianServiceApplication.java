package com.kumao.musician;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.kumao.api.client")
@SpringBootApplication(scanBasePackages = {"com.kumao.musician", "com.kumao.common"})
public class MusicianServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicianServiceApplication.class, args);
    }
}
