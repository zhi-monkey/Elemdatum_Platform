package org.dlut.adv.mineai.model.kubernetes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志持久化服务
 * @author mingming
 * @date 2025/08/10
 */
@Slf4j
@Service
public class LogPersistenceService {

    @Value("${storage.file-store-root-path}")
    private String nfs;

    @Value("/${minio.bucketName}/")
    private String bucketName;

    // 使用 ConcurrentHashMap 来为每个 jobName 维护一个锁
    // key: jobName
    // value: Lock object
    // 这样可以实现对不同文件的并发写入，但对同一文件的写入是串行的，防止冲突
    private final ConcurrentHashMap<String, Lock> fileLocks = new ConcurrentHashMap<>();

    /**
     * 异步地将日志数据持久化到NFS文件系统中。
     * 此方法被 @Async 注解，将在独立的线程中执行，不会阻塞调用方。
     *
     * @param jobName  任务名称，用于构建文件名和获取锁。
     * @param logData  要写入的日志数据 (TreeMap可以保证日志按时间戳有序)。
     */
    @Async("logFileWriteExecutor")
    public void persistLogsToFileAsync(String jobName, Map<String, String> logData) {
        if (logData == null || logData.isEmpty()) {
            log.info("Log data for job '{}' is empty, skipping persistence.", jobName);
            return;
        }

        // 构建完整的文件路径  Z:/dubhe-storage/cz-dev/train-log/
        String logDirPath = Paths.get(nfs, bucketName, "train-log").toString();
        String logFilePath = Paths.get(logDirPath, jobName + ".log").toString();

        log.info("Attempting to asynchronously write logs for job '{}' to path: {}", jobName, logFilePath);

        // 为当前 jobName 获取或创建一个锁，确保对同一文件的操作是线程安全的
        Lock lock = fileLocks.computeIfAbsent(jobName, k -> new ReentrantLock());

        lock.lock();
        try {
            // 确保目录存在
            File directory = new File(logDirPath);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    log.error("Failed to create log directory: {}", logDirPath);
                    return;
                }
            }

            // 使用 try-with-resources 确保 FileWriter 和 BufferedWriter 总是被关闭
            // FileWriter 第二个参数为 true，表示以追加模式写入文件
            try (FileWriter fileWriter = new FileWriter(logFilePath, false);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                for (Map.Entry<String, String> entry : logData.entrySet()) {
                    // 写入格式: "timestamp log_message\n"
                    bufferedWriter.write(entry.getKey() + " " + entry.getValue());
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush(); // 确保所有缓冲数据写入文件
                log.info("Successfully wrote {} log entries for job '{}'.", logData.size(), jobName);

            } catch (IOException e) {
                log.error("Error writing logs to file for job '{}'", jobName, e);
            }
        } finally {
            lock.unlock(); // 在 finally 块中释放锁，保证锁一定会被释放
        }
    }

    /**
     * 从NFS文件中读取指定job的日志。
     *
     * @param jobName 任务名称。
     * @return 一个包含日志时间戳和内容的Map，如果文件不存在或为空则返回null。
     */
    public Map<String, String> getLogsFromFile(String jobName) {
        String logFilePath = Paths.get(nfs, bucketName, "train-log", jobName + ".log").toString();
        File logFile = new File(logFilePath);

        if (!logFile.exists() || logFile.length() == 0) {
            log.warn("Log file does not exist or is empty for job '{}' at path: {}", jobName, logFilePath);
            // 文件不存在或为空
            return null;
        }

        // 使用 TreeMap 保证从文件中读取的日志也是按时间戳排序的
        Map<String, String> logData = new TreeMap<>();

        // 使用 try-with-resources 自动关闭 BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // 跳过空行
                }

                // 使用 split(" ", 2) 来分割，确保日志内容中的空格不会被错误分割
                // "timestamp log message with spaces" -> ["timestamp", "log message with spaces"]
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    String timestamp = parts[0];
                    String message = parts[1];
                    logData.put(timestamp, message);
                } else {
                    // 如果某行格式不正确，可以记录一个警告并跳过
                    log.warn("Skipping malformed log line in file {}: {}", logFilePath, line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading log file for job '{}'", jobName, e);
            return null;
        }

        log.info("Successfully read {} log entries from file for job '{}'.", logData.size(), jobName);
        return logData;
    }



}

