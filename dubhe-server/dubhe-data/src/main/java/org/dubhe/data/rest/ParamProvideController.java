package org.dubhe.data.rest;

import com.alibaba.fastjson.JSONObject;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.config.MinioConnectConfig;
import org.dubhe.data.domain.entity.MinIOConnectInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@EnableScheduling
@RequestMapping("/params")
public class ParamProvideController {

    @Autowired
    private MinioConnectConfig minioConnectConfig;

    @PostMapping("/minioUrl")
    public DataResponseBody getMinIOUrl(@RequestBody JSONObject jsonObject) {
        String hostIp = jsonObject.getString("hostIp");
        List<MinIOConnectInfo> info = minioConnectConfig.getInfo();
        MinIOConnectInfo matchedConnectedInfo = null;
        for (MinIOConnectInfo minIOConnectInfo : info) {
            //查找对应ip的配置
            if (minIOConnectInfo.getHostIp().equals(hostIp)) {
                matchedConnectedInfo = minIOConnectInfo;
            }
        }
        if (matchedConnectedInfo == null) {
            return new DataResponseBody(ResponseCode.ERROR, "未找到对象存储接入点，请联系管理员");
        }

        return new DataResponseBody(matchedConnectedInfo.getFullURL());
    }

    @PostMapping("/minioConfig")
    public DataResponseBody getMinIOConfig(@RequestBody JSONObject jsonObject) {
        String hostIp = jsonObject.getString("hostIp");
        List<MinIOConnectInfo> info = minioConnectConfig.getInfo();
        if (info == null || info.isEmpty()) {
            return new DataResponseBody(ResponseCode.ERROR, "对象存储接入点未配置，请联系管理员");
        }
        MinIOConnectInfo matchedConnectedInfo = null;
        for (MinIOConnectInfo minIOConnectInfo : info) {
            // hostIp 可能包含端口；本地开发 localhost 与 127.0.0.1 视为同一入口。
            String configuredHost = minIOConnectInfo.getHostIp();
            if (configuredHost != null && (configuredHost.equals(hostIp)
                    || stripPort(configuredHost).equals(stripPort(hostIp))
                    || isLocalHost(configuredHost) && isLocalHost(hostIp))) {
                matchedConnectedInfo = minIOConnectInfo;
                break;
            }
        }
        if (matchedConnectedInfo == null) {
            return new DataResponseBody(ResponseCode.ERROR, "未找到对象存储接入点，请联系管理员");
        }

        // 构造配置对象
        Map<String, Object> config = new HashMap<>();
        config.put("endPoint", matchedConnectedInfo.getMinioIp());
        config.put("port", matchedConnectedInfo.getPort());
        config.put("bucketName", matchedConnectedInfo.getBucketName());
        config.put("prefix", matchedConnectedInfo.getPrefix());

        return new DataResponseBody(config);
    }

    private static String stripPort(String host) {
        if (host == null) {
            return "";
        }
        int portSeparator = host.lastIndexOf(':');
        return portSeparator > -1 ? host.substring(0, portSeparator) : host;
    }

    private static boolean isLocalHost(String host) {
        String normalizedHost = stripPort(host);
        return "localhost".equalsIgnoreCase(normalizedHost) || "127.0.0.1".equals(normalizedHost);
    }
}
