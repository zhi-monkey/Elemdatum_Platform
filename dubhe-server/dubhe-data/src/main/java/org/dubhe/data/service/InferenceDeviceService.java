package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.InferenceDeviceListDTO;
import org.dubhe.data.domain.entity.InferenceDevice;

/**
 * 算力推理设备 Service
 */
public interface InferenceDeviceService extends IService<InferenceDevice> {

    /**
     * 算力设备分页查询
     */
    Page<InferenceDeviceListDTO> page(Page<InferenceDevice> page, String deviceName);

    /**
     * 新增或更新（编辑时 authCode 为空则保持原值）
     */
    InferenceDevice saveOrUpdate(InferenceDevice inferenceDevice, String authCode);
}
