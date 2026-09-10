package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.data.dao.HttpCameraMapper;
import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.datasource.DataSourceFactory;
import org.dubhe.data.datasource.IDataSource;
import org.dubhe.data.domain.dto.HttpCameraSyncResult;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.domain.entity.HttpCameraServer;
import org.dubhe.data.service.HttpCameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(rollbackFor = Exception.class)
public class HttpCameraServiceImpl extends ServiceImpl<HttpCameraMapper, HttpCamera> implements HttpCameraService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Override
    public List<HttpCamera> listByServerId(Long serverId) {
        return lambdaQuery()
                .eq(HttpCamera::getHttpCameraServerId, serverId)
                .eq(HttpCamera::getIsDelete, NOT_DELETED)
                .orderByAsc(HttpCamera::getId)
                .list();
    }

    @Override
    public List<HttpCamera> listAllAvailable() {
        return lambdaQuery()
                .eq(HttpCamera::getIsDelete, NOT_DELETED)
                .orderByAsc(HttpCamera::getHttpCameraServerId)
                .orderByAsc(HttpCamera::getId)
                .list();
    }

    @Override
    public HttpCamera findAvailableById(Long id) {
        return lambdaQuery()
                .eq(HttpCamera::getId, id)
                .eq(HttpCamera::getIsDelete, NOT_DELETED)
                .one();
    }

    @Override
    public HttpCameraSyncResult syncFromServer(HttpCameraServer server) {
        IDataSource dataSource = dataSourceFactory.create(server);
        List<CameraInfo> remoteCameras = dataSource.listCameras();

        List<HttpCamera> localCameras = listByServerId(server.getId());
        Map<String, HttpCamera> localMap = new HashMap<>();
        for (HttpCamera localCamera : localCameras) {
            localMap.put(localCamera.getCameraId(), localCamera);
        }

        int addedCount = 0;
        int updatedCount = 0;
        int offlineCount = 0;

        LocalDateTime now = LocalDateTime.now();
        Set<String> seenCameraIds = new HashSet<>();
        for (CameraInfo remote : remoteCameras) {
            String remoteCameraId = remote.getCameraId() == null ? "" : remote.getCameraId().trim();
            if (remoteCameraId.isEmpty()) {
                continue;
            }
            seenCameraIds.add(remoteCameraId);
            HttpCamera existing = localMap.get(remoteCameraId);
            if (existing == null) {
                HttpCamera camera = new HttpCamera();
                camera.setHttpCameraServerId(server.getId());
                camera.setCameraId(remoteCameraId);
                camera.setName(remote.getName());
                camera.setStreamUrl(remote.getStreamUrl());
                camera.setDescription(remote.getDescription());
                camera.setStatus(normalizeStatus(remote.getStatus()));
                camera.setLastSeenTime(now);
                camera.setIsDelete(NOT_DELETED);
                camera.setCreateTime(now);
                camera.setUpdateTime(now);
                save(camera);
                addedCount++;
            } else {
                existing.setName(remote.getName());
                existing.setStreamUrl(remote.getStreamUrl());
                existing.setDescription(remote.getDescription());
                existing.setStatus(normalizeStatus(remote.getStatus()));
                existing.setLastSeenTime(now);
                existing.setUpdateTime(now);
                updateById(existing);
                updatedCount++;
            }
        }

        for (HttpCamera local : localCameras) {
            if (!seenCameraIds.contains(local.getCameraId())) {
                local.setStatus("OFFLINE");
                local.setUpdateTime(now);
                updateById(local);
                offlineCount++;
            }
        }

        HttpCameraSyncResult result = new HttpCameraSyncResult();
        result.setAddedCount(addedCount);
        result.setUpdatedCount(updatedCount);
        result.setOfflineCount(offlineCount);
        result.setTotalCount(remoteCameras == null ? 0 : remoteCameras.size());
        return result;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String value = status.trim().toUpperCase();
        if ("ONLINE".equals(value) || "OFFLINE".equals(value) || "UNKNOWN".equals(value)
                || "INACTIVE".equals(value) || "DELETED".equals(value)) {
            return value;
        }
        return status.trim();
    }
}
