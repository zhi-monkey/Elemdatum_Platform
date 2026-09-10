package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class AutoLabelSuccessFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.AUTO_LABEL_SUCCESS;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String datasetName = json.getString("datasetName");
            Long datasetId = json.getLong("datasetId");

            return String.format("数据集【%s】自动标注完成（数据集id：%d）",
                    datasetName, datasetId);
        } catch (Exception e) {
            return "自动标注完成";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "";
    }
}
