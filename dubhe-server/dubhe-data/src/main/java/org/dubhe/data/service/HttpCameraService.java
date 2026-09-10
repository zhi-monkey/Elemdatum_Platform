package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.HttpCameraSyncResult;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.domain.entity.HttpCameraServer;

import java.util.List;

/**
 * HTTP 摄像机 Service
 */
public interface HttpCameraService extends IService<HttpCamera> {

    List<HttpCamera> listByServerId(Long serverId);

    List<HttpCamera> listAllAvailable();

    HttpCamera findAvailableById(Long id);

    HttpCameraSyncResult syncFromServer(HttpCameraServer server);
}
