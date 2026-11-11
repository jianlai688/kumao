package com.kumao.live;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRabbit
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.kumao.api.client")
@SpringBootApplication(scanBasePackages = {"com.kumao.live", "com.kumao.common"})
public class LiveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveServiceApplication.class, args);
    }
}
