package org.dubhe.data.service.formatter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredFormatter implements NotificationFormatter {

    @Override
    public NotificationOperationTypeEnum supportType() {
        return NotificationOperationTypeEnum.USER_REGISTERED;
    }

    @Override
    public String format(String payload) {
        try {
            JSONObject json = JSON.parseObject(payload);
            String username = json.getString("username");
            String email = json.getString("email");

            return String.format("用户【%s】注册为游客身份，请前往用户管理为其分配角色和部门",
                    username);
        } catch (Exception e) {
            return "有新用户注册，请前往用户管理进行处理";
        }
    }

    @Override
    public String getUrl(Notification notification) {
        return "mineai/system/user/index";
    }
}
