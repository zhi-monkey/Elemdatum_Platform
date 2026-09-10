package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MultiRecordImportProgressDTO {
    @NotBlank
    private String workerId;
    @NotNull
    private Long scannedMessages;
    @NotNull
    private Long processedImages;
    @NotNull
    private Long processedPointclouds;
}
