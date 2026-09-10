package org.dlut.adv.mineai.model.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dlut.adv.mineai.core.entity.ModelApplication;
import org.dlut.adv.mineai.core.entity.ModelGeneration;

import java.util.Map;

/**
 * @author mingming
 * @date 2024/10/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuidedGenerationStartDTO {
    ModelGeneration modelGeneration;
    Map<String, String> trainHyperParams;

    ModelConvertDTO modelConvert;

    Map<String, String> quantizationParams;

    /**
     * 用来找appZipPath
     */
    ModelApplication modelApplication;
    @JsonProperty("HardwareParamsID")
    long HardwareParamsID;


    String datasetName;

    // 是fork就赋值id，不是就默认null
    Long forkDatasetId;

    String datasetGroupName;
    Integer gpuCount;
    String gpuMode;

    /**
     * 引导式时获取到上次的信息
     */
    Long modelGenerationId;
}
