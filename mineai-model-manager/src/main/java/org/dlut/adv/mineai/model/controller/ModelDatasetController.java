package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.vo.DatasetVO;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.ModelDatasetService;
import org.dlut.adv.mineai.model.service.ModelExploreService;
import org.dlut.adv.mineai.model.service.ModelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/modelDataset")
public class ModelDatasetController {


    @Resource
    ModelService modelService;
    @Resource
    ModelDatasetService modelDatasetService;

    @Resource
    ModelExploreService modelExploreService;


    /**
     * 查询当前model数据类型的数据集
     */
    @RequestMapping("/findDatasetByDataType")
    public Msg<List<DatasetVO>> findDatasetByDataType(@RequestParam long modelId) {
        ModelExplore model = modelExploreService.getModelExploreById(modelId);
        List<DatasetVO> datasets = modelDatasetService.getDatasetByDataType(model.getDataType()).getData();
        Collections.reverse(datasets);
        return new Msg<>(MsgCode.SUCCEED, datasets);
    }

    /**
     * 根据数据集版本的id查询数据集版本
     */
    @RequestMapping("/findDatasetById")
    public Msg<DatasetVersionVO> findDatasetById(@RequestParam long datasetVersionId) {
        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.selectDatasetVersionById(datasetVersionId));
    }
//    /**
//     * 查询当前所有数据集
//     */
//    @RequestMapping("/findAllDatasets")
//    public Msg<List<DatasetVO>> findAllDatasets() {
//        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.getAllDatasets().getData());
//    }


    /**
     * 查询当前所有数据集
     */
    @RequestMapping("/findAllDatasets")
    public Msg<List<DatasetVersionVO>> getAllDatasetVersion() {
        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.datasetVersionList());
    }

    @RequestMapping("/findPrivateAndPublicDatasets")
    public Msg<List<DatasetVersionVO>> findPrivateAndPublicDatasets() {

        UserContext userContext = UserContextHolder.getUserContext();
        Integer userId = userContext.getId();
        if (userId == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.datasetVersionList().stream()
                .filter(datasetVersionVO ->
                        datasetVersionVO.getIsPublic() == DatasetVersionVO.IS_PUBLIC ||
                                Objects.equals(datasetVersionVO.getCreateUserId(), userId.longValue()))
                .collect(Collectors.toList()));
    }

    @RequestMapping("/findDatasetFullName")
    public Msg<List<DatasetVersionVO>> findDatasetFullName() {
        String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
        Integer userId = UserContextHolder.getUserContext().getId();
        if (userId == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            return new Msg<>(MsgCode.SUCCEED, modelDatasetService.datasetFullNameList().stream()
                    .filter(datasetVersionVO ->
                            datasetVersionVO.getIsPublic() == DatasetVersionVO.IS_PUBLIC ||
                                    Objects.equals(datasetVersionVO.getCreateUserId(), userId.longValue()))
                    .collect(Collectors.toList()));
        }
        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.datasetFullNameList());
    }

    /**
     * 查询当前所有数据集名称
     */
    @RequestMapping("/findAllDatasetName")
    public Msg<List<String>> getAllDatasetVersionName() {
        return new Msg<>(MsgCode.SUCCEED, modelDatasetService.datasetVersionList().stream().map(DatasetVersionVO::getFullName).collect(Collectors.toList()));
    }
}
