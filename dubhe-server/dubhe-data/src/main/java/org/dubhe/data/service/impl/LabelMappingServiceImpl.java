package org.dubhe.data.service.impl;

import org.dubhe.data.dao.LabelMappingMapper;
import org.dubhe.data.domain.dto.LabelMappingDTO;
import org.dubhe.data.domain.entity.LabelMapping;
import org.dubhe.data.service.LabelMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabelMappingServiceImpl  implements LabelMappingService {

    @Autowired
    private LabelMappingMapper labelMappingMapper;

    @Override
    public void insertLabelMapping(List<LabelMapping> labelMapping) {
        for (LabelMapping lm : labelMapping) {
            labelMappingMapper.insert(lm);
        }
    }


    @Override
    public void insertLabelMapping(List<LabelMappingDTO> labelMap, Long datasetVersionId) {
        List<LabelMapping> labelMapping =  LabelMapping.fromDtoList(labelMap,datasetVersionId);
        for (LabelMapping lm : labelMapping) {
            labelMappingMapper.insert(lm);
        }
    }

    @Override
    public List<LabelMapping> findByDatasetVersionId(Long datasetVersionId) {
        return labelMappingMapper.findByDatasetVersionId(datasetVersionId);
    }
}
