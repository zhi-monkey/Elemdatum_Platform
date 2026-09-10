package org.dlut.adv.mineai.model.controller;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.impl.ModelVersionDefaultLabelsServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author mingming
 * @date 2025/06/11
 */
@Slf4j
@RequestMapping("/modelVersionDefaultLabels")
@RestController
public class ModelVersionDefaultLabelsController {
    private final ModelVersionDefaultLabelsServiceImpl modelVersionDefaultLabelsServiceImpl;

    public ModelVersionDefaultLabelsController(ModelVersionDefaultLabelsServiceImpl modelVersionDefaultLabelsServiceImpl) {
        this.modelVersionDefaultLabelsServiceImpl = modelVersionDefaultLabelsServiceImpl;
    }

    @RequestMapping("/getLabelsByImageUrl")
    public Msg<List<String>> getLabelsByImageUrl(String imageUrl) {
        return new Msg<>(MsgCode.SUCCEED, modelVersionDefaultLabelsServiceImpl.getLabelsByImageUrl(imageUrl));
    }

    @GetMapping("/getLabelsByImageUrlAndDatasetId")
    public Msg<List<String>> getLabelsByImageUrlAndDatasetId(String imageUrl, Long datasetId) {
        return new Msg<>(MsgCode.SUCCEED, modelVersionDefaultLabelsServiceImpl.getLabelsByImageUrlAndDatasetId(imageUrl, datasetId));
    }
}
