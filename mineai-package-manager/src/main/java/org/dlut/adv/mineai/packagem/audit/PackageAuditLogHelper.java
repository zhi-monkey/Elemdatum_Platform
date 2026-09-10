package org.dlut.adv.mineai.packagem.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.packagem.client.AuditLogFeign;
import org.dlut.adv.mineai.packagem.client.DubheAuthFeign;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageAuditLogHelper {

    private final DubheAuthFeign dubheAuthFeign;
    private final AuditLogFeign auditLogFeign;

    public void saveDownloadAuditLog(String description, HttpServletRequest request) {
        try {
            if (request == null) {
                return;
            }
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (token == null || token.isEmpty()) {
                return;
            }
            UserContext userContext = dubheAuthFeign.getCurUser(token);
            if (userContext == null || userContext.getUsername() == null) {
                return;
            }

            AuditLogModel auditLogModel = new AuditLogModel();
            auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
            auditLogModel.setOperationType(AuditLogConstants.OperationType.DOWNLOAD);
            auditLogModel.setUid(userContext.getId());
            auditLogModel.setUname(userContext.getUsername());
            auditLogModel.setCreateDate(new Date());
            auditLogModel.setIp(resolveIpAddress(request));
            auditLogModel.setRequestUri(request.getRequestURI());
            auditLogModel.setMethod(request.getMethod());
            auditLogModel.setExecutionTime(0L);
            auditLogModel.setDescription(description);
            auditLogModel.setParams(null);
            auditLogFeign.save(auditLogModel);
        } catch (Exception e) {
            log.warn("save package download audit log failed: description={}", description, e);
        }
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
