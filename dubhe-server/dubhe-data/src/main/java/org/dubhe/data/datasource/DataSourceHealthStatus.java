package org.dubhe.data.datasource;

/**
 * 数据源健康状态
 */
public class DataSourceHealthStatus {

    /** 是否在线/可达 */
    private boolean online;

    /** 状态描述信息 */
    private String message;

    /** 延迟（毫秒），-1 表示未知 */
    private long latencyMs;

    public DataSourceHealthStatus() {
    }

    public DataSourceHealthStatus(boolean online, String message, long latencyMs) {
        this.online = online;
        this.message = message;
        this.latencyMs = latencyMs;
    }

    public static DataSourceHealthStatus online(long latencyMs) {
        return new DataSourceHealthStatus(true, "在线", latencyMs);
    }

    public static DataSourceHealthStatus offline(String reason) {
        return new DataSourceHealthStatus(false, reason, -1);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
