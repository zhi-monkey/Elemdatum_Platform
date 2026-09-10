package org.dlut.adv.mineai.model.domain.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Desc：模型转换参数
 */

@Data
public class ModelConvertDTO {

    // 转换任务名称
    String jobName;
    // 镜像
    String image;
    // dataset路径
    String datasetPath;
    // weight路径
    String weightPath;
    // output
    String outputWeightPath;
    // config
    String gpuNum;
    String cpuNum;
    String memoryNum;
    // 容器启动参数
    Map<String, String> params;

}
