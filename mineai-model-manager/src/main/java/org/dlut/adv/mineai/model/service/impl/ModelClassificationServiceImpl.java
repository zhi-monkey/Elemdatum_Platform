package org.dlut.adv.mineai.model.service.impl;

import org.dlut.adv.mineai.core.entity.ModelClassification;
import org.dlut.adv.mineai.model.repository.ModelClassificationRepo;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;

/**
 * @package: org.dlut.adv.mineai.model.service.impl
 * @author: chystart
 * @create: 2024-06-13 09:21
 * @description: 算法分类实现类
 **/
@Service
public class ModelClassificationServiceImpl implements ModelClassificationService {

    @Resource
    private ModelClassificationRepo modelClassificationRepo;

    @Override
    public ModelClassification insertModelClassification(ModelClassification modelClassification) {
        return modelClassificationRepo.save(modelClassification);
    }

    @Override
    public void deleteModelClassification(Long id) {
        modelClassificationRepo.deleteById(id);
    }

    @Override
    public ModelClassification updateModelClassification(ModelClassification modelClassification) {
        return modelClassificationRepo.save(modelClassification);
    }

    @Override
    public ModelClassification getModelClassification(Long id) {
        return modelClassificationRepo.getById(id);
    }

    @Override
    public List<ModelClassification> getModelClassifications() {
        return modelClassificationRepo.findAll();
    }
}
