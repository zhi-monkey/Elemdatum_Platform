package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.data.dao.DatasetDatasetGroupMapper;
import org.dubhe.data.domain.entity.DatasetDatasetGroup;
import org.dubhe.data.service.DatasetDatasetGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description 数据集数据集组关系服务实现类
 * @author mingming
 * @date 2025/09/05
 */
@Slf4j
@Service
public class DatasetDatasetGroupServiceImpl extends ServiceImpl<DatasetDatasetGroupMapper, DatasetDatasetGroup> implements DatasetDatasetGroupService {
    private final DatasetDatasetGroupMapper datasetDatasetGroupMapper;

    public DatasetDatasetGroupServiceImpl(DatasetDatasetGroupMapper datasetDatasetGroupMapper) {
        this.datasetDatasetGroupMapper = datasetDatasetGroupMapper;
    }

    @Override
    public void bind(Long datasetGroupId, Long datasetId) {
        DatasetDatasetGroup datasetDatasetGroup = new DatasetDatasetGroup(datasetGroupId, datasetId);
        this.save(datasetDatasetGroup);
    }

    @Override
    public void unbind(List<Long> datasetIds) {
        if (datasetIds.isEmpty()) {
            log.error("datasetIds is empty, unbind failed");
        }
        datasetDatasetGroupMapper.unbind(datasetIds);
    }
}
