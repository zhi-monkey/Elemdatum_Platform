package org.dlut.adv.mineai.model.vo;

import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.core.vo.ModelGenerationVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModelGenerationVOTest {

    @Test
    void constructorShouldNotCallGuidedStatusForUnGuidedGeneration() {
        ModelGeneration modelGeneration = new ModelGeneration();
        modelGeneration.setId(1L);
        modelGeneration.setName("mg");
        modelGeneration.setIsGuided(false);

        ModelGenerationVO vo = new ModelGenerationVO(modelGeneration);

        Assertions.assertEquals(ModelGeneration.NOT_ACTIVE_GUIDED, vo.getStatusForGuided());
    }
}
