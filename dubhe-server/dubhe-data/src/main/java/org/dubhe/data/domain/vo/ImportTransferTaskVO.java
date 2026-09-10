package org.dubhe.data.domain.vo;

import lombok.Data;
import org.dubhe.data.domain.entity.ImportTransferTaskEvent;

import java.util.Date;
import java.util.List;

@Data
public class ImportTransferTaskVO {
    private Long id;
    private String taskName;
    private String datasetType;
    private Long datasetId;
    private String sourceType;
    private String status;
    private String stage;
    private Integer progress;
    private Integer totalFiles;
    private Integer successFiles;
    private Integer failedFiles;
    private Long totalBytes;
    private Long transferredBytes;
    private Integer retryCount;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;
    private Date lastHeartbeatAt;
    private Date createTime;
    private Date updateTime;
    private List<ImportTransferTaskEvent> events;
}
