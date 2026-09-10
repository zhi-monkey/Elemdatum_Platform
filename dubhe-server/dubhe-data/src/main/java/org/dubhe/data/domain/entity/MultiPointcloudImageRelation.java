package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("multi_pointcloud_image_relation")
public class MultiPointcloudImageRelation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("pointcloud_file_id")
    private Long pointcloudFileId;
    @TableField("image_file_id")
    private Long imageFileId;
    @TableField("time_offset_ns")
    private Long timeOffsetNs;
}
