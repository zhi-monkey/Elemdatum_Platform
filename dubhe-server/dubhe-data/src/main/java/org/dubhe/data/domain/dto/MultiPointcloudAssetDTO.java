package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MultiPointcloudAssetDTO {
    @NotBlank
    private String name;
    @NotBlank
    private String topic;
    @NotNull
    private Long timestampNs;
    @NotNull
    private Integer sequenceNo;
    @NotBlank
    private String objectKey;
    @NotNull
    private Long fileSize;
    private String contentType;
    private String etag;
    private Long pointCount;
}
