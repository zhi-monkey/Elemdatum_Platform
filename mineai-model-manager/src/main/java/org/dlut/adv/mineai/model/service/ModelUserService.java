package org.dlut.adv.mineai.model.service;

import com.alibaba.fastjson.JSONObject;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.client.DubheAuthFeign;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author oyjp
 * @create 2024/4/7 19:04
 */

@Service
public class ModelUserService {


    @Resource
    private DubheUserFeign dubheUserFeign;

    @Resource
    private DubheAuthFeign dubheAuthFeign;
    @Value("${dubhe.cluster.user}")
    private String clusterUser;

    @Value("${dubhe.cluster.password}")
    private String clusterPassword;

    // 使用volatile关键字确保多线程安全
    private volatile String authorization;

    private String getAuthorization() {
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
    public List<UserDTO> findUsersByIds(List<Long> ids){
        return dubheUserFeign.findUsersByIds(getAuthorization(), ids).getData();
    }
}
