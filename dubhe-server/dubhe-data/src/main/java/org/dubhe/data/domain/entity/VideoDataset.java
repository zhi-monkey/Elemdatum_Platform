package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** Dedicated video dataset. */
@Data
@Accessors(chain = true)
@TableName(value = "video_dataset", autoResultMap = true)
public class VideoDataset extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("name")
    private String name;
    @TableField("label_group_id")
    private Long labelGroupId;
    @TableField("file_count")
    private Long fileCount;
    @TableField("status")
    private Integer status;
    @TableField("remark")
    private String remark;
    @TableField("storage_prefix")
    private String storagePrefix;
    @TableField("upload_status")
    private String uploadStatus;
    @TableField("upload_error")
    private String uploadError;
}
