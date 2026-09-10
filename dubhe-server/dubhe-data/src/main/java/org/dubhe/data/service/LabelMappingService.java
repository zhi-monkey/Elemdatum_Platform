package org.dubhe.data.service;


import org.dubhe.data.domain.dto.LabelMappingDTO;
import org.dubhe.data.domain.entity.LabelMapping;

import java.util.List;
import java.util.Map;

public interface LabelMappingService {


    /**
     * LabelMapping 实体插入
     *
     * @param labelMapping 列表
     * @return
     */

    void insertLabelMapping(List<LabelMapping> labelMapping);


    /**
     * LabelMapping 通过map插入
     *
     * @param labelMapping     Map<long,long>
     * @param datasetVersionId 数据集版本ID
     */

    void insertLabelMapping(List<LabelMappingDTO> labelMapping, Long datasetVersionId);


    /**
     *  根据datasetVersionID 查询对应的标签映射关系
     *
     * @param datasetVersionId 数据集版本ID
     */

    List<LabelMapping> findByDatasetVersionId(Long datasetVersionId);


}
