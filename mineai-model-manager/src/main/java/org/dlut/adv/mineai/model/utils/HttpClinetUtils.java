package org.dlut.adv.mineai.model.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpClinetUtils {
    /**
     * @param url
     * @return
     */
    public static JSONObject doGet(String url) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String result = null;
        try {
            httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            // 构造请求头
            httpGet.setHeader("Content-type", "application/json; charset=utf-8");

            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setSocketTimeout(500); //设置请求时间
            builder.setConnectTimeout(500); //设置超时时间
            builder.setRedirectsEnabled(false);//设置是否跳转链接(反向代理)
            // 设置 连接 属性
            httpGet.setConfig(builder.build());
            // 执行Get请求
            response = httpClient.execute(httpGet);
            // 获取响应实体
            HttpEntity responseEntity = response.getEntity();
            // 检验返回码
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == statusCode) {
                result = EntityUtils.toString(responseEntity, "utf-8");
                return JSONObject.parseObject(result);
            }
        } catch (IOException e) {
            System.out.println("获取日志。发送Get请求失败"+ e);
        } finally {
            // 关闭资源
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    System.out.println("response流关闭异常："+e.getMessage());
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    System.out.println("httpClient流关闭异常："+ e.getMessage());
                }
            }
        }
        return JSONObject.parseObject(result);
    }
}
