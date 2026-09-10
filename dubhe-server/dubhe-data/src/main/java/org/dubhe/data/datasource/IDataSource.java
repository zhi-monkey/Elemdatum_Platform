package org.dubhe.data.datasource;

import java.util.List;

/**
 * 数据源抽象接口 - 统一描述不同类型数据源（RTSP流、HTTP摄像头服务器等）的能力
 * <p>
 * 实现类：
 * <ul>
 *   <li>{@link org.dubhe.data.datasource.impl.RtspDataSourceAdapter} - RTSP视频流数据源适配器</li>
 *   <li>{@link org.dubhe.data.datasource.impl.HttpCameraDataSourceAdapter} - HTTP摄像头服务器数据源适配器</li>
 * </ul>
 */
public interface IDataSource {

    /**
     * 获取该数据源的唯一标识ID（对应数据库主键）
     */
    Long getId();

    /**
     * 获取数据源名称
     */
    String getName();

    /**
     * 获取数据源类型
     */
    DataSourceType getSourceType();

    /**
     * 获取数据源描述
     */
    String getDescription();

    /**
     * 检查数据源健康状态（连通性检查）
     *
     * @return 健康状态对象
     */
    DataSourceHealthStatus checkHealth();

    /**
     * 获取该数据源下的摄像头/通道列表（摘要信息）
     * <p>
     * RTSP场景：返回单个自身（RTSP地址即为摄像头）<br>
     * HTTP服务器场景：调用对方接口获取摄像头列表
     *
     * @return 摄像头摘要列表
     */
    List<CameraInfo> listCameras();

    /**
     * 获取指定摄像头的实时截图（字节数组，JPEG格式）
     * <p>
     * RTSP场景：通过 ZLMediaKit 截图接口获取<br>
     * HTTP服务器场景：调用对方截图接口
     *
     * @param cameraId 摄像头ID（RTSP场景可传 null，HTTP场景传摄像头ID）
     * @return JPEG 图片字节数组，失败返回 null
     */
    byte[] captureSnapshot(String cameraId);

    /**
     * 获取指定摄像头的流地址（供后续回流任务使用）
     *
     * @param cameraId 摄像头ID（RTSP场景可传 null）
     * @return 流地址字符串
     */
    String getStreamUrl(String cameraId);
}
