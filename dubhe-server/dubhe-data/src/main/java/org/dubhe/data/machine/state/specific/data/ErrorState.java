package org.dubhe.data.machine.state.specific.data;

import org.dubhe.data.machine.state.AbstractDataState;
import org.dubhe.data.machine.statemachine.DataStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author mingming
 */
@Component
public class ErrorState extends AbstractDataState {

    @Autowired
    @Lazy
    private DataStateMachine dataStateMachine;

    /**
     * 错误状态下的处理逻辑（如重试、回滚等）
     */
    // public void handleError(Dataset dataset) {
    //     // 实现具体错误恢复逻辑
    // }
}