package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.data.dao.InferenceDeviceMapper;
import org.dubhe.data.domain.dto.InferenceDeviceListDTO;
import org.dubhe.data.domain.entity.InferenceDevice;
import org.dubhe.data.service.InferenceDeviceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class InferenceDeviceServiceImpl extends ServiceImpl<InferenceDeviceMapper, InferenceDevice>
        implements InferenceDeviceService {

    @Autowired
    private UserContextService userContextService;

    @Override
    public Page<InferenceDeviceListDTO> page(Page<InferenceDevice> page, String deviceName) {
        LambdaQueryWrapper<InferenceDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(deviceName != null && !deviceName.trim().isEmpty(),
                        InferenceDevice::getDeviceName,
                        deviceName)
                .orderByDesc(InferenceDevice::getCreatedAt)
                .orderByDesc(InferenceDevice::getId);

        Page<InferenceDevice> entityPage = page(page, wrapper);

        Page<InferenceDeviceListDTO> dtoPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );

        List<InferenceDeviceListDTO> records = entityPage.getRecords()
                .stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());

        dtoPage.setRecords(records);
        return dtoPage;
    }

    @Override
    public InferenceDevice saveOrUpdate(InferenceDevice inferenceDevice, String authCode) {
        InferenceDevice old = null;
        if (inferenceDevice.getId() != null && (authCode == null || authCode.trim().isEmpty())) {
            old = getById(inferenceDevice.getId());
            if (old != null) {
                inferenceDevice.setAuthCode(old.getAuthCode());
            }
        } else if (authCode != null && !authCode.trim().isEmpty()) {
            inferenceDevice.setAuthCode(authCode.trim());
        }

        if (inferenceDevice.getId() == null) {
            UserContext curUser = userContextService.getCurUser();
            if (curUser != null && curUser.getUsername() != null) {
                inferenceDevice.setCreatedBy(curUser.getUsername());
            }
        } else {
            if (old == null) {
                old = getById(inferenceDevice.getId());
            }
            if (old != null) {
                inferenceDevice.setCreatedBy(old.getCreatedBy());
            }
        }

        super.saveOrUpdate(inferenceDevice);
        return inferenceDevice;
    }

    private InferenceDeviceListDTO toListDTO(InferenceDevice entity) {
        InferenceDeviceListDTO dto = new InferenceDeviceListDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setDeviceStatus(null);
        dto.setLoadedAlgorithmApp(null);
        return dto;
    }
}
