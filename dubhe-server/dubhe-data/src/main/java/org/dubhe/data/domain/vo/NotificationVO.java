package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

@Data
@ApiModel(value = "通知VO", description = "格式化后的通知信息")
public class NotificationVO {

    @ApiModelProperty("通知ID")
    private Long id;

    @ApiModelProperty("消息类型")
    private Integer notificationType;

    @ApiModelProperty("操作类型")
    private String operationType;

    @ApiModelProperty("格式化后的消息内容")
    private String message;

    @ApiModelProperty("读取状态")
    private Integer readStatus;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("原始payload（可选）")
    private String payload;

    @ApiModelProperty("跳转url（可选）")
    private String url;


}
