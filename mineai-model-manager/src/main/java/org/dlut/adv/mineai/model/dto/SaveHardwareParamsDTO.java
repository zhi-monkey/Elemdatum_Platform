package org.dlut.adv.mineai.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author mingming
 * @date 2024/11/26
 */
@Data
public class SaveHardwareParamsDTO {
    private String cpus;
    private String memory;
    private String gpuMemory;
    @JsonProperty("vGpuCores")
    private String vGpuCores;
    private String description;
    private String title;
    @JsonProperty("isDefault")
    private Integer isDefault = 0;
}
