package org.dlut.adv.mineai.model.statusMachine.statemachine;

import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.dlut.adv.mineai.model.statusMachine.state.specific.*;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 工厂模式，负责状态机的创建和管理
@Component
public class StateMachineFactory {
    /**
     * 创建任务成功
     */
    @Autowired
    private TrainCreateState trainCreateState;
    /**
     * 数据切分中类
     */
    @Autowired
    private SplittingState splittingState;
    /**
     * 排队中类
     */
    @Autowired
    private QueueingState queueingState;
    /**
     * 训练中类
     */
    @Autowired
    private TrainingState trainingState;
    /**
     * 训练成功类
     */
    @Autowired
    private TrainSucceededState trainSucceededState;
    /**
     * 子任务转换中类
     */
    @Autowired
    private SonConvertingstate sonConvertingstate;
    /**
     * 子任务转换中类
     */
    @Autowired
    private ConvertingState convertingState;;

    @Autowired
    private ModelJobRepo modelJobRepo;

    private final Map<String, ModelJobStateMachine> stateMachines = new ConcurrentHashMap<>();

    // 创建状态机
    public ModelJobStateMachine createStateMachine(String instanceName) {
        ModelJobStateMachine stateMachine = new ModelJobStateMachine();
        // 初始化状态机
        if (instanceName.contains("convert")) {
            stateMachine.setMemoryModelJobState(SpringContextHolder.getBean(ModelJobEnum.getStateMachine(10)));
        } else {
            stateMachine.setMemoryModelJobState(SpringContextHolder.getBean(ModelJobEnum.getStateMachine(0)));
        }
        stateMachine.setModelJobRepo(modelJobRepo);
        stateMachines.put(instanceName, stateMachine);
        return stateMachine;
    }

    // 获取状态机
    public ModelJobStateMachine getStateMachine(String instanceName) {
        return stateMachines.get(instanceName);
    }
}
