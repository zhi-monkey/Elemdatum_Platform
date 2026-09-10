package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
@TableName("multi_import_task")
public class MultiRecordImportTask extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("multi_dataset_id")
    private Long multiDatasetId;
    @TableField("source_name")
    private String sourceName;
    @TableField("source_bucket")
    private String sourceBucket;
    @TableField("source_object_key")
    private String sourceObjectKey;
    @TableField("source_size")
    private Long sourceSize;
    @TableField("source_etag")
    private String sourceEtag;
    @TableField("status")
    private String status;
    @TableField("scanned_messages")
    private Long scannedMessages;
    @TableField("processed_images")
    private Long processedImages;
    @TableField("processed_pointclouds")
    private Long processedPointclouds;
    @TableField("attempt_count")
    private Integer attemptCount;
    @TableField("worker_id")
    private String workerId;
    @TableField("heartbeat_at")
    private Timestamp heartbeatAt;
    @TableField("last_dispatched_at")
    private Timestamp lastDispatchedAt;
    @TableField("error_message")
    private String errorMessage;
    @TableField("started_at")
    private Timestamp startedAt;
    @TableField("finished_at")
    private Timestamp finishedAt;
    @TableField("transfer_task_id")
    private Long transferTaskId;
}
