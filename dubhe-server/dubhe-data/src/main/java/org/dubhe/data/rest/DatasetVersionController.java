

package org.dubhe.data.rest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.domain.entity.DatasetVersion;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.service.DatasetOperationEventService;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.DatasetVersionFileService;
import org.dubhe.data.service.DatasetVersionService;
import org.dubhe.data.service.task.TaskStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @description 数据集版本管理
 * @date 2020-05-14
 */
@Api(tags = "数据处理：数据集版本管理")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/versions")
public class DatasetVersionController {

    @Resource
    private DatasetVersionService datasetVersionService;

    @Resource
    private DatasetVersionFileService datasetVersionFileService;

    @Resource
    private DatasetService datasetService;

    @Autowired
    private DatasetOperationEventService datasetOperationEventService;

    @Autowired
    private TaskStatusService taskStatusService;

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private AuditLogHelper auditLogHelper;


    @ApiOperation("数据集版本保存")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody publish(@Validated(DatasetVersionCreateDTO.Create.class)
                                    @RequestBody DatasetVersionCreateDTO datasetVersionCreateDTO) {
        return new DataResponseBody(datasetVersionService.publish(datasetVersionCreateDTO));
    }

    @ApiOperation("数据集版本发布")
    @PostMapping("/release")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody release(@Validated(DatasetVersionCreateDTO.Create.class)
                                    @RequestBody DatasetVersionCreateDTO datasetVersionCreateDTO) {
        return new DataResponseBody(datasetVersionService.release(datasetVersionCreateDTO));
    }

    @ApiOperation("数据集版本发布（支持选择是否存档）")
    @PostMapping("/releaseWithOption")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody releaseWithOption(@Validated(DatasetVersionCreateDTO.Create.class)
                                              @RequestBody DatasetVersionCreateDTO datasetVersionCreateDTO) {
        return new DataResponseBody(datasetVersionService.releaseWithOption(datasetVersionCreateDTO));
    }

    @ApiOperation("数据集切分版本发布")
    @PostMapping("/split/{versionId}/{splitSize}")
    //@PreAuthorize(Permissions.DATA)
    public DataResponseBody publishSplit(@PathVariable(name = "versionId") Long versionId,
                                         @PathVariable(name = "splitSize") Double splitSize) {
        return new DataResponseBody(datasetVersionService.publishSplit(versionId, splitSize));
    }

    @ApiOperation("数据集版本url")
    @GetMapping(value = "/{datasetId}/list")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody versionList(@PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(datasetVersionService.versionList(datasetId));
    }

    @ApiOperation("数据集版本列表")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody datasetVersionList(@Validated DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        return new DataResponseBody(datasetVersionService.getList(datasetVersionQueryCriteria));
    }

    /**
     * 数据集版本列表（带图片数量，大小，标签，标签数量等详细信息）
     *
     * @param datasetVersionQueryCriteria 查询条件
     * @return DataResponseBody 数据集版本详细列表
     */
    @ApiOperation("数据集版本列表（带图片数量，大小，标签，标签数量等）")
    @GetMapping("/versionsDetailList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody datasetVersionListWithDetailInfo(@Validated DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        return new DataResponseBody(datasetVersionService.getDetailList(datasetVersionQueryCriteria));
    }
    /**
     * 数据集版本列表（带图片数量，大小，标签，标签数量等详细信息）
     *
     * @param datasetVersionQueryCriteria 查询条件
     * @return DataResponseBody 数据集版本详细列表
     */
    @ApiOperation("数据集版本列表-分页（带图片数量，大小，标签，标签数量等）")
    @GetMapping("/publicVersionsDetailList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody publicDatasetVersionListWithDetailInfo(@Validated DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        return new DataResponseBody(datasetVersionService.getPublicDetailList(datasetVersionQueryCriteria));
    }

    @ApiOperation("数据集版本列表-全部（带图片数量，大小，标签，标签数量等）")
    @GetMapping("/publicVersionsDetailListAll")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody publicDatasetVersionListAllWithDetailInfo(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        return new DataResponseBody(datasetVersionService.getPublicDetailListAll(datasetVersionQueryCriteria));
    }

