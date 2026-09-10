

package org.dubhe.data;

import org.dubhe.data.config.MinioConnectConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @description 数据处理模块服务启动类
 * @date 2020-12-16
 */
@SpringBootApplication(scanBasePackages = "org.dubhe")
@MapperScan(basePackages = {"org.dubhe.**.dao"})
@EnableConfigurationProperties(MinioConnectConfig.class)
@EnableScheduling
public class DubheDataApplication {

    public static void main(String[] args) {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        SpringApplication.run(DubheDataApplication.class, args);
    }

}
