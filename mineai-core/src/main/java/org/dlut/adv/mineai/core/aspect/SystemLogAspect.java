package org.dlut.adv.mineai.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
public class SystemLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogAspect.class);
    private static final int MAX_LENGTH_TO_RECORD_PARAMS = 1000;

    /**
     * Controller层切点 注解拦截
     */
    @Pointcut("@annotation(org.dlut.adv.mineai.core.annotation.SystemControllerLog)")
    public void controllerAspect(){}

    @Resource
    private AuditLogService auditLogService;


    /**
     * 环绕增强
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("controllerAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable{

        Object res = null;
        long time = System.currentTimeMillis();
        AuditLogModel auditLogModel = new AuditLogModel();
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        MethodSignature debugSignature = (MethodSignature) joinPoint.getSignature();
        Method debugMethod = debugSignature.getMethod();
        SystemControllerLog debugControllerLog = debugMethod.getAnnotation(SystemControllerLog.class);
        logger.info("audit around enter, class={}, method={}, description={}, userContextPresent={}, requestInfoPresent={}",
                joinPoint.getTarget().getClass().getName(),
                debugMethod.getName(),
                debugControllerLog != null ? debugControllerLog.description() : null,
                userContext != null,
                requestInfoContext != null);
        try {
            res = joinPoint.proceed();
            // 执行时长(毫秒)
            time = System.currentTimeMillis() - time;
        } catch (Throwable throwable){
            throwable.printStackTrace();
            //方法有业务异常了
            auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.EXCEPTION);
            auditLogModel.setExceptionDesc(throwable.getMessage());

            if (userContext != null) {
                // Log对象封装值
                setLogValue(joinPoint, userContext, requestInfoContext, auditLogModel);
                // 插入异常记录
                auditLogService.saveAuditLog(auditLogModel);
            }
            throw throwable;
        }

        //方法正常执行完成了
        try {
            //方法执行完成后增加日志
            addAuditLog(joinPoint, time, userContext, requestInfoContext, res);
        }catch (Exception e){
            System.out.println("LogAspect 操作失败：" + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }



    /**
     * 插入操作记录
     * @param joinPoint
     * @param time
     */
    public void addAuditLog(JoinPoint joinPoint, long time, UserContext userContext, RequestInfoContext requestInfoContext, Object result) {

        AuditLogModel auditLogModel = new AuditLogModel();
        //进入到这一方法中，说明请求异常没有抛出到这一层
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setExecutionTime(time);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SystemControllerLog controllerLog = method.getAnnotation(SystemControllerLog.class);
        logger.info("audit addAuditLog, class={}, method={}, description={}, userContextPresent={}, requestInfoPresent={}, timeMs={}",
                joinPoint.getTarget().getClass().getName(),
                method.getName(),
                controllerLog != null ? controllerLog.description() : null,
                userContext != null,
                requestInfoContext != null,
                time);

        if (userContext != null) {
            // Log对象封装值
            setLogValue(joinPoint, userContext,requestInfoContext, auditLogModel);
            overrideParamsForSpecialCases(auditLogModel, result);
            // 插入操作记录
            auditLogService.saveAuditLog(auditLogModel);
        } else {
            logger.warn("audit skipped because userContext is null, class={}, method={}",
                    joinPoint.getTarget().getClass().getName(), method.getName());
        }
    }


    /**
     * Log对象封装值
     */
    public void setLogValue(JoinPoint joinPoint, UserContext userContext,RequestInfoContext requestInfoContext, AuditLogModel auditLogModel){
        // 是否记录参数
        boolean recordParams = true;

        //设置信息
        auditLogModel.setUname(userContext.getUsername());//用户名
        auditLogModel.setUid(userContext.getId()); //用户id
        auditLogModel.setCreateDate(new Date()); //创建时间
        auditLogModel.setIp(requestInfoContext.getIpAddress()); //请求ip
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri()); //请求路径
        auditLogModel.setMethod(requestInfoContext.getRequestMethod()); //请求方法

        // 用反射获取注解中的信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SystemControllerLog controllerLog = method.getAnnotation(SystemControllerLog.class);
        if (controllerLog != null) {
            // 请求方法上的注解
            String description = controllerLog.description();
            recordParams = controllerLog.recordParams();
            auditLogModel.setDescription(description);
            auditLogModel.setOperationType(controllerLog.operationType());
        }

        // 请求方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        auditLogModel.setMethod(className + "." + methodName);
        // 请求方法参数值
        Object[] args = joinPoint.getArgs();

        // 请求方法参数名称
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = u.getParameterNames(method);
        if (args != null && paramNames != null && recordParams) {
            String params = "";
            for (int i = 0; i < args.length; i++) {
                params += "  " + paramNames[i] + ": " + args[i];
            }
            // 长度超过1000字符串的大参数也不记录
            if (params.length() <= MAX_LENGTH_TO_RECORD_PARAMS) {
                auditLogModel.setParams(params);
            }
        }
        logger.info("audit model prepared, description={}, requestUri={}, method={}, uname={}",
                auditLogModel.getDescription(), auditLogModel.getRequestUri(), auditLogModel.getMethod(), auditLogModel.getUname());
    }

    private void overrideParamsForSpecialCases(AuditLogModel auditLogModel, Object result) {
        if (auditLogModel == null || auditLogModel.getDescription() == null) {
            return;
        }
        if (!"self_iteration_add".equals(auditLogModel.getDescription())) {
            return;
        }
        String taskIdParam = extractTaskIdParam(result);
        auditLogModel.setParams(taskIdParam);
        logger.info("audit params overridden for self_iteration_add, params={}", taskIdParam);
    }

    private String extractTaskIdParam(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getPayload = result.getClass().getMethod("getPayload");
            Object payload = getPayload.invoke(result);
            if (payload == null) {
                return null;
            }
            Method getId = payload.getClass().getMethod("getId");
            Object taskId = getId.invoke(payload);
            return taskId == null ? null : "  taskId: " + taskId;
        } catch (Exception e) {
            logger.warn("failed to extract taskId for self_iteration_add params: {}", e.getMessage());
            return null;
        }
    }

}

