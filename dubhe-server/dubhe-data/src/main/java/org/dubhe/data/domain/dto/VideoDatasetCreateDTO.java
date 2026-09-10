package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class VideoDatasetCreateDTO {
    @NotBlank
    private String name;
    private Long labelGroupId;
    @NotNull
    private Long datasetGroupId;
    private String remark;
}
