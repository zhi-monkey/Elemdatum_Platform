package org.dlut.adv.mineai.model.kubernetes.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.ModelJobLogData;
import org.dlut.adv.mineai.model.repository.ModelJobLogDataRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LogService {

    @Value("${loki.ip}")
    String lokiIp;

    @Value("${loki.port}")
    String lokiPort;

    @Value("${storage.file-store-root-path}")
    private String nfs;

    @Value("/${minio.bucketName}/")
    private String bucketName;

    @Value("${kubernetes.namespace}")
    private String kubeNamespace;

    @Resource
    LogFeign logFeign;

    @Resource
    ModelJobRepo modelJobRepo;

    @Resource
    ModelJobLogDataRepo modelJobLogDataRepo;

    // 使用 ConcurrentHashMap 来为每个 jobName 维护一个锁
    // key: jobName
    // value: Lock object
    // 这样可以实现对不同文件的并发写入，但对同一文件的写入是串行的，防止冲突
    private final ConcurrentHashMap<String, Lock> fileLocks = new ConcurrentHashMap<>();


    /**
     * 获取任务日志的核心方法。
     * 策略:
     * 1. 首先尝试从文件系统（归档日志源）获取日志。
     * 2. 如果文件系统中没有日志，则回退到从 Loki（实时日志源）读取。
     *
     * @param namespace 命名空间
     * @param jobName   任务名称
     * @param direction 查询方向 ("forward" or "backward")
     * @param limit     日志条数限制
     * @param start     查询开始时间 (纳秒)
     * @param end       查询结束时间 (纳秒)
     * @return 包含时间戳和日志内容的 Map，或在完全找不到日志时返回 null。
     */
    public Map<String, String> getLog(String namespace, String jobName, String direction, Long limit, Long start, Long end) {
        // 1. 优先尝试从本地文件获取日志，根据limit大小判断是否分段获取
        Map<String, String> logsFromFile = getLogsFromFileWithLimit(jobName, limit);

        if (logsFromFile != null && !logsFromFile.isEmpty()) {
            log.info("Successfully retrieved {} log entries from local file for job '{}'.", logsFromFile.size(), jobName);
            return logsFromFile;
        }

        // 2. 本地文件没有日志，回退到 Loki 查询
       // log.warn("No logs found in local file for job '{}'. Falling back to Loki.", jobName);
        Map<String, String> logsFromLoki = queryLogsFromLoki(namespace, jobName, direction, limit, start, end);

        if (logsFromLoki != null && !logsFromLoki.isEmpty()) {
            log.info("Successfully retrieved {} log entries from Loki for job '{}'.", logsFromLoki.size(), jobName);
            return logsFromLoki;
        }

        // 3. 如果 Loki 也没有日志，返回 null
     //   log.error("No logs found in both local file and Loki for job '{}'.", jobName);
        return null;
    }

    @Async("logFileWriteExecutor")
    public void persistLogsFromLokiAsync(String jobName) {
        // 1. 参数验证
        if (jobName == null || jobName.isEmpty()) {
            log.warn("Job name is empty, skipping async log persistence.");
            return;
        }

        log.info("Starting async log persistence for job '{}'", jobName);

        try {
            // 2. 提取jobId，增加异常处理
            Long jobId = extractJobId(jobName);
            if (jobId == null) {
                log.error("Failed to extract job ID from job name '{}', aborting log persistence", jobName);
                return;
            }

            // 3. 设置时间范围
            Instant now = Instant.now();
            Long startTime = now.minus(7, ChronoUnit.DAYS).toEpochMilli() * 1_000_000L;
            Long endTime = now.plus(3, ChronoUnit.DAYS).toEpochMilli() * 1_000_000L;

            log.info("Time range - start: {}, end: {}", startTime, endTime);

            // 4. 构建文件路径并创建目录
            String logDirPath = Paths.get(nfs, bucketName, "train-log").toString();
            String logFilePath = Paths.get(logDirPath, jobName + ".log").toString();

            if (!ensureDirectoryExists(logDirPath)) {
                log.error("Failed to create or verify log directory: {}", logDirPath);
                return;
            }

            // 5. 初始化查询参数
            Long currentStart = startTime;
            Long currentEnd = endTime;
            final int batchSize = 600;
            int totalPersisted = 0;
            int remainingIterations = 60;

            ModelJobLogData modelJobLogData = new ModelJobLogData();

            // 6. 使用try-with-resources确保资源关闭
            try (FileWriter fileWriter = new FileWriter(logFilePath, false);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                // 7. 主循环：持续获取日志
                while (currentStart != null && (endTime == null || currentStart < endTime)) {
                    try {
                        // 检查线程中断状态
                        if (Thread.currentThread().isInterrupted()) {
                            log.warn("Thread interrupted, stopping log persistence for job '{}'", jobName);
                            break;
                        }

                        log.debug("Fetching batch - start: {}, end: {}", currentStart, currentEnd);

                        // 8. 查询数据库获取任务状态（带异常处理）
                        ModelJob modelJob = safelyGetModelJob(jobId);
                        if (modelJob == null) {
                            log.warn("Cannot find model job with ID {}, continuing with current data", jobId);
                        } else {
                            // 检查任务状态
                            Integer status = modelJob.getStatus();
                            if (isJobInFinalState(status)) {
                                if (remainingIterations <= 0) {
                                    //log.info("Job '{}' in final state {} and iterations complete", jobName, status);
                                    break;
                                }
                                remainingIterations--;
                                //log.info("Job '{}' in final state, {} iterations remaining", jobName, remainingIterations);
                            }
                        }

                        // 9. 从Loki获取日志
                        Map<String, String> logBatch = safelyGetLokiLog(
                                kubeNamespace, jobName, "FORWARD", (long) batchSize, currentStart, currentEnd
                        );

                        if (logBatch == null || logBatch.isEmpty()) {
                            //log.info("No logs retrieved in current batch, continuing...");
                            Thread.sleep(10000);
                            continue;
                        }


                        // 10. 解析并更新epoch数据（带异常处理）
                        if (modelJob != null) {
                            safelyUpdateEpochData(logBatch, modelJobLogData, modelJob, jobName);

                            // 不用管是否complete：每批日志都尝试解析最终的 acc 和 recall
                            Map<String, List<String>> testedDetail = parseTestedDetailFromLogs(logBatch);

                            // 只有当日志中确实包含 accuracy 或 recall 数据时才认为完成：设置进度100并落库
                            if (testedDetail != null) {
                                List<String> accList = testedDetail.get("accuracy");
                                List<String> recallList = testedDetail.get("recall");

                                double maxAcc = (accList != null && !accList.isEmpty())
                                        ? accList.stream().mapToDouble(Double::parseDouble).max().orElse(0.0)
                                        : 0.0;
                                double maxRecall = (recallList != null && !recallList.isEmpty())
                                        ? recallList.stream().mapToDouble(Double::parseDouble).max().orElse(0.0)
                                        : 0.0;

                                modelJobLogData.setAccuracy(BigDecimal.valueOf(maxAcc));
                                modelJobLogData.setRecall(BigDecimal.valueOf(maxRecall));
                                modelJobLogData.setProgress(BigDecimal.valueOf(100.0));
                            }
                            modelJobLogDataRepo.save(modelJobLogData);

                        }

                        // 11. 写入日志到文件
                        int writtenCount = safelyWriteLogsToFile(logBatch, bufferedWriter);
                        totalPersisted += writtenCount;

                        // 12. 更新游标
                        String lastTimestamp = getLastTimestamp(logBatch);
                        if (lastTimestamp != null) {
                            currentStart = safelyParseTimestamp(lastTimestamp, currentStart);
                            if (currentStart != null) {
                                currentStart += 1; // 避免重复
                            }
                        } else {
                            log.warn("No timestamp found in batch, advancing by default interval");
                        }

                        log.info("Batch complete - written: {}, total: {}", writtenCount, totalPersisted);

                    } catch (Exception e) {
                        log.error("Error processing batch for job '{}', continuing with next batch", jobName, e);
                    }

                    // 13. 短暂休眠
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Sleep interrupted for job '{}'", jobName);
                        break;
                    }
                }

                log.info("Completed log persistence for job '{}', total logs: {}", jobName, totalPersisted);

            } catch (IOException e) {
                log.error("Fatal I/O error writing logs for job '{}'", jobName, e);
            }

        } catch (Exception e) {
            log.error("Unexpected fatal error in log persistence for job '{}'", jobName, e);
        }
    }

