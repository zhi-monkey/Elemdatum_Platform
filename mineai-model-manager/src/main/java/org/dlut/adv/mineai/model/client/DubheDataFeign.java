package org.dlut.adv.mineai.model.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dlut.adv.mineai.core.entity.Notification;
import org.dlut.adv.mineai.core.vo.*;
import org.dlut.adv.mineai.model.domain.dto.DatasetCreateDTO;
import org.dlut.adv.mineai.model.domain.dto.DatasetDeleteDTO;
import org.dlut.adv.mineai.model.domain.dto.GuidedAndLabelsVO;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.domain.vo.TaskStatusVO;
import org.dlut.adv.mineai.model.dto.DatasetLabelInfoDTO;
import org.dlut.adv.mineai.model.dto.DatasetMergeRequest;
import org.dlut.adv.mineai.model.dto.DatasetVersionCreateDTO;
import org.dlut.adv.mineai.model.dto.QuantizedMergeRequest;
import org.dlut.adv.mineai.model.vo.DatasetVersionDetailVO;
import org.dlut.adv.mineai.model.vo.DatasetVersionInfoVO;
import org.dlut.adv.mineai.model.vo.RtspCaptureExecutionVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Component
@FeignClient(value = "dubhe-data")
public interface DubheDataFeign {

    @GetMapping("/datasets/versions/getDatasetVersion/{datasetVersionId}")
    DataResponseBody<DatasetVersionVO> selectDatasetVersionById(@RequestHeader("Authorization") String authorization, @PathVariable Long datasetVersionId);

    @GetMapping("/datasets/getAllDatasets")
    DataResponseBody<List<DatasetVO>> getAllDatasets(@RequestHeader("Authorization") String authorization);

