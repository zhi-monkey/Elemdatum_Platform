package org.dlut.adv.mineai.model.service;


import org.dlut.adv.mineai.core.entity.ModelConfig;
import org.dlut.adv.mineai.model.repository.ModelConfigRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ModelConfigService {
    @Resource
    ModelConfigRepo modelConfigRepo;
    public void saveModelConfig(ModelConfig modelConfig){
        modelConfigRepo.save(modelConfig);
    }
}
