package org.dubhe.data.datasource;

/**
 * 数据源类型枚举
 */
public enum DataSourceType {

    /**
     * RTSP 视频流数据源
     */
    RTSP,

    /**
     * HTTP 摄像头服务器数据源（甲方提供的 HTTP 接口服务）
     */
    HTTP;

    public static DataSourceType of(String value) {
        if (value == null) {
            return RTSP;
        }
        for (DataSourceType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return RTSP;
    }
}
