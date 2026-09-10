package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.ModelExploreService;
import org.dlut.adv.mineai.model.service.ModelGenerationService;
import org.dlut.adv.mineai.model.service.ModelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ModelGenerationControllerTest {

    @Mock
    private ModelGenerationService modelGenerationService;

    @Mock
    private ModelExploreService modelExploreService;

    @Mock
    private ModelService modelService;

    @InjectMocks
    private ModelGenerationController modelGenerationController;

    @Test
    void addModelGenerationShouldKeepOriginalExploreFlow() {
        ModelGeneration request = new ModelGeneration();
        request.setName("mg-explore");
        ModelExplore requestExplore = new ModelExplore();
        requestExplore.setId(1L);
        request.setModelExplore(requestExplore);

        ModelVersion trainVersion = new ModelVersion();
        trainVersion.setAnnotationType("Detection");
        trainVersion.setAnnotationFormat("YOLO");
        ModelExplore dbExplore = new ModelExplore();
        dbExplore.setId(1L);
        dbExplore.setTrainModelVersion(trainVersion);

        when(modelGenerationService.isModelGenerationExist(request)).thenReturn(false);
        when(modelExploreService.getModelExploreById(1L)).thenReturn(dbExplore);

        Msg<Long> result = modelGenerationController.addModelGeneration(request);

        Assertions.assertEquals(MsgCode.SUCCEED.getCode(), result.getCode());
        Assertions.assertEquals("Detection", request.getAnnotationType());
        Assertions.assertEquals("YOLO", request.getAnnotationFormat());
        verify(modelExploreService).saveModel(dbExplore);
        verify(modelService, never()).getModelById(1L);
        verify(modelGenerationService).saveModelGeneration(request);
    }

    @Test
    void addModelGenerationShouldSupportBaseModelFlow() {
        ModelGeneration request = new ModelGeneration();
        request.setName("mg-base");
        Model requestModel = new Model();
        requestModel.setId(2L);
        request.setModel(requestModel);

        ModelVersion trainVersion = new ModelVersion();
        trainVersion.setAnnotationType("Segmentation");
        trainVersion.setAnnotationFormat("COCO");
        Model dbModel = new Model();
        dbModel.setId(2L);
        dbModel.setTrainModelVersion(trainVersion);

        when(modelGenerationService.isModelGenerationExist(request)).thenReturn(false);
        when(modelService.getModelById(2L)).thenReturn(dbModel);

        Msg<Long> result = modelGenerationController.addModelGeneration(request);

        Assertions.assertEquals(MsgCode.SUCCEED.getCode(), result.getCode());
        Assertions.assertEquals("Segmentation", request.getAnnotationType());
        Assertions.assertEquals("COCO", request.getAnnotationFormat());
        verify(modelExploreService, never()).saveModel(any(ModelExplore.class));
        verify(modelGenerationService).saveModelGeneration(request);
    }
}
