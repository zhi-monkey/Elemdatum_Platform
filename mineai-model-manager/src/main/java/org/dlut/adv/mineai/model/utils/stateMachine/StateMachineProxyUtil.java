
package org.dlut.adv.mineai.model.utils.stateMachine;

import org.apache.commons.lang3.StringUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description 代理执行状态机
 * @date 2020-08-27
 */
@Component
public class StateMachineProxyUtil {

    /**
     * 代理执行单个状态机的状态切换
     *
     * @param stateChangeDTO 数据集状态切换信息
     * @param objectService  服务类
     */
    public static void proxyExecutionSingleState(StateChangeDTO stateChangeDTO, Object objectService) {
        //获取需要执行的状态机对象
        Object stateMachineObject = SpringContextHolder.getBean(Introspector.decapitalize(objectService.getClass().getSimpleName()));
        try {
            //获取目标执行方法的参数类型
            List<Class<?>> paramTypesList = getMethodParamTypes(stateMachineObject, stateChangeDTO.getEventMethodName());
            //构造目标执行方法
            Method method = ReflectionUtils.findMethod(stateMachineObject.getClass(), stateChangeDTO.getEventMethodName(), paramTypesList.toArray(new Class[paramTypesList.size()]));
            if (stateChangeDTO.getObjectParam().length != paramTypesList.size()) {
                System.out.println("Target execution method parameters {} Inconsistent with the number of incoming "+paramTypesList.size()+stateChangeDTO.getObjectParam().length);
            } else {
                ReflectionUtils.invokeMethod(method, stateMachineObject, stateChangeDTO.getObjectParam());
            }
        } catch (ClassNotFoundException e) {
            System.out.println("The specified class was not found "+e);
            throw new RuntimeException("The specified class was not found");
        }
    }

    /**
     * 根据方法名获取所有参数的类型
     *
     * @param classInstance 类实例
     * @param methodName    方法名
     * @return List<Class<T>> 对象集合
     * @throws ClassNotFoundException
     */
    public static List<Class<?>> getMethodParamTypes(Object classInstance, String methodName) throws ClassNotFoundException {
        List<Class<?>> paramTypes = new ArrayList<>();
        Method[] methods = classInstance.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> classParamType : params) {
                    paramTypes.addAll(Collections.singleton((Class<?>) Class.forName(classParamType.getName())));
                }
                break;
            }
        }
        return paramTypes;
    }

}
