package org.dlut.adv.mineai.model.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.dto.AuditLogQueryDTO;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auditLog")
public class AuditLogController {
    @Resource
    private AuditLogService auditLogService;


    @PostMapping("/save")
    public Msg<Void> save(@RequestBody AuditLogModel auditLogModel) {
        try {
            auditLogService.saveAuditLog(auditLogModel);
            return new Msg<>(MsgCode.SUCCEED);
        } catch (Exception e) {
            return new Msg<>(MsgCode.APPLICATION_IS_BINDED);
        }
    }

    @ApiOperation(value = "审计日志分页列表")
    @GetMapping(value = "/query")
    public DataResponseBody query(
            @PageableDefault(sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            AuditLogQueryDTO queryDTO) {  // 改为使用 DTO
        return new DataResponseBody(auditLogService.getAuditLogsByPage(pageable, queryDTO));
    }

    @ApiOperation(value = "导出审计日志")
    @GetMapping(value = "/export")
    public Msg<List<AuditLogModel>> export(AuditLogQueryDTO queryDTO) {
        List<AuditLogModel> list = auditLogService.exportAuditLogs(queryDTO);
        return new Msg<>(MsgCode.SUCCEED, list);
    }
}
