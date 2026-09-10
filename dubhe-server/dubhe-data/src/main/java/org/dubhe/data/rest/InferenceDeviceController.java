package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.InferenceDeviceListDTO;
import org.dubhe.data.domain.entity.InferenceDevice;
import org.dubhe.data.service.InferenceDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "数据处理：算力推理设备")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/inference-devices")
public class InferenceDeviceController {

    @Autowired
    private InferenceDeviceService inferenceDeviceService;

    @ApiOperation(value = "算力推理设备分页查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<InferenceDeviceListDTO>> page(Page<InferenceDevice> page,
                                                                @RequestParam(required = false) String deviceName) {
        return new DataResponseBody<>(inferenceDeviceService.page(page, deviceName));
    }

    @ApiOperation(value = "新增算力推理设备")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<InferenceDevice> create(@RequestBody InferenceDeviceSaveDTO dto) {
        InferenceDevice inferenceDevice = buildEntity(dto);
        return new DataResponseBody<>(inferenceDeviceService.saveOrUpdate(inferenceDevice, dto.getAuthCode()));
    }

    @ApiOperation(value = "更新算力推理设备")
    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<InferenceDevice> update(@PathVariable Long id, @RequestBody InferenceDeviceSaveDTO dto) {
        InferenceDevice inferenceDevice = buildEntity(dto);
        inferenceDevice.setId(id);
        return new DataResponseBody<>(inferenceDeviceService.saveOrUpdate(inferenceDevice, dto.getAuthCode()));
    }

    @ApiOperation(value = "删除算力推理设备")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> delete(@PathVariable Long id) {
        return new DataResponseBody<>(inferenceDeviceService.removeById(id));
    }

    private InferenceDevice buildEntity(InferenceDeviceSaveDTO dto) {
        InferenceDevice inferenceDevice = new InferenceDevice();
        inferenceDevice.setId(dto.getId());
        inferenceDevice.setDeviceName(dto.getDeviceName());
        inferenceDevice.setInferenceIp(dto.getInferenceIp());
        inferenceDevice.setInferencePort(dto.getInferencePort());
        inferenceDevice.setAuthCode(dto.getAuthCode());
        inferenceDevice.setRemark(dto.getRemark());
        return inferenceDevice;
    }

    @Data
    public static class InferenceDeviceSaveDTO {
        private Long id;
        private String deviceName;
        private String inferenceIp;
        private Integer inferencePort;
        private String authCode;
        private String remark;
    }
}