// ==================== 辅助方法 ====================

    /**
     * 安全地提取jobId
     */
    private Long extractJobId(String jobName) {
        try {
            String[] nameParts = jobName.split("-");
            if (nameParts.length > 3) {
                return Long.parseLong(nameParts[3]);
            }
            log.error("Invalid job name format: {}", jobName);
            return null;
        } catch (NumberFormatException e) {
            log.error("Failed to parse job ID from job name '{}'", jobName, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error extracting job ID from '{}'", jobName, e);
            return null;
        }
    }

    /**
     * 确保目录存在
     */
    private boolean ensureDirectoryExists(String dirPath) {
        try {
            File directory = new File(dirPath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    log.info("Created directory: {}", dirPath);
                } else {
                    log.error("Failed to create directory: {}", dirPath);
                    return false;
                }
            }
            return true;
        } catch (SecurityException e) {
            log.error("Security exception creating directory: {}", dirPath, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error creating directory: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 安全地获取ModelJob
     */
    private ModelJob safelyGetModelJob(Long jobId) {
        try {
            return modelJobRepo.findModelJobByIdWithParams(jobId);
        } catch (Exception e) {
            log.error("Error querying model job with ID {}", jobId, e);
            return null;
        }
    }

    /**
     * 判断任务是否处于最终状态
     */
    private boolean isJobInFinalState(Integer status) {
        if (status == null) return false;
        return status.equals(ModelJobEnum.TRAIN_FAILED.getCode()) ||
                status.equals(ModelJobEnum.CANCELED.getCode()) ||
                status.equals(ModelJobEnum.TRAIN_SUCCEEDED.getCode()) ||
                status.equals(ModelJobEnum.PUBLISHED.getCode()) ||
                status.equals(ModelJobEnum.CONVERT_SUCCEEDED.getCode()) ||
                status.equals(ModelJobEnum.CONVERT_FAILED.getCode());
    }

    /**
     * 安全地获取Loki日志
     */
    private Map<String, String> safelyGetLokiLog(String namespace, String jobName,
                                                 String direction, Long limit,
                                                 Long start, Long end) {
        try {
            return getLokiLog(namespace, jobName, direction, limit, start, end);
        } catch (Exception e) {
            log.error("Error fetching logs from Loki for job '{}'", jobName, e);
            return null;
        }
    }

    /**
     * 安全地更新epoch数据
     */
    private void safelyUpdateEpochData(Map<String, String> logBatch,
                                       ModelJobLogData modelJobLogData,
                                       ModelJob modelJob,
                                       String jobName) {
        try {
            Map<String, List<String>> currentEpochDetail = parseEpochDetailFromLogs(logBatch);
            if (currentEpochDetail == null || currentEpochDetail.isEmpty()) {
                return;
            }

            // 合并epoch数据
            String existingJson = modelJobLogData.getEpochDetail();
            Map<String, List<String>> mergedEpochDetail = new HashMap<>();

            if (existingJson != null && !existingJson.isEmpty()) {
                try {
                    mergedEpochDetail = new ObjectMapper().readValue(existingJson,
                            new TypeReference<Map<String, List<String>>>() {});
                } catch (IOException e) {
                    log.warn("Failed to parse existing epoch detail, starting fresh", e);
                }
            }

            // 合并新数据
            for (Map.Entry<String, List<String>> entry : currentEpochDetail.entrySet()) {
                mergedEpochDetail.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .addAll(entry.getValue());
            }

            // 序列化并保存
            if (!mergedEpochDetail.isEmpty()) {
                String json = new ObjectMapper().writeValueAsString(mergedEpochDetail);
                modelJobLogData.setEpochDetail(json);

                // 计算进度
                updateProgress(mergedEpochDetail, modelJobLogData, modelJob);
            }
            // 保存到数据库
            modelJobLogData.setModelJob(modelJob);
            modelJobLogDataRepo.save(modelJobLogData);

        } catch (JsonProcessingException e) {
            log.error("JSON processing error for job '{}'", jobName, e);
        } catch (Exception e) {
            log.error("Error data for job '{}'", jobName, e);
        }
    }

    /**
     * 更新训练进度
     */
    private void updateProgress(Map<String, List<String>> epochDetail,
                                ModelJobLogData logData,
                                ModelJob modelJob) {
        try {
            if (!epochDetail.containsKey("epoch") || epochDetail.get("epoch").isEmpty()) {
                logData.setProgress(BigDecimal.valueOf(0.0));
                return;
            }

            String totalEpochsStr = modelJob.getParams().get("HP_EPOCHES");
            if (totalEpochsStr == null || totalEpochsStr.isEmpty()) {
                logData.setProgress(BigDecimal.valueOf(0.0));
                return;
            }

            List<String> epochList = epochDetail.get("epoch");
            int currentEpoch = epochList.stream()
                    .mapToInt(s -> {
                        try {
                            return Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);

            double totalEpochs = Double.parseDouble(totalEpochsStr);
            if (totalEpochs > 0) {
                double progress = Math.min((currentEpoch / totalEpochs) * 100.0, 100.0);
                logData.setProgress(BigDecimal.valueOf(progress));
            } else {
                logData.setProgress(BigDecimal.valueOf(0.0));
            }
        } catch (Exception e) {
            log.error("Error calculating progress", e);
            logData.setProgress(BigDecimal.valueOf(0.0));
        }
    }

    /**
     * 安全地写入日志到文件
     */
    private int safelyWriteLogsToFile(Map<String, String> logBatch,
                                      BufferedWriter writer) {
        int written = 0;
        try {
            for (Map.Entry<String, String> entry : logBatch.entrySet()) {
                try {
                    writer.write(entry.getKey() + " " + entry.getValue());
                    writer.newLine();
                    written++;
                } catch (IOException e) {
                    log.warn("Failed to write log entry with timestamp {}", entry.getKey(), e);
                }
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error flushing log buffer", e);
        }
        return written;
    }

    /**
     * 获取最后一个时间戳
     */
    private String getLastTimestamp(Map<String, String> logBatch) {
        if (logBatch == null || logBatch.isEmpty()) {
            return null;
        }
        return logBatch.keySet().stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * 安全地解析时间戳
     */
    private Long safelyParseTimestamp(String timestamp, Long fallback) {
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse timestamp '{}', using fallback", timestamp);
            return fallback;
        }
    }

    /**
     * 推进时间戳
     */
    private Long advanceTimestamp(Long current, Long increment) {
        if (current == null) {
            return Instant.now().toEpochMilli() * 1_000_000L;
        }
        return current + increment;
    }

    /**
     * 【最终修正版】
     * 严格按照您提供的逻辑，解析Epoch相关的详细数据。
     *
     * @param logMap 从 getLog() 获取的日志数据
     * @return 包含各Epoch参数列表的Map, e.g., {"epoch": ["1", "2"], "train loss": ["15.2", "14.8"]}
     */
    public Map<String, List<String>> parseEpochDetailFromLogs(Map<String, String> logMap) {
        if (logMap == null || logMap.isEmpty()) {
            return null;
        }

        // 1. 获取参数名列表 (keys)
        List<String> epochParams = getEpochParam(logMap);
        if (epochParams == null) {
            //log.warn("Could not find any line starting with 'epoch' to determine parameters.");
            return null;
        }

        // 2. 初始化结果Map
        Map<String, List<String>> epochDetail = new HashMap<>();
        for (String epochParam : epochParams) {
            epochDetail.put(epochParam, new ArrayList<>());
        }

        // 3. 遍历所有日志行，填充数据
        for (String strRow : logMap.values()) {
            // 只处理以 "epoch" 开头的行，这与您的逻辑完全一致
            if (strRow.toLowerCase().startsWith("epoch")) {
                // 使用'|'分割出各个键值对片段
                String[] segments = strRow.split("\\|");

                for (String segment : segments) {
                    // 使用':'分割键和值
                    String[] paramDetail = segment.split(":", 2); // limit=2 确保值中即使有冒号也不会被错误分割
                    if (paramDetail.length == 2) {
                        String key = paramDetail[0].trim().toLowerCase();
                        String value = paramDetail[1].trim();
                        // 确保这个key是我们从第一行识别出来的，防止日志格式错乱
                        if (epochDetail.containsKey(key)) {
                            epochDetail.get(key).add(value);
                        }
                    }
                }
            }
        }

        // 验证数据完整性：如果任何一个列表为空，可能说明解析有问题或日志不完整
        if (epochDetail.values().stream().anyMatch(List::isEmpty)) {
            log.warn("Epoch detail parsing resulted in one or more empty lists. Check log format consistency.");
        }

        return epochDetail;
    }

    /**
     * 从 Loki 获取任务日志的核心方法。
     * 专门用于实时日志获取，不包含文件回退逻辑。
     *
     * @param namespace 命名空间
     * @param jobName   任务名称
     * @param direction 查询方向 ("FORWARD" or "BACKWARD")
     * @param limit     日志条数限制
     * @param start     查询开始时间 (纳秒)
     * @param end       查询结束时间 (纳秒)
     * @return 包含时间戳和日志内容的 Map，如果没有日志则返回 null 或空 Map
     */
    public Map<String, String> getLokiLog(String namespace, String jobName, String direction, Long limit, Long start, Long end) {

        // 直接从 Loki 获取日志
        Map<String, String> logsFromLoki = queryLogsFromLoki(namespace, jobName, direction, limit, start, end);

        if (logsFromLoki != null && !logsFromLoki.isEmpty()) {
            log.info("Retrieved {} log entries from Loki for job '{}'", logsFromLoki.size(), jobName);
        } else {
            //log.info("No logs found in Loki for job '{}' in time range {} - {}", jobName, start, end);
        }

        return logsFromLoki;
    }

    /**
     * 私有辅助方法，封装了从 Loki 查询日志的所有逻辑。
     *
     * @return 从 Loki 解析出的日志 Map，如果查询失败或无结果则返回 null。
     */
    private Map<String, String> queryLogsFromLoki(String namespace, String jobName, String direction, Long limit, Long startParam, Long endParam) {
        // 1. 构建 Loki 查询语句 (LQL)
        String query = buildLokiQuery(namespace, jobName);
        if (query == null) {
            // jobName 格式不被支持
            return null;
        }

        // 2. URL编码查询语句
        String queryEncoded;
        try {
            queryEncoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("URL encoding failed for Loki query.", e);
            return null;
        }

        // 3. 计算查询的时间范围
        // Loki 的时间戳是纳秒级别的
        long start = (startParam != null) ? startParam : 0L;
        long end = (endParam != null) ? endParam : 0L;

        ModelJob modeljob = modelJobRepo.findModelJobByName(jobName);
        Calendar calendar = Calendar.getInstance();
        long currentTimeNs = System.currentTimeMillis() * 1_000_000;

        if (start == 0L) {
            if (modeljob == null) {
                // 默认查询最近一天
                calendar.setTime(new Date());
                end = currentTimeNs;
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                start = calendar.getTimeInMillis() * 1_000_000;
            } else {
                // 使用数据库时间并扩展范围
                Date createTime = modeljob.getCreateTime();
                Date lastJobTime = modeljob.getLastJobTime();

                calendar.setTime(createTime);
                calendar.add(Calendar.HOUR_OF_DAY, -12);
                start = calendar.getTimeInMillis() * 1_000_000;

                calendar.setTime(lastJobTime);
                calendar.add(Calendar.HOUR_OF_DAY, 12);
                end = calendar.getTimeInMillis() * 1_000_000;
            }
        }

        // 3. 关键修复：确保end不早于start
        if (end <= start) {
            end = currentTimeNs;
            if (end <= start) {
                // 极端情况：start是未来时间
                start = end - 86_400_000_000_000L;
            }
        }

        // 4. 调用 Feign 客户端执行查询
        // log.info("Querying Loki with LQL: [{}], start: [{}], end: [{}], limit: [{}]", query, start, end, limit);
        JSONObject queryResult;
        try {
            queryResult = logFeign.queryLog(queryEncoded, direction, limit, start, end);
            // 新增日志打印 HTTP 返回结果
            // log.info("Loki HTTP response: {}", queryResult != null ? queryResult.toJSONString() : "null");
        } catch (Exception e) {
            return null;
        }

        // 5. 解析查询结果
        if (queryResult == null || !queryResult.containsKey("data")) {
            log.warn("Loki query result for job '{}' is null or does not contain 'data' field.", jobName);
            return null;
        }

        JSONObject data = queryResult.getJSONObject("data");
        if (!"streams".equals(data.getString("resultType")) || !data.containsKey("result")) {
            log.warn("Loki query result for job '{}' is not of type 'streams' or is missing 'result' array.", jobName);
            return null;
        }

        JSONArray logArray = data.getJSONArray("result");
        if (logArray.isEmpty()) {
            return new TreeMap<>(); // 没有日志流
        }

        Map<String, String> resultMap = new TreeMap<>();
        for (int i = 0; i < logArray.size(); i++) {
            JSONObject stream = logArray.getJSONObject(i);
            JSONArray values = stream.getJSONArray("values");
            for (int j = 0; j < values.size(); j++) {
                JSONArray logEntry = values.getJSONArray(j);
                if (logEntry.size() >= 2) {
                    resultMap.put(logEntry.getString(0), logEntry.getString(1));
                }
            }
        }

        return resultMap;
    }

    /**
     * 根据 jobName 动态构建 Loki 查询语句。
     *
     * @param jobName 任务名称
     * @return 构建好的LQL，如果jobName格式无法识别则返回null。
     */
    private String buildLokiQuery(String namespace, String jobName) {
        if (jobName.contains("-convert-")) {
            return String.format("{job_name=\"%s\",convert_job=\"%s\",namespace=\"%s\"}", jobName, jobName, namespace);
        } else if (jobName.startsWith("job-")) {
            return String.format("{job_name=\"%s\",train_job=\"%s\",namespace=\"%s\"}", jobName, jobName, namespace);
        } else if (jobName.startsWith("validate-")) {
            return String.format("{job_name=\"%s\",validate_job=\"%s\",namespace=\"%s\"}", jobName, jobName, namespace);
        } else {
            log.error("Unrecognized jobName format: {}", jobName);
            return null;
        }
    }


    /**
     * 【新增的纯解析方法】 - 示例
     * 从给定的日志Map中解析出测试结果（如 accuracy, recall）。
     * 此方法不执行任何I/O操作。
     * 注意: 这里的解析逻辑需要您根据实际的日志格式进行调整。
     *
     * @param logMap 从 getLog() 获取的日志数据
     * @return 包含 "accuracy" 和 "recall" 值列表的Map
     */
    public Map<String, List<String>> parseTestedDetailFromLogs(Map<String, String> logMap) {
        if (logMap == null || logMap.isEmpty()) {
            return null;
        }

        List<String> accuracyList = new ArrayList<>();
        List<String> recallList = new ArrayList<>();

        Pattern accuracyPattern = Pattern.compile("accuracy:\\s*([0-9.]+)");
        Pattern recallPattern = Pattern.compile("recall:\\s*([0-9.]+)");

        for (String logBlock : logMap.values()) {
            String[] lines = logBlock.split("\\r?\\n");
            for (String line : lines) {
                String trimmedLine = line.trim().toLowerCase();

                if (trimmedLine.contains("tested:")) {
                    Matcher accuracyMatcher = accuracyPattern.matcher(trimmedLine);
                    if (accuracyMatcher.find()) {
                        String accuracyValue = accuracyMatcher.group(1);
                        accuracyList.add(accuracyValue);
                        log.info("解析到accuracy: {}", accuracyValue);
                    }

                    // 解析recall
                    Matcher recallMatcher = recallPattern.matcher(trimmedLine);
                    if (recallMatcher.find()) {
                        String recallValue = recallMatcher.group(1);
                        recallList.add(recallValue);
                        log.info("解析到recall: {}", recallValue);
                    }
                }
            }
        }

        if (accuracyList.isEmpty() && recallList.isEmpty()) {
            log.info("未解析到任何数据");
            return null;
        }

        Map<String, List<String>> testedDetail = new HashMap<>();
        testedDetail.put("accuracy", accuracyList);
        testedDetail.put("recall", recallList);

        log.info("解析完成: accuracy={}, recall={}", accuracyList.size(), recallList.size());
        return testedDetail;
    }

    /**
     * 获取部署日志内容
     *
     * @param namespace
     * @param deployName
     * @param direction
     * @param limit
     * @param start
     * @param end
     * @return
     */
    public Map<String, String> getDeployLog(String namespace, String deployName, String direction, Long limit, Long start, Long end) {
        String query = "{app=\"" + deployName + "\"" + "," + "namespace=" + "\"" + namespace + "\"}";
        String queryEncoded = null;
        try {
            queryEncoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        JSONObject queryResult = logFeign.queryLog(queryEncoded, direction, limit, start, end);
        if (queryResult.containsKey("data")) {
            if (queryResult.getJSONObject("data").containsKey("result")) {
                JSONArray logArray = queryResult.getJSONObject("data").getJSONArray("result");
                Map<String, String> map = new TreeMap<>();
                for (int i = 0; i < logArray.size(); i++) {
                    JSONObject jsonObject = logArray.getJSONObject(i);
                    JSONArray jsonArrayNew = jsonObject.getJSONArray("values");
                    for (int j = 0; j < jsonArrayNew.size(); j++) {
                        JSONArray jsonArray2 = jsonArrayNew.getJSONArray(j);
                        map.put(jsonArray2.getString(0), jsonArray2.getString(1));
                    }
                }
                return map;
            }
        }
        return null;
    }

    /**
     * 获取打印内容，并将每个参数的值整理进list中
     * 如  epoch:[1,2,3] loss:[0.005,0.003,0.004]
     *
     * @return
     */
    public Map<String, List<String>> getEpochDetail(String namespace, String jobName, Long limit, Long start, Long end) {
        Map<String, String> queryMap = getLog(namespace, jobName, "FORWARD", limit, start, end);
        return parseEpochDetailFromLogs(queryMap);
    }

    /**
     * 【最终修正版】
     * 严格按照您提供的逻辑，从第一条符合条件的日志中提取出所有的参数名。
     * @param logMap 日志数据
     * @return 参数名列表
     */
    private List<String> getEpochParam(Map<String, String> logMap) {
        for (String str : logMap.values()) {
            if (str.toLowerCase().startsWith("epoch")) {
                List<String> epochParams = new ArrayList<>();
                // 分割出如 "epoch: 1", "train loss: 15.22" 的片段
                String[] segments = str.split("\\|");
                for (String segment : segments) {
                    // 分割出 key 和 value
                    String[] pair = segment.split(":", 2);
                    if(pair.length >= 1) {
                        // 只取 key，并清理空格和转为小写，以保持一致性
                        epochParams.add(pair[0].trim().toLowerCase());
                    }
                }
                return epochParams; // 找到第一行就返回
            }
        }
        return null; // 如果找不到任何 epoch 开头的行，返回 null
    }


    /**
     * 获取打印信息中每行有多少个关于tested的参数内容,仅仅适用于当前的手写数字数据集！！！
     * 如 [epoch, loss, accuracy]
     */
    private List<String> getTestedParam(Map<String, String> queryMap) {
        List<String> testedParams = new ArrayList<>();
        for (String str : queryMap.values()) {
            if (str.toLowerCase().contains("tested")) {
                for (String s : str.toLowerCase().split(",")) {
                    testedParams.add(s.split(":")[0].trim());
                }
                return testedParams;
            }
        }
        return null;
    }

    public String getTotal(String namespace, String jobName, Long limit, Long start, Long end) {
        Map<String, String> queryMap = getLog(namespace, jobName, "FORWARD", limit, start, end);
        String totalImage = null;
        for (String str : queryMap.values()) {
            if (str.toLowerCase().contains("total sample number")) {
                totalImage = str.split(":")[1].trim();
            }
        }
        return totalImage;
    }

    /**
     * 获取打印内容，并将每个参数的值整理进list中
     * 如  epoch:[1,2,3] loss:[0.005,0.003,0.004]
     *
     * @return
     */
    public Map<String, List<String>> getTestedDetail(String namespace, String jobName, Long limit, Long start, Long end) {
        // 获取日志
        Map<String, String> queryMap = getLog(namespace, jobName, "FORWARD", limit, start, end);
        return parseTestedDetailFromLogs(queryMap);
    }


//    public boolean jobLogStorage(String namespace, String jobName, Long limit, Long start, Long end, String filePath) {
//        Map<String, String> queryMap = getLog(namespace, jobName, "FORWARD", limit, start, end);
//        JSONObject logJson = JSONObject.parseObject(JSONObject.toJSONString(queryMap));
//        return JsonUtils.createJsonFile(logJson, filePath);
//    }

//    public boolean testJobLogStorage(String namespace, String testJobName, String anotherTestJobName, Long limit, Long start, Long end, String filePath) {
//        if (anotherTestJobName == null) {
//            Map<String, String> queryMap = getLog(namespace, testJobName, "FORWARD", limit, start, end);
//            JSONObject nowJobLogJson = JSONObject.parseObject(JSONObject.toJSONString(queryMap));
//            Map<String, Object> testJobLog = new HashMap<>();
//            testJobLog.put("当前模型质检日志", nowJobLogJson);
//            List<String> accAList = getTestedDetail(namespace, testJobName, limit, start, end).get("accuracy");
//            double accAMax = accAList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
//            String testResult = "当前模型accuracy：" + accAMax;
//            testJobLog.put("result", testResult);
//            return JsonUtils.createJsonFile(testJobLog, filePath);
//        } else {
//            Map<String, String> queryMap = getLog(namespace, testJobName, "FORWARD", limit, start, end);
//            Map<String, String> anotherQueryMap = getLog(namespace, anotherTestJobName, "FORWARD", limit, start, end);
//            JSONObject nowJobLogJson = JSONObject.parseObject(JSONObject.toJSONString(queryMap));
//            JSONObject originJobLogJson = JSONObject.parseObject(JSONObject.toJSONString(anotherQueryMap));
//            Map<String, Object> testJobLog = new HashMap<>();
//
//            testJobLog.put("当前模型质检日志", nowJobLogJson);
//            testJobLog.put("原始模型质检日志", originJobLogJson);
//
//            List<String> accAList = getTestedDetail(namespace, testJobName, limit, start, end).get("accuracy");
//            List<String> accBList = getTestedDetail(namespace, anotherTestJobName, limit, start, end).get("accuracy");
//            double accAMax = accAList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
//            double accBMax = accBList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
//            int result = Double.compare(accAMax, accBMax);
//            String testResult = null;
//            if (result == 1) {
//                testResult = "当前模型accuracy：" + accAMax + "，" + "原始模型accuracy：" + accBMax + "，" + "，因此当前模型更优";
//            } else if (result == -1) {
//                testResult = "当前模型accuracy：" + accAMax + "，" + "原始模型accuracy：" + accBMax + "，" + "，因此原始模型更优";
//            } else {
//                testResult = "当前模型accuracy：" + accAMax + "，" + "原始模型accuracy：" + accBMax + "，" + "，因此当前模型与原始模型效果相同";
//            }
//            testJobLog.put("result", testResult);
//            return JsonUtils.createJsonFile(testJobLog, filePath);
//        }
//
//    }

    /**
     * 获取工作负载中指定 Job 日志中最高的 ACC
     *
     * @param nameSpace
     * @param modelJobName
     * @param limit
     * @param start
     * @param end
     * @return
     */
    public Double getJobAcc(String nameSpace, String modelJobName, Long limit, Long start, Long end) {
        Map<String, String> jobLogInfo = getLog(nameSpace, modelJobName, "FORWARD", limit, start, end);
        Set<Map.Entry<String, String>> entries = jobLogInfo.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            System.out.println("key: " + entry.getKey() + " value: " + entry.getValue());
        }
        return null;
    }


    /**
     * 从文件系统中获取日志，支持大 limit 值时的分批获取
     *
     * @param jobName 任务名称
     * @param limit 要获取的日志条数限制
     * @return 包含时间戳和日志内容的 Map，或在找不到日志时返回 null
     */
    public Map<String, String> getLogsFromFileWithLimit(String jobName, Long limit) {
        if (limit == null || limit <= 30000) {
            return getLogsFromFile(jobName, limit, null);
        }

        Map<String, String> allLogs = new LinkedHashMap<>();
        long remaining = limit;
        String currentLastTimestamp = null;
        String prevLastTimestamp = null; // 防止游标不前进导致死循环

        while (remaining > 0) {
            long batchSize = Math.min(30000, remaining);

            Map<String, String> batchLogs = getLogsFromFile(jobName, batchSize, currentLastTimestamp);
            if (batchLogs == null || batchLogs.isEmpty()) {
                break; // 没有更多日志
            }

            // 合并结果（LinkedHashMap 会保持已有顺序；如果 key 唯一，不会覆盖）
            allLogs.putAll(batchLogs);

            // 更新剩余数量
            remaining -= batchLogs.size();

            // 计算本批次的“最后一个”时间戳（保持通用，不做类型强转）
            String lastKey = null;
            if (batchLogs instanceof java.util.NavigableMap) {
                lastKey = ((java.util.NavigableMap<String, String>) batchLogs).lastKey();
            } else {
                // 依赖 LinkedHashMap 的插入顺序：迭代最后一个 key
                for (String k : batchLogs.keySet()) {
                    lastKey = k;
                }
            }

            // 防御：如果游标没有前进，说明后端查询可能是“>= lastTimestamp”，会卡住
            if (lastKey == null || lastKey.equals(prevLastTimestamp)) {
                break;
            }

            prevLastTimestamp = lastKey;
            // 让下一次从“lastKey 之后”开始。注意：确保 getLogsFromFile 的实现是严格大于 lastTimestamp
            currentLastTimestamp = lastKey;
        }

        return allLogs.isEmpty() ? null : allLogs;
    }


    public Map<String, String> getLogsFromFile(String jobName, Long maxLines, String lastTimestamp) {
        String logFilePath = Paths.get(nfs, bucketName, "train-log", jobName + ".log").toString();
        File logFile = new File(logFilePath);

        if (!logFile.exists() || logFile.length() == 0) {
            log.warn("Log file does not exist or is empty for job '{}' at path: {}", jobName, logFilePath);
            return null;
        }

        Map<String, String> logData = new LinkedHashMap<>(); // 保持插入顺序

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            boolean foundLastTimestamp = (lastTimestamp == null); // 是否已经找到 lastTimestamp
            StringBuilder currentContent = new StringBuilder();
            String currentTimestamp = null;
            int entryCount = 0;

            while ((line = reader.readLine()) != null && entryCount < maxLines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 检查是否是时间戳行或包含时间戳的行
                if (line.matches("^\\d{10,}$")) { // 纯时间戳行
                    if (foundLastTimestamp) {
                        // 已经过了 lastTimestamp，开始收集日志
                        if (currentTimestamp != null) {
                            logData.put(currentTimestamp, currentContent.toString());
                            entryCount++;
                            currentContent.setLength(0);
                        }
                        currentTimestamp = line;
                    } else if (line.equals(lastTimestamp)) {
                        // 找到 lastTimestamp，之后的内容才是我们需要的
                        foundLastTimestamp = true;
                    }
                }
                else {
                    // 尝试分割出可能的时间戳
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length > 0 && parts[0].matches("^\\d{10,}$")) {
                        if (foundLastTimestamp) {
                            // 已经过了 lastTimestamp，开始收集日志
                            if (currentTimestamp != null) {
                                logData.put(currentTimestamp, currentContent.toString());
                                entryCount++;
                                currentContent.setLength(0);
                            }
                            currentTimestamp = parts[0];
                            if (parts.length > 1) {
                                currentContent.append(parts[1]);
                            }
                        } else if (parts[0].equals(lastTimestamp)) {
                            // 找到 lastTimestamp，之后的内容才是我们需要的
                            foundLastTimestamp = true;
                        }
                    }
                    else if (currentTimestamp != null && foundLastTimestamp) {
                        // 多行日志内容，添加到当前条目的前面（因为我们是反向读取）
                        if (currentContent.length() > 0) {
                            currentContent.insert(0, "\n");
                        }
                        currentContent.insert(0, line);
                    }
                }
            }

            // 处理最后一个条目
            if (currentTimestamp != null && foundLastTimestamp && entryCount < maxLines) {
                logData.put(currentTimestamp, currentContent.toString());
            }

        } catch (IOException e) {
            log.error("Error reading log file for job '{}'", jobName, e);
            return null;
        }

        // 因为我们是从文件末尾开始读取的，所以收集的条目是时间倒序的
        // 反转结果，以保持时间正序（最新的日志在最后）
        Map<String, String> orderedLogData = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(logData.keySet());
        Collections.reverse(keys);
        for (String key : keys) {
            orderedLogData.put(key, logData.get(key));
        }

        return orderedLogData;
    }
}
