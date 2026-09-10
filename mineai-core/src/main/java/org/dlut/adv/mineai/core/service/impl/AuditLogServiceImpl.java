package org.dlut.adv.mineai.core.service.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.dto.AuditLogQueryDTO;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.repository.AuditLogRepository;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    protected static final Log logger = LogFactory.getLog(AuditLogServiceImpl.class);


    // 构造器注入
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Async
    public void saveAuditLog(AuditLogModel auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public Page<AuditLogModel> getAuditLogsByPage(Pageable pageable, AuditLogQueryDTO queryDTO) {
        // 确保有默认排序
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createDate")
            );
        }

        // 构建查询条件
        Specification<AuditLogModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 操作人筛选
            if (StringUtils.isNotBlank(queryDTO.getUname())) {
                predicates.add(cb.like(root.get("uname"), "%" + queryDTO.getUname() + "%"));
            }

            // 操作类型筛选
            if (queryDTO.getOperationType() != null) {
                predicates.add(cb.equal(root.get("operationType"), queryDTO.getOperationType()));
            }

            // 状态筛选
            if (queryDTO.getRequestStatus() != null) {
                predicates.add(cb.equal(root.get("requestStatus"), queryDTO.getRequestStatus()));
            }

            // 时间范围筛选
            if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
                predicates.add(cb.between(root.get("createDate"),
                        queryDTO.getStartTime(),
                        queryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable);
    }

    @Override
    public List<AuditLogModel> exportAuditLogs(AuditLogQueryDTO queryDTO) {
        // 构建查询条件
        Specification<AuditLogModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 操作类型筛选（支持多个，用逗号分隔）
            if (StringUtils.isNotBlank(queryDTO.getOperationTypes())) {
                String[] typeArray = queryDTO.getOperationTypes().split(",");
                List<Integer> typeList = Arrays.stream(typeArray)
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                predicates.add(root.get("operationType").in(typeList));
            }

            // 操作人筛选（精确匹配）
            if (StringUtils.isNotBlank(queryDTO.getUname())) {
                predicates.add(cb.equal(root.get("uname"), queryDTO.getUname()));
            }

            // IP地址筛选（精确匹配）
            if (StringUtils.isNotBlank(queryDTO.getIp())) {
                predicates.add(cb.equal(root.get("ip"), queryDTO.getIp()));
            }

            // 操作描述筛选（模糊匹配）
            if (StringUtils.isNotBlank(queryDTO.getDescription())) {
                predicates.add(cb.like(root.get("description"), "%" + queryDTO.getDescription() + "%"));
            }

            // 状态筛选
            if (queryDTO.getRequestStatus() != null) {
                predicates.add(cb.equal(root.get("requestStatus"), queryDTO.getRequestStatus()));
            }

            // 时间范围筛选
            if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
                predicates.add(cb.between(root.get("createDate"),
                        queryDTO.getStartTime(),
                        queryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 按时间从早到晚排序（ASC）
        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");

        return auditLogRepository.findAll(spec, sort);
    }
}
