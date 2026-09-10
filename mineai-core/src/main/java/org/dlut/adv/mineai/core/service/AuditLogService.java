package org.dlut.adv.mineai.core.service;

import org.dlut.adv.mineai.core.dto.AuditLogQueryDTO;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

public interface AuditLogService {
    /**
     * 保存审计日志（自动处理新增/更新）
     * @param auditLog 审计日志实体
     * @return 保存后的实体（包含生成的主键）
     */
    void saveAuditLog(AuditLogModel auditLog);

    Page<AuditLogModel> getAuditLogsByPage(Pageable pageable, AuditLogQueryDTO queryDTO);

    /**
     * 导出审计日志（不分页，按时间从早到晚排序）
     * @param queryDTO 查询条件
     * @return 审计日志列表
     */
    List<AuditLogModel> exportAuditLogs(AuditLogQueryDTO queryDTO);

}

