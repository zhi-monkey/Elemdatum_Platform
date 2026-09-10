package org.dubhe.data.capture;

public enum CaptureExecutionStatus {
    QUEUED(0, "排队中"),
    RUNNING(1, "运行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String label;

    CaptureExecutionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static CaptureExecutionStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CaptureExecutionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
