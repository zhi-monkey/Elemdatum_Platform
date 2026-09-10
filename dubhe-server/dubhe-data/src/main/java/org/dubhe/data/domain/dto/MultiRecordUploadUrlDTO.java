package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MultiRecordUploadUrlDTO {
    @NotBlank
    private String name;
}
