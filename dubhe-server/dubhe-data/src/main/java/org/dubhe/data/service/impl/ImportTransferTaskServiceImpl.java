package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.data.dao.ImportTransferTaskEventMapper;
import org.dubhe.data.dao.ImportTransferTaskMapper;
import org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO;
import org.dubhe.data.domain.dto.ImportTransferTaskProgressDTO;
import org.dubhe.data.domain.entity.ImportTransferTask;
import org.dubhe.data.domain.entity.ImportTransferTaskEvent;
import org.dubhe.data.domain.vo.ImportTransferTaskVO;
import org.dubhe.data.service.ImportTransferTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImportTransferTaskServiceImpl implements ImportTransferTaskService {
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    private final ImportTransferTaskMapper taskMapper;
    private final ImportTransferTaskEventMapper eventMapper;
    private final UserContextService userContextService;

    public ImportTransferTaskServiceImpl(ImportTransferTaskMapper taskMapper,
                                         ImportTransferTaskEventMapper eventMapper,
                                         UserContextService userContextService) {
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Long create(ImportTransferTaskCreateDTO dto) {
        Date now = new Date();
        ImportTransferTask task = new ImportTransferTask();
        task.setTaskName(blankToDefault(dto.getTaskName(), "数据导入"));
        task.setDatasetType(blankToDefault(dto.getDatasetType(), "IMAGE"));
        task.setDatasetId(dto.getDatasetId());
        task.setSourceType(blankToDefault(dto.getSourceType(), "LOCAL"));
        task.setStatus(PROCESSING);
        task.setStage("UPLOAD");
        task.setProgress(0);
        task.setTotalFiles(Math.max(0, valueOrZero(dto.getTotalFiles())));
        task.setSuccessFiles(0);
        task.setFailedFiles(0);
        task.setTotalBytes(Math.max(0L, valueOrZero(dto.getTotalBytes())));
        task.setTransferredBytes(0L);
        task.setRetryCount(0);
        task.setStartedAt(now);
        task.setLastHeartbeatAt(now);
        task.setCreateUserId(userContextService.getCurUserId());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setDeleted(false);
        taskMapper.insert(task);
        addEvent(task.getId(), "CREATED", "任务已创建", null);
        return task.getId();
    }

    @Override
    public IPage<ImportTransferTaskVO> list(long current, long size, String status, String datasetType) {
        Page<ImportTransferTask> page = new Page<>(current, size);
        QueryWrapper<ImportTransferTask> query = new QueryWrapper<ImportTransferTask>()
                .eq("deleted", 0)
                .orderByDesc("id");
        Long currentUserId = userContextService.getCurUserId();
        if (currentUserId != null) query.eq("create_user_id", currentUserId);
        if (status != null && !status.trim().isEmpty()) query.eq("status", status);
        if (datasetType != null && !datasetType.trim().isEmpty()) query.eq("dataset_type", datasetType);
        IPage<ImportTransferTask> result = taskMapper.selectPage(page, query);
        Page<ImportTransferTaskVO> voPage = new Page<>(current, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(task -> toVO(task, false)).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ImportTransferTaskVO get(Long id) {
        return toVO(require(id), true);
    }

    @Override
    public void progress(Long id, ImportTransferTaskProgressDTO dto) {
        ImportTransferTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        Integer progress = dto.getProgress() == null ? task.getProgress() : Math.max(0, Math.min(100, dto.getProgress()));
        ImportTransferTask update = new ImportTransferTask();
        update.setId(id);
        update.setStage(blankToDefault(dto.getStage(), task.getStage()));
        update.setProgress(progress);
        update.setLastHeartbeatAt(new Date());
        update.setUpdateTime(new Date());
        if (dto.getTotalFiles() != null) update.setTotalFiles(Math.max(0, dto.getTotalFiles()));
        if (dto.getSuccessFiles() != null) update.setSuccessFiles(Math.max(0, dto.getSuccessFiles()));
        if (dto.getFailedFiles() != null) update.setFailedFiles(Math.max(0, dto.getFailedFiles()));
        if (dto.getTransferredBytes() != null) update.setTransferredBytes(Math.max(0L, dto.getTransferredBytes()));
        taskMapper.updateById(update);
        if (dto.getMessage() != null && !dto.getMessage().trim().isEmpty()) addEvent(id, "PROGRESS", dto.getMessage(), null);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        ImportTransferTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        ImportTransferTask update = new ImportTransferTask();
        update.setId(id);
        update.setStatus(CANCELLED);
        update.setFinishedAt(new Date());
        update.setUpdateTime(new Date());
        taskMapper.updateById(update);
        addEvent(id, "CANCELLED", "任务已取消", null);
    }

    @Override
    @Transactional
    public void retry(Long id) {
        ImportTransferTask task = require(id);
        if (!FAILED.equals(task.getStatus()) && !CANCELLED.equals(task.getStatus())) throw new BusinessException("当前状态不可重试");
        ImportTransferTask update = new ImportTransferTask();
        update.setId(id);
        update.setStatus(PROCESSING);
        update.setStage("UPLOAD");
        update.setProgress(0);
        update.setErrorMessage(null);
        update.setFinishedAt(null);
        update.setRetryCount(valueOrZero(task.getRetryCount()) + 1);
        update.setLastHeartbeatAt(new Date());
        taskMapper.updateById(update);
        addEvent(id, "RETRY", "任务已重新提交", null);
    }

    @Override
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            require(id);
            ImportTransferTask update = new ImportTransferTask();
            update.setId(id);
            update.setDeleted(true);
            update.setUpdateTime(new Date());
            taskMapper.updateById(update);
        }
    }

    @Transactional
    public void complete(Long id, String message) {
        ImportTransferTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        ImportTransferTask update = new ImportTransferTask();
        update.setId(id);
        update.setStatus(COMPLETED);
        update.setStage("DONE");
        update.setProgress(100);
        update.setSuccessFiles(task.getTotalFiles());
        update.setFinishedAt(new Date());
        update.setLastHeartbeatAt(new Date());
        update.setErrorMessage(null);
        update.setUpdateTime(new Date());
        taskMapper.updateById(update);
        addEvent(id, "COMPLETED", message == null ? "任务已完成" : message, null);
    }

    @Transactional
    public void fail(Long id, String message) {
        ImportTransferTask task = require(id);
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) return;
        String error = message == null || message.trim().isEmpty() ? "导入失败" : message.substring(0, Math.min(1000, message.length()));
        ImportTransferTask update = new ImportTransferTask();
        update.setId(id);
        update.setStatus(FAILED);
        update.setFinishedAt(new Date());
        update.setErrorMessage(error);
        update.setUpdateTime(new Date());
        taskMapper.updateById(update);
        addEvent(id, "FAILED", error, null);
    }

    private ImportTransferTask require(Long id) {
        ImportTransferTask task = taskMapper.selectById(id);
        if (task == null || Boolean.TRUE.equals(task.getDeleted())) throw new BusinessException("导入任务不存在");
        Long currentUserId = userContextService.getCurUserId();
        if (currentUserId != null && task.getCreateUserId() != null && !currentUserId.equals(task.getCreateUserId())) {
            throw new BusinessException("无权操作该导入任务");
        }
        return task;
    }

    private ImportTransferTaskVO toVO(ImportTransferTask task, boolean includeEvents) {
        ImportTransferTaskVO vo = new ImportTransferTaskVO();
        vo.setId(task.getId());
        vo.setTaskName(task.getTaskName());
        vo.setDatasetType(task.getDatasetType());
        vo.setDatasetId(task.getDatasetId());
        vo.setSourceType(task.getSourceType());
        vo.setStatus(task.getStatus());
        vo.setStage(task.getStage());
        vo.setProgress(task.getProgress());
        vo.setTotalFiles(task.getTotalFiles());
        vo.setSuccessFiles(task.getSuccessFiles());
        vo.setFailedFiles(task.getFailedFiles());
        vo.setTotalBytes(task.getTotalBytes());
        vo.setTransferredBytes(task.getTransferredBytes());
        vo.setRetryCount(task.getRetryCount());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        vo.setLastHeartbeatAt(task.getLastHeartbeatAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        if (includeEvents) {
            vo.setEvents(eventMapper.selectList(new QueryWrapper<ImportTransferTaskEvent>().eq("task_id", task.getId()).orderByAsc("id")));
        }
        return vo;
    }

    private void addEvent(Long taskId, String type, String message, String detail) {
        ImportTransferTaskEvent event = new ImportTransferTaskEvent();
        event.setTaskId(taskId);
        event.setEventType(type);
        event.setMessage(message);
        event.setDetail(detail);
        event.setCreateUserId(userContextService.getCurUserId());
        event.setCreateTime(new Date());
        eventMapper.insert(event);
    }

    private static String blankToDefault(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value; }
    private static int valueOrZero(Integer value) { return value == null ? 0 : value; }
    private static long valueOrZero(Long value) { return value == null ? 0L : value; }
}