    @GetMapping("/datasets/guidedAndLabels/{datasetId}")
    DataResponseBody<GuidedAndLabelsVO> guidedAndLabels(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetId") Long datasetId);

    @GetMapping("/datasets/guidedLabelsNames/{datasetStartList}")
    DataResponseBody<List<List<String>>> guidedLabelsNames(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetStartList") List<Long> datasetStartList);

    @PostMapping("/datasets")
    DataResponseBody createDataset(@RequestHeader("Authorization") String authorization, @Validated(DatasetCreateDTO.Create.class) @RequestBody DatasetCreateDTO datasetCreateDTO);

    @DeleteMapping("/datasets")
    DataResponseBody deleteDataset(@RequestHeader("Authorization") String authorization, @Validated(DatasetDeleteDTO.Create.class) @RequestBody DatasetDeleteDTO datasetDeleteDTO);

    @GetMapping(value = "/datasets/{datasetId}")
    DataResponseBody get(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetId") Long datasetId);

    @GetMapping(value = "/datasets/batchGet/{datasetIds}")
    DataResponseBody batchGet(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetIds") List<Long> datasetIds);

    @GetMapping("/datasets/selectByType/{datasetType}")
    DataResponseBody<List<DatasetVO>> getDatasetByDataType(@RequestHeader("Authorization") String authorization, @PathVariable Integer datasetType);

    @GetMapping("/datasets/versions/datasetVersionList")
    DataResponseBody<List<DatasetVersionVO>> datasetVersionList(@RequestHeader("Authorization") String authorization);

    @GetMapping(value = "/datasets/versions/datasetNameAndVersionList")
    DataResponseBody<List<DatasetVersionVO>> datasetNameAndVersionList(@RequestHeader("Authorization") String authorization);

    @PostMapping("/datasets/versions/listByIds")
    DataResponseBody<List<DatasetVersionVO>> findDatasetVersionsByIds(@RequestHeader("Authorization") String authorization, @RequestBody List<Long> ids);

    @PostMapping("/datasets/versions/split/{versionId}/{splitSize}")
    DataResponseBody<List<Long>> publishSplit(@RequestHeader("Authorization") String authorization,
                                              @PathVariable(name = "versionId") Long versionId,
                                              @PathVariable(name = "splitSize") Double splitSize);

    @GetMapping(value = "/datasets/versions/getDatasetVersion/{datasetName}/{versionName}")
    DataResponseBody<DatasetVersionVO> getDatasetVersionByNameAndVersion(@RequestHeader("Authorization") String authorization, @PathVariable("datasetName") String datasetName, @PathVariable("versionName") String versionName);

    @PostMapping("/datasets/autoLabel/{datasetId}/start")
    DataResponseBody<List<DatasetVersionVO>> autoLabelStart(@RequestHeader("Authorization") String authorization, @PathVariable(value = "datasetId") Long datasetId);

    @PostMapping("/datasets/autoLabel/{datasetId}/startWithNames")
    DataResponseBody<List<DatasetVersionVO>> autoLabelStartWithNames(@RequestHeader("Authorization") String authorization, @PathVariable(value = "datasetId") Long datasetId, @RequestBody List<String> labelNames);

    @PostMapping("/datasets/autoLabel/{datasetId}/startWithNamesAndClearOption")
    DataResponseBody<List<DatasetVersionVO>> autoLabelStartWithNamesAndClearOption(@RequestHeader("Authorization") String authorization, @PathVariable(value = "datasetId") Long datasetId, @RequestParam(value = "clearExistingLabels") Boolean clearExistingLabels, @RequestParam(value = "requireManualConfirmation") Boolean requireManualConfirmation, @RequestParam(value = "jobName", required = false) String jobName, @RequestBody List<String> labelNames);

    @PostMapping("/datasets/autoLabel/{datasetId}/end")
    DataResponseBody<List<DatasetVersionVO>> autoLabelEnd(@RequestHeader("Authorization") String authorization, @PathVariable(value = "datasetId") Long datasetId, @RequestBody Boolean isFailed);

    @GetMapping("/datasets/versions/getDatasetVersionList/{datasetId}")
    DataResponseBody<List<DatasetVersionVO>> selectDatasetVersionByDatasetId(@RequestHeader("Authorization") String authorization, @PathVariable Long datasetId);

    @PostMapping("/datasets/saveLabelByReadFile")
    DataResponseBody<List<Long>> saveLabelByReadFile(@RequestHeader("Authorization") String authorization, @RequestBody String appZipPath);

    @DeleteMapping("/datasets/removeLabelByIds")
    DataResponseBody<Void> removeLabelByIds(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> labelIds);

    @GetMapping("/datasets/labels/ids")
    DataResponseBody<List<LabelDTO>> findLabelByIds(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> labelIds);


    /**
     * @param authorization
     * @param datasetVersionCreateDTO
     * @return 发布的数据集版本（datasetVersion）id
     */
    @PostMapping("/datasets/versions/publish2")
    DataResponseBody<Long> publish(@RequestHeader("Authorization") String authorization, @RequestBody DatasetVersionCreateDTO datasetVersionCreateDTO);


    @PostMapping("/datasets/{datasetId}/bandLabels")
    DataResponseBody<Void> bandLabels(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetId") Long datasetId, @RequestBody List<Long> labelIds);

    @GetMapping("/datasets/isPublishing/{datasetId}")
    DataResponseBody<Boolean> isPublishingByDatasetId(@RequestHeader("Authorization") String authorization, @PathVariable(name = "datasetId") Long datasetId);

    @GetMapping("/datasets/versions/getDatasetVersionLabelsByVersionId")
    DataResponseBody<List<LabelDTO>> getDatasetVersionLabelsByVersionId(@RequestHeader("Authorization") String authorization, @RequestParam Long datasetVersionId);

    @PutMapping("/datasets/versions/publish/{datasetId}/{versionName}")
    DataResponseBody<Boolean> publishDatasetVersion(@RequestHeader("Authorization") String authorization, @PathVariable("datasetId") Long datasetId, @PathVariable("versionName") String versionName);

    @GetMapping("/datasets/versions/getVersionsImgCount")
    DataResponseBody<Integer> getVersionsImgCount(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetVersionIds);

    @GetMapping("/datasets/versions/getFilteredVersionsImgCount")
    DataResponseBody<Integer> getFilteredVersionsImgCount(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetVersionIds, @RequestParam List<String> labelNames);

    @GetMapping("/datasets/getDatasetImageCount/{datasetId}")
    DataResponseBody<Integer> getDatasetImageCount(@RequestHeader("Authorization") String authorization, @PathVariable("datasetId") Long datasetId);

    @GetMapping("/datasets/progress")
    DataResponseBody<Map<Long, ProgressVO>> getDatasetsProgress(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetIds);

    @GetMapping("/datasets/status")
    DataResponseBody<Map<Long, Map<String, Object>>> getDatasetStatus(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetIds);

    @GetMapping("datasets/getDatasetLabels")
    DataResponseBody<List<DatasetLabelInfoDTO>> getDatasetLabels(@RequestHeader("Authorization") String authorization, @RequestParam Long datasetId);

    @GetMapping("/datasets/versions//labels/batch")
    DataResponseBody<Map<Long, List<LabelDTO>>> getLabelsForVersions(@RequestHeader("Authorization") String authorization, @RequestBody List<Long> versionIds);

    @PostMapping("/datasets/versions/datasetAssembleFromMultipleVersionAsync")
    DataResponseBody<String> datasetAssembleFromMultipleVersionAsync(
            @RequestHeader("Authorization") String authorization,
            @RequestBody DatasetMergeRequest request);

    @PostMapping("/datasets/versions/quantizedMerge")
    DataResponseBody<String> quantizedMerge(
            @RequestHeader("Authorization") String authorization,
            @RequestBody QuantizedMergeRequest request);


    @GetMapping("/datasets/versions/datasetTask/{taskId}/status")
    DataResponseBody<TaskStatusVO> getTaskStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String taskId);

    @PostMapping("/datasets/versions/datasetTask/{taskId}/cancel")
    DataResponseBody<String> cancelTask(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String taskId);
    @GetMapping("/datasets/group/getDatasetGroupsMapByDatasetIds")
    DataResponseBody<List<DatasetGroupMappingVO>> getDatasetGroupsMapByDatasetIds(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetIds);

    @GetMapping("/datasets/versions/batchGetVersionsLabelCountMap")
    DataResponseBody<List<DatasetVersionDetailVO>> batchGetVersionsLabelCountMap(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> versionIds, @RequestParam List<String> labelNames);

    @GetMapping("/datasets/group//getDatasetGroupNameByDatasetId")
    DataResponseBody<String> getDatasetGroupNameByDatasetId(@RequestHeader("Authorization") String authorization, @RequestParam Long datasetId);

    @GetMapping("/datasets/versions/getVersionIdsWithDatasetInfo")
    DataResponseBody<List<DatasetVersionInfoVO>> getVersionIdsWithDatasetInfo(@RequestHeader("Authorization") String authorization, @RequestParam List<Long> datasetIds);

    @GetMapping("/notifications")
    DataResponseBody<Page<Notification>> page(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) Integer notificationType,
            @RequestParam(required = false) String operationType);

    /**
     * 分页查询通知（格式化）
     */
    @GetMapping("/notifications/formatted")
    DataResponseBody<Page<NotificationVO>> pageFormatted(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) Integer notificationType,
            @RequestParam(required = false) String operationType);

    /**
     * 获取未读数量
     */
    @GetMapping("/notifications/unread-count")
    DataResponseBody<Integer> unreadCount(
            @RequestHeader("Authorization") String authorization);

    /**
     * 标记单条为已读
     */
    @PostMapping("/notifications/{id}/read")
    DataResponseBody<Boolean> markRead(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id);

    /**
     * 标记全部为已读
     */
    @PostMapping("/notifications/read-all")
    DataResponseBody<Integer> markAllRead(
            @RequestHeader("Authorization") String authorization);

    /**
     * 创建通知（内部/管理使用）
     */
    @PostMapping("/notifications/createAsync")
    DataResponseBody<Long> createAsync(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long toUserId,
            @RequestParam Integer notificationType,
            @RequestParam String operationType,
            @RequestBody String payload);

    // ===================== 数据回流 =====================

    /**
     * 触发数据回流任务采集，返回本次创建的 RtspCaptureExecution ID。
     * dubhe-data 侧需暴露此接口（或现有 startCapture 改为返回 executionId）。
     */
    @PostMapping("/rtsp-capture-tasks/{taskId}/start")
    DataResponseBody<Boolean> startCapture(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("taskId") Long taskId);

    @PostMapping("/rtsp-capture-tasks/{taskId}/start")
    DataResponseBody<Boolean> startCapture(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("taskId") Long taskId,
            @RequestBody Map<String, Object> request);

    @GetMapping("/rtsp-capture-executions")
    DataResponseBody<Page<RtspCaptureExecutionVO>> pageCaptureExecutions(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("current") Long current,
            @RequestParam("size") Long size,
            @RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "status", required = false) Integer status);

    /**
     * 查询 RtspCaptureExecution 详情（包含 datasetId、status 等）。
     */
    @GetMapping("/rtsp-capture-executions/{executionId}")
    DataResponseBody<RtspCaptureExecutionVO> getCaptureExecution(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("executionId") Long executionId);

    @PostMapping("/rtsp-capture-executions/{executionId}/cancel")
    DataResponseBody<Boolean> cancelCaptureExecution(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("executionId") Long executionId);

    /**
     * 创建数据回流任务（RtspCaptureTask）
     */
    @PostMapping("/rtsp-capture-tasks")
    DataResponseBody<Object> createRtspCaptureTask(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RtspCaptureTaskCreateDTO dto);

    @GetMapping("/rtsp-capture-tasks/{id}")
    DataResponseBody<RtspCaptureTaskVO> getRtspCaptureTaskDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id);

