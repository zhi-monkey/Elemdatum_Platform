package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class VideoDatasetFileVO {
    private Long id;
    private Long datasetId;
    private String name;
    private String fileType;
    private String url;
    private String objectKey;
    private String storageBucket;
    private Long fileSize;
    private String contentType;
    private String etag;
    private String uploadStatus;
    private String mediaStatus;
    private String extractStatus;
    private String convertedUrl;
    private String annotationStatus;
    private Timestamp createTime;
}
