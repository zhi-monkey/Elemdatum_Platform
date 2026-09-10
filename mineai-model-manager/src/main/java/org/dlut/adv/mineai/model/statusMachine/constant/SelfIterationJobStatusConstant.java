package org.dlut.adv.mineai.model.statusMachine.constant;

/**
 * 自迭代训练子任务（一轮迭代）状态码常量
 *
 * @see org.dlut.adv.mineai.core.entity.SelfIterationJob
 */
public class SelfIterationJobStatusConstant {

    private SelfIterationJobStatusConstant() {
    }

    // ===================== 阶段常量 =====================

    /** 数据采集阶段 */
    public static final Integer PHASE_DATA_COLLECT = 1;
    /** 数据标注阶段（自动标注 + 可选人工审核）*/
    public static final Integer PHASE_DATA_ANNOTATE = 2;
    /** 模型训练阶段（训练 + 转换）*/
    public static final Integer PHASE_MODEL_TRAIN = 3;
    /** 模型下发阶段（预留）*/
    public static final Integer PHASE_MODEL_DEPLOY = 4;

    // ===================== 子任务状态码 =====================

    /** 已创建，待启动 */
    public static final Integer CREATED = 0;

    /** 数据采集中 */
    public static final Integer COLLECTING = 1;

    /** 数据采集失败 */
    public static final Integer COLLECT_FAILED = 11;

    /** 自动标注中 */
    public static final Integer AUTO_LABELING = 2;

    /** 等待人工审核（父任务同步进入 PAUSED）*/
    public static final Integer WAITING_REVIEW = 21;

    /** 标注失败 */
    public static final Integer ANNOTATE_FAILED = 22;

    /** 训练中 */
    public static final Integer TRAINING = 3;

    /** 转换中 */
    public static final Integer CONVERTING = 31;

    /** 训练/转换失败 */
    public static final Integer TRAIN_FAILED = 32;

    /** 打包中 */
    public static final Integer PACKAGING = 4;

    /** 打包失败 */
    public static final Integer PACKAGE_FAILED = 41;

    /** 待下发（预留，打包完成后进入）*/
    public static final Integer PENDING_DEPLOY = 5;

    /** 本轮完成 */
    public static final Integer COMPLETED = 6;

    /** 已取消 */
    public static final Integer CANCELLED = -10;
}
