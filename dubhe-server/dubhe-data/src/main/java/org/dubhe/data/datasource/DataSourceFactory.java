package org.dubhe.data.datasource;

import org.dubhe.cloud.remotecall.config.RestTemplateHolder;
import org.dubhe.data.datasource.impl.HttpCameraDataSourceAdapter;
import org.dubhe.data.datasource.impl.RtspDataSourceAdapter;
import org.dubhe.data.domain.entity.HttpCameraServer;
import org.dubhe.data.domain.entity.RtspSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 数据源工厂
 * <p>
 * 根据实体类型创建对应的 {@link IDataSource} 实现。
 */
@Component
public class DataSourceFactory {

    @Value("${rtsp.password-aes-key:}")
    private String aesKey;

    @Value("${http-camera.download-dir-name:{cameraId}}")
    private String httpCameraDownloadDirName;

    /**
     * 使用框架提供的 RestTemplateHolder（专用于调用非 Nacos 注册的外部服务）
     * 避免引入与已有 HttpClientConfig 冲突的新 Bean
     */
    @Resource
    private RestTemplateHolder restTemplateHolder;

    /**
     * 从 RtspSource 实体创建数据源适配器
     */
    public IDataSource create(RtspSource rtspSource) {
        return new RtspDataSourceAdapter(rtspSource, aesKey);
    }

    /**
     * 从 HttpCameraServer 实体创建数据源适配器
     */
    public IDataSource create(HttpCameraServer httpCameraServer) {
        return new HttpCameraDataSourceAdapter(httpCameraServer, aesKey, restTemplateHolder.getRestTemplate(), httpCameraDownloadDirName);
    }
}
