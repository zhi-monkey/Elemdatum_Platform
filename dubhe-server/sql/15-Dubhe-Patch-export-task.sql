-- 15-Dubhe-Patch-export-task.sql
-- 数据批量导出任务表（图像检索批量导出用，支持任务进度查看与追溯）

CREATE TABLE IF NOT EXISTS `export_task` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` varchar(64) NOT NULL COMMENT '任务号',
    `data_type` int(11) DEFAULT 0 COMMENT '数据类型，0图像',
    `search_condition` text COMMENT '检索条件JSON',
    `file_ids` text COMMENT '导出文件ID JSON',
    `format` varchar(16) DEFAULT NULL COMMENT '导出格式 csv/json/zip',
    `status` varchar(16) DEFAULT NULL COMMENT 'PENDING等待确认/COMPLETED已完成/CANCELLED已取消',
    `progress` int(11) DEFAULT 0 COMMENT '进度0-100',
    `result_url` varchar(255) DEFAULT NULL COMMENT '导出文件相对路径',
    `error_message` varchar(255) DEFAULT NULL COMMENT '错误信息',
    `file_count` int(11) DEFAULT 0 COMMENT '导出数据条数',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `log` text COMMENT '执行日志',
    `dataset_summary` varchar(500) DEFAULT NULL COMMENT '数据集统计',
    `condition_summary` text COMMENT '筛选条件摘要',
    `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
    `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_create_user_time` (`create_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导出任务';
