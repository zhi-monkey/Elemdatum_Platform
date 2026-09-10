package org.dubhe.data.service.formatter;

import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;

public interface NotificationFormatter {

    /**
     * 获取支持的操作类型枚举
     */
    NotificationOperationTypeEnum supportType();

    /**
     * 格式化消息内容
     * @param payload JSON字符串
     * @return 格式化后的消息
     */
    String format(String payload);

    String getUrl(Notification notification);

}
