package org.dlut.adv.mineai.model.domain.entity;

import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;

public class ModelConversionTask {
    private final ModelJob modelJob;
    private final ModelConvertDTO modelConvertDTO;
    private final String targetObjName;

    public ModelConversionTask(ModelJob modelJob, ModelConvertDTO modelConvertDTO, String targetObjName) {
        this.modelJob = modelJob;
        this.modelConvertDTO = modelConvertDTO;
        this.targetObjName = targetObjName;
    }

    public ModelJob getModelJob() {
        return modelJob;
    }

    public ModelConvertDTO getModelConvertDTO() {
        return modelConvertDTO;
    }

    public String getTargetObjName() {
        return targetObjName;
    }

}
