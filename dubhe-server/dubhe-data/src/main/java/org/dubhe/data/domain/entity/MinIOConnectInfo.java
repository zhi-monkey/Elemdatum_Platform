package org.dubhe.data.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinIOConnectInfo {
    private String hostIp;
    private String fullURL;
    private String minioIp;
    private String port;
    private String prefix;
    private String bucketName;
}
