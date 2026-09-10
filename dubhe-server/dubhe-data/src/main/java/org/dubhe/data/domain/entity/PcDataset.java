
package org.dubhe.data.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/**
 * @description 点云数据集
 * @date 2022-04-01
 **/
@Data
@Accessors(chain = true)
@TableName(value = "pc_dataset", autoResultMap = true)
public class PcDataset extends BaseEntity {
    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 数据集名称
     */
    @TableField(value = "name")
    private String name;
    /**
     * 标签组id
     */
    @TableField(value = "label_group_id")
    private Long labelGroupId;
    /**
     * 文件数
     */
    @TableField(value = "file_count")
    private Long fileCount;
    /**
     *数据集状态
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 数据集描述
     */
    @TableField(value = "remark")
    private String remark;
    @TableField("storage_prefix")
    private String storagePrefix;

    @TableField("upload_status")
    private String uploadStatus;

    @TableField("upload_error")
    private String uploadError;
}
