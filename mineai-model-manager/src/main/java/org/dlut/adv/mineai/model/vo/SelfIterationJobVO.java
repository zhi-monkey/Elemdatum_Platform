package org.dlut.adv.mineai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 自迭代训练子任务（一轮迭代）响应 VO
 */
@Data
public class SelfIterationJobVO {

    private Long id;

    /** 轮次编号（从 1 开始） */
    private int round;

    /** 当前阶段：1-数据采集 2-数据标注 3-模型训练 4-模型下发 */
    private int phase;

    /** 阶段中文标签 */
    private String phaseLabel;

    /** 当前状态码 */
    private int status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 本轮触发的 RtspCaptureExecution ID 列表 */
    private List<Long> captureExecutionIds;

    /** 本轮标注完成后发布的数据集版本 ID 列表 */
    private List<Long> datasetVersionIds;

    /** 训练阶段的 ModelJob ID */
    private Long trainJobId;

    /** 转换阶段的 ModelJob ID */
    private Long convertJobId;

    /** 打包任务 ID（预留） */
    private String packageTaskId;

    /** 训练精度指标 */
    private BigDecimal trainAccuracy;

    /** 训练召回率指标 */
    private BigDecimal trainRecall;

    /** 训练各 epoch 详情 JSON（前端自行解析） */
    private String epochDetail;

    private Date createTime;
    private Date updateTime;
}
