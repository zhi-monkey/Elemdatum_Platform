package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MultiRecordImportClaimDTO {
    @NotNull
    private Long taskId;
    @NotBlank
    private String workerId;
}
