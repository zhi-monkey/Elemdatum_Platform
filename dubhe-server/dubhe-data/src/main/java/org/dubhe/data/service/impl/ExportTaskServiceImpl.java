package org.dubhe.data.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.permission.base.BaseService;
import org.dubhe.biz.permission.util.SqlUtil;
import org.dubhe.data.client.UserClient;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.dao.ExportTaskMapper;
import org.dubhe.data.dao.FileSearchMapper;
import org.dubhe.data.domain.dto.ExportTaskCreateDTO;
import org.dubhe.data.domain.dto.FileSearchDTO;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.ExportTask;
import org.dubhe.data.domain.vo.ExportTaskVO;
import org.dubhe.data.domain.vo.FileSearchResultVO;
import org.dubhe.data.service.ExportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StreamUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @description 数据导出任务服务实现
 * @date 2026-08-29
 */
@Slf4j
@Service
public class ExportTaskServiceImpl implements ExportTaskService {

    private static final List<String> VALID_FORMATS = Arrays.asList("csv", "json", "zip");
    private static final String EXPORT_DIR = "export";
    private static final int PAGE_SIZE = 500;
    /** 导出文件有效期（天） */
    private static final int EXPIRE_DAYS = 7;

    /** 等待确认 */
    private static final String STATUS_PENDING = "PENDING";
    /** 排队 */
    private static final String STATUS_QUEUED = "QUEUED";
    /** 执行中 */
    private static final String STATUS_PROCESSING = "PROCESSING";
    /** 已完成 */
    private static final String STATUS_COMPLETED = "COMPLETED";
    /** 失败 */
    private static final String STATUS_FAILED = "FAILED";
    /** 已取消 */
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    @Autowired
    private FileSearchMapper fileSearchMapper;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private UserClient userClient;

    @Autowired
    private DatasetMapper datasetMapper;

    @Resource
    private UserContextService userContextService;

    @Value("${minio.bucketName}")
    private String bucketName;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public String createTask(ExportTaskCreateDTO dto) {
        String format = dto.getFormat();
        if (format == null || !VALID_FORMATS.contains(format)) {
            throw new BusinessException("导出格式仅支持 csv/json/zip");
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTask task = new ExportTask()
                .setTaskId(taskId)
                .setDataType(0)
                .setSearchCondition(dto.getCondition() == null ? null : JSON.toJSONString(dto.getCondition()))
                .setFileIds(dto.getFileIds() == null ? null : JSON.toJSONString(dto.getFileIds()))
                .setFormat(format)
                .setStatus(STATUS_PENDING)
                .setProgress(0)
                .setExpireTime(new Timestamp(System.currentTimeMillis() + EXPIRE_DAYS * 24L * 3600 * 1000));
        exportTaskMapper.insert(task);
        return taskId;
    }

    @Override
    public void confirm(String taskId) {
        ExportTask task = getByTaskId(taskId);
        if (task == null) {
            throw new BusinessException("导出任务不存在");
        }
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException("当前状态不可确认");
        }
        // 数据权限（异步线程拿不到用户上下文，提前算好）
        Set<Long> resourceUserIds = null;
        UserContext curUser = userContextService.getCurUser();
        if (curUser != null && !BaseService.isAdmin(curUser)) {
            resourceUserIds = new HashSet<>(SqlUtil.getResourceIds(curUser));
        }
        // 进入排队，异步执行
        updateStatus(task.getId(), STATUS_QUEUED, 0);
        appendLog(task.getId(), "任务已确认，进入执行队列");
        final Set<Long> finalResourceUserIds = resourceUserIds;
        executorService.submit(() -> doExport(task.getId(), finalResourceUserIds));
    }

