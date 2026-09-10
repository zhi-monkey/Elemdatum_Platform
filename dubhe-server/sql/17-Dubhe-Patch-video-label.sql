-- 17-Dubhe-Patch-video-label.sql
-- 视频标签库（独立表 label_template_video，视频标注均为矩形，不需要 shape 形状列）

CREATE TABLE IF NOT EXISTS `label_template_video` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '标签库中标签id',
    `name` varchar(128) DEFAULT NULL COMMENT '标注名称（英文，标注时写入）',
    `display_name` varchar(128) DEFAULT NULL COMMENT '中文名称（展示用）',
    `annotation_name` varchar(128) DEFAULT NULL COMMENT '标注名称（英文，与 name 一致）',
    `color` varchar(16) DEFAULT NULL COMMENT '类别颜色（#RRGGBB，可空；空则由前端按标签名哈希生成）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_annotation_name` (`annotation_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频标签库';

-- 若表已按旧版（含 shape 列）创建过，执行下面这句删除冗余的 shape 列（视频标注均为矩形）。
-- 重复执行会因列已不存在而报错，属预期；若需幂等，可手工改为 DROP COLUMN IF EXISTS（MySQL 8.0.29+）。
ALTER TABLE `label_template_video` DROP COLUMN `shape`;
