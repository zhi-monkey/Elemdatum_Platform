package org.dubhe.data.service;

import org.dubhe.data.domain.dto.CutRuleDTO;

/**
 * @package: org.dubhe.data.service
 * @author: chystart
 * @create: 2024-04-06 16:22
 * @description: 数据集切分服务
 **/
public interface DataSetCutService {

    /**
     * 根据数据集id查询标注信息路径
     *
     * @param dataSetId
     * @param cutRuleDTO
     */
    void cutCocoAnnotationDataSet(Long dataSetId, CutRuleDTO cutRuleDTO);
}
