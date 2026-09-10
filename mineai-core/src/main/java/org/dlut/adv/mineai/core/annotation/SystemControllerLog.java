package org.dlut.adv.mineai.core.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemControllerLog {

    /**
     * 描述业务方法 例：Xxx管理-执行Xxx操作
     * @return
     */
    // 方法描述
    String description() default "";

    // 是否需要记录方法参数
    boolean recordParams() default true;

    // 方法类型（0-新增，1-修改，2-删除）
    int operationType();
}

//使用例
// @SystemControllerLog(description = "数据集查询请求-测试用",recordParams = true, operationType = AuditLogConstants.OperationType.QUERY)
// 加在需要使用的Controller层方法上，description里面写上这个方法的描述， recordParams 可以选择true或false，决定是否记录本次请求的参数，后面的operationType用于记录这次请求的类型。需要和接口匹配

