package org.dlut.adv.mineai.packagem;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"org.dlut.adv.mineai"})
@EntityScan("org.dlut.adv.mineai")
@EnableAsync
@EnableScheduling
public class PackageMApplication {
    public static void main(String[] args) {
        SpringApplication.run(PackageMApplication.class, args);
    }
}
