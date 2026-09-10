package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class DatasetImportSuccessFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.DATASET_IMPORT_SUCCESS;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String datasetName = json.getString("datasetName");
            String datasetType = json.getString("datasetType");
            Integer imageCount = json.getInteger("imageCount");
            
            if (imageCount != null) {
                return String.format("数据集【%s】导入成功（格式：%s），共导入 %d 张图片", 
                        datasetName, datasetType, imageCount);
            } else {
                return String.format("数据集【%s】导入成功（格式：%s）", 
                        datasetName, datasetType);
            }
        } catch (Exception e) {
            return "数据集导入成功";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "";
    }
}
