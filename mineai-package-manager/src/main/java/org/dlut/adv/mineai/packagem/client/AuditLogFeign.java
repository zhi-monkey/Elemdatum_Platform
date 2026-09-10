package org.dlut.adv.mineai.packagem.client;

import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "mineai-model-manager")
public interface AuditLogFeign {

    @PostMapping("/auditLog/save")
    Msg<Void> save(@RequestBody AuditLogModel auditLogModel);
}
