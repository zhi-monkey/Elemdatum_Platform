package org.dlut.adv.mineai.model.service;

import java.util.List;

/**
 * @author mingming
 * @date 2025/06/11
 */
public interface ModelVersionDefaultLabelsService {
    void insertRelations(Long modelVersionId, String[] labelNames);

    List<String> getLabelsByImageUrl(String imageUrl);
    List<String> getLabelsByImageUrlAndDatasetId(String imageUrl, Long datasetId);
}
