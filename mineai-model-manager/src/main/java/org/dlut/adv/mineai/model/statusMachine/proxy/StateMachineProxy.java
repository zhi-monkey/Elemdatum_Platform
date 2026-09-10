package org.dlut.adv.mineai.model.statusMachine.proxy;

import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.dlut.adv.mineai.model.utils.stateMachine.StateChangeDTO;
import org.dlut.adv.mineai.model.utils.stateMachine.StateMachineProxyUtil;
import org.springframework.stereotype.Component;

/**
 * @description 代理执行状态机
 * @date
 */
@Component
public class StateMachineProxy {

    /**
     * 获取全局状态机Bean
     *
     * @return Object
     */
    public static Object getGlobalStateMachine() {
        return SpringContextHolder.getBean(ModelJobStateMachine.class);
    }

    /**
     * 代理执行单个状态机的状态切换
     *
     * @param stateChangeDTO 数据集状态切换信息
     */
    public static void proxyExecutionSingleState(StateChangeDTO stateChangeDTO) {
        StateMachineProxyUtil.proxyExecutionSingleState(stateChangeDTO,getGlobalStateMachine());
    }
}
