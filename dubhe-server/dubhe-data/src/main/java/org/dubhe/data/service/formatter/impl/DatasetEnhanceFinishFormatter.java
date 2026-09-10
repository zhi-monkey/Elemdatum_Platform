package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;


@Component
public class DatasetEnhanceFinishFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.DATASET_ENHANCE_FINISH;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String datasetName = json.getString("datasetName");
            Long id = json.getLong("datasetId");

            return String.format("数据集【%s】数据增强完成（数据集id：%d）",
                    datasetName, id);
        } catch (Exception e) {
            return "数据增强完成";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "";
    }
}
