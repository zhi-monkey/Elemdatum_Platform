package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PcDatasetFileCommitDTO {
    @NotBlank
    private String name;
    @NotBlank
    private String objectKey;
}
