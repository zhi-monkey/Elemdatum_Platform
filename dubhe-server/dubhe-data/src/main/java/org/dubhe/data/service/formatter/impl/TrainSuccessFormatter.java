package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class TrainSuccessFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.TRAIN_SUCCESS;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String jobName = json.getString("modelJobName");
            String generationName = json.getString("generationName");
            return String.format("【标准化】训练任务【%s】（子任务%s）执行成功！", generationName, jobName);
        } catch (Exception e) {
            return "训练任务执行成功";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "";
    }
}