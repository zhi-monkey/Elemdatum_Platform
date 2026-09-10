package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.domain.entity.HttpCameraServer;

import java.util.List;

/**
 * HTTP摄像头服务器 Service
 */
public interface HttpCameraServerService extends IService<HttpCameraServer> {

    /**
     * 分页查询（不含已删除）
     */
    Page<HttpCameraServer> page(Page<HttpCameraServer> page, HttpCameraServer query);

    /**
     * 获取所有可用（未删除）的服务器列表
     */
    List<HttpCameraServer> findAllAvailable();

    /**
     * 新增或更新（Token 加密存储）
     *
     * @param server      实体
     * @param plainToken  明文 Token（RSA 密文，由前端加密传入），为空则保持原 Token
     */
    HttpCameraServer saveOrUpdate(HttpCameraServer server, String plainToken);

    /**
     * 软删除
     */
    boolean softDelete(Long id);

    /**
     * 获取指定服务器下的摄像头列表（调用甲方 HTTP 接口）
     */
    List<CameraInfo> listCameras(Long id);

    /**
     * 获取指定摄像头的截图字节（JPEG）
     *
     * @param serverId 服务器 ID
     * @param cameraId 摄像头 ID
     */
    byte[] captureSnapshot(Long serverId, String cameraId);

    /**
     * 下载并解压摄像机图片包
     *
     * @param serverId 服务器ID
     * @param cameraId 摄像机ID（甲方 taskid）
     * @param delRaw   是否删除甲方原始图片
     */
    List<byte[]> downloadSnapshots(Long serverId, String cameraId, boolean delRaw);
}
