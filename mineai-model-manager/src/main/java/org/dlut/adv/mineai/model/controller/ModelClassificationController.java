package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.ModelClassification;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @package: org.dlut.adv.mineai.model.controller
 * @author: chystart
 * @create: 2024-06-13 09:30
 * @description: 算法分类的请求控制器
 **/
@Slf4j
@RestController
@RequestMapping("/modelClassifications")
public class ModelClassificationController {

    @Resource
    private ModelClassificationService modelClassificationService;

    @PostMapping("")
    @ApiOperation(value = "新增算法分类实体")
    public Msg<String> insertModelClassification(@RequestBody ModelClassification modelClassification) {
        List<ModelClassification> modelClassifications = modelClassificationService.getModelClassifications();
        List<ModelClassification> collect = modelClassifications.stream()
                .filter(modelClassification1 -> modelClassification.getClassificationName().equals(modelClassification1.getClassificationName()))
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return new Msg<>(MsgCode.MODEL_CLASSIFICATION_EXIST);
        }
        ModelClassification result = modelClassificationService.insertModelClassification(modelClassification);
        if (result == null) {
            return new Msg<>(MsgCode.ADD_MODEL_CLASSIFICATION_FAIL);
        } else {
            return new Msg<>(MsgCode.SUCCEED);
        }
    }

    @DeleteMapping("/{modelClassificationId}")
    @ApiOperation(value = "删除算法分类实体", notes = "按照算法分类id删除")
    public Msg<String> deleteModelClassification(@PathVariable("modelClassificationId") Long id) {
        List<ModelClassification> modelClassifications = modelClassificationService.getModelClassifications();
        if (!modelClassifications.isEmpty()) {
            List<ModelClassification> collect = modelClassifications.stream()
                    .filter(modelClassification -> modelClassification.getId() == id)
                    .collect(Collectors.toList());
            if (collect.isEmpty()) {
                return new Msg<>(MsgCode.MODEL_CLASSIFICATION_ID_NOT_EXISTS);
            }
        }
        modelClassificationService.deleteModelClassification(id);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PutMapping("")
    @ApiOperation(value = "更新算法分类实体")
    public Msg<String> updateModelClassification(@RequestBody ModelClassification modelClassification) {
        ModelClassification result = modelClassificationService.updateModelClassification(modelClassification);
        if (result == null) {
            return new Msg<>(MsgCode.UPDATE_MODEL_CLASSIFICATION_FAIL);
        } else {
            return new Msg<>(MsgCode.SUCCEED);
        }
    }

    @GetMapping("/{modelClassificationId}")
    @ApiOperation(value = "查询算法实体", notes = "按照算法实体id查询算法实体")
    public Msg<ModelClassification> getModelClassification(@PathVariable("modelClassificationId") Long id) {
        ModelClassification modelClassification = modelClassificationService.getModelClassification(id);
        if (modelClassification == null) {
            return new Msg<>(MsgCode.GET_MODEL_CLASSIFICATION_FAIL);
        } else {
            return new Msg<>(MsgCode.SUCCEED, modelClassification);
        }
    }

    @GetMapping("")
    @ApiOperation(value = "获取所有的算法分类实体")
    public Msg<List<ModelClassification>> getModelClassifications() {
        List<ModelClassification> modelClassifications = modelClassificationService.getModelClassifications();
        if (modelClassifications == null) {
            return new Msg<>(MsgCode.GET_MODEL_CLASSIFICATION_FAIL);
        } else {
            return new Msg<>(MsgCode.SUCCEED, modelClassifications);
        }
    }
}
