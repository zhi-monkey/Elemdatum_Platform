package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class ImportTransferTaskProgressDTO {
    private String stage;
    private Integer progress;
    private Integer totalFiles;
    private Integer successFiles;
    private Integer failedFiles;
    private Long transferredBytes;
    private String message;
}
