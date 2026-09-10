package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.TransferExportTaskEventMapper;
import org.dubhe.data.dao.TransferExportTaskMapper;
import org.dubhe.data.domain.entity.TransferExportTask;
import org.dubhe.data.domain.entity.TransferExportTaskEvent;
import org.dubhe.data.domain.vo.TransferExportTaskVO;
import org.dubhe.data.service.TransferExportTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransferExportTaskServiceImpl implements TransferExportTaskService {
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    private final TransferExportTaskMapper taskMapper;
    private final TransferExportTaskEventMapper eventMapper;
    private final UserContextService userContextService;
    private final MinioUtil minioUtil;
    @Value("${minio.bucketName}")
    private String bucketName;

    public TransferExportTaskServiceImpl(TransferExportTaskMapper taskMapper,
                                         TransferExportTaskEventMapper eventMapper,
                                         UserContextService userContextService,
                                         MinioUtil minioUtil) {
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.userContextService = userContextService;
        this.minioUtil = minioUtil;
    }

    @Override
    @Transactional
    public Long createDatasetVersionTask(String taskName, Long datasetId, Long datasetVersionId,
                                         String versionName, String format, Integer totalFiles) {
        Date now = new Date();
        TransferExportTask task = new TransferExportTask().setTaskName(taskName == null || taskName.trim().isEmpty() ? "数据集版本导出" : taskName)
                .setTaskType("DATASET_VERSION").setDatasetType("IMAGE").setDatasetId(datasetId)
                .setDatasetVersionId(datasetVersionId).setVersionName(versionName).setFormat(format)
                .setStatus(PROCESSING).setStage("PREPARE").setProgress(0)
                .setTotalFiles(Math.max(0, totalFiles == null ? 0 : totalFiles)).setSuccessFiles(0).setFailedFiles(0)
                .setRetryCount(0).setStartedAt(now).setCreateUserId(userContextService.getCurUserId())
                .setCreateTime(now).setUpdateTime(now).setDeleted(false);
        taskMapper.insert(task);
        addEvent(task.getId(), "CREATED", "导出任务已创建", null);
        return task.getId();
    }

    @Override
    public IPage<TransferExportTaskVO> list(long current, long size, String status, String datasetType) {
        Page<TransferExportTask> page = new Page<>(current, size);
        QueryWrapper<TransferExportTask> query = new QueryWrapper<TransferExportTask>().eq("deleted", 0).orderByDesc("id");
        Long userId = userContextService.getCurUserId();
        if (userId != null) query.eq("create_user_id", userId);
        if (status != null && !status.trim().isEmpty()) query.eq("status", status);
        if (datasetType != null && !datasetType.trim().isEmpty()) query.eq("dataset_type", datasetType);
        IPage<TransferExportTask> result = taskMapper.selectPage(page, query);
        Page<TransferExportTaskVO> voPage = new Page<>(current, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(t -> toVO(t, false)).collect(Collectors.toList()));
        return voPage;
    }

    @Override public TransferExportTaskVO get(Long id) { return toVO(require(id), true); }

    @Override public TransferExportTask require(Long id) {
        TransferExportTask task = taskMapper.selectById(id);
        if (task == null || Boolean.TRUE.equals(task.getDeleted())) throw new BusinessException("导出任务不存在");
        Long userId = userContextService.getCurUserId();
        if (userId != null && task.getCreateUserId() != null && !userId.equals(task.getCreateUserId())) throw new BusinessException("无权操作该导出任务");
        return task;
    }

    @Override public void progress(Long id, String stage, int progress, Integer totalFiles, Integer successFiles, String message) {
        TransferExportTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        TransferExportTask update = new TransferExportTask().setId(id).setStage(stage)
                .setProgress(Math.max(0, Math.min(100, progress))).setUpdateTime(new Date());
        if (totalFiles != null) update.setTotalFiles(Math.max(0, totalFiles));
        if (successFiles != null) update.setSuccessFiles(Math.max(0, successFiles));
        taskMapper.updateById(update);
        if (message != null && !message.trim().isEmpty()) addEvent(id, "PROGRESS", message, null);
    }

    @Override @Transactional public void complete(Long id, String resultObjectKey, String message) {
        TransferExportTask task = require(id);
        if (CANCELLED.equals(task.getStatus())) return;
        taskMapper.updateById(new TransferExportTask().setId(id).setStatus(COMPLETED).setStage("DONE").setProgress(100)
                .setSuccessFiles(task.getTotalFiles()).setResultObjectKey(resultObjectKey).setFinishedAt(new Date()).setUpdateTime(new Date()).setErrorMessage(null));
        addEvent(id, "COMPLETED", message == null ? "导出任务已完成" : message, null);
    }

    @Override @Transactional public void fail(Long id, String message) {
        require(id);
        String error = message == null || message.trim().isEmpty() ? "导出失败" : message;
        taskMapper.updateById(new TransferExportTask().setId(id).setStatus(FAILED).setStage("DONE")
                .setErrorMessage(error.substring(0, Math.min(1000, error.length()))).setFinishedAt(new Date()).setUpdateTime(new Date()));
        addEvent(id, "FAILED", error, null);
    }

    @Override @Transactional public void cancel(Long id) {
        TransferExportTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        taskMapper.updateById(new TransferExportTask().setId(id).setStatus(CANCELLED).setFinishedAt(new Date()).setUpdateTime(new Date()));
        addEvent(id, "CANCELLED", "导出任务已取消", null);
    }

    @Override public void delete(List<Long> ids) {
        if (ids == null) return;
        for (Long id : ids) taskMapper.updateById(new TransferExportTask().setId(require(id).getId()).setDeleted(true).setUpdateTime(new Date()));
    }

    @Override public void download(Long id, HttpServletResponse response) throws Exception {
        TransferExportTask task = require(id);
        if (!COMPLETED.equals(task.getStatus()) || task.getResultObjectKey() == null) throw new BusinessException("导出文件尚未准备完成");
        String name = task.getDatasetId() + "_" + task.getFormat() + "_" + task.getVersionName() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        try (InputStream in = minioUtil.getObjectInputStream(bucketName, task.getResultObjectKey()); OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        }
    }

    private TransferExportTaskVO toVO(TransferExportTask t, boolean includeEvents) {
        TransferExportTaskVO v = new TransferExportTaskVO();
        v.setId(t.getId()); v.setTaskName(t.getTaskName()); v.setTaskType(t.getTaskType()); v.setDatasetType(t.getDatasetType()); v.setDatasetId(t.getDatasetId());
        v.setDatasetVersionId(t.getDatasetVersionId()); v.setVersionName(t.getVersionName()); v.setFormat(t.getFormat()); v.setStatus(t.getStatus()); v.setStage(t.getStage());
        v.setProgress(t.getProgress()); v.setTotalFiles(t.getTotalFiles()); v.setSuccessFiles(t.getSuccessFiles()); v.setFailedFiles(t.getFailedFiles()); v.setResultObjectKey(t.getResultObjectKey());
        v.setRetryCount(t.getRetryCount()); v.setErrorMessage(t.getErrorMessage()); v.setStartedAt(t.getStartedAt()); v.setFinishedAt(t.getFinishedAt()); v.setCreateTime(t.getCreateTime()); v.setUpdateTime(t.getUpdateTime());
        if (includeEvents) v.setEvents(eventMapper.selectList(new QueryWrapper<TransferExportTaskEvent>().eq("task_id", t.getId()).orderByAsc("id")));
        return v;
    }

    private void addEvent(Long id, String type, String message, String detail) {
        TransferExportTaskEvent e = new TransferExportTaskEvent(); e.setTaskId(id); e.setEventType(type); e.setMessage(message); e.setDetail(detail); e.setCreateUserId(userContextService.getCurUserId()); e.setCreateTime(new Date()); eventMapper.insert(e);
    }
}
