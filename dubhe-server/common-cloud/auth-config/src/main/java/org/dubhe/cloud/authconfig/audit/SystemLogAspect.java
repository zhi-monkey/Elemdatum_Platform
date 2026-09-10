package org.dubhe.cloud.authconfig.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.cloud.authconfig.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
    @Pointcut("@annotation(org.dubhe.cloud.authconfig.audit.SystemControllerLog)")
    public void controllerAspect(){}

    @Resource
    private AuditLogService auditLogService;

    @Resource
    private UserContextService userContextService;


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

        //获取用户信息
        UserContext userContext = userContextService.getCurUser();
        try {
            res = joinPoint.proceed();
            // 执行时长(毫秒)
            time = System.currentTimeMillis() - time;
        } catch (Throwable throwable){
            throwable.printStackTrace();

            AuditLogModel auditLogModel = new AuditLogModel();
            //方法有业务异常了
            auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.EXCEPTION);
            auditLogModel.setExceptionDesc(throwable.getMessage());

            if (userContext != null) {
                // Log对象封装值
                setLogValue(joinPoint, userContext, auditLogModel);
                // 插入异常记录
                auditLogService.saveAuditLog(auditLogModel);
            }
            throw throwable;
        }

        //方法正常执行完成了
        try {
            //方法执行完成后增加日志
            addAuditLog(joinPoint,time,userContext);
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
    public void addAuditLog(JoinPoint joinPoint, long time, UserContext userContext) {


        AuditLogModel auditLogModel = new AuditLogModel();
        //进入到这一方法中，说明请求异常没有抛出到这一层
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setExecutionTime(time);

        if (userContext != null) {
            // Log对象封装值
            setLogValue(joinPoint, userContext, auditLogModel);
            // 插入操作记录
            auditLogService.saveAuditLog(auditLogModel);
        }
    }


    /**
     * Log对象封装值
     */
    public void setLogValue(JoinPoint joinPoint, UserContext userContext, AuditLogModel auditLogModel){
        // 是否记录参数
        boolean recordParams = true;

        // 初始化默认值
        String ipAddress = "获取失败";
        String requestUri = "获取失败";
        String requestMethod = "获取失败";

        // 分步获取避免链式调用NPE
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            ipAddress = request.getRemoteAddr();
            requestUri = request.getRequestURI();
            requestMethod = request.getMethod();
        } else {
            logger.warn("无法获取请求属性，当前线程: {}", Thread.currentThread().getName());
        }

        //设置信息
        auditLogModel.setUname(userContext.getUsername());//用户名
        auditLogModel.setUid(userContext.getId().intValue()); //用户id
        auditLogModel.setCreateDate(new Date()); //创建时间
        auditLogModel.setIp(ipAddress); //请求ip
        auditLogModel.setRequestUri(requestUri); //请求路径
        auditLogModel.setMethod(requestMethod); //请求方法

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
    }

}