    @ApiOperation("数据集版本切换")
    @PutMapping("/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody versionSwitch(@PathVariable(value = "datasetId", required = true) Long datasetId,
                                          @RequestParam(value = "versionName", required = true) String versionName) {
        datasetVersionService.versionSwitch(datasetId, versionName);
        return new DataResponseBody();
    }

    @ApiOperation("数据集版本删除")
    @DeleteMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody delete(@Validated @RequestBody DatasetVersionDeleteDTO datasetVersionDeleteDTO) {
        datasetVersionService.versionDelete(datasetVersionDeleteDTO);
        return new DataResponseBody();
    }

    @ApiOperation("获取下一个版本号")
    @GetMapping("/{datasetId}/nextVersionName")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getNextVersionName(@PathVariable(value = "datasetId", required = true) Long datasetId) {
        return new DataResponseBody(datasetVersionService.getNextVersionName(datasetId));
    }

    @ApiOperation("转换完成回调接口")
    @PostMapping(value = "/{datasetVersionId}/convert/finish")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody finishConvert(@PathVariable(value = "datasetVersionId") Long datasetVersionId, @Validated @RequestBody ConversionCreateDTO conversionCreateDTO) {
        return new DataResponseBody(datasetVersionService.finishConvert(datasetVersionId, conversionCreateDTO));
    }

    @ApiOperation("查询当前数据集版本的原始文件数量")
    @GetMapping("/{datasetId}/originFileCount")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getFileCount(@PathVariable(value = "datasetId", required = true) Long datasetId) {
        return new DataResponseBody(datasetVersionService.getSourceFileCount(datasetId));
    }

    @ApiOperation("生成ofRecord")
    @PutMapping(value = "/{datasetId}/ofRecord")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createOfRecord(@PathVariable(value = "datasetId") Long datasetId,
                                           @RequestParam(value = "versionName", required = true) String versionName) {
        datasetVersionService.createOfRecord(datasetId, versionName);
        return new DataResponseBody();
    }

    @ApiOperation("统计图片标注")
    @GetMapping(value = "/countByFileAnnotate")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetVersionFileLabelStatVO> countByFileAnnotate() {
        return new DataResponseBody<>(datasetVersionFileService.countByFileAnnotate());
    }

    @ApiOperation("统计数据集发布情况")
    @GetMapping(value = "/getDatasetVersionStat")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetVersionStatVO> getDatasetVersionStat() {
        return new DataResponseBody<>(datasetVersionService.getDatasetVersionStat());
    }

    @ApiOperation("数据集版本查询")
    @GetMapping(value = "/datasetVersionList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> datasetVersionList() {
        List<DatasetVersionVO> datasetVersionVOList = new ArrayList<>();
        for (DatasetVO dataset : datasetService.getAllDatasets()) {
            List<DatasetVersionVO> versionVOList = datasetVersionService.versionList(dataset.getId());
            versionVOList.forEach(datasetVersionVO -> {
                        datasetVersionVO.setDatasetId(dataset.getId());
                        datasetVersionVO.setName(dataset.getName());
                    }
            );
            datasetVersionVOList.addAll(versionVOList);
        }
        return new DataResponseBody<>(datasetVersionVOList);
    }

    @ApiOperation("数据集名字和版本查询")
    @GetMapping(value = "/datasetNameAndVersionList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> datasetNameAndVersionList() {
        return new DataResponseBody<>(datasetService.getAllDatasetNameAndVersion());
    }

    @ApiOperation("根据Id查询数据集版本")
    @GetMapping(value = "/getDatasetVersion/{datasetVersionId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetVersionVO> getDatasetVersionById(@PathVariable("datasetVersionId") Long datasetVersionId) {
        return new DataResponseBody<>(datasetVersionService.getDatasetVersionById(datasetVersionId));
    }

    @ApiOperation("根据Id list 查询数据集版本 list")
    @PostMapping(value = "/listByIds")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> listByIds(@RequestBody List<Long> ids) {
        return new DataResponseBody<>(datasetVersionService.listByIds(ids));
    }


