

package org.dubhe.data.machine.state.specific.data;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.state.AbstractDataState;
import org.dubhe.data.machine.statemachine.DataStateMachine;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 压缩包导入中状态类
 * @date 2021-04-06
 */
@Component
public class ZipImportState extends AbstractDataState {

    @Autowired
    @Lazy
    private DataStateMachine dataStateMachine;

    @Autowired
    private StateIdentifyUtil stateIdentify;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 压缩包导入完成时间 导入中 --> 解析压缩包 --> 标注中
     *
     * @param dataset 数据集详情
     */
    @Override
    public void zipImportFinishEvent(Dataset dataset) {
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataStateMachine.getMemoryDataState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", dataset);
        DataStateEnum status = stateIdentify.getStatus(dataset.getId(),dataset.getCurrentVersionName(), true);
        switch (status){
            case MANUAL_ANNOTATION_STATE:
                //手动标注中
                dataStateMachine.doStateChange(dataset.getId(),DataStateEnum.MANUAL_ANNOTATION_STATE.getCode(),dataStateMachine.getAnnotationCompleteState());
                break;
            case ANNOTATION_COMPLETE_STATE:
                //标注完成
                dataStateMachine.doStateChange(dataset.getId(),DataStateEnum.ANNOTATION_COMPLETE_STATE.getCode(),dataStateMachine.getAnnotationCompleteState());
                break;
            case NOT_ANNOTATION_STATE:
                //未标注
                dataStateMachine.doStateChange(dataset.getId(),DataStateEnum.NOT_ANNOTATION_STATE.getCode(),dataStateMachine.getNotAnnotationState());
                break;
            default:
                throw new StateMachineException(ErrorMessageConstant.DATASET_CHANGE_ERR_MESSAGE);
        }
        LogUtil.info(LogEnum.STATE_MACHINE, " 【压缩包导入中】 执行事件后内存状态机的切换： {}", dataStateMachine.getMemoryDataState());
    }

    /**
     * 导入错误事件（与 DataStateMachineConstant.IMPORT_ERROR_EVENT 匹配）
     * @param dataset 数据集对象
     */
    @Override
    public void importErrorEvent(Dataset dataset) {
        LogUtil.info(LogEnum.STATE_MACHINE, " 【压缩包导入中】 触发错误事件，参数: {} ", dataset);
        // 1. 更新数据集状态为错误状态（需在 DataStateEnum 中定义 ERROR_STATE）
        dataStateMachine.doStateChange(
                dataset.getId(),
                (Integer) redisUtils.hget("datasetOriginStatus", String.valueOf(dataset.getId())),
                dataStateMachine.getErrorState()
        );
        LogUtil.info(LogEnum.STATE_MACHINE, " 数据集 {} 的状态已修改为： {}", dataset.getId(), redisUtils.hget("datasetOriginStatus", String.valueOf(dataset.getId())));
        // 2. 其他错误处理逻辑（如记录日志、清理资源等）
        System.err.println("数据集导入失败: " + dataset.getId());
    }

}
