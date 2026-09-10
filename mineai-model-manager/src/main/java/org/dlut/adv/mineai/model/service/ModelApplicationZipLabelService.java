package org.dlut.adv.mineai.model.service;

import java.util.List;

/**
 * @author mingming
 * @date 2025/03/31
 */
public interface ModelApplicationZipLabelService {

    List<Long> getBoundLabelIds(Long modelApplicationId);

    void saveBoundLabelIds(Long modelApplicationId, List<Long> labelIds);

    void removeBoundLabelByModelApplicationId(Long modelApplicationId);
}
