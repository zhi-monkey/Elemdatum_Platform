package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MultiRecordImportFinishDTO {
    @NotBlank
    private String workerId;
}
