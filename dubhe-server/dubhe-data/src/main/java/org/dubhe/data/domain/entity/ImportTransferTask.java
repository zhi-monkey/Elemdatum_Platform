package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** Persistent user-facing import task for the transfer center. */
@Data
@TableName("transfer_import_task")
public class ImportTransferTask {
    @TableId(type = IdType.AUTO)
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
    private Long createUserId;
    private Date createTime;
    private Date updateTime;
    private Boolean deleted;
}
