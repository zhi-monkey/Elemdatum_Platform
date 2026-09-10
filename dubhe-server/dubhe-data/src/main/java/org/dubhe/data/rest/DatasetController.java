package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.dao.DatasetVersionFileMapper;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @description 数据集管理
 * @date 2020-04-10
 */
@Api(tags = "数据处理：数据集管理")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets")
public class
DatasetController {

    @Autowired
    private LabelService labelService;

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DatasetVersionFileMapper datasetVersionFileMapper;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private AuditLogHelper auditLogHelper;

    @ApiOperation(value = "数据集创建")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "dataset_add",recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody createDataset(@Validated(DatasetCreateDTO.Create.class) @RequestBody DatasetCreateDTO datasetCreateDTO) {
        return new DataResponseBody(datasetService.create(datasetCreateDTO));
    }

    @ApiOperation(value = "数据集查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(Page page, DatasetQueryDTO datasetQueryDTO) {
        return new DataResponseBody(datasetService.listVO(page, datasetQueryDTO));
    }

    @ApiOperation(value="查询所有有version的数据集")
    @GetMapping("getAllPublishedDatasets")
    public DataResponseBody getAllPublishedDatasets() {
        return new DataResponseBody(datasetService.getAllPublishedDatasets());
    }

    @ApiOperation(value = "数据集查询")
    @GetMapping("getAllPublicDatasets")
    public DataResponseBody getAllPublicDatasets(Page page, DatasetQueryDTO datasetQueryDTO) {
        return new DataResponseBody(datasetService.getAllPublicDatasets(page, datasetQueryDTO));
    }

    @ApiOperation(value = "获取数据集总数（含公开与私有）")
    @GetMapping("getAllDatasetCount")
    public DataResponseBody getAllDatasetCount() {
        return new DataResponseBody(datasetService.getAllDatasetCount());
    }

    @ApiOperation(value = "数据集详情")
    @GetMapping(value = "/{datasetId:\\d+}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(datasetService.get(datasetId));
    }

    @ApiOperation(value = "数据集批量详情参数校验")
    @GetMapping(value = "/batchGet")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody batchGetWithoutIds() {
        return new DataResponseBody(ResponseCode.ERROR, "datasetIds不能为空");
    }

    @ApiOperation(value = "数据集批量详情")
    @GetMapping(value = "/batchGet/{datasetIds}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody batchGet(@PathVariable(name = "datasetIds") List<Long> datasetIds) {
        return new DataResponseBody(datasetService.batchGet(datasetIds));
    }

    // 查询数据集是不是引导式和查询对应的所有标签
    @ApiOperation(value = "数据集标签详情")
    @GetMapping(value = "/guidedAndLabels/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody guidedAndLabels(@PathVariable(name = "datasetId") Long datasetId) {
        GuidedAndLabelsVO guidedAndLabelsVO = datasetService.guidedAndLabels(datasetId);
        return new DataResponseBody(guidedAndLabelsVO);
    }

    // 一次性查询引导式任务的所有标签名字
    @ApiOperation(value = "数据集标签名字")
    @GetMapping(value = "/guidedLabelsNames/{datasetStartList}")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody<List<List<String>>> guidedLabelsNames(@PathVariable(name = "datasetStartList") List<Long> datasetStartList) {
        List<List<String>> result = new ArrayList<>();
        for (Long datasetId : datasetStartList) {
            GuidedAndLabelsVO guidedAndLabelsVO = datasetService.guidedAndLabels(datasetId);
            List<String> labelNames = guidedAndLabelsVO.getLabels().stream()
                    .map(LabelZipDTO::getName)
                    .collect(Collectors.toList());
            result.add(labelNames);
        }
        return new DataResponseBody(result);
    }

    @ApiOperation(value = "数据集进度")
    @GetMapping(value = "/progress")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody progress(@RequestParam List<Long> datasetIds) {
        return new DataResponseBody(datasetService.progress(datasetIds));
    }

    @ApiOperation(value = "数据集修改")
    @PutMapping(value = "/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@PathVariable(name = "datasetId") Long datasetId,
                                   @Validated @RequestBody DatasetCreateDTO datasetCreateDTO) {
        boolean result = datasetService.update(datasetCreateDTO, datasetId);
        if (result) {
            auditLogHelper.saveUpdateAuditLog("dataset_update",
                    "  datasetId: " + datasetId + "  name: " + datasetCreateDTO.getName());
        }
        return new DataResponseBody(result);
    }

    @ApiOperation(value = "数据集删除", notes = "数据集下的文件会同时被删除")
    @DeleteMapping
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "dataset_delete",recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Validated @RequestBody DatasetDeleteDTO datasetDeleteDTO) {
        try {
            datasetService.delete(datasetDeleteDTO);
        } catch (BusinessException e) {
            return new DataResponseBody(ResponseCode.ERROR, e.getMessage());
        }
//        datasetService.delete(datasetDeleteDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "数据集下载", notes = "压缩数据集并下载")
    @GetMapping(value = "/{datasetId}/download")
    @PreAuthorize(Permissions.DATA)
    public void download(@PathVariable(name = "datasetId") Long datasetId, HttpServletResponse httpServletResponse) {
        datasetService.download(datasetId, httpServletResponse);
    }

    @ApiOperation(value = "数据集查询(有版本)")
    @GetMapping(value = "/versions/filter")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryConfirmDatasetVersion(Page page, DatasetIsVersionDTO datasetIsVersionDTO) {
        return new DataResponseBody(datasetService.dataVersionListVO(page, datasetIsVersionDTO));
    }

    @ApiOperation(value = "数据集增强")
    @PostMapping(value = "/enhance")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody enhance(@Validated @RequestBody DatasetEnhanceRequestDTO datasetEnhanceRequestDTO) {
        datasetService.enhance(datasetEnhanceRequestDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "查询公共和个人数据集的数量")
    @GetMapping(value = "/count")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryDatasetsCount() {
        return new DataResponseBody(datasetService.queryDatasetsCount());
    }

    @ApiOperation(value = "查询数据集状态")
    @GetMapping(value = "/status")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody determineIfTheDatasetIsAnImport(@RequestParam List<Long> datasetIds) {
        return new DataResponseBody(datasetService.determineIfTheDatasetIsAnImport(datasetIds));
    }

    // 在状态查询轮询结束时移除这个数据集的时间，进度记录，防止内存泄露和出错
    @ApiOperation(value = "根据ID删除progressTimeMap中的记录")
    @DeleteMapping(value = "/map/delete")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteMapRecordById(@RequestParam Long id) {
        return new DataResponseBody(datasetService.deleteMapRecordById(id));
    }

    @ApiOperation(value = "导入数据集压缩包")
    @PostMapping(value = "/datasetImport")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody datasetImport(@RequestBody DatasetImportDTO datasetImportDTO) {
        datasetService.datasetImport(datasetImportDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "导入用户自定义数据集（未用到）")
    @PostMapping(value = "/custom")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody importDataset(@RequestBody DatasetCustomCreateDTO datasetCustomCreateDTO) {
        return new DataResponseBody(datasetService.importDataset(datasetCustomCreateDTO));
    }

    @ApiOperation(value = "数据集置顶")
    @GetMapping(value = "/{datasetId}/top")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody topDataset(@PathVariable(name = "datasetId") Long datasetId) {
        datasetService.topDataset(datasetId);
        return new DataResponseBody();
    }

    @ApiOperation(value = "普通数据集转预置")
    @PostMapping(value = "/convertPreset")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody convertPreset(@RequestBody DatasetConvertPresetDTO datasetConvertPresetDTO) {
        datasetService.convertPreset(datasetConvertPresetDTO);
        return new DataResponseBody();
    }


    @ApiOperation(value = "根据数据集ID查询数据集是否转换信息")
    @GetMapping(value = "/getConvertInfoByDatasetId")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getConvertInfoByDatasetId(@RequestParam(value = "datasetId") Long datasetId) {
        return new DataResponseBody(datasetService.getConvertInfoByDatasetId(datasetId));
    }

    @ApiOperation("获取预置数据集列表")
    @GetMapping(value = "/getPresetDataset")
    public DataResponseBody getPresetDataset() {
        return new DataResponseBody(datasetService.getPresetDataset());
    }

    @ApiOperation(value = "任务停止")
    @PutMapping(value = "/task/{datasetId}/stop")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody taskStop(@PathVariable(name = "datasetId") Long datasetId) {
        datasetService.taskStop(datasetId);
        return new DataResponseBody();
    }

    @ApiOperation(value = "ofrecord停止")
    @PutMapping(value = "/ofRecord/{datasetId}/stop")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody ofRecordStop(@PathVariable(name = "datasetId") Long datasetId, @RequestParam(name = "version") String version) {
        datasetService.ofRecordStop(datasetId, version);
        return new DataResponseBody();
    }

    @ApiOperation("获取指定名称预置数据集(远程调用)")
    @GetMapping(value = "/getPresetDatasetByName")
    public DataResponseBody<DatasetVO> getPresetDatasetByName(@RequestParam String datasetName) {
        return new DataResponseBody(datasetService.getPresetDatasetByName(datasetName));
    }

    @ApiOperation("获取当前所有的数据集(监控设备绑定用)")
    @GetMapping(value = "/getAllDatasets")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getAllDatasets() {
        return new DataResponseBody(datasetService.getAllDatasets());
    }

    @ApiOperation("按数据类型获取数据集(检索数据集下拉用)")
    @GetMapping(value = "/getAllDatasetsByType")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getAllDatasetsByType(@RequestParam(value = "dataType", required = true) Integer dataType) {
        return new DataResponseBody(datasetService.getAllDatasetsByType(dataType));
    }

    @ApiOperation("从检索结果新建数据集")
    @PostMapping(value = "/createFromSearch")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createFromSearch(@Validated @RequestBody CreateDatasetFromSearchDTO dto) {
        return new DataResponseBody(datasetService.createFromSearch(dto));
    }

    @ApiOperation("合并文件到已有数据集")
    @PostMapping(value = "/mergeToDataset")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody mergeToDataset(@Validated @RequestBody MergeToDatasetDTO dto) {
        datasetService.mergeToDataset(dto);
        return new DataResponseBody();
    }

    @ApiOperation("获取数据集创建的统计信息")
    @GetMapping(value = "/getPeriodData")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetPeriodDataVO> getPeriodData() {
        return new DataResponseBody<>(datasetService.getPeriodData());
    }

    @ApiOperation("获取数据集对应的标注信息")
    @GetMapping(value = "/{datasetId}/annotationName")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Integer> getAnnotationType(@PathVariable(value = "datasetId") Long id) {
        Dataset dataset = datasetService.getOneById(id);
        return new DataResponseBody<>(dataset.getAnnotateType());
    }

    @ApiOperation("分天数获取数据集创建的统计信息")
    @GetMapping(value = "/getPeriodDataByDate")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetPeriodDataVO> getPeriodDataByDate(@RequestParam Integer dateNum) {
        return new DataResponseBody<>(datasetService.getPeriodDataByDate(dateNum));
    }

    @ApiOperation("获取数据集类别统计信息")
    @GetMapping(value = "/getDatasetStatByType")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetTypeStatVO> getDatasetStatByType() {
        return new DataResponseBody<>(datasetService.getDatasetStatByType());
    }

    @ApiOperation("分天数获取数据集类别统计信息")
    @GetMapping(value = "/getDatasetStatByTypeAndDate")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetTypeStatVO> getDatasetStatByTypeAndDate(@RequestParam Integer dateNum) {
        return new DataResponseBody<>(datasetService.getDatasetStatByTypeAndDate(dateNum));
    }


    @ApiOperation("返回数据集具体标签信息")
    @GetMapping(value = "/getDatasetLabelInfo")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetLabelInfoDTO>> getDatasetStatByTypeAndDate(@RequestParam Long datasetId) {
        return new DataResponseBody<>(datasetService.getDatasetLabelInfo(datasetId));
    }

    /**
     * 根据数据类型查询数据
     *
     * @param dataType
     * @return
     */
    @ApiOperation("根据具体数据类型返回数据集")
    @GetMapping(value = "/selectByDataType")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody<List<Dataset>> selectByDataType(Integer dataType) {
        return new DataResponseBody<>(datasetService.selectByDataType(dataType));
    }

    @ApiOperation("获取未标注且未发布的数据集")
    @GetMapping(value = "/getUnpublishedDatasets")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody<List<DatasetVO>> getUnpublishedDatasets() {
        return new DataResponseBody<>(datasetService.getUnpublishedDatasets());
    }

    @ApiOperation("根据数据集组ID获取未标注且未发布的数据集")
    @GetMapping(value = "/getUnpublishedDatasetsByGroupId")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody<List<DatasetVO>> getUnpublishedDatasetsByGroupId(@RequestParam Long datasetGroupId) {
        return new DataResponseBody<>(datasetService.getUnpublishedDatasetsByGroupId(datasetGroupId));
    }

    @ApiOperation("将指定数据集的内容导入到标注任务")
    @PostMapping(value = "{dataRepoId}/{datasetId}/{dataType}/import")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody importDatasetFromDataRepo(@PathVariable(value = "dataRepoId") Long dataRepoId, @PathVariable(value = "datasetId") Long datasetId, @PathVariable(value = "dataType") Integer dataType) {
        datasetService.importDatasetFromDataRepo(dataRepoId, datasetId, dataType);
        return new DataResponseBody();
    }

    @ApiOperation("启动自动标注任务")
    @PostMapping(value = "/autoLabel/{datasetId}/start")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody autoLabelStart(@PathVariable(value = "datasetId") Long datasetId) {
        datasetService.autoLabelStart(datasetId);
        return new DataResponseBody();
    }

    @ApiOperation("启动自动标注任务-带标签名")
    @PostMapping(value = "/autoLabel/{datasetId}/startWithNames")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody autoLabelStartWithNames(
            @PathVariable("datasetId") Long datasetId,
            @RequestBody  List<String> labelNames) {
        datasetService.autoLabelStartWithNamesToFilter(datasetId, labelNames);
        return new DataResponseBody();
    }

    @ApiOperation("启动自动标注任务-带标签名和清除选项")
    @PostMapping(value = "/autoLabel/{datasetId}/startWithNamesAndClearOption")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody autoLabelStartWithNamesAndClearOption(
            @PathVariable("datasetId") Long datasetId,
            @RequestParam(value = "clearExistingLabels") Boolean clearExistingLabels,
            @RequestParam(value = "requireManualConfirmation") Boolean requireManualConfirmation,
            @RequestParam(value = "jobName", required = false) String jobName,
            @RequestBody List<String> labelNames) {
        datasetService.autoLabelStartWithNamesToFilterAndClearOption(datasetId, labelNames, clearExistingLabels, requireManualConfirmation, jobName);
        return new DataResponseBody();
    }


    @ApiOperation("完成自动标注任务")
    @PostMapping(value = "/autoLabel/{datasetId}/end")
    @PreAuthorize(Permissions.DATA)
    DataResponseBody autoLabelEnd(@PathVariable(value = "datasetId") Long datasetId, @RequestBody Boolean isFailed) {
        datasetService.autoLabelEnd(datasetId, isFailed);
        return new DataResponseBody();
    }

    @ApiOperation("获取前十条已发布的数据集信息")
    @GetMapping(value = "/info/published")
    DataResponseBody<List<DatasetInfoVO>> getPublishedDatasetsInfo() {
        return new DataResponseBody<>(datasetService.getPublishedDatasetsInfo());
    }


    @ApiOperation(value = "引导式训练部分数据集详情接口")
    @GetMapping(value = "/guidedDatasetCardInfo/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetCardInfoVO> getGuidedDatasetCardInfo(@PathVariable(value = "datasetId") Long datasetId) {
        return new DataResponseBody<>(datasetService.getGuidedDatasetCardInfo(datasetId));
    }


    @ApiOperation(value = "给一个数据集id，判断其是否在发布版本过程中")
    @GetMapping(value = "/isPublishing/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> isPublishingByDatasetId(@PathVariable(value = "datasetId") Long datasetId) {
        return new DataResponseBody<>(datasetService.isPublishingByDatasetId(datasetId));
    }

    /**
     * @param selectedDatasetId 选择出来的数据集id
     * @param targetDatasetId 引导式训练自己的datasetId
     * @return {@link DataResponseBody }<{@link List }<{@link DatasetVersionVO }>>
     */
    @ApiOperation(value = "获取一个数据集的数据集版本列表, 再根据映射表筛选")
    @GetMapping(value = "/getAvailableVersionList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> getAvailableVersionList(@RequestParam Long selectedDatasetId, @RequestParam Long targetDatasetId) {
        return new DataResponseBody<>(datasetService.getAvailableVersionList(selectedDatasetId, targetDatasetId));
    }


    @ApiOperation(value = "从数据集某版本导入到到另一个数据集")
    @PostMapping(value = "/importDatasetFromDatasetVersion")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody importDatasetFromDatasetVersion(@RequestParam Long targetDatasetId, @RequestParam Long datasetVersionId) {
        datasetService.importDatasetFromDatasetVersion(targetDatasetId,datasetVersionId);
        return new DataResponseBody();
    }

    /**
     * 注意这里查出来的图片数量是所有版本图片数量, 多人标注处只能选择未发布的所以图片数量是没问题的, 但是其他地方想复用要注意
     * @param datasetId 数据集id
     * @return {@link DataResponseBody }<{@link Integer }>
     */
    @ApiOperation(value = "获取数据集图片数量")
    @GetMapping(value = "/getDatasetImageCount/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Integer> getDatasetImageCount(@PathVariable(value = "datasetId") Long datasetId) {
        return new DataResponseBody<>(datasetVersionFileMapper.getDatasetVersionAllFileCount(datasetId, null));
    }

    @GetMapping(value = "/isGuided/{datasetId}")
    @ApiOperation(value = "判断数据集是否是引导式数据集")
    public DataResponseBody<Boolean> isGuided(@PathVariable(value = "datasetId") Long datasetId) {
        return new DataResponseBody<>(datasetService.isGuided(datasetId));
    }

    @PostMapping(value = "/batchRecoverDataset")
    @ApiOperation(value = "批量恢复数据集")
    public DataResponseBody batchRecoverDataset(@RequestBody DatasetDeleteDTO datasetDeleteDTO) {
        datasetService.batchRecoverDataset(datasetDeleteDTO.getIds());
        return new DataResponseBody();
    }

    @GetMapping("/getDatasetLabels")
    @ApiOperation(value = "获取数据集标签")
    public DataResponseBody<List<DatasetLabelInfoDTO>> getDatasetLabels(@RequestParam Long datasetId) {
        return new DataResponseBody<>(datasetService.getDatasetLabelInfo(datasetId));
    }

    @ApiOperation(value = "删除指定桶下前缀（目录）", notes = "调用 deleteDirectory(bucketName, prefix) 删除前缀下所有对象")
    @PostMapping("/deleteDirectory")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "dataset_delete_minio_directory", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody deleteDirectory(
            @RequestParam String bucketName,
            @RequestParam String prefix) {
        try {
            minioUtil.deleteDirectory(bucketName, prefix);
            return new DataResponseBody("删除成功");
        } catch (BusinessException e) {
            return new DataResponseBody(ResponseCode.ERROR, e.getMessage());
        } catch (Exception e) {
            return new DataResponseBody(ResponseCode.ERROR, "MinIO 目录删除失败: " + e.getMessage());
        }
    }

    @ApiOperation(value = "获取当前文件之后最近的未标注图片信息（包含ID和距离）")
    @GetMapping("/{datasetId}/nearest-unannotated-after-current")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getNearestUnannotatedFileInfo(
            @PathVariable Long datasetId,
            @RequestParam Long currentFileId,
            @RequestParam(required = false) String versionName,
            @RequestParam(required = false) List<Long> labelId) {
        org.dubhe.data.domain.dto.NearestUnannotatedFileDTO result = datasetService.getNearestUnannotatedFileInfo(
            datasetId, 
            currentFileId, 
            versionName, 
            labelId
        );
        return new DataResponseBody(result);
    }

}
