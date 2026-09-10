package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/**
 * @description 数据导出任务
 * @date 2026-08-29
 */
@Data
@Accessors(chain = true)
@TableName("export_task")
public class ExportTask extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("data_type")
    private Integer dataType;

    @TableField("search_condition")
    private String searchCondition;

    @TableField("file_ids")
    private String fileIds;

    @TableField("format")
    private String format;

    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress;

    @TableField("result_url")
    private String resultUrl;

    @TableField("error_message")
    private String errorMessage;

    @TableField("file_count")
    private Integer fileCount;

    @TableField("expire_time")
    private java.sql.Timestamp expireTime;

    @TableField("log")
    private String log;

    @TableField("dataset_summary")
    private String datasetSummary;

    @TableField("condition_summary")
    private String conditionSummary;
}
