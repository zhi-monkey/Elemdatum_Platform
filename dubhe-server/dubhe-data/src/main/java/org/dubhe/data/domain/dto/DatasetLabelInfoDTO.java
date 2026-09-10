package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class DatasetLabelInfoDTO {
    private Long labelId;
    private Long annotationCount;
    private String labelName;
}
