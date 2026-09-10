package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RTSP回流执行记录实体类 (子任务)
 */
@Data
@TableName("rtsp_capture_execution")
public class RtspCaptureExecution {

    /**
     * 运行记录主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的父任务ID
     */
    private Long taskId;

    /**
     * 此次采集自动创建的数据集 ID（dataset.id）
     * 采集完成后图片写入该数据集
     */
    private Long datasetId;

    /**
     * 执行状态: 0-排队中, 1-运行中, 2-成功, 3-失败, 4-已手动取消
     */
    private Integer status;

    /**
     * 当前收集进度百分比 (0.00 - 100.00)
     */
    private BigDecimal progress;

    /**
     * 实际已捕获数量
     */
    private Integer capturedCount;

    /**
     * 快照: 执行时的捕获间隔
     */
    private Integer captureInterval;

    /**
     * 快照: 执行时的计划捕获数量
     */
    private Integer imageQuantity;

    /**
     * 失败状态或错误信息日志
     */
    private String message;

    /**
     * 实际开始执行的时间
     */
    private LocalDateTime startTime;

    /**
     * 执行结束或中断的时间
     */
    private LocalDateTime endTime;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除标识: 0-未删除, 1-已删除
     */
    private Integer isDeleted;
}

//
//-- ----------------------------
//        -- 2. 子任务表：RTSP 回流执行记录
//-- ----------------------------
//CREATE TABLE `rtsp_capture_execution` (
//        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '运行记录主键ID',
//        `task_id` bigint NOT NULL COMMENT '关联的父任务ID',
//        `status` tinyint NOT NULL DEFAULT '0' COMMENT '执行状态: 0-排队中, 1-运行中, 2-成功, 3-失败, 4-已手动取消',
//        `progress` decimal(5,2) DEFAULT '0.00' COMMENT '当前收集进度(0.00 - 100.00)',
//        `captured_count` int NOT NULL DEFAULT '0' COMMENT '实际已捕获数量',
//        `capture_interval` int DEFAULT NULL COMMENT '快照: 执行时的捕获间隔',
//        `image_quantity` int DEFAULT NULL COMMENT '快照: 执行时的计划捕获数量',
//        `message` text COMMENT '失败状态或错误信息日志',
//        `start_time` datetime DEFAULT NULL COMMENT '实际开始执行的时间',
//        `end_time` datetime DEFAULT NULL COMMENT '执行结束或中断的时间',
//        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
//        `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
//PRIMARY KEY (`id`),
//KEY `idx_task_id` (`task_id`),
//KEY `idx_status` (`status`)
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RTSP回流执行记录表';

