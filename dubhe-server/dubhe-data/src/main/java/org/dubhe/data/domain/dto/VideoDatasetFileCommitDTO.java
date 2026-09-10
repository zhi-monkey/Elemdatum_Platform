package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VideoDatasetFileCommitDTO {
    @NotBlank
    private String name;
    @NotBlank
    private String objectKey;
}
