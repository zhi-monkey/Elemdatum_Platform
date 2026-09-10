package org.dubhe.cloud.authconfig.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dubhe.cloud.authconfig.audit.AuditLogModel;
import org.springframework.stereotype.Component;

@Component
public class AuditLogClientFallback implements AuditLogClient {

    protected static final Log logger = LogFactory.getLog(AuditLogClientFallback.class);
    @Override
    public void saveAuditLog(AuditLogModel auditLogModel) {
       logger.error("call saveAuditLog failed");
    }
}
