package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author mingming
 * @date 2025/09/04
 */
@Data
@TableName("dataset_group")
@Builder
public class DatasetGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 数据集组名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建者用户ID
     */
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * 是否公开 (0: false, 1: true)
     */
    @TableField("is_public")
    private Boolean isPublic;

    /**
     * 创建时间 (插入时自动填充)
     */
    @TableField(value = "creation_time")
    private LocalDateTime creationTime;

    /**
     * 更新时间 (插入和更新时自动填充)
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
    /**
     *
     * 新增DatasetGroup组实体(dataset_group表)，新增dataset_dataset_group表来记录当前dataset group下有哪些数据集
     *
     *   CREATE TABLE `dataset_group` (
     *   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
     *   `name` VARCHAR(255) NOT NULL COMMENT '数据集组名称',
     *   `description` TEXT NULL COMMENT '描述',
     *   `create_user_id` BIGINT NOT NULL COMMENT '创建者用户ID',
     *   `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开 (0: false, 1: true)',
     *   `creation_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *   `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     *   PRIMARY KEY (`id`),
     *   INDEX `idx_create_user_id` (`create_user_id`) -- 为创建者ID添加索引，方便查询
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集组表';
     */
}