package org.dlut.adv.mineai.model.vo;

import lombok.Data;

/**
 * RtspCaptureExecution 查询结果 VO（dubhe-data 侧回流执行记录）
 */
@Data
public class RtspCaptureExecutionVO {

    private Long id;

    /** 关联的父回流任务 ID */
    private Long taskId;

    /** 本次采集自动创建的数据集 ID */
    private Long datasetId;

    /**
     * 执行状态：0-排队中, 1-运行中, 2-成功, 3-失败, 4-已手动取消
     */
    private Integer status;

    private Integer capturedCount;

    private String message;
}
