package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class DataRepoFileCreateDTO {
    private String name;
    private Long datasetId;
    private String url;
    private Long createUserId;
    private Integer fileType;
    private Integer width;
    private Integer height;
    private Long originUserId;
}
