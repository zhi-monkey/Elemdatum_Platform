
package org.dubhe.data.machine.utils;


import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.data.machine.proxy.StateMachineProxy;

import java.util.List;

/**
 * @description 状态机工具类   业务层注入此类调用代理方法
 * @date 2020-08-27
 */
public class StateMachineUtil {

    /**
     * 执行单个状态机的状态切换
     *
     * @param stateChangeDTO 状态切换信息
     */
    public static void stateChange(StateChangeDTO stateChangeDTO) {
        StateMachineProxy.proxyExecutionSingleState(stateChangeDTO);
    }

    /**
     * 执行关联状态机的状态切换
     *
     * @param stateChangeDTOList 状态切换信息
     */
    public static void stateChange(List<StateChangeDTO> stateChangeDTOList) {
        StateMachineProxy.proxyExecutionRelationState(stateChangeDTOList);
    }

}
