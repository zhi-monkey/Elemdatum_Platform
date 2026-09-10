package org.dubhe.data.config;

import org.dubhe.data.domain.entity.MinIOConnectInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "minioinfo.connect")
public class MinioConnectConfig {
    private List<MinIOConnectInfo> info;

    // Getter和Setter
    public List<MinIOConnectInfo> getInfo() {
        return info;
    }

    public void setInfo(List<MinIOConnectInfo> info) {
        this.info = info;
    }
}
