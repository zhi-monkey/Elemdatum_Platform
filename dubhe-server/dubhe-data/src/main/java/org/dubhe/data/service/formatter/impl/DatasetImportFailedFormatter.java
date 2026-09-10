package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class DatasetImportFailedFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.DATASET_IMPORT_FAILED;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String datasetName = json.getString("datasetName");
            String datasetType = json.getString("datasetType");
            String errorMessage = json.getString("errorMessage");
            
            return String.format("数据集【%s】导入失败（格式：%s）：%s", 
                    datasetName, datasetType, errorMessage);
        } catch (Exception e) {
            return "数据集导入失败";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "";
    }
}
