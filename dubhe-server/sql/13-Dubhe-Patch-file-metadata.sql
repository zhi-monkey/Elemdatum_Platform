-- 13-Dubhe-Patch-file-metadata.sql
-- 文件元信息表：上传时由系统自动提取 + 授权用户补充，供"数据筛选"模块使用。
-- 说明：自动字段（上传时间/数据模态/所属数据集/处理状态）已存在于 data_file 与 data_dataset_version_file，不在本表重复落。

CREATE TABLE IF NOT EXISTS `data_file_metadata` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_id` bigint(20) NOT NULL COMMENT '文件ID，关联 data_file.id',
    `dataset_id` bigint(20) DEFAULT NULL COMMENT '数据集ID（冗余，便于按数据集筛选）',
    `source_type` varchar(64) DEFAULT '系统登记' COMMENT '数据来源',
    `capture_time` datetime DEFAULT NULL COMMENT '采集时间',
    `device` varchar(128) DEFAULT NULL COMMENT '车辆或设备',
    `device_sn` varchar(128) DEFAULT NULL COMMENT '设备或相机编号',
    `location` varchar(255) DEFAULT NULL COMMENT '采集地点',
    `scenario` varchar(128) DEFAULT NULL COMMENT '业务场景',
    `lighting` varchar(64) DEFAULT NULL COMMENT '光照或环境条件',
    `quality` varchar(64) DEFAULT NULL COMMENT '图像质量',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_id` (`file_id`),
    KEY `idx_dataset_scenario` (`dataset_id`,`scenario`),
    KEY `idx_dataset_capture_time` (`dataset_id`,`capture_time`),
    KEY `idx_dataset_location` (`dataset_id`,`location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元信息';
