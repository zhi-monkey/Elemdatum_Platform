package org.dubhe.data.service.formatter;

import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationFormatterFactory {

    @Autowired
    private List<NotificationFormatter> formatters;

    private Map<NotificationOperationTypeEnum, NotificationFormatter> formatterMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (NotificationFormatter formatter : formatters) {
            NotificationOperationTypeEnum type = formatter.supportType();
            if (type != null) {
                formatterMap.put(type, formatter);
            }
        }
    }

    /**
     * 根据操作类型枚举获取格式化器
     */
    public NotificationFormatter getFormatter(NotificationOperationTypeEnum type) {
        return formatterMap.getOrDefault(type, new DefaultFormatter());
    }

    /**
     * 根据操作类型code获取格式化器
     */
    public NotificationFormatter getFormatter(String operationTypeCode) {
        NotificationOperationTypeEnum type = NotificationOperationTypeEnum.fromCode(operationTypeCode);
        if (type == null) {
            return new DefaultFormatter();
        }
        return getFormatter(type);
    }

    /**
     * 默认格式化器
     */
    private static class DefaultFormatter implements NotificationFormatter {
        @Override
        public NotificationOperationTypeEnum supportType() {
            return null;
        }

        @Override
        public String format(String payload) {
            return "您有一条新通知";
        }

        @Override
        public String getUrl(Notification notification) {
            return "";
        }
    }
}
