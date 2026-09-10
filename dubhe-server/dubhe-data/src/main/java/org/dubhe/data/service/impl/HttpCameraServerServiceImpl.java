package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.data.dao.HttpCameraServerMapper;
import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.datasource.DataSourceFactory;
import org.dubhe.data.datasource.IDataSource;
import org.dubhe.data.datasource.impl.HttpCameraDataSourceAdapter;
import org.dubhe.data.domain.entity.HttpCameraServer;
import org.dubhe.data.service.HttpCameraServerService;
import org.dubhe.data.util.RsaCryptoUtil;
import org.dubhe.data.util.RtspPasswordCryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class HttpCameraServerServiceImpl
        extends ServiceImpl<HttpCameraServerMapper, HttpCameraServer>
        implements HttpCameraServerService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Value("${rtsp.password-aes-key:}")
    private String aesKey;

    @Value("${rsa.private_key:}")
    private String rsaPrivateKey;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Override
    public Page<HttpCameraServer> page(Page<HttpCameraServer> page, HttpCameraServer query) {
        HttpCameraServer q = query == null ? new HttpCameraServer() : query;
        LambdaQueryWrapper<HttpCameraServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HttpCameraServer::getIsDelete, NOT_DELETED)
                .like(q.getName() != null && !q.getName().isEmpty(), HttpCameraServer::getName, q.getName())
                .like(q.getServerUrl() != null && !q.getServerUrl().isEmpty(), HttpCameraServer::getServerUrl, q.getServerUrl())
                .like(q.getDescription() != null && !q.getDescription().isEmpty(), HttpCameraServer::getDescription, q.getDescription())
                .orderByDesc(HttpCameraServer::getId);
        return page(page, wrapper);
    }

    @Override
    public List<HttpCameraServer> findAllAvailable() {
        return lambdaQuery()
                .eq(HttpCameraServer::getIsDelete, NOT_DELETED)
                .orderByDesc(HttpCameraServer::getId)
                .list();
    }

    @Override
    public HttpCameraServer saveOrUpdate(HttpCameraServer server, String plainToken) {
        if (server.getId() != null && (plainToken == null || plainToken.trim().isEmpty())) {
            // 编辑时未传 Token，保留原 Token
            HttpCameraServer old = getById(server.getId());
            if (old != null) {
                server.setAuthToken(old.getAuthToken());
            }
        } else if (plainToken != null && !plainToken.trim().isEmpty()) {
            // 前端通过 RSA 公钥加密传入，先 RSA 解密再 AES 加密存储
            String decrypted = RsaCryptoUtil.decryptByPrivateKey(plainToken, rsaPrivateKey);
            server.setAuthToken(RtspPasswordCryptoUtil.encrypt(decrypted, aesKey));
        }
        if (server.getIsDelete() == null) {
            server.setIsDelete(NOT_DELETED);
        }
        super.saveOrUpdate(server);
        return server;
    }

    @Override
    public boolean softDelete(Long id) {
        return lambdaUpdate()
                .eq(HttpCameraServer::getId, id)
                .eq(HttpCameraServer::getIsDelete, NOT_DELETED)
                .set(HttpCameraServer::getIsDelete, DELETED)
                .update();
    }

    @Override
    public List<CameraInfo> listCameras(Long id) {
        HttpCameraServer server = getById(id);
        if (server == null || DELETED.equals(server.getIsDelete())) {
            return Collections.emptyList();
        }
        IDataSource ds = dataSourceFactory.create(server);
        return ds.listCameras();
    }

    @Override
    public byte[] captureSnapshot(Long serverId, String cameraId) {
        HttpCameraServer server = getById(serverId);
        if (server == null || DELETED.equals(server.getIsDelete())) {
            return null;
        }
        IDataSource ds = dataSourceFactory.create(server);
        return ds.captureSnapshot(cameraId);
    }

    @Override
    public List<byte[]> downloadSnapshots(Long serverId, String cameraId, boolean delRaw) {
        HttpCameraServer server = getById(serverId);
        if (server == null || DELETED.equals(server.getIsDelete())) {
            return Collections.emptyList();
        }
        IDataSource ds = dataSourceFactory.create(server);
        if (ds instanceof HttpCameraDataSourceAdapter) {
            return ((HttpCameraDataSourceAdapter) ds).downloadSnapshots(cameraId, delRaw);
        }
        byte[] one = ds.captureSnapshot(cameraId);
        if (one == null || one.length == 0) {
            return Collections.emptyList();
        }
        return Collections.singletonList(one);
    }
}
