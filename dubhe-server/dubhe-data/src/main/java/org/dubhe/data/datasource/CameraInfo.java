package org.dubhe.data.datasource;

/**
 * 摄像头/通道摘要信息
 */
public class CameraInfo {

    /** 摄像头ID（RTSP场景下等同于数据源ID的字符串形式） */
    private String cameraId;

    /** 摄像头名称 */
    private String name;

    /** 摄像头所属的数据源ID */
    private Long sourceId;

    /** 数据源类型 */
    private DataSourceType sourceType;

    /** 描述/位置信息 */
    private String description;

    /** 视频流地址 */
    private String streamUrl;

    /** 状态描述（在线/离线等） */
    private String status;

    public CameraInfo() {
    }

    public CameraInfo(String cameraId, String name, Long sourceId, DataSourceType sourceType) {
        this.cameraId = cameraId;
        this.name = name;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public DataSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DataSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
