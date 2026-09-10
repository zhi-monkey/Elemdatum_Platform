package org.dlut.adv.mineai.model.domain.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dlut.adv.mineai.model.domain.dto
 * @Project：mineai
 * @name：ModelValidationDTO
 * @Date：2024/5/27 17:39
 * @Filename：ModelValidationDTO
 * @Desc：
 */

@Data
public class ModelValidationDTO {

    // id
    Long id;
    // 验证名称
    String jobName;
    // 镜像
    String image;
    // 临时路径
    String path;
    // weight路径
    String weightPath;
    // config
    String gpuNum;
    String cpuNum;
    String memoryNum;
    // 容器启动参数
    Map<String, String> params;

}
