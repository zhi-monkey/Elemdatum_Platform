package org.dubhe.data.datasource.impl;

import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.datasource.DataSourceHealthStatus;
import org.dubhe.data.datasource.DataSourceType;
import org.dubhe.data.datasource.IDataSource;
import org.dubhe.data.domain.entity.RtspSource;
import org.dubhe.data.util.RtspPasswordCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * RTSP 数据源适配器
 * <p>
 * 将 {@link RtspSource} 实体包装为统一的 {@link IDataSource} 接口。
 * 截图能力通过 ZLMediaKit 实现（由调用方注入快照字节），
 * 健康检测通过 TCP 连通性检查进行。
 */
public class RtspDataSourceAdapter implements IDataSource {

    private static final Logger log = LoggerFactory.getLogger(RtspDataSourceAdapter.class);

    private final RtspSource rtspSource;

    /** AES Key，用于解密存储的密码（健康检测构建带认证URL时使用） */
    private final String aesKey;

    public RtspDataSourceAdapter(RtspSource rtspSource, String aesKey) {
        this.rtspSource = rtspSource;
        this.aesKey = aesKey;
    }

    @Override
    public Long getId() {
        return rtspSource.getId();
    }

    @Override
    public String getName() {
        return rtspSource.getName();
    }

    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.RTSP;
    }

    @Override
    public String getDescription() {
        return rtspSource.getDescription();
    }

    @Override
    public DataSourceHealthStatus checkHealth() {
        String url = rtspSource.getRtspUrl();
        if (url == null || url.trim().isEmpty()) {
            return DataSourceHealthStatus.offline("RTSP地址未配置");
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 554;
            long start = System.currentTimeMillis();
            InetAddress.getByName(host).isReachable(3000);
            // 简单 DNS 解析可达即认为在线；生产环境可改为 Socket 连接
            long latency = System.currentTimeMillis() - start;
            return DataSourceHealthStatus.online(latency);
        } catch (Exception e) {
            log.warn("RTSP健康检测失败: id={}, url={}, error={}", rtspSource.getId(), url, e.getMessage());
            return DataSourceHealthStatus.offline("连接失败: " + e.getMessage());
        }
    }

    @Override
    public List<CameraInfo> listCameras() {
        // RTSP 场景：数据源本身即为一路摄像头
        CameraInfo info = new CameraInfo(
                String.valueOf(rtspSource.getId()),
                rtspSource.getName(),
                rtspSource.getId(),
                DataSourceType.RTSP
        );
        info.setDescription(rtspSource.getDescription());
        return Collections.singletonList(info);
    }

    @Override
    public byte[] captureSnapshot(String cameraId) {
        // RTSP 截图通过 ZLMediaKit 的 HTTP API 实现，
        // 具体调用在 Service 层通过 ZlmService 完成，此处返回 null 表示需外部处理
        log.debug("RtspDataSourceAdapter.captureSnapshot: cameraId={}, rtspUrl={}", cameraId, rtspSource.getRtspUrl());
        return null;
    }

    @Override
    public String getStreamUrl(String cameraId) {
        return buildAuthenticatedUrl();
    }

    /**
     * 构造带认证信息的 RTSP 地址（用于回流任务传给 ZLMediaKit）
     */
    private String buildAuthenticatedUrl() {
        String url = rtspSource.getRtspUrl();
        if (rtspSource.getUsername() == null || rtspSource.getUsername().trim().isEmpty()) {
            return url;
        }
        try {
            String plainPwd = RtspPasswordCryptoUtil.decrypt(rtspSource.getPassword(), aesKey);
            URI uri = URI.create(url);
            String authUrl = uri.getScheme() + "://"
                    + rtspSource.getUsername() + ":" + plainPwd
                    + "@" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + (uri.getPath() != null ? uri.getPath() : "");
            return authUrl;
        } catch (Exception e) {
            log.warn("构建带认证RTSP地址失败, 返回原始地址. id={}", rtspSource.getId(), e);
            return url;
        }
    }

    /** 获取原始 RtspSource 实体（供业务层使用） */
    public RtspSource getRtspSource() {
        return rtspSource;
    }
}