    @Override
    public void cancel(String taskId) {
        ExportTask task = getByTaskId(taskId);
        if (task == null) {
            throw new BusinessException("导出任务不存在");
        }
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }
        updateResult(task.getId(), STATUS_CANCELLED, null, null, null, null);
    }

    @Override
    public void deleteTasks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        exportTaskMapper.deleteBatchIds(ids);
    }

    @Override
    public IPage<ExportTaskVO> listTasks(long current, long size) {
        Page<ExportTask> page = new Page<>(current, size);
        IPage<ExportTask> result = exportTaskMapper.selectPage(page,
                new LambdaQueryWrapper<ExportTask>().orderByDesc(ExportTask::getId));
        List<ExportTaskVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<ExportTaskVO> voPage = new Page<>(current, size, result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ExportTaskVO getTask(String taskId) {
        ExportTask task = getByTaskId(taskId);
        return task == null ? null : toVO(task);
    }

    /**
     * 执行导出（异步，排队后执行，更新进度与日志）
     */
    private void doExport(Long taskId, Set<Long> resourceUserIds) {
        ExportTask task = getById(taskId);
        if (task == null) {
            return;
        }
        try {
            updateStatus(taskId, STATUS_PROCESSING, 10);
            appendLog(taskId, "开始查询数据");
            FileSearchDTO query = task.getSearchCondition() == null ? new FileSearchDTO()
                    : JSON.parseObject(task.getSearchCondition(), FileSearchDTO.class);
            if (task.getFileIds() != null) {
                query.setFileIds(JSON.parseArray(task.getFileIds(), Long.class));
            }
            List<FileSearchResultVO> files = queryAllFiles(query, resourceUserIds);
            appendLog(taskId, "查询完成，共 " + files.size() + " 条数据");
            String datasetSummary = buildDatasetSummary(files);
            String conditionSummary = buildConditionSummary(query);
            updateStatus(taskId, STATUS_PROCESSING, 90);
            appendLog(taskId, "导出准备完成，下载时获取文件");
            ExportTask update = new ExportTask()
                    .setId(taskId)
                    .setStatus(STATUS_COMPLETED)
                    .setProgress(100)
                    .setFileCount(files.size())
                    .setDatasetSummary(datasetSummary)
                    .setConditionSummary(conditionSummary);
            exportTaskMapper.updateById(update);
            appendLog(taskId, "导出完成");
        } catch (Exception e) {
            log.error("导出失败 taskId={}", taskId, e);
            updateResult(taskId, STATUS_FAILED, 0, null, e.getMessage(), null);
            appendLog(taskId, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 循环分页拉全量检索结果
     */
    private List<FileSearchResultVO> queryAllFiles(FileSearchDTO query, Set<Long> resourceUserIds) {
        List<FileSearchResultVO> all = new ArrayList<>();
        long current = 1;
        while (true) {
            Page<FileSearchResultVO> page = new Page<>(current, PAGE_SIZE);
            IPage<FileSearchResultVO> result = fileSearchMapper.search(page, query, resourceUserIds, "f.id ASC");
            if (result.getRecords() != null) {
                all.addAll(result.getRecords());
            }
            if (current * PAGE_SIZE >= result.getTotal()) {
                break;
            }
            current++;
        }
        return all;
    }

    @Override
    public void download(String taskId, HttpServletResponse response) throws Exception {
        ExportTask task = getByTaskId(taskId);
        if (task == null || !STATUS_COMPLETED.equals(task.getStatus())) {
            throw new BusinessException("任务未完成，无法下载");
        }
        FileSearchDTO query = task.getSearchCondition() == null ? new FileSearchDTO()
                : JSON.parseObject(task.getSearchCondition(), FileSearchDTO.class);
        if (task.getFileIds() != null) {
            query.setFileIds(JSON.parseArray(task.getFileIds(), Long.class));
        }
        Set<Long> resourceUserIds = null;
        UserContext curUser = userContextService.getCurUser();
        if (curUser != null && !BaseService.isAdmin(curUser)) {
            resourceUserIds = new HashSet<>(SqlUtil.getResourceIds(curUser));
        }
        List<FileSearchResultVO> files = queryAllFiles(query, resourceUserIds);

        String format = task.getFormat();
        if ("csv".equals(format)) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(taskId + ".csv", "UTF-8"));
            response.getWriter().write(buildCsv(files));
        } else if ("json".equals(format)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(taskId + ".json", "UTF-8"));
            response.getWriter().write(JSON.toJSONString(files));
        } else {
            // zip：实时生成临时 ZIP，流式返回后删除临时文件（不落 MinIO）
            File tempZip = buildZip(files);
            try (InputStream in = new FileInputStream(tempZip);
                 OutputStream out = response.getOutputStream()) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition",
                        "attachment; filename=" + URLEncoder.encode(taskId + "_images.zip", "UTF-8"));
                StreamUtils.copy(in, out);
            } finally {
                tempZip.delete();
            }
        }
    }

    /**
     * 实时把图片打包成临时 ZIP（供下载，返回临时文件）
     */
    private File buildZip(List<FileSearchResultVO> files) throws Exception {
        File tempZip = File.createTempFile("export", ".zip");
        try (FileOutputStream fos = new FileOutputStream(tempZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (FileSearchResultVO f : files) {
                String url = f.getUrl();
                if (url == null) {
                    continue;
                }
                // URL格式: bucketName/dataset/image/{id}/origin/xxx.jpg 或 bucketName/dataset/{id}/origin/xxx.jpg
                // 需要去掉 bucketName/ 前缀得到 MinIO objectName
                String objectName;
                if (url.startsWith(bucketName + "/")) {
                    objectName = url.substring(bucketName.length() + 1);
                } else {
                    // 兼容没有 bucketName 前缀的情况
                    objectName = url;
                }
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                try (InputStream in = minioUtil.getObjectInputStream(bucketName, objectName)) {
                    zos.putNextEntry(new ZipEntry(fileName));
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("打包文件失败: {}", objectName, e);
                }
            }
        }
        return tempZip;
    }

    /**
     * 统计数据集分布（A数据集（2条） / B数据集（3条））
     */
    private String buildDatasetSummary(List<FileSearchResultVO> files) {
        Map<String, Long> countMap = files.stream()
                .filter(f -> f.getDatasetName() != null)
                .collect(Collectors.groupingBy(FileSearchResultVO::getDatasetName, Collectors.counting()));
        if (countMap.isEmpty()) {
            return "无";
        }
        return countMap.entrySet().stream()
                .map(e -> e.getKey() + "（" + e.getValue() + "条）")
                .collect(Collectors.joining(" / "));
    }

    /**
     * 生成筛选条件摘要（一行一个条件，未选填"无"）
     */
    private String buildConditionSummary(FileSearchDTO query) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据集：").append(formatDatasetNames(query.getDatasetIds())).append('\n');
        sb.append("文件名：").append(emptyToNone(query.getName())).append('\n');
        sb.append("标注状态：").append(formatAnnotationStatuses(query.getAnnotationStatus())).append('\n');
        sb.append("标签：").append(listToNone(query.getLabelNames())).append('\n');
        sb.append("业务场景：").append(listToNone(query.getScenario())).append('\n');
        sb.append("采集地点：").append(listToNone(query.getLocation())).append('\n');
        sb.append("数据来源：").append(listToNone(query.getSourceType())).append('\n');
        sb.append("车辆/设备：").append(listToNone(query.getDevice())).append('\n');
        sb.append("设备/相机编号：").append(emptyToNone(query.getDeviceSn())).append('\n');
        sb.append("光照/环境：").append(listToNone(query.getLighting())).append('\n');
        sb.append("数据质量：").append(listToNone(query.getQuality())).append('\n');
        sb.append("采集时间：").append(rangeToNone(query.getCaptureTimeStart(), query.getCaptureTimeEnd())).append('\n');
        sb.append("更新时间：").append(rangeToNone(query.getUpdateTimeStart(), query.getUpdateTimeEnd())).append('\n');
        return sb.toString();
    }

    private String emptyToNone(Object v) {
        if (v == null) {
            return "无";
        }
        String s = v.toString().trim();
        return s.isEmpty() ? "无" : s;
    }

    private String listToNone(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "无";
        }
        return String.join("、", list);
    }

    private String rangeToNone(Object start, Object end) {
        if (start == null && end == null) {
            return "无";
        }
        return (start == null ? "" : start) + " ~ " + (end == null ? "" : end);
    }

    private String formatDatasetNames(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return "无";
        }
        List<Dataset> datasets = datasetMapper.selectBatchIds(datasetIds);
        if (datasets == null || datasets.isEmpty()) {
            return "无";
        }
        return datasets.stream().map(Dataset::getName).collect(Collectors.joining("/"));
    }

    private String formatAnnotationStatuses(List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "无";
        }
        return statuses.stream().map(this::formatAnnotationStatus).collect(Collectors.joining("/"));
    }

    private String formatAnnotationStatus(Integer s) {
        if (s == null) {
            return "无";
        }
        if (s == 101) {
            return "未标注";
        }
        if (s == 102) {
            return "标注中";
        }
        if (s == 103) {
            return "自动标注完成";
        }
        if (s == 104) {
            return "已标注";
        }
        return String.valueOf(s);
    }

    /**
     * 生成 CSV 文本
     */
    private String buildCsv(List<FileSearchResultVO> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,datasetName,annotationStatus,labels,width,height,sourceType,captureTime,")
                .append("device,deviceSn,location,scenario,lighting,quality,updateTime\n");
        for (FileSearchResultVO f : files) {
            sb.append(csv(f.getId())).append(',')
                    .append(csv(f.getName())).append(',')
                    .append(csv(f.getDatasetName())).append(',')
                    .append(csv(f.getAnnotationStatus())).append(',')
                    .append(csv(f.getLabels())).append(',')
                    .append(csv(f.getWidth())).append(',')
                    .append(csv(f.getHeight())).append(',')
                    .append(csv(f.getSourceType())).append(',')
                    .append(csv(f.getCaptureTime())).append(',')
                    .append(csv(f.getDevice())).append(',')
                    .append(csv(f.getDeviceSn())).append(',')
                    .append(csv(f.getLocation())).append(',')
                    .append(csv(f.getScenario())).append(',')
                    .append(csv(f.getLighting())).append(',')
                    .append(csv(f.getQuality())).append(',')
                    .append(csv(f.getUpdateTime())).append('\n');
        }
        return sb.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private ExportTask getByTaskId(String taskId) {
        return exportTaskMapper.selectOne(
                new LambdaQueryWrapper<ExportTask>().eq(ExportTask::getTaskId, taskId));
    }

    private ExportTask getById(Long id) {
        return exportTaskMapper.selectById(id);
    }

    private void updateStatus(Long id, String status, Integer progress) {
        ExportTask task = new ExportTask().setId(id).setStatus(status).setProgress(progress);
        exportTaskMapper.updateById(task);
    }

    private void appendLog(Long id, String message) {
        ExportTask current = getById(id);
        if (current == null) {
            return;
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String old = current.getLog();
        String line = timestamp + " " + message;
        ExportTask task = new ExportTask().setId(id).setLog((old == null ? "" : old + "\n") + line);
        exportTaskMapper.updateById(task);
    }

    /**
     * 定时清理过期任务（每天凌晨 3 点，逻辑删除）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTasks() {
        try {
            exportTaskMapper.delete(new LambdaQueryWrapper<ExportTask>()
                    .lt(ExportTask::getExpireTime, new Timestamp(System.currentTimeMillis())));
        } catch (Exception e) {
            log.error("清理过期导出任务失败", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    private void updateResult(Long id, String status, Integer progress, String resultUrl, String errorMessage, Integer fileCount) {
        ExportTask task = new ExportTask()
                .setId(id)
                .setStatus(status)
                .setProgress(progress)
                .setResultUrl(resultUrl)
                .setErrorMessage(errorMessage)
                .setFileCount(fileCount);
        exportTaskMapper.updateById(task);
    }

    private ExportTaskVO toVO(ExportTask t) {
        String createUserName = null;
        if (t.getCreateUserId() != null) {
            try {
                createUserName = userClient.getNameById(t.getCreateUserId());
            } catch (Exception e) {
                // 忽略，查不到用户名就空
            }
        }
        return ExportTaskVO.builder()
                .id(t.getId())
                .taskId(t.getTaskId())
                .format(t.getFormat())
                .status(t.getStatus())
                .progress(t.getProgress())
                .resultUrl(t.getResultUrl())
                .errorMessage(t.getErrorMessage())
                .fileCount(t.getFileCount())
                .createUserName(createUserName)
                .expireTime(t.getExpireTime() == null ? null
                        : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t.getExpireTime()))
                .log(t.getLog())
                .datasetSummary(t.getDatasetSummary())
                .conditionSummary(t.getConditionSummary())
                .createTime(t.getCreateTime() == null ? null
                        : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t.getCreateTime()))
                .build();
    }
}
