package org.dubhe.data.domain.vo;

import lombok.Data;
import org.dubhe.data.domain.entity.TransferExportTaskEvent;

import java.util.Date;
import java.util.List;

@Data
public class TransferExportTaskVO {
    private Long id;
    private String taskName;
    private String taskType;
    private String datasetType;
    private Long datasetId;
    private Long datasetVersionId;
    private String versionName;
    private String format;
    private String status;
    private String stage;
    private Integer progress;
    private Integer totalFiles;
    private Integer successFiles;
    private Integer failedFiles;
    private String resultObjectKey;
    private Integer retryCount;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;
    private Date createTime;
    private Date updateTime;
    private List<TransferExportTaskEvent> events;
}
