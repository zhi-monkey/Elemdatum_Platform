package org.dlut.adv.mineai.core.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ZLMediaKit HTTP API client wrapper.
 */

//nacos 配置
//zlm:
//secret: 035c73f7-bb6b-4889-a715-d9eb2d1925cc
//address: http://210.30.96.107:32205/index/api
//app: monitor
//host: __defaultVhost__
//record:
//path: /opt/media/record/
//second: 60
//rtmp:
//prefix: rtmp://210.30.96.107:32202
//rtsp:
//prefix: rtsp://210.30.96.107:32204

@Component
public class ZLMediaKitClient {

    @Resource
    private MediaServerFeignService mediaServerFeignService;

    @Value("${zlm.address}")
    private String zlmAddress;

    @Value("${zlm.secret}")
    private String secret;

    @Value("${zlm.host:__defaultVhost__}")
    private String host;

    @Value("${zlm.app:monitor}")
    private String defaultApp;

    public JSONObject testConnection() {
        Map<String, Object> request = new HashMap<>(3);
        request.put("secret", secret);
        request.put("schema", "rtmp");
        request.put("app", defaultApp);
        String response = mediaServerFeignService.getMediaList(request);
        return parse(response);
    }

    public JSONObject addRtspProxy(String rtspUrl, String stream, String app, boolean closeOld) {
        String finalApp = StringUtils.hasText(app) ? app : defaultApp;
        if (closeOld) {
            deleteStreamProxyQuietly(finalApp, stream);
        }
        Map<String, Object> request = new HashMap<>(8);
        request.put("secret", secret);
        request.put("vhost", host);
        request.put("app", finalApp);
        request.put("stream", stream);
        request.put("url", rtspUrl);
        request.put("enable_rtsp", 1);
        request.put("enable_rtmp", 1);
        String response = mediaServerFeignService.addStreamProxy(request);
        return parse(response);
    }

    public byte[] getSnap(String streamUrl, int timeoutSec, int expireSec) throws IOException {
        String endpoint = buildSnapEndpoint(streamUrl, timeoutSec, expireSec);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(Math.max(timeoutSec, 3) * 1000);
        connection.setReadTimeout(Math.max(timeoutSec, 3) * 1000);
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("ZLM getSnap failed, httpStatus=" + statusCode);
        }
        try (InputStream in = connection.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private void deleteStreamProxyQuietly(String app, String stream) {
        Map<String, Object> request = new HashMap<>(3);
        request.put("secret", secret);
        request.put("key", host + "/" + app + "/" + stream);
        try {
            mediaServerFeignService.delStreamProxy(request);
        } catch (Exception ignore) {
        }
    }

    private JSONObject parse(String response) {
        if (!StringUtils.hasText(response)) {
            JSONObject empty = new JSONObject();
            empty.put("code", -1);
            empty.put("msg", "empty response");
            return empty;
        }
        return JSON.parseObject(response);
    }

    private String buildSnapEndpoint(String streamUrl, int timeoutSec, int expireSec) {
        String base = zlmAddress.endsWith("/") ? zlmAddress.substring(0, zlmAddress.length() - 1) : zlmAddress;
        return base + "/getSnap?secret=" + urlEncode(secret)
                + "&url=" + urlEncode(streamUrl)
                + "&timeout_sec=" + timeoutSec
                + "&expire_sec=" + expireSec;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }
}
