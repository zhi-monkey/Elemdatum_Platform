package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class MultiRecordImportAssetsDTO {
    @NotBlank
    private String workerId;
    @NotNull
    private Long scannedMessages;
    @NotNull
    private Long processedImages;
    @NotNull
    private Long processedPointclouds;
    @Valid
    private List<MultiImageAssetDTO> images = new ArrayList<>();
    @Valid
    private List<MultiPointcloudAssetDTO> pointclouds = new ArrayList<>();
}
