package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class DataRepoFileUpdateDTO {
    private String name;
    private Long datasetId;
    private String url;
    private Integer fileType;
    private Integer width;
    private Integer height;
    private Long originUserId;
}
