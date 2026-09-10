package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@TableName("notification")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "Notification对象", description = "站内通知")
public class Notification {

    public static class NotificationType {
        public static final Integer INFO = 0;
        public static final Integer WARNING = 2;
        public static final Integer ERROR = 3;
    }

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("接收用户ID")
    @TableField("to_user_id")
    private Long toUserId;

    @ApiModelProperty("消息类型（0=INFO,1=SUCCESS,2=WARNING,3=ERROR）")
    @TableField("notification_type")
    private Integer notificationType;

    @ApiModelProperty("操作类型标识（详见 NotificationOperationTypeEnum 枚举类）")
    @TableField("operation_type")
    private String operationType;


    @ApiModelProperty("消息上下文JSON字符串")
    private String payload;

    @ApiModelProperty("读取状态（0未读/1已读）")
    @TableField("read_status")
    private Integer readStatus;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Date updateTime;

    @ApiModelProperty("逻辑删除标志（0/1）")
    private Boolean deleted;
}

/*
CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  to_user_id BIGINT NOT NULL,
  notification_type TINYINT NOT NULL COMMENT '0=INFO,1=SUCCESS,2=WARNING,3=ERROR',
  operation_type VARCHAR(128) NOT NULL,
  payload JSON NOT NULL,
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_user_read (to_user_id, read_status, create_time),
  KEY idx_user_time (to_user_id, create_time),
  KEY idx_op_type (operation_type)
) COMMENT='站内通知';
 */