    @ApiOperation("根据datasetName和versionName查找 数据集版本")
    @GetMapping(value = "/getDatasetVersion/{datasetName}/{versionName}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetVersionVO> getDatasetVersionByNameAndVersion(@PathVariable("datasetName") String datasetName, @PathVariable("versionName") String versionName) {
        List<Dataset> datasets = datasetService.queryList(new QueryWrapper<Dataset>().eq("name", datasetName).eq("deleted", false));
        if (datasets.size() == 0) {
            return new DataResponseBody<>(ResponseCode.ERROR, "没有找到对应的数据集", null);
        } else if (datasets.size() > 1) {
            return new DataResponseBody<>(ResponseCode.ERROR, "存在重名数据集", null);
        }
        Dataset dataset = datasets.get(0);
        DatasetVersion versionByDatasetIdAndVersionName = datasetVersionService.getVersionByDatasetIdAndVersionName(dataset.getId(), versionName);
        return new DataResponseBody<>(new DatasetVersionVO(versionByDatasetIdAndVersionName, dataset));
    }


    @ApiOperation("根据datasetId和versionName查找 数据集版本")
    @GetMapping(value = "/getDatasetVersionByDatasetId/{datasetId}/{versionName}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetVersionVO> getDatasetVersionByIdAndVersion(@PathVariable("datasetId") Long datasetId, @PathVariable("versionName") String versionName) {
        List<Dataset> datasets = datasetService.queryList(new QueryWrapper<Dataset>().eq("id", datasetId).eq("deleted", false));
        if (datasets.size() == 0) {
            return new DataResponseBody<>(ResponseCode.ERROR, "没有找到对应的数据集", null);
        } else if (datasets.size() > 1) {
            return new DataResponseBody<>(ResponseCode.ERROR, "存在重名数据集", null);
        }
        Dataset dataset = datasets.get(0);
        DatasetVersion versionByDatasetIdAndVersionName = datasetVersionService.getVersionByDatasetIdAndVersionName(dataset.getId(), versionName);

        return new DataResponseBody<>(new DatasetVersionVO(versionByDatasetIdAndVersionName, dataset));
    }

    @ApiOperation("根据数据集id获取datasetVersion实体列表")
    @GetMapping(value = "/getDatasetVersionList/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> getDatasetVersionList(@PathVariable("datasetId") Long datasetId) {
        List<DatasetVersionVO> datasetVersionList = datasetVersionService.getDatasetVersionList(datasetId);
        return new DataResponseBody<>(datasetVersionList);
    }

    @ApiOperation("根据数据集id获取已发布的datasetVersion实体列表")
    @GetMapping(value = "/getPublicDatasetVersionList/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetVersionVO>> getPublicDatasetVersionList(@PathVariable("datasetId") Long datasetId) {
        List<DatasetVersionVO> datasetVersionList = datasetVersionService.getPublicDatasetVersionList(datasetId);
        return new DataResponseBody<>(datasetVersionList);
    }

    @ApiOperation("公开dataVersion")
    @PutMapping(value = "/publish/{datasetId}/{versionName}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> publishDatasetVersion(
            @PathVariable("datasetId") Long datasetId,
            @PathVariable("versionName") String versionName
    ) {
        if (datasetVersionService.publishDatasetVersion(datasetId, versionName)) {
            return new DataResponseBody<>(ResponseCode.SUCCESS, "发布成功", true);
        } else {
            return new DataResponseBody<>(ResponseCode.ERROR, "发布失败", false);
        }
    }

    @ApiOperation("取消公开dataVersion")
    @PutMapping(value = "/cancelPublish/{datasetId}/{versionName}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> cancelPublishDatasetVersion(
            @PathVariable("datasetId") Long datasetId,
            @PathVariable("versionName") String versionName
    ) {
        if (datasetVersionService.cancelPublishDatasetVersion(datasetId, versionName)) {
            return new DataResponseBody<>(ResponseCode.SUCCESS, "取消发布成功", true);
        } else {
            return new DataResponseBody<>(ResponseCode.ERROR, "取消发布失败", false);
        }
    }


    @ApiOperation("数据集版本发布(但是返回versionID)")
    @PostMapping(value = "/publish2")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody publish2(@Validated(DatasetVersionCreateDTO.Create.class)
                                     @RequestBody DatasetVersionCreateDTO datasetVersionCreateDTO) {
        return new DataResponseBody(datasetVersionService.publish2(datasetVersionCreateDTO));
    }



