package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class AnnotationTaskAssignedFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.ANNOTATION_TASK_ASSIGNED;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String taskName = json.getString("taskName");
            String datasetName = json.getString("datasetName");
            Integer allocCount = json.getInteger("allocCount");
            String teamName = json.getString("teamName");

            return String.format("您被分配了新的标注任务【%s】，数据集：%s，团队：%s，需标注 %d 张图片",
                    taskName, datasetName, teamName, allocCount);
        } catch (Exception e) {
            return "您被分配了新的标注任务";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        // 返回前端路由路径(不包含域名和端口)
        //return "/maData/groupLabeling";
        return "";
    }
}
