package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.dao.MultiImageFileMapper;
import org.dubhe.data.dao.MultiDatasetDatasetGroupMapper;
import org.dubhe.data.dao.MultiDatasetMapper;
import org.dubhe.data.dao.MultiPointcloudFileMapper;
import org.dubhe.data.dao.MultiPointcloudImageRelationMapper;
import org.dubhe.data.dao.MultiRecordImportTaskMapper;
import org.dubhe.data.domain.dto.MultiDatasetCreateDTO;
import org.dubhe.data.domain.dto.MultiImageAssetDTO;
import org.dubhe.data.domain.dto.MultiPointcloudAssetDTO;
import org.dubhe.data.domain.dto.MultiRecordImportAssetsDTO;
import org.dubhe.data.domain.dto.MultiRecordImportClaimDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFailDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFinishDTO;
import org.dubhe.data.domain.dto.MultiRecordImportProgressDTO;
import org.dubhe.data.domain.dto.MultiRecordUploadUrlDTO;
import org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO;
import org.dubhe.data.domain.dto.ImportTransferTaskProgressDTO;
import org.dubhe.data.domain.entity.MultiDataset;
import org.dubhe.data.domain.entity.MultiDatasetDatasetGroup;
import org.dubhe.data.domain.entity.MultiImageFile;
import org.dubhe.data.domain.entity.MultiPointcloudFile;
import org.dubhe.data.domain.entity.MultiPointcloudImageRelation;
import org.dubhe.data.domain.entity.MultiRecordImportTask;
import org.dubhe.data.domain.vo.MultiRecordUploadUrlVO;
import org.dubhe.data.service.MultiDatasetService;
import org.dubhe.data.service.ImportTransferTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
@Service
public class MultiDatasetServiceImpl implements MultiDatasetService {
    private static final int STATUS_PENDING_IMPORT = 3001;
    private static final int STATUS_IMPORTING = 3002;
    private static final int STATUS_READY = 3003;
    private static final int STATUS_IMPORT_FAILED = 3004;
    private static final String TASK_UPLOADING = "UPLOADING";
    private static final String TASK_QUEUED = "QUEUED";
    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_RETRY = "RETRY";
    private static final String TASK_SUCCEEDED = "SUCCEEDED";
    private static final String TASK_FAILED = "FAILED";
    private static final String QUEUE_STREAM = "multi:record-import";
    private static final int MAX_ASSET_BATCH = 200;
    private static final long MAX_IMAGE_OFFSET_NS = 50_000_000L;

    private final MultiDatasetMapper datasetMapper;
    private final MultiDatasetDatasetGroupMapper datasetGroupRelationMapper;
    private final DatasetGroupMapper groupMapper;
    private final UserContextService userContextService;
    private final MultiRecordImportTaskMapper importTaskMapper;
    private final MultiImageFileMapper imageFileMapper;
    private final MultiPointcloudFileMapper pointcloudFileMapper;
    private final MultiPointcloudImageRelationMapper relationMapper;
    private final MinioUtil minioUtil;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ImportTransferTaskService transferTaskService;

    @Value("${minio.bucketName}")
    private String bucketName;
    /** Record 文件通常是数 GB，不能使用普通小文件的 5 分钟签名有效期。 */
    @Value("${multi.record-import.presigned-url-expiry-seconds:86400}")
    private Integer presignedUrlExpiry;

    public MultiDatasetServiceImpl(MultiDatasetMapper datasetMapper,
                                   MultiDatasetDatasetGroupMapper datasetGroupRelationMapper,
                                   DatasetGroupMapper groupMapper,
                                   UserContextService userContextService,
                                   MultiRecordImportTaskMapper importTaskMapper,
                                   MultiImageFileMapper imageFileMapper,
                                   MultiPointcloudFileMapper pointcloudFileMapper,
                                   MultiPointcloudImageRelationMapper relationMapper,
                                   MinioUtil minioUtil,
                                   RedisTemplate<Object, Object> redisTemplate,
                                   ImportTransferTaskService transferTaskService) {
        this.datasetMapper = datasetMapper;
        this.datasetGroupRelationMapper = datasetGroupRelationMapper;
        this.groupMapper = groupMapper;
        this.userContextService = userContextService;
        this.importTaskMapper = importTaskMapper;
        this.imageFileMapper = imageFileMapper;
        this.pointcloudFileMapper = pointcloudFileMapper;
        this.relationMapper = relationMapper;
        this.minioUtil = minioUtil;
        this.redisTemplate = redisTemplate;
        this.transferTaskService = transferTaskService;
    }

