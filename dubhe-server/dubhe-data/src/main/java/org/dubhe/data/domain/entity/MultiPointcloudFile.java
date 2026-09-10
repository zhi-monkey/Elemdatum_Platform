package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

@Data
@Accessors(chain = true)
@TableName("multi_pointcloud_file")
public class MultiPointcloudFile extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("file_type")
    private String fileType;
    @TableField("dataset_id")
    private Long datasetId;
    @TableField("import_task_id")
    private Long importTaskId;
    @TableField("name")
    private String name;
    @TableField("sensor_name")
    private String sensorName;
    @TableField("topic_name")
    private String topicName;
    @TableField("capture_timestamp_ns")
    private Long captureTimestampNs;
    @TableField("sequence_no")
    private Integer sequenceNo;
    @TableField("url")
    private String url;
    @TableField("object_key")
    private String objectKey;
    @TableField("storage_bucket")
    private String storageBucket;
    @TableField("file_size")
    private Long fileSize;
    @TableField("content_type")
    private String contentType;
    @TableField("etag")
    private String etag;
    @TableField("point_count")
    private Long pointCount;
    @TableField("data_encoding")
    private String dataEncoding;
    @TableField("upload_status")
    private String uploadStatus;
}
