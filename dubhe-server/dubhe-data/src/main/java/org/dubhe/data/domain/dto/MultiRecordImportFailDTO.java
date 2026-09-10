package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MultiRecordImportFailDTO {
    @NotBlank
    private String workerId;
    @NotBlank
    private String errorMessage;
    @NotNull
    private Boolean retryable;
}
