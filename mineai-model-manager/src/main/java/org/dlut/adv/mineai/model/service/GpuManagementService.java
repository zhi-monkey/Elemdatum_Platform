package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GPU管理服务 - 管理禁用的GPU UUID列表
 * @author system
 */
@Slf4j
@Service
public class GpuManagementService {
    
    /**
     * 存储禁用的GPU UUID集合（线程安全）
     */
    private final Set<String> disabledGpuUuids = ConcurrentHashMap.newKeySet();
    
    /**
     * 禁用指定的GPU
     * @param gpuUuid GPU的UUID
     */
    public void disableGpu(String gpuUuid) {
        if (gpuUuid != null && !gpuUuid.trim().isEmpty()) {
            disabledGpuUuids.add(gpuUuid);
            log.info("GPU已禁用: {}", gpuUuid);
        }
    }
    
    /**
     * 启用指定的GPU（从禁用列表中移除）
     * @param gpuUuid GPU的UUID
     */
    public void enableGpu(String gpuUuid) {
        if (gpuUuid != null) {
            disabledGpuUuids.remove(gpuUuid);
            log.info("GPU已启用: {}", gpuUuid);
        }
    }
    
    /**
     * 检查GPU是否被禁用
     * @param gpuUuid GPU的UUID
     * @return true表示已禁用，false表示可用
     */
    public boolean isGpuDisabled(String gpuUuid) {
        return disabledGpuUuids.contains(gpuUuid);
    }
    
    /**
     * 获取所有禁用的GPU UUID列表
     * @return 禁用的GPU UUID集合
     */
    public Set<String> getDisabledGpuUuids() {
        return new HashSet<>(disabledGpuUuids);
    }
    
    /**
     * 获取禁用GPU UUID的字符串形式（逗号分隔）
     * 用于传递给Kubernetes annotation
     * @return 格式: "gpu1,gpu2,gpu3"
     */
    public String getDisabledGpuUuidsString() {
        if (disabledGpuUuids.isEmpty()) {
            return "";
        }
        return String.join(",", disabledGpuUuids);
    }
    
    /**
     * 清空所有禁用的GPU
     */
    public void clearAllDisabled() {
        disabledGpuUuids.clear();
        log.info("已清空所有禁用的GPU");
    }
    
    /**
     * 获取禁用GPU的数量
     * @return 禁用GPU的数量
     */
    public int getDisabledCount() {
        return disabledGpuUuids.size();
    }
}
