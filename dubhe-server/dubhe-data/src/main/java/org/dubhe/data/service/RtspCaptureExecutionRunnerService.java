package org.dubhe.data.service;

public interface RtspCaptureExecutionRunnerService {

    /**
     * 异步触发采集执行
     */
    void triggerAsync(Long executionId);

    void triggerAsync(Long executionId, boolean delRaw);
}
