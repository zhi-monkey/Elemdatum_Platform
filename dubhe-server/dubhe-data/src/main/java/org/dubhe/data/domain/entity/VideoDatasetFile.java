package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** A video object stored in a dedicated video dataset. */
@Data
@Accessors(chain = true)
@TableName(value = "video_dataset_file", autoResultMap = true)
public class VideoDatasetFile extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("name")
    private String name;
    @TableField("file_type")
    private String fileType;
    @TableField("file_ext")
    private String fileExt;
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
    @TableField("upload_status")
    private String uploadStatus;
    @TableField("upload_error")
    private String uploadError;
    @TableField("media_status")
    private String mediaStatus;
    @TableField("extract_status")
    private String extractStatus;
    @TableField("width")
    private Integer width;
    @TableField("height")
    private Integer height;
    @TableField("fps")
    private Double fps;
    @TableField("frame_count")
    private Integer frameCount;
    @TableField("duration")
    private Double duration;
    @TableField("codec")
    private String codec;
    @TableField("converted_url")
    private String convertedUrl;
    @TableField("annotation_status")
    private String annotationStatus;
}
