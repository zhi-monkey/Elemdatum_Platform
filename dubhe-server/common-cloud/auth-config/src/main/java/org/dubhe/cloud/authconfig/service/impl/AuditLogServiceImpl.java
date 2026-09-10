package org.dubhe.cloud.authconfig.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dubhe.cloud.authconfig.audit.AuditLogModel;
import org.dubhe.cloud.authconfig.service.AuditLogClient;
import org.dubhe.cloud.authconfig.service.AuditLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    protected static final Log logger = LogFactory.getLog(AuditLogServiceImpl.class);

    @Resource
    private AuditLogClient auditLogClient;

    @Override
    @Async
    public void saveAuditLog(AuditLogModel auditLog) {
        try {
            auditLogClient.saveAuditLog(auditLog);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
