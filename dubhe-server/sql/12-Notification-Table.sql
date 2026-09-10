-- ========================================
-- 消息通知表创建脚本
-- 用于支持站内消息通知功能
-- ========================================

USE mineai_cz_TYX;

-- 删除已存在的表（如果需要重建）
-- DROP TABLE IF EXISTS notification;

-- 创建通知表
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  to_user_id BIGINT NOT NULL COMMENT '接收用户ID',
  notification_type TINYINT NOT NULL COMMENT '消息类型：0=INFO, 1=SUCCESS, 2=WARNING, 3=ERROR',
  operation_type VARCHAR(128) NOT NULL COMMENT '操作类型标识（详见 NotificationOperationTypeEnum 枚举类）',
  payload TEXT COMMENT '消息上下文JSON字符串',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '读取状态：0=未读, 1=已读',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0=未删除, 1=已删除',
  
  -- 索引
  KEY idx_user_read (to_user_id, read_status, create_time) COMMENT '用户未读消息查询索引',
  KEY idx_user_time (to_user_id, create_time) COMMENT '用户消息时间索引',
  KEY idx_op_type (operation_type) COMMENT '操作类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

-- 插入测试数据（可选）
INSERT INTO notification (to_user_id, notification_type, operation_type, payload, read_status, create_time, update_time, deleted)
VALUES 
(1, 0, 'MSG_DATASET_IMPORT_SUCCESS', '{"datasetId": 1, "datasetName": "测试数据集"}', 0, NOW(), NOW(), 0),
(1, 2, 'MSG_DATASET_ENHANCE_FINISH', '{"datasetId": 2, "datasetName": "增强数据集"}', 0, NOW(), NOW(), 0),
(1, 3, 'MSG_TRAIN_FAILED', '{"modelJobName": "测试训练任务", "generationId": 1}', 0, NOW(), NOW(), 0);

-- 查看表结构
DESC notification;

-- 查看测试数据
SELECT * FROM notification;

-- 统计未读消息数量
SELECT to_user_id, COUNT(*) as unread_count 
FROM notification 
WHERE read_status = 0 AND deleted = 0 
GROUP BY to_user_id;
