package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("transfer_export_task")
public class TransferExportTask {
    @TableId(type = IdType.AUTO)
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
    private Long createUserId;
    private Date createTime;
    private Date updateTime;
    private Boolean deleted;
}