    @ApiOperation("数据集版本导出")
    @PostMapping(value = "/datasetExport/{datasetId}/{versionName}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> datasetExport(
            @PathVariable("datasetId") Long datasetId,
            @PathVariable("versionName") String versionName
    ) {
        if (datasetVersionService.datasetExport(datasetId, versionName)) {
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(), "数据集导出任务提交", DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_EXPORT);
            return new DataResponseBody<>(ResponseCode.SUCCESS, "导出中，请等待", true);
        } else {
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(), "数据集导出任务提交失败", DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_EXPORT);
            return new DataResponseBody<>(ResponseCode.ERROR, "导出出错", false);
        }
    }

    @ApiOperation("数据集从origin目录导出（未版本化）")
    @PostMapping(value = "/datasetExportFromOrigin/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> datasetExportFromOrigin(
            @PathVariable("datasetId") Long datasetId
    ) {
        String errorMsg = datasetVersionService.datasetExportFromOrigin(datasetId);
        if (errorMsg == null) {
            return new DataResponseBody<>(ResponseCode.SUCCESS, "导出中，请等待", true);
        } else {
            return new DataResponseBody<>(ResponseCode.ERROR, errorMsg, false);
        }
    }

    @GetMapping("/checkTaskStatus")
    public DataResponseBody<String> checkTaskStatus(@RequestParam("datasetVersionId") Long datasetVersionId) {
        return new DataResponseBody<>(ResponseCode.SUCCESS, "导出状态查询成功", datasetVersionService.checkTaskStatus(datasetVersionId));
    }

    @GetMapping("/checkOriginExportStatus")
    public DataResponseBody<String> checkOriginExportStatus(@RequestParam("datasetId") Long datasetId) {
        String status = datasetVersionService.checkOriginExportStatus(datasetId);
        return new DataResponseBody<>(ResponseCode.SUCCESS, "导出状态查询成功", status);
    }

    @GetMapping("/getOriginExportUrl")
    public DataResponseBody<String> getOriginExportUrl(@RequestParam("datasetId") Long datasetId) {
        String url = datasetVersionService.getOriginExportUrl(datasetId);
        return new DataResponseBody<>(ResponseCode.SUCCESS, "导出URL查询成功", url);
    }

    /**
     * 从origin导出未标注的数据
     */
    @GetMapping("/exportUnannotatedFromOrigin")
    public DataResponseBody<Boolean> exportUnannotatedDatasetFromOrigin(@RequestParam("datasetId") Long datasetId) {
        String error = datasetVersionService.datasetExportUnannotatedFromOrigin(datasetId);
        if (error != null) {
            return new DataResponseBody<>(ResponseCode.ERROR, error, false);
        }
        return new DataResponseBody<>(ResponseCode.SUCCESS, "导出任务已提交", true);
    }

    /**
     * 检查从origin导出未标注数据的任务状态
     */
    @GetMapping("/checkOriginUnannotatedExportStatus")
    public DataResponseBody<String> checkOriginUnannotatedExportStatus(@RequestParam("datasetId") Long datasetId) {
        String status = datasetVersionService.checkOriginUnannotatedExportStatus(datasetId);
        return new DataResponseBody<>(ResponseCode.SUCCESS, "状态查询成功", status);
    }

    /**
     * 获取从origin导出未标注数据的下载 URL
     */
    @GetMapping("/getOriginUnannotatedExportUrl")
    public DataResponseBody<String> getOriginUnannotatedExportUrl(@RequestParam("datasetId") Long datasetId) {
        String url = datasetVersionService.getOriginUnannotatedExportUrl(datasetId);
        return new DataResponseBody<>(ResponseCode.SUCCESS, "导出URL查询成功", url);
    }

    @PostMapping("/recordDatasetDownload")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> recordDatasetDownload() {
        auditLogHelper.saveDownloadAuditLog("dataset_download");
        return new DataResponseBody<>(ResponseCode.SUCCESS, "记录下载成功", true);
    }

    @PostMapping(value = "/datasetExportZipsClean")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody datasetExportZipsClean() {
        datasetVersionService.cleanZips();
        return new DataResponseBody(ResponseCode.SUCCESS, "清除进程启动成功", true);
    }

    @PostMapping(value = "/getDatasetVersionLabels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<Label>> getDatasetVersionLabels(@RequestBody DatasetVersion datasetVersion) {
        List<Label> labels = datasetVersionService.getDatasetVersionLabels(datasetVersion.getDatasetId(), datasetVersion.getVersionName());
        return new DataResponseBody<>(labels);
    }

    @GetMapping(value = "/getDatasetVersionLabelsByVersionId")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<Label>> getDatasetVersionLabels(@RequestParam(value = "datasetVersionId", required = true) Long datasetVersionId) {
        DatasetVersionVO datasetVersion = datasetVersionService.getDatasetVersionById(datasetVersionId);
        if (datasetVersion == null) {
            return new DataResponseBody<>(ResponseCode.ERROR, "数据集版本不存在", null);
        }
        List<Label> labels = datasetVersionService.getDatasetVersionLabels(datasetVersion.getDatasetId(), datasetVersion.getVersionName());
        return new DataResponseBody<>(labels);
    }

    @GetMapping(value = "getVersionsImgCount")
    @ApiOperation("获取数据集版本图片数量")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Integer> getVersionsImgCount(@RequestParam(value = "datasetVersionIds") List<Long> datasetVersionIds) {
        if (datasetVersionIds == null || datasetVersionIds.isEmpty()) {
            return new DataResponseBody<>(0);
        }
        List<DatasetVersionVO> datasetVersionVos = datasetVersionService.listByIds(datasetVersionIds);
        if (datasetVersionVos.isEmpty()) {
            return new DataResponseBody<>(ResponseCode.ERROR, "数据集版本不存在", 0);
        }
        Integer count = 0;
        for (DatasetVersionVO datasetVersionVO : datasetVersionVos) {
            count += datasetVersionService.getVersionImgCount(datasetVersionVO.getDatasetId(), datasetVersionVO.getVersionName());
        }
        return new DataResponseBody<>(count);
    }

    @GetMapping(value = "getFilteredVersionsImgCount")
    @ApiOperation("获取数据集版本图片数量")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Integer> getFilteredVersionsImgCount(@RequestParam(value = "datasetVersionIds") List<Long> datasetVersionIds, @RequestParam List<String> labelNames ) {
        if (datasetVersionIds == null || datasetVersionIds.isEmpty()) {
            return new DataResponseBody<>(0);
        }
        List<DatasetVersionVO> datasetVersionVos = datasetVersionService.listByIds(datasetVersionIds);
        if (datasetVersionVos.isEmpty()) {
            return new DataResponseBody<>(ResponseCode.ERROR, "数据集版本不存在", 0);
        }
        Integer count = 0;
        for (DatasetVersionVO datasetVersionVO : datasetVersionVos) {
            count += datasetVersionService.getFilteredVersionImgCount(datasetVersionVO.getDatasetId(), datasetVersionVO.getVersionName(), labelNames);
        }
        return new DataResponseBody<>(count);
    }


    @GetMapping(value = "getImportingVersionInfo")
    @ApiOperation("获取导入中的数据集版本信息")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<ImportingVersionInfoDTO> getImportingVersionInfo(@RequestParam(value = "fromVersionId") Long fromVersionId, @RequestParam(value = "targetDatasetId") Long targetDatasetId) {
        ImportingVersionInfoDTO importingVersionInfo = datasetVersionService.getImportingVersionInfo(fromVersionId, targetDatasetId);
        return new DataResponseBody<>(importingVersionInfo);
    }

    /**
     * 批量根据版本ID获取标签列表的接口
     *
     * @param versionIds 数据集版本ID列表
     * @return Map，key为版本ID，value为该版本的标签列表
     */
    @PostMapping("/labels/batch")
    public DataResponseBody<Map<Long, List<Label>>> getLabelsForVersions(@RequestBody List<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return new DataResponseBody<>(Collections.emptyMap());
        }
        Map<Long, List<Label>> labelsMap = datasetVersionService.getLabelsForVersions(versionIds);
        return new DataResponseBody<>(labelsMap);
    }


    /**
     * 异步数据集合并和切分接口
     */
    @PostMapping("/datasetAssembleFromMultipleVersionAsync")
    public DataResponseBody<String> datasetAssembleFromMultipleVersionAsync(@RequestBody DatasetMergeRequest request) {
        try {
            //System.out.println("request = " + request);
            // 参数验证
            if (request.getVersions() == null || request.getVersions().isEmpty()) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "版本为空", null);
            }

            if (StringUtils.isBlank(request.getTargetDir())) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "目标目录为空", null);
            }

            if (StringUtils.isBlank(request.getSplitRatio()) && request.getIsSplit()) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "切分比例未提供", null);
            }

            // 生成任务ID
            String taskId = UUID.randomUUID().toString();
            taskStatusService.createTask(taskId);
            // 异步执行数据集处理，并保存CompletableFuture引用
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                datasetVersionService.processDatasetAsync(taskId, request);
            }, asyncExecutor);

            // 保存Future引用以便后续取消
            taskStatusService.setTaskFuture(taskId, future);

            return new DataResponseBody<>(ResponseCode.SUCCESS, "任务已提交", taskId);

        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, "提交失败: " + e.getMessage(), null);
        }
    }

    /**
     * 量化图片文件夹生成
     */
    @PostMapping("/quantizedMerge")
    public DataResponseBody<String> quantizedMerge(@RequestBody QuantizedMergeRequest request) {
        try {
            //System.out.println("request = " + request);
            // 参数验证
            if (request.getVersions() == null || request.getVersions().isEmpty()) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "版本为空", null);
            }

            if (StringUtils.isBlank(request.getTargetDir())) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "目标目录为空", null);
            }

            if ( request.getTotal()<=0) {
                return new DataResponseBody<>(ResponseCode.BADREQUEST, "图片数量无效", null);
            }

            // 生成任务ID
            String taskId = UUID.randomUUID().toString();
            taskStatusService.createTask(taskId);
            // 异步执行数据集处理，并保存CompletableFuture引用
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                datasetVersionService.processQuantizedAsync(taskId, request);
            }, asyncExecutor);

            // 保存Future引用以便后续取消
            taskStatusService.setTaskFuture(taskId, future);

            return new DataResponseBody<>(ResponseCode.SUCCESS, "任务已提交", taskId);

        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, "提交失败: " + e.getMessage(), null);
        }
    }

    /**
     * 查询任务状态接口
     */
    @GetMapping("/datasetTask/{taskId}/status")
    public DataResponseBody<TaskStatusVO> getTaskStatus(@PathVariable String taskId) {
        try {
            TaskStatusVO taskStatus = taskStatusService.getTaskStatus(taskId);
            if (taskStatus == null) {
                return new DataResponseBody<>(ResponseCode.ERROR, "任务不存在", null);
            }
            return new DataResponseBody<>(ResponseCode.SUCCESS, "success", taskStatus);
        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, "查询失败", null);
        }
    }

    /**
     * 停止任务接口
     */
    @PostMapping("/datasetTask/{taskId}/cancel")
    public DataResponseBody<String> cancelTask(@PathVariable String taskId) {
        try {
            boolean cancelled = taskStatusService.cancelTask(taskId);
            if (cancelled) {
                return new DataResponseBody<>(ResponseCode.SUCCESS, "任务已取消", taskId);
            } else {
                return new DataResponseBody<>(ResponseCode.ERROR, "任务取消失败，可能任务不存在或已完成", null);
            }
        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, "取消失败: " + e.getMessage(), null);
        }
    }

    @GetMapping("/batchGetVersionsLabelCountMap")
    public DataResponseBody<List<DatasetVersionDetailVO>> batchGetVersionsLabelCountMap(@RequestParam List<Long> versionIds, @RequestParam List<String> labelNames) {
        List<DatasetVersionDetailVO> versions = datasetVersionService.batchGetVersionsLabelCountMap(versionIds, labelNames);
        return new DataResponseBody<>(versions);
    }

    @GetMapping("/getVersionIdsWithDatasetInfo")
    public DataResponseBody<List<DatasetVersionInfoVO>> getVersionIdsWithDatasetInfo(@RequestParam List<Long> datasetIds) {
        List<DatasetVersionInfoVO> versions = datasetVersionService.getVersionIdsWithDatasetInfo(datasetIds);
        return new DataResponseBody<>(versions);
    }


    @GetMapping("/CalculateImportStats")
    public DataResponseBody<Map<String, Object>> CalculateImportStats(@RequestParam(value = "sourceVersionId") Long sourceVersionId, @RequestParam(value = "targetLabelNames") List<String> targetLabelNames) throws Exception {
        Map<String, Object> res = datasetVersionService.quickCalculateImportStats(sourceVersionId,targetLabelNames);
        return new DataResponseBody<>(res);
    }





    @PreDestroy
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


}
