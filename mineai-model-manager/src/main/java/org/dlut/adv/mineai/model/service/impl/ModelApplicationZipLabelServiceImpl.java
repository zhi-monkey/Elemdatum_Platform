package org.dlut.adv.mineai.model.service.impl;

import org.dlut.adv.mineai.model.repository.ModelApplicationZipLabelRepo;
import org.dlut.adv.mineai.model.service.ModelApplicationZipLabelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author mingming
 * @date 2025/03/31
 */
@Service
public class ModelApplicationZipLabelServiceImpl implements ModelApplicationZipLabelService {

    @Resource
    private ModelApplicationZipLabelRepo modelApplicationZipLabelRepo;

    @Override
    public List<Long> getBoundLabelIds(Long modelApplicationId) {
        return modelApplicationZipLabelRepo.getBoundLabelIds(modelApplicationId);
    }

    @Override
    public void saveBoundLabelIds(Long modelApplicationId, List<Long> labelIds) {
        for (Long labelId : labelIds) {
            modelApplicationZipLabelRepo.saveBoundLabelIds(modelApplicationId, labelId);
        }
    }

    @Override
    public void removeBoundLabelByModelApplicationId(Long modelApplicationId) {
        modelApplicationZipLabelRepo.removeBoundLabelByModelApplicationId(modelApplicationId);
    }
}
