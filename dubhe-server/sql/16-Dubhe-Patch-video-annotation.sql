-- 16-Dubhe-Patch-video-annotation.sql
-- 视频 BBox 跟踪标注（专用视频数据集体系 B）
-- 1) video_dataset_file 补充视频元信息 + 标注状态字段
-- 2) 新增视频标注任务 / Track / 关键帧三张表

-- 注意：以下 ALTER 需在 video_dataset_file 表已存在的前提下执行一次；
-- 重复执行会因列已存在而报错，属预期。若需幂等，可手工改为 ADD COLUMN IF NOT EXISTS（MySQL 8.0.29+）。
ALTER TABLE `video_dataset_file`
    ADD COLUMN `width` int(11) DEFAULT NULL COMMENT '视频宽(像素)',
    ADD COLUMN `height` int(11) DEFAULT NULL COMMENT '视频高(像素)',
    ADD COLUMN `fps` decimal(10,3) DEFAULT NULL COMMENT '帧率',
    ADD COLUMN `frame_count` int(11) DEFAULT NULL COMMENT '总帧数',
    ADD COLUMN `duration` decimal(12,3) DEFAULT NULL COMMENT '时长(秒)',
    ADD COLUMN `codec` varchar(64) DEFAULT NULL COMMENT '编码',
    ADD COLUMN `converted_url` varchar(512) DEFAULT NULL COMMENT 'H.264 转码文件路径（H.265 原文件转码后，前端播放用）',
    ADD COLUMN `annotation_status` varchar(32) DEFAULT 'READY' COMMENT '标注状态(冗余自video_annotation_task.status)';

CREATE TABLE IF NOT EXISTS `video_annotation_task` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `video_file_id` bigint(20) NOT NULL COMMENT '视频文件ID，关联video_dataset_file.id',
    `dataset_id` bigint(20) DEFAULT NULL COMMENT '视频数据集ID，冗余便于筛选',
    `annotator_id` bigint(20) DEFAULT NULL COMMENT '标注人ID',
    `status` varchar(32) DEFAULT 'READY' COMMENT 'READY/ANNOTATING/SUBMITTED/REVIEWING/APPROVED',
    `last_frame_index` int(11) DEFAULT NULL COMMENT '上次标注停留帧号(断点续标)',
    `annotation_version` int(11) DEFAULT 1 COMMENT '标注版本号',
    `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
    `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_video_file` (`video_file_id`),
    KEY `idx_dataset` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频标注任务';

CREATE TABLE IF NOT EXISTS `video_annotation_track` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` bigint(20) NOT NULL COMMENT '标注任务ID，关联video_annotation_task.id',
    `track_no` int(11) DEFAULT NULL COMMENT '目标序号(#001展示用)',
    `label_id` bigint(20) DEFAULT NULL COMMENT '标签ID，关联data_label.id',
    `start_frame` int(11) DEFAULT NULL COMMENT '起始帧号(1-based)',
    `end_frame` int(11) DEFAULT NULL COMMENT '结束帧号(1-based)',
    `status` varchar(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FINISHED',
    `source` varchar(16) DEFAULT 'manual' COMMENT 'manual/auto',
    `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
    `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频标注Track(一个目标)';

CREATE TABLE IF NOT EXISTS `video_track_keyframe` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `track_id` bigint(20) NOT NULL COMMENT 'Track ID，关联video_annotation_track.id',
    `frame_index` int(11) NOT NULL COMMENT '视频帧号(1-based)',
    `x` double DEFAULT NULL COMMENT '归一化bbox x(0~1)',
    `y` double DEFAULT NULL COMMENT '归一化bbox y(0~1)',
    `width` double DEFAULT NULL COMMENT '归一化bbox宽(0~1)',
    `height` double DEFAULT NULL COMMENT '归一化bbox高(0~1)',
    `outside` bit(1) DEFAULT b'0' COMMENT '完全不可见(二期)',
    `occluded` bit(1) DEFAULT b'0' COMMENT '被遮挡(二期)',
    `source` varchar(16) DEFAULT 'manual' COMMENT 'manual/interpolation',
    `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
    `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) DEFAULT b'0' COMMENT '删除标识',
    PRIMARY KEY (`id`),
    KEY `idx_track` (`track_id`),
    KEY `idx_track_frame` (`track_id`, `frame_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频Track关键帧';

-- ============ 视频标注工作台菜单（挂在「数据中心」目录下）============
-- 前端组件对应 src/views/mineai/data/dataset-details2/video-annotate/index.vue
INSERT INTO `menu`
(`pid`,`type`,`name`,`icon`,`path`,`component`,`component_name`,`layout`,`permission`,`back_to`,`ext_config`,`hidden`,`cache`,`sort`,`create_user_id`,`update_user_id`,`deleted`)
VALUES
(
  (SELECT id FROM (SELECT id FROM `menu` WHERE `name` = '数据中心' AND `type` = 0 LIMIT 1) t),
  1,
  '视频标注',
  NULL,
  'videoAnnotate/:id/:name',
  'mineai/data/dataset-details2/video-annotate/index',
  'VideoAnnotate',
  'BaseLayout',
  'data:videoAnnotate',
  NULL,
  NULL,
  b'1',
  b'0',
  999,
  NULL,
  NULL,
  b'0'
);

-- 把「视频标注」授权给目标角色（把 1 换成你的角色 id，管理员一般是 1）：
INSERT INTO `roles_menus` (`role_id`, `menu_id`)
SELECT 1, id FROM `menu` WHERE `name` = '视频标注';