    @Override
    @Transactional
    public MultiDataset create(MultiDatasetCreateDTO dto) {
        if (groupMapper.selectById(dto.getDatasetGroupId()) == null) {
            throw new BusinessException("Multi-modal dataset group does not exist");
        }

        MultiDataset dataset = new MultiDataset()
                .setName(dto.getName().trim())
                .setLabelGroupId(dto.getLabelGroupId())
                .setRemark(dto.getRemark())
                .setStatus(STATUS_PENDING_IMPORT)
                .setUploadStatus("CREATING");
        dataset.setDeleted(false);
        dataset.setCreateUserId(userContextService.getCurUserId());
        datasetMapper.insert(dataset);
        datasetGroupRelationMapper.insert(new MultiDatasetDatasetGroup(dto.getDatasetGroupId(), dataset.getId()));
        dataset.setStoragePrefix("dataset/multi/" + dataset.getId());
        datasetMapper.updateById(dataset);
        return dataset;
    }

    @Override
    @Transactional
    public MultiRecordUploadUrlVO createRecordUploadUrl(Long datasetId, MultiRecordUploadUrlDTO dto) {
        MultiDataset dataset = requireDataset(datasetId);
        String name = cleanFileName(dto.getName());
        if (!name.toLowerCase(Locale.ROOT).endsWith(".record")) {
            throw new BusinessException("Only .record files are supported");
        }
        MultiRecordImportTask task = new MultiRecordImportTask()
                .setMultiDatasetId(datasetId)
                .setSourceName(name)
                .setSourceBucket(bucketName)
                .setStatus(TASK_UPLOADING)
                .setScannedMessages(0L)
                .setProcessedImages(0L)
                .setProcessedPointclouds(0L)
                .setAttemptCount(0);
        task.setDeleted(false);
        task.setCreateUserId(userContextService.getCurUserId());
        // source_object_key is NOT NULL, but the final path contains the
        // auto-generated task id. Use a temporary non-empty value for the
        // insert, then replace it once the id is available.
        task.setSourceObjectKey("pending/" + UUID.randomUUID().toString().replace("-", ""));
        importTaskMapper.insert(task);
        ImportTransferTaskCreateDTO transfer = new ImportTransferTaskCreateDTO();
        transfer.setTaskName("数据集“" + dataset.getName() + "”导入任务");
        transfer.setDatasetType("MULTI");
        transfer.setDatasetId(datasetId);
        transfer.setSourceType("RECORD");
        transfer.setTotalFiles(0);
        Long transferTaskId = transferTaskService.create(transfer);
        importTaskMapper.updateById(new MultiRecordImportTask().setId(task.getId()).setTransferTaskId(transferTaskId));
        String objectKey = ensurePrefix(dataset) + "/record/" + task.getId() + "/source/"
                + UUID.randomUUID().toString().replace("-", "") + "-" + name;
        importTaskMapper.updateById(new MultiRecordImportTask().setId(task.getId()).setSourceObjectKey(objectKey));
        String uploadUrl = minioUtil.getEncryptedPutUrl(bucketName, objectKey, presignedUrlExpiry);
        return new MultiRecordUploadUrlVO(task.getId(), datasetId, bucketName, objectKey, uploadUrl);
    }

    @Override
    @Transactional
    public MultiRecordImportTask commitRecord(Long datasetId, Long taskId) {
        MultiRecordImportTask task = requireTask(datasetId, taskId);
        if (!TASK_UPLOADING.equals(task.getStatus())) {
            throw new BusinessException("Record upload has already been committed");
        }
        Map<String, Object> details = minioUtil.getFileDetails(task.getSourceBucket(), task.getSourceObjectKey());
        if (details == null) {
            throw new BusinessException("Uploaded Record was not found in MinIO");
        }
        task.setSourceSize(asLong(details.get("size")))
                .setSourceEtag((String) details.get("etag"))
                .setStatus(TASK_QUEUED)
                .setErrorMessage(null);
        importTaskMapper.updateById(task);
        transferProgress(task.getTransferTaskId(), "PROCESS", 10, "Record 文件上传完成，等待处理");
        datasetMapper.updateById(new MultiDataset().setId(datasetId).setStatus(STATUS_PENDING_IMPORT)
                .setUploadStatus("READY").setUploadError(null));
        scheduleTaskDispatch(task.getId());
        return task;
    }

    @Override
    public MultiRecordImportTask getImportTask(Long datasetId, Long taskId) {
        return requireTask(datasetId, taskId);
    }

