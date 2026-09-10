package org.dubhe.cloud.authconfig.audit;

import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.cloud.authconfig.service.AuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class AuditLogHelper {

    @Resource
    private AuditLogService auditLogService;

    @Resource
    private UserContextService userContextService;

    public void saveUpdateAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.UPDATE);
    }

    public void saveDownloadAuditLog(String description) {
        saveAuditLog(description, null, AuditLogConstants.OperationType.DOWNLOAD);
    }

    public void saveAuditLog(String description, String params, int operationType) {
        UserContext userContext = userContextService.getCurUser();
        if (userContext == null) {
            return;
        }

        String ipAddress = "unavailable";
        String requestUri = "unavailable";
        String requestMethod = "unavailable";
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            ipAddress = request.getRemoteAddr();
            requestUri = request.getRequestURI();
            requestMethod = request.getMethod();
        }

        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(operationType);
        auditLogModel.setUid(userContext.getId().intValue());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(ipAddress);
        auditLogModel.setRequestUri(requestUri);
        auditLogModel.setMethod(requestMethod);
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }
}
