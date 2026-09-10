package org.dubhe.cloud.authconfig.service;

import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = ApplicationNameConst.MODEL_MANAGER,fallback = AuditLogClientFallback.class)
public interface AuditLogClient {

    @PostMapping("/auditLog/save")
    void saveAuditLog(@RequestBody AuditLogModel auditLogModel);

}


