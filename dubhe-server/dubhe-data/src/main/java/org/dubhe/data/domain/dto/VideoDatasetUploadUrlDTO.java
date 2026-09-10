package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VideoDatasetUploadUrlDTO {
    @NotBlank
    private String name;
}
