-- 数据回流新增实体建表 SQL（汇总）
-- 按当前 mineai_cz_local 库表结构整理，仅保留 CREATE TABLE 语句

CREATE TABLE IF NOT EXISTS `rtsp_source` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '源名称',
  `rtsp_url` varchar(1024) NOT NULL COMMENT 'RTSP地址',
  `username` varchar(128) DEFAULT NULL COMMENT 'RTSP认证用户名',
  `password` varchar(1024) DEFAULT NULL COMMENT 'RTSP认证密码（AES-GCM密文Base64）',
  `description` varchar(512) DEFAULT NULL COMMENT '描述信息',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTSP数据源表';


CREATE TABLE IF NOT EXISTS `http_camera_server` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '服务器名称',
  `server_url` varchar(1024) NOT NULL COMMENT 'HTTP服务器基础地址，例如 http://192.168.1.100:8080',
  `auth_token` varchar(1024) DEFAULT NULL COMMENT '访问令牌（AES-GCM密文Base64）',
  `description` varchar(512) DEFAULT NULL COMMENT '描述信息',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HTTP摄像头服务器表';


CREATE TABLE IF NOT EXISTS `http_camera` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `http_camera_server_id` bigint(20) unsigned NOT NULL COMMENT '关联HTTP服务器ID',
  `camera_id` varchar(128) NOT NULL COMMENT '摄像机ID',
  `name` varchar(128) NOT NULL COMMENT '摄像机名称',
  `stream_url` varchar(1024) DEFAULT NULL COMMENT '视频流地址',
  `description` varchar(512) DEFAULT NULL COMMENT '描述信息',
  `status` varchar(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT '状态: ONLINE/OFFLINE/UNKNOWN',
  `last_seen_time` datetime DEFAULT NULL COMMENT '最近一次同步出现时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除:0-否,1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_server_camera` (`http_camera_server_id`,`camera_id`),
  KEY `idx_server_delete` (`http_camera_server_id`,`is_delete`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HTTP摄像机实体表';


-- 已有环境升级时，如果 rtsp_capture_task 已存在且缺少创建人字段，需单独执行：
-- ALTER TABLE `http_camera`
--   ADD COLUMN `stream_url` varchar(1024) DEFAULT NULL COMMENT '视频流地址' AFTER `name`;
--
-- ALTER TABLE `rtsp_capture_task`
--   ADD COLUMN `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人ID' AFTER `label_ids`;
-- ALTER TABLE `rtsp_capture_task`
--   ADD KEY `idx_create_user_id` (`create_user_id`);