    @GetMapping("/rtsp-capture-tasks")
    DataResponseBody<Page<RtspCaptureTaskVO>> pageRtspCaptureTasks(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("current") Long current,
            @RequestParam("size") Long size,
            @RequestParam(value = "taskName", required = false) String taskName);

    /**
     * 这里仍调用 dubhe-data 的历史接口名 inference-devices，
     * 当前前端业务展示语义已调整为“模型接收地址”。
     */
    @GetMapping("/inference-devices")
    DataResponseBody<Page<InferenceDeviceVO>> pageInferenceDevices(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("current") Long current,
            @RequestParam("size") Long size,
            @RequestParam(value = "deviceName", required = false) String deviceName);

    /**
     * 数据回流任务创建 DTO（内部类）
     */
    @lombok.Data
    class RtspCaptureTaskCreateDTO {
        private String taskName;
        private String sourceType;  // "RTSP" 或 "HTTP"
        private Long rtspSourceId;
        private Long httpCameraServerId;
        private Long httpCameraId;
        private Long datasetGroupId;
        private String datasetGroupName;
        private Integer captureInterval;
        private Integer imageQuantity;
        /** 数据集标注类型（102=目标检测, 103=目标分割），为空则 dubhe-data 默认 102 */
        private Integer annotateType;
        /** 绑定的标签 ID 列表（dubhe-data 全局 label 表的 id），自动创建数据集后立即绑定 */
        private List<Long> labelIds;
    }

    @lombok.Data
    class RtspCaptureTaskVO {
        private Long id;
        private String taskName;
        private Long httpCameraServerId;
        private Long httpCameraId;
        private Long datasetGroupId;
        private Integer captureInterval;
        private Integer imageQuantity;
    }

    @lombok.Data
    class InferenceDeviceVO {
        /** 历史 VO 命名，实际业务语义对应“模型接收地址”。 */
        private Long id;
        private String deviceName;
        private String inferenceIp;
        private Integer inferencePort;
        private String remark;
        private String createdBy;
    }
}