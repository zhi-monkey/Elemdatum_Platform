package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PcDatasetUploadUrlDTO {
    @NotBlank
    private String name;
}
