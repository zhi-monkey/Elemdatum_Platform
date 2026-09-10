package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** A file stored in a point-cloud dataset. */
@Data
@Accessors(chain = true)
@TableName(value = "pc_dataset_file", autoResultMap = true)
public class PcDatasetFile extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("name")
    private String name;
    @TableField("file_type")
    private String fileType;
    @TableField("dataset_id")
    private Long datasetId;
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
    @TableField("sha256")
    private String sha256;
    @TableField("upload_status")
    private String uploadStatus;
    @TableField("upload_error")
    private String uploadError;
    @TableField("data_encoding")
    private String dataEncoding;
    @TableField("point_count")
    private Long pointCount;
    @TableField("pcd_metadata")
    private String pcdMetadata;
}
