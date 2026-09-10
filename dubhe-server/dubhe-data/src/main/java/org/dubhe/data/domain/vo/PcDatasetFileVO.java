package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class PcDatasetFileVO {
    private Long id;
    private Long datasetId;
    private String name;
    private String fileType;
    private String objectKey;
    private String storageBucket;
    private Long fileSize;
    private String contentType;
    private String etag;
    private String uploadStatus;
    private String dataEncoding;
    private Long pointCount;
    private String pcdMetadata;
    private Timestamp createTime;
}
