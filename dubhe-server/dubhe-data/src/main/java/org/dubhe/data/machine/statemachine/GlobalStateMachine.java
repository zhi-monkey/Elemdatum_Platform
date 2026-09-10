
package org.dubhe.data.machine.statemachine;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @description 全局状态机
 * @date 2020-08-27
 */
@Data
@Component
public class GlobalStateMachine {

    /**
     * 数据状态机
     */
    private DataStateMachine dataStateMachine;

    /**
     * 文件状态机
     */
    private FileStateMachine fileStateMachine;

    /**
     * 模型服务状态机
     */
    private ModelStateMachine modelStateMachine;

    /**
     * 多人标注子任务状态机
     */
    private DataTeamSubtaskStateMachine dataTeamSubtaskStateMachine;

    /**
     * 多人标注任务状态机
     */
    private DataTeamTaskStateMachine dataTeamTaskStateMachine;
}
