package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用于存储数据集和数据集组关系
 * @author mingming
 * @date 2025/09/05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("dataset_dataset_group")
public class DatasetDatasetGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 数据集组ID
     */
    @TableField("dataset_group_id")
    private Long datasetGroupId;

    /**
     * 数据集ID
     */
    @TableField("dataset_id")
    private Long datasetId;

    public DatasetDatasetGroup(Long datasetGroupId, Long datasetId) {
        this.datasetGroupId = datasetGroupId;
        this.datasetId = datasetId;
    }

    /**
     * CREATE TABLE `dataset_dataset_group` (
     *   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     *   `dataset_group_id` BIGINT NOT NULL COMMENT '数据集组ID',
     *   `dataset_id` BIGINT NOT NULL COMMENT '数据集ID',
     *   PRIMARY KEY (`id`),
     *   -- 创建联合唯一索引，防止同一个数据集被重复添加到同一个组中
     *   UNIQUE KEY `uk_group_dataset` (`dataset_group_id`, `dataset_id`),
     *   -- 为 dataset_id 创建独立索引，方便通过数据集ID反查其所属的组
     *   KEY `idx_dataset_id` (`dataset_id`)
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集与数据集组关联表';
     */
}