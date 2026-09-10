package org.dubhe.data.service;

import java.util.List;

/**
 * @description 数据集分组服务
 * @author mingming
 * @date 2025/09/05
 */
public interface DatasetDatasetGroupService {
    void bind(Long datasetGroupId, Long datasetId);

    void unbind(List<Long> datasetIds);
}
