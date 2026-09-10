package org.dlut.adv.mineai.model.utils;

import com.alibaba.fastjson.JSONObject;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.model.client.DubheAuthFeign;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import javax.annotation.Resource;

@Service
public class DubheUtils {
    @Resource
    private DubheAuthFeign dubheAuthFeign;

    @Value("${dubhe.cluster.password}")
    private String clusterPassword;

    @Value("${dubhe.cluster.user}")
    private String clusterUser;

    // 使用volatile关键字确保多线程安全
    private volatile String authorization;

    //这个方法提供的是admin账户的权限。
    public String getAuthorization() {
        // 检查是否已经有有效的authorization，如果有则直接返回
        if (authorization != null) {
            return authorization;
        }
        synchronized (this) {
            // 再次检查authorization，避免在多线程情况下重复获取
            if (authorization == null) {
                LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
                params.add("grant_type", "password");
                params.add("username", clusterUser);
                params.add("password", clusterPassword);
                params.add("client_id", "dubhe-client");
                params.add("client_secret", "dubhe-secret");
                params.add("scope", "all");

                JSONObject loginInfo = dubheAuthFeign.dubheAuth(params);
                JSONObject data = loginInfo.getJSONObject("data");
                authorization = data.getString("tokenHead") + data.getString("token");
            }
        }
        return authorization;
    }

    //这个提供的是当前用户的权限
    // 直接从ThreadLocal获取，避免使用实例变量造成线程安全问题
    public String getCurrentAuthorization() {
        return UserContextHolder.getUserToken();
    }
}
