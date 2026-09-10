package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class ImportTransferTaskCreateDTO {
    private String taskName;
    private String datasetType;
    private Long datasetId;
    private String sourceType;
    private Integer totalFiles;
    private Long totalBytes;
}
