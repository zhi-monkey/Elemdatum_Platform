package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RequestInfoContext;
import org.dlut.adv.mineai.core.entity.RequestInfoContextHolder;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.ChipUpdateDTO;
import org.dlut.adv.mineai.model.service.ChipService;
import org.dlut.adv.mineai.model.service.impl.ChipServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chips")
@Api(tags = "关于芯片的请求控制器")
public class ChipController {

    @Resource
    private ChipService chipService;
    @Resource
    private AuditLogService auditLogService;

    @GetMapping
    @ApiOperation(value = "查询所有的芯片信息")
    public Msg<List<Chip>> getChips() {
        return new Msg<>(MsgCode.SUCCEED, chipService.findAll());
    }

    @GetMapping("/findAll")
    public Msg<List<Chip>> getAllChips(Pageable pageable) {
        List<Chip> chipPage = chipService.findAll();
        return new Msg<>(MsgCode.SUCCEED, chipPage);
    }

    @PostMapping("/save")
    public Msg<Chip> createOrUpdateChip(@RequestBody ChipUpdateDTO chipUpdateDTO) {
        boolean isCreate = chipUpdateDTO.getId() == null;
        Chip chip = new Chip();
        chip.setChipType(chipUpdateDTO.getChipType());
        // 前端已有唯一性校验, 此处仅为防止意外
        if (this.checkChipTypeUnique(chip).getPayload()) {
            return new Msg<>(MsgCode.CHIP_TYPE_EXIST);
        }
        chip.setId(chipUpdateDTO.getId());
        Chip savedChip = chipService.createOrUpdateChip(chip);
        if (isCreate) {
            saveAddAuditLog("chip_add", "  chipType: " + chipUpdateDTO.getChipType());
        } else {
            saveUpdateAuditLog("chip_update",
                    "  chipId: " + chipUpdateDTO.getId() + "  chipType: " + chipUpdateDTO.getChipType());
        }
        return new Msg<>(MsgCode.SUCCEED, savedChip);
    }

    private void saveAddAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.ADD);
    }

    private void saveUpdateAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.UPDATE);
    }

    private void saveAuditLog(String description, String params, int operationType) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            return;
        }
        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(operationType);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new java.util.Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }

    @GetMapping("/options")
    public List<ChipServiceImpl.ChipOption> getChipOptions() {
        return chipService.getAllChipOptions();
    }

    @DeleteMapping("/delete/{id}")
    @SystemControllerLog(description = "chip_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<Void> deleteChip(@PathVariable Long id) {
//        chipService.deleteById(id);
//        return new Msg<>(MsgCode.SUCCEED, null);
        try {
            chipService.deleteById(id);
        }catch (Exception e){
            return new Msg<>(MsgCode.CHIP_IS_BINDED);
        }
        return new Msg<>(MsgCode.SUCCEED, null);
    }

    /**
     * 分页查询查询芯片信息
     *
     * @param page
     * @param order
     * @param pageSize
     * @param chipType
     * @return
     */
    @GetMapping("/dynamicFindChips")
    public Msg<Page<Chip>> dynamicFindChips(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(defaultValue = "15") int pageSize,
                                            @RequestParam(required = false) String chipType) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<Chip> chipsPage = chipService.findChipsByType(chipType, pageable);
        return new Msg<>(MsgCode.SUCCEED, chipsPage);
    }

    @GetMapping("/findChipsById/{id}")
    public Msg<Chip> findChipById(@PathVariable Long id) {
        Chip findChip =chipService.getChipById(id);
        return new Msg<>(MsgCode.SUCCEED,findChip);
    }

    @PostMapping("checkChipTypeUnique")
    public Msg<Boolean> checkChipTypeUnique(@RequestBody Chip chip) {
        Chip findChip = chipService.findByChipType(chip.getChipType());
        if (findChip != null) {
            return new Msg<>(MsgCode.SUCCEED, true);
        }
        return new Msg<>(MsgCode.SUCCEED, false);
    }


    @GetMapping("/chipCount")
    public Msg<Integer> chipCount() {
        Integer findChip =chipService.chipCount();
        return new Msg<>(MsgCode.SUCCEED,findChip);
    }
}
