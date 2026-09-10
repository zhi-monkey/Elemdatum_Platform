package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DataRepoFileVO {
    private Long id;
    private String name;
    private Long datasetId;
    private String url;
    private Long createUserId;
    private Timestamp createTime;
    private Boolean deleted;
    private Integer fileType;
    private Integer width;
    private Integer height;
    private Long originUserId;
    private String fileSize;
}
