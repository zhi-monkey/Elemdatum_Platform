package org.dubhe.cloud.authconfig.service;


import org.dubhe.cloud.authconfig.audit.AuditLogModel;

public interface AuditLogService {
    /**
     * 保存审计日志（自动处理新增/更新）
     * @param auditLog 审计日志实体
     * @return 保存后的实体（包含生成的主键）
     */
    void saveAuditLog(AuditLogModel auditLog);

}