    @Override
    @Transactional
    public MultiRecordImportTask claimImportTask(MultiRecordImportClaimDTO dto) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (importTaskMapper.claim(dto.getTaskId(), dto.getWorkerId(), now) != 1) {
            throw new BusinessException("Import task is no longer available");
        }
        MultiRecordImportTask task = requireTask(dto.getTaskId());
        datasetMapper.updateById(new MultiDataset().setId(task.getMultiDatasetId()).setStatus(STATUS_IMPORTING)
                .setUploadStatus("IMPORTING").setUploadError(null));
        transferProgress(task.getTransferTaskId(), "PROCESS", 30, "开始解析 Record 文件");
        return task;
    }

    @Override
    public void reportProgress(Long taskId, MultiRecordImportProgressDTO dto) {
        updateHeartbeat(taskId, dto.getWorkerId(), dto.getScannedMessages(), dto.getProcessedImages(), dto.getProcessedPointclouds());
        MultiRecordImportTask task = importTaskMapper.selectById(taskId);
        long scanned = Math.max(0L, dto.getScannedMessages());
        long processed = Math.max(0L, dto.getProcessedImages()) + Math.max(0L, dto.getProcessedPointclouds());
        int progress = scanned == 0 ? 30 : (int) Math.min(95L, Math.max(30L, processed * 100L / scanned));
        if (task != null && task.getTransferTaskId() != null) {
            ImportTransferTaskProgressDTO transferProgress = new ImportTransferTaskProgressDTO();
            transferProgress.setStage("PROCESS");
            transferProgress.setProgress(progress);
            transferProgress.setTotalFiles(toTaskFileCount(scanned));
            transferProgress.setSuccessFiles(toTaskFileCount(processed));
            transferProgress.setMessage("Record 正在处理");
            transferTaskService.progress(task.getTransferTaskId(), transferProgress);
        }
    }

    @Override
    @Transactional
    public void saveAssets(Long taskId, MultiRecordImportAssetsDTO dto) {
        if (dto.getImages().size() + dto.getPointclouds().size() > MAX_ASSET_BATCH) {
            throw new BusinessException("Asset batch is too large");
        }
        MultiRecordImportTask task = requireRunningTask(taskId, dto.getWorkerId());
        for (MultiImageAssetDTO asset : dto.getImages()) {
            validateWorkerObjectKey(task, asset.getObjectKey());
            if (imageFileMapper.selectCount(new QueryWrapper<MultiImageFile>().eq("dataset_id", task.getMultiDatasetId())
                    .eq("object_key", asset.getObjectKey())) == 0) {
                MultiImageFile file = new MultiImageFile().setFileType("image").setDatasetId(task.getMultiDatasetId())
                        .setImportTaskId(taskId).setName(cleanFileName(asset.getName())).setSensorName(sensorName(asset.getTopic()))
                        .setTopicName(asset.getTopic()).setCaptureTimestampNs(asset.getTimestampNs()).setSequenceNo(asset.getSequenceNo())
                        .setUrl(asset.getObjectKey()).setObjectKey(asset.getObjectKey()).setStorageBucket(task.getSourceBucket())
                        .setFileSize(asset.getFileSize()).setContentType(asset.getContentType()).setEtag(asset.getEtag())
                        .setWidth(asset.getWidth()).setHeight(asset.getHeight()).setImageEncoding(imageEncoding(asset.getContentType()))
                        .setUploadStatus("READY");
                file.setDeleted(false);
                imageFileMapper.insert(file);
            }
        }
        for (MultiPointcloudAssetDTO asset : dto.getPointclouds()) {
            validateWorkerObjectKey(task, asset.getObjectKey());
            if (pointcloudFileMapper.selectCount(new QueryWrapper<MultiPointcloudFile>().eq("dataset_id", task.getMultiDatasetId())
                    .eq("object_key", asset.getObjectKey())) == 0) {
                MultiPointcloudFile file = new MultiPointcloudFile().setFileType("pcd").setDatasetId(task.getMultiDatasetId())
                        .setImportTaskId(taskId).setName(cleanFileName(asset.getName())).setSensorName(sensorName(asset.getTopic()))
                        .setTopicName(asset.getTopic()).setCaptureTimestampNs(asset.getTimestampNs()).setSequenceNo(asset.getSequenceNo())
                        .setUrl(asset.getObjectKey()).setObjectKey(asset.getObjectKey()).setStorageBucket(task.getSourceBucket())
                        .setFileSize(asset.getFileSize()).setContentType(asset.getContentType()).setEtag(asset.getEtag())
                        .setPointCount(asset.getPointCount()).setDataEncoding("ascii").setUploadStatus("READY");
                file.setDeleted(false);
                pointcloudFileMapper.insert(file);
            }
        }
        updateHeartbeat(taskId, dto.getWorkerId(), dto.getScannedMessages(), dto.getProcessedImages(), dto.getProcessedPointclouds());
    }

    @Override
    @Transactional
    public void finishImport(Long taskId, MultiRecordImportFinishDTO dto) {
        MultiRecordImportTask task = requireRunningTask(taskId, dto.getWorkerId());
        buildPointcloudImageRelations(task.getMultiDatasetId(), taskId);
        task.setStatus(TASK_SUCCEEDED).setFinishedAt(new Timestamp(System.currentTimeMillis())).setErrorMessage(null);
        importTaskMapper.updateById(task);
        if (task.getTransferTaskId() != null) {
            transferTaskService.complete(task.getTransferTaskId(), "多模态 Record 导入完成");
        }
        datasetMapper.updateById(new MultiDataset().setId(task.getMultiDatasetId()).setStatus(STATUS_READY)
                .setUploadStatus("READY").setUploadError(null));
    }

    @Override
    @Transactional
    public void failImport(Long taskId, MultiRecordImportFailDTO dto) {
        MultiRecordImportTask task = requireRunningTask(taskId, dto.getWorkerId());
        boolean retry = dto.getRetryable() && task.getAttemptCount() < 3;
        task.setStatus(retry ? TASK_RETRY : TASK_FAILED).setWorkerId(null).setErrorMessage(limitError(dto.getErrorMessage()))
                .setFinishedAt(retry ? null : new Timestamp(System.currentTimeMillis()));
        importTaskMapper.updateById(task);
        if (retry) {
            transferProgress(task.getTransferTaskId(), "PROCESS", 0, "任务将自动重试");
        } else if (task.getTransferTaskId() != null) {
            transferTaskService.fail(task.getTransferTaskId(), dto.getErrorMessage());
        }
        if (retry) {
            scheduleTaskDispatch(taskId);
        } else {
            datasetMapper.updateById(new MultiDataset().setId(task.getMultiDatasetId()).setStatus(STATUS_IMPORT_FAILED)
                    .setUploadStatus("FAILED").setUploadError(limitError(dto.getErrorMessage())));
        }
    }

    @Override
    public void reconcileImportTasks() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        importTaskMapper.retryStale(new Timestamp(now.getTime() - 10 * 60 * 1000L));
        List<MultiRecordImportTask> tasks = importTaskMapper.selectList(new QueryWrapper<MultiRecordImportTask>()
                .in("status", TASK_QUEUED, TASK_RETRY).eq("deleted", 0).and(wrapper -> wrapper.isNull("last_dispatched_at")
                        .or().lt("last_dispatched_at", new Timestamp(now.getTime() - 60 * 1000L))).last("limit 100"));
        for (MultiRecordImportTask task : tasks) {
            scheduleTaskDispatch(task.getId());
        }
    }

    private void updateHeartbeat(Long taskId, String workerId, Long scannedMessages, Long processedImages, Long processedPointclouds) {
        if (importTaskMapper.heartbeat(taskId, workerId, scannedMessages, processedImages, processedPointclouds,
                new Timestamp(System.currentTimeMillis())) != 1) {
            throw new BusinessException("Import task is not owned by this worker");
        }
    }

    private void transferProgress(Long transferTaskId, String stage, int progress, String message) {
        if (transferTaskId == null) {
            return;
        }
        ImportTransferTaskProgressDTO progressDTO = new ImportTransferTaskProgressDTO();
        progressDTO.setStage(stage);
        progressDTO.setProgress(progress);
        progressDTO.setMessage(message);
        transferTaskService.progress(transferTaskId, progressDTO);
    }

    private int toTaskFileCount(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    private void scheduleTaskDispatch(Long taskId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    publishTask(taskId);
                }
            });
            return;
        }
        publishTask(taskId);
    }

    private void publishTask(Long taskId) {
        redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute("XADD", bytes(QUEUE_STREAM), bytes("*"),
                bytes("taskId"), bytes(String.valueOf(taskId))));
        importTaskMapper.updateById(new MultiRecordImportTask().setId(taskId)
                .setLastDispatchedAt(new Timestamp(System.currentTimeMillis())));
    }

    private void buildPointcloudImageRelations(Long datasetId, Long importTaskId) {
        List<MultiPointcloudFile> pointclouds = pointcloudFileMapper.selectList(new QueryWrapper<MultiPointcloudFile>()
                .eq("dataset_id", datasetId).eq("import_task_id", importTaskId).eq("deleted", 0)
                .orderByAsc("capture_timestamp_ns"));
        List<MultiImageFile> images = imageFileMapper.selectList(new QueryWrapper<MultiImageFile>()
                .eq("dataset_id", datasetId).eq("import_task_id", importTaskId).eq("deleted", 0)
                .orderByAsc("topic_name").orderByAsc("capture_timestamp_ns"));
        Map<String, List<MultiImageFile>> imagesByTopic = new HashMap<>();
        for (MultiImageFile image : images) {
            imagesByTopic.computeIfAbsent(image.getTopicName(), key -> new ArrayList<>()).add(image);
        }
        for (MultiPointcloudFile pointcloud : pointclouds) {
            for (List<MultiImageFile> imageTopic : imagesByTopic.values()) {
                MultiImageFile closest = findClosestImage(imageTopic, pointcloud.getCaptureTimestampNs());
                if (closest != null && relationMapper.selectCount(new QueryWrapper<MultiPointcloudImageRelation>()
                        .eq("pointcloud_file_id", pointcloud.getId()).eq("image_file_id", closest.getId())) == 0) {
                    relationMapper.insert(new MultiPointcloudImageRelation().setPointcloudFileId(pointcloud.getId())
                            .setImageFileId(closest.getId())
                            .setTimeOffsetNs(closest.getCaptureTimestampNs() - pointcloud.getCaptureTimestampNs()));
                }
            }
        }
    }

    private MultiImageFile findClosestImage(List<MultiImageFile> images, long timestampNs) {
        int low = 0;
        int high = images.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (images.get(middle).getCaptureTimestampNs() < timestampNs) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        MultiImageFile closest = null;
        for (int index : new int[]{low - 1, low}) {
            if (index >= 0 && index < images.size()) {
                MultiImageFile candidate = images.get(index);
                if (closest == null || Math.abs(candidate.getCaptureTimestampNs() - timestampNs)
                        < Math.abs(closest.getCaptureTimestampNs() - timestampNs)) {
                    closest = candidate;
                }
            }
        }
        return closest != null && Math.abs(closest.getCaptureTimestampNs() - timestampNs) <= MAX_IMAGE_OFFSET_NS ? closest : null;
    }

    private MultiDataset requireDataset(Long datasetId) {
        MultiDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException("Multi-modal dataset does not exist");
        }
        return dataset;
    }

    private MultiRecordImportTask requireTask(Long datasetId, Long taskId) {
        MultiRecordImportTask task = requireTask(taskId);
        if (!datasetId.equals(task.getMultiDatasetId())) {
            throw new BusinessException("Record import task does not belong to this dataset");
        }
        return task;
    }

    private MultiRecordImportTask requireTask(Long taskId) {
        MultiRecordImportTask task = importTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Record import task does not exist");
        }
        return task;
    }

    private MultiRecordImportTask requireRunningTask(Long taskId, String workerId) {
        MultiRecordImportTask task = requireTask(taskId);
        if (!TASK_RUNNING.equals(task.getStatus()) || !workerId.equals(task.getWorkerId())) {
            throw new BusinessException("Import task is not owned by this worker");
        }
        return task;
    }

    private String ensurePrefix(MultiDataset dataset) {
        if (dataset.getStoragePrefix() != null && !dataset.getStoragePrefix().trim().isEmpty()) {
            return dataset.getStoragePrefix().replaceAll("/+", "/").replaceAll("/$", "");
        }
        String prefix = "dataset/multi/" + dataset.getId();
        datasetMapper.updateById(new MultiDataset().setId(dataset.getId()).setStoragePrefix(prefix));
        return prefix;
    }

    private void validateWorkerObjectKey(MultiRecordImportTask task, String objectKey) {
        String prefix = "dataset/multi/" + task.getMultiDatasetId() + "/";
        if (!objectKey.startsWith(prefix)) {
            throw new BusinessException("Worker object does not belong to this import task");
        }
    }

    private static String cleanFileName(String name) {
        String normalized = name == null ? "" : name.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return (slash >= 0 ? normalized.substring(slash + 1) : normalized).trim();
    }

    private static String sensorName(String topic) {
        String normalized = topic == null ? "" : topic.replace('\\', '/');
        String value = normalized.replaceAll("^/+|/+$", "").trim();
        if (value.isEmpty()) {
            throw new BusinessException("Record topic must include a sensor name");
        }
        if (value.length() <= 128) {
            return value;
        }
        return value.substring(0, 91) + "-" + UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String imageEncoding(String contentType) {
        if (contentType == null) {
            return null;
        }
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return "jpeg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        return null;
    }

    private static Long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static String limitError(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
