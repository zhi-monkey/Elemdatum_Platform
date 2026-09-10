package org.dlut.adv.mineai.model.dto;

import lombok.Data;

@Data
public class DatasetLabelInfoDTO {
    private Long labelId;
    private Long annotationCount;
    private String labelName;
}
