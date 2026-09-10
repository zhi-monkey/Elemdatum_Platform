package org.dlut.adv.mineai.model.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dlut.adv.mineai.model.domain.vo
 * @Project：mineai
 * @name：ModelValidationVO
 * @Date：2024/5/27 15:14
 * @Filename：ModelValidationVO
 * @Desc：
 */

@Data
public class ModelValidationVO implements Serializable {

    // 验证名称
    String name;

    // 验证完成时间
    Long finishTime;

    // 验证结果保存路径
    String path;


}
