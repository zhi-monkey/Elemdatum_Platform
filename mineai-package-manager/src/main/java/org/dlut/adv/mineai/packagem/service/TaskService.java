package org.dlut.adv.mineai.packagem.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.packagem.entity.PackedFileResult;
import org.dlut.adv.mineai.packagem.entity.Task;
import org.dlut.adv.mineai.packagem.enums.TaskStatus;
import org.dlut.adv.mineai.packagem.utils.MinioUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 * @author mingming
 * @date 2024/09/29
 */
@Slf4j
@Service
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    @Getter
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    @Resource
    private MinioUtil minioUtil;

    private static final String RUN_BASH_COMMAND = "firejail --private --read-only=/package --net=none --caps.drop=all --seccomp --noroot --nogroups --private-tmp --trace --rlimit-cpu=10 --rlimit-fsize=10000 --rlimit-nofile=50 bash file_encryption.sh";

    /**
     * 命令执行结果
     */
    @Getter
    private static class CommandResult {
        private final int exitCode;
        private final List<String> outputLines;

        public CommandResult(int exitCode, List<String> outputLines) {
            this.exitCode = exitCode;
            this.outputLines = outputLines;
        }
    }

    public String startPackaging(String appZipPath, String authCode) {
        String taskId = generateTaskId();
        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartTime(System.currentTimeMillis());
        tasks.put(taskId, task);
        System.out.println("Task " + taskId + " started.");
        logger.info("Task {} started.", taskId);

        runPackagingTask(taskId, appZipPath, authCode);

        // 开始监听任务，并将调度任务的 future 保存到 task 对象中
        ScheduledFuture<?> future = listenToTask(taskId);
        task.setScheduledFuture(future);
        tasks.put(taskId, task);

        return taskId;
    }

    private String generateTaskId() {
        return "task-" + taskIdCounter.incrementAndGet();
    }

    @Async
    protected void runPackagingTask(String taskId, String appZipPath, String authCode) {

        try {
            pack(taskId, appZipPath, authCode);

            Task task = tasks.get(taskId);
            // 只有在任务没有失败的情况下才设置为 COMPLETED
            if (task.getStatus() != TaskStatus.FAILED) {
                task.setStatus(TaskStatus.COMPLETED);
                tasks.put(taskId, task);
                System.out.println("Task " + taskId + " completed.");
            } else {
                log.warn("Task {} 已标记为 FAILED，不设置为 COMPLETED", taskId);
            }
        } catch (InterruptedException e) {
            Task task = tasks.get(taskId);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorType("SYSTEM_ERROR");
            task.setErrorMessage("任务执行异常：" + e.getMessage());
            tasks.put(taskId, task);
            System.err.println("Task " + taskId + " failed: " + e.getMessage());
            log.error("Task {} failed: {}", taskId, e.getMessage());
            cleanupTask(taskId);
        }
    }

    @Async
    protected ScheduledFuture<?> listenToTask(String taskId) {
        Runnable taskChecker = () -> {
            Task task = tasks.get(taskId);
            if (task == null) {
                cleanupTask(taskId);
                return;
            }

            TaskStatus status = task.getStatus();
            if (status != null) {
                System.out.println("监听任务 " + taskId + ": 状态 - " + status);
                log.info("监听任务 {} 状态 - {}", taskId, status);

                long currentTime = System.currentTimeMillis();
                long startTime = task.getStartTime();

                // 如果任务完成或失败，开始计时是否超过3分钟
                if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                    // 超过3分钟则清理
                    if (currentTime - startTime > 3 * 60 * 1000) {
                        System.out.println("任务 " + taskId + " 超时未完成，开始清理...");
                        log.info("任务 {} 超时未完成，开始清理...", taskId);
                        cleanupTask(taskId);

                        // 取消调度任务，释放资源
                        ScheduledFuture<?> future = task.getScheduledFuture();
                        if (future != null) {
                            future.cancel(false);
                        }
                        return;
                    }
                }

                // 检查是否任务正在进行，但已经超过3分钟
                if (status == TaskStatus.IN_PROGRESS && currentTime - startTime > 3 * 60 * 1000) {
                    cleanupTask(taskId);

                    // 取消调度任务
                    ScheduledFuture<?> future = task.getScheduledFuture();
                    if (future != null) {
                        future.cancel(false);
                    }
                }
            }
        };

        // 每分钟调度一次任务检查
        return scheduler.scheduleAtFixedRate(taskChecker, 0, 1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    private void cleanupTask(String taskId) {
        // 移除任务
        Task task = tasks.get(taskId);
        String folderName = task.getFileName();
        // 删linux目录
//        String command = "cd /home/package && " +
//                "rm -rf " + folderName;
        String[] command = new String[]{"rm", "-rf", "/home/package/" + folderName};
        runCommand(command);

        tasks.remove(taskId);
        // 删minio文件
//        minioUtil.removeObject(task.getFullFileName());
        System.out.println("Task " + taskId + " has been cleaned up.");
        logger.info("Task {} has been cleaned up.", taskId);
    }

    private void pack(String taskId, String appZipPath, String authCode) throws InterruptedException {
        // appZipPath: cz-dev/package/test_make_bin-1-1730709966573.zip
        String fileName = null;
        try {
            String decodedPath = URLDecoder.decode(appZipPath, StandardCharsets.UTF_8.name());
            // fileName: test_make_bin-1-1730709966573.zip
            fileName = getFileName(decodedPath);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String objectName = "package/" + fileName;
        String targetDirPath = "/home/package";
        minioUtil.downloadObject(objectName, targetDirPath); // 下载到 /home/package
        log.info("minio下载成功");
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);

        Task task = tasks.get(taskId);
        task.setFileName(nameWithoutExtension);
        task.setFullFileName(fileName);
        tasks.put(taskId, task);
        String[] fullCommand = getPackStrings(fileName,authCode);
        try {
            // 使用 ProcessBuilder 构建进程
            CommandResult result = runCommand(fullCommand);

            // 根据退出码判断是否执行成功
            if (result.getExitCode() == 0) {
                System.out.println("Command executed successfully");
                log.info("Command executed successfully");
            } else {
                System.out.println("Command execution failed with exit code " + result.getExitCode());
                log.error("Command execution failed with exit code {}", result.getExitCode());
                log.error("命令输出行数: {}", result.getOutputLines().size());
                
                // 解析错误信息
                parseErrorFromOutput(task, result.getOutputLines(), result.getExitCode());
                
                log.error("解析后的错误类型: {}", task.getErrorType());
                log.error("解析后的错误信息: {}", task.getErrorMessage());
                
                task.setStatus(TaskStatus.FAILED);
                tasks.put(taskId, task);
            }

        } catch (Exception e) {
            logger.error("Exception occurred while executing command", e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorType("SYSTEM_ERROR");
            task.setErrorMessage("系统异常：" + e.getMessage());
            tasks.put(taskId, task);
            cleanupTask(taskId);
        }
    }

    /**
     * @param filePath 解码后的路径
     * @return {@link String }
     */
    private static String getFileName(String filePath) {
        String fileName;
        int lastUnixPos = filePath.lastIndexOf('/');
        int lastWindowsPos = filePath.lastIndexOf('\\');
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);

        if (lastSeparator != -1) {
            fileName = filePath.substring(lastSeparator + 1);
        } else {
            // 无路径分隔符，直接使用整个字符串
            fileName = filePath;
        }

        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file path: " + filePath);
        }
        return fileName;
    }

    private static CommandResult runCommand(String[] fullCommand) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> outputLines = new ArrayList<>();

        // 实时读取输出日志
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
                outputLines.add(line);
            }
        }

        // 等待命令执行完成
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, outputLines);
    }

    /**
     * 从命令输出中解析错误信息
     */
    private void parseErrorFromOutput(Task task, List<String> outputLines, int exitCode) {
        log.info("开始解析错误信息，退出码: {}, 输出行数: {}", exitCode, outputLines.size());
        
        // 先检查输出中是否有特定的错误标识
        for (String line : outputLines) {
            log.debug("检查输出行: {}", line);
            
            // 检查是否是授权码错误
            if (line.contains("ERROR_AUTH_CODE_INVALID") || line.contains("授权码验证失败")) {
                task.setErrorType("AUTH_CODE_INVALID");
                task.setErrorMessage("授权码验证失败，请检查授权码是否正确");
                log.warn("检测到授权码验证失败 - 从输出行识别");
                return;
            }
        }

        // 根据退出码判断错误类型
        if (exitCode == 1) {
            // 退出码 1 表示打包失败
            task.setErrorType("PACK_ERROR");
            task.setErrorMessage("打包失败，退出码：" + exitCode);
            log.error("打包错误，退出码: {}", exitCode);
        } else if (exitCode == 2) {
            // 退出码 2 表示授权码错误
            task.setErrorType("AUTH_CODE_INVALID");
            task.setErrorMessage("授权码验证失败，请检查授权码是否正确");
            log.warn("检测到授权码验证失败 (exitCode={})", exitCode);
        } else {
            // 其他退出码
            task.setErrorType("PACK_ERROR");
            task.setErrorMessage("打包失败，退出码：" + exitCode);
            log.error("其他打包错误，退出码: {}", exitCode);
        }
    }

    private static String[] getPackStrings(String fullFileName, String authCode) {
        //输出授权码
        log.info("授权码为：{}", authCode);
        int dotIndex = fullFileName.lastIndexOf('.');
        String nameWithoutExtension = (dotIndex == -1) ? fullFileName : fullFileName.substring(0, dotIndex);

        // 判断授权码是否为空，构建不同的命令
        String authCodeParam = (authCode != null && !authCode.trim().isEmpty()) 
                ? " -o " + authCode 
                : "";

        String command = "cd /home/package && " +
                "mkdir -p " + nameWithoutExtension + " && " +
                "unzip -o " + fullFileName + " -d " + nameWithoutExtension + " && " +
                "rm " + fullFileName + " && " +
                "cd /home/package/" + nameWithoutExtension + " && " +
                // 替换为检测唯一子目录（无论名称）
                "if [ $(ls -d */ 2>/dev/null | wc -l) -eq 1 ]; then " +
                "   dir_name=$(ls -d */); " +
                "   mv \"$dir_name\"* . 2>/dev/null || true; " +
                "   mv \"$dir_name\"/.[^.]* . 2>/dev/null || true; " +
                "   rmdir \"$dir_name\" 2>/dev/null || true; " +
                "fi; " +
                RUN_BASH_COMMAND + authCodeParam;

        return new String[]{"/bin/bash", "-c", command};
    }

    public String checkTaskStatus(String taskId) {
        return tasks.get(taskId).getStatus().toString();
    }

    /**
     * 获取任务详细信息
     */
    public Task getTaskDetail(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            log.info("获取任务详情: taskId={}, status={}, errorType={}, errorMessage={}", 
                    taskId, task.getStatus(), task.getErrorType(), task.getErrorMessage());
        } else {
            log.warn("任务不存在: taskId={}", taskId);
        }
        return task;
    }

    @SneakyThrows
    public PackedFileResult getPackedFile(String taskId) {
        String outputDirPath = "/home/package/" + tasks.get(taskId).getFileName() + "/output/";
        Path outputDir = Paths.get(outputDirPath);

        log.info("Searching for packed file in directory: {}", outputDir);

        if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
            throw new FileNotFoundException("Output directory not found for task: " + taskId);
        }

        try (Stream<Path> stream = Files.list(outputDir)) {
            Optional<Path> firstFile = stream
                    .filter(Files::isRegularFile)
                    .findFirst();

            if (firstFile.isPresent()) {
                Path filePath = firstFile.get();
                String fileName = filePath.getFileName().toString();
                log.info("Found packed file: {}", fileName);

                byte[] bytes = Files.readAllBytes(filePath);
                cleanupTask(taskId);

                return new PackedFileResult(fileName, bytes);
            } else {
                throw new FileNotFoundException("No packed file found in output directory for task: " + taskId);
            }
        }
    }

    /**
     * 开始替换并打包
     * @param alterFilePath 替换文件的minio文件路径 /tmp/best.rknn
     * @param appZipPath 参考modelApplication 的minio路径 cz-dev/package/test_make_bin-1-1730709966573.zip
     * @return {@link String }
     */
    public String startAlterPackaging(String appZipPath, String alterFilePath, String localFileName, String classesUrl, String authCode) {
        String taskId = generateTaskId();
        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartTime(System.currentTimeMillis());
        tasks.put(taskId, task);
        logger.info("AlterTask {} started.", taskId);
        runAlterPackagingTask(taskId, alterFilePath, appZipPath, localFileName, classesUrl, authCode);

        // 开始监听任务，并将调度任务的 future 保存到 task 对象中
        ScheduledFuture<?> future = listenToTask(taskId);
        task.setScheduledFuture(future);
        tasks.put(taskId, task);

        return taskId;
    }

    @Async
    protected void runAlterPackagingTask(String taskId, String alterFilePath, String appZipPath, String localFileName, String classesUrl, String authCode) {
        try {
            alterAndPack(taskId, alterFilePath, appZipPath, localFileName, classesUrl, authCode);

            Task task = tasks.get(taskId);
            // 只有在任务没有失败的情况下才设置为 COMPLETED
            if (task.getStatus() != TaskStatus.FAILED) {
                task.setStatus(TaskStatus.COMPLETED);
                tasks.put(taskId, task);
                System.out.println("Task " + taskId + " completed.");
            } else {
                log.warn("AlterTask {} 已标记为 FAILED，不设置为 COMPLETED", taskId);
            }
        } catch (Exception e) {
            Task task = tasks.get(taskId);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorType("SYSTEM_ERROR");
            task.setErrorMessage("任务执行异常：" + e.getMessage());
            tasks.put(taskId, task);
            System.err.println("AlterTask " + taskId + " failed: " + e.getMessage());
            log.error("AlterTask {} failed: {}", taskId, e.getMessage(), e);
            cleanupTask(taskId);
        }
    }

    public void alterAndPack(String taskId, String alterFilePath, String appZipPath, String localFileName, String classesUrl, String authCode) {
        // appZipPath: cz-dev/package/test_make_bin-1-1730709966573.zip
        String fileName = Paths.get(appZipPath).getFileName().toString();
        // fileName: test_make_bin-1-1730709966573.zip
        log.info("fileName{}", fileName);
        // 下载示例zip包
        String objectPath = "/package" + "/" + fileName;
        String targetDirPath = "/home/package";
        minioUtil.downloadObject(objectPath, targetDirPath);

        log.info("minio下载成功");
        int dotIndex = fileName.lastIndexOf('.');
        // test_make_bin-1-1730709966573
        String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        // /home/package/test_make_bin-1-1730709966573
        String folderPath = "/home/package/" + nameWithoutExtension;
        // 从localFilePath中获取文件并写入文件夹中
        // 创建文件夹
        File folder = new File(folderPath);
        // 理论上来说不应该存在
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                log.info("文件夹创建成功: {}", folderPath);
            } else {
                log.error("文件夹创建失败: {}", folderPath);
                throw new RuntimeException("文件夹创建失败");
            }
        }
        // 下载替换的模型到指定目录(与之后解压的那一坨在一起)
        // 目前替换文件位置: /home/package/test_make_bin-1-1730709966573/best.rknn
        // 目前zip包的位置: /home/package/test_make_bin-1-1730709966573.zip
        minioUtil.downloadObject(alterFilePath, folderPath);
        // 把classes.txt下载到与zip包同名的文件夹中 /home/package/test_make_bin-1-1730709966573/classes.txt  classesFileUrl:/cz-dev/dataset/${datasetId}/?versionFile/${versionName}/YOLO/annotation/classes.txt subString之后为 dataset/...
        minioUtil.downloadObject(classesUrl, folderPath);

        // 更新Task对象
        Task task = tasks.get(taskId);
        task.setFileName(nameWithoutExtension);
        task.setFullFileName(fileName);
        tasks.put(taskId, task);
        String[] fullCommand = getAlterPackStrings(fileName, authCode);

        try {
            // 使用 ProcessBuilder 构建进程
            CommandResult result = runCommand(fullCommand);

            // 根据退出码判断是否执行成功
            if (result.getExitCode() == 0) {
                log.info("Command executed successfully");
            } else {
                log.error("Command execution failed with exit code {}", result.getExitCode());
                
                // 解析错误信息
                parseErrorFromOutput(task, result.getOutputLines(), result.getExitCode());

                task.setStatus(TaskStatus.FAILED);
                tasks.put(taskId, task);
                log.info("已更新 task 到 map: taskId={}, errorType={}, errorMessage={}", 
                        taskId, task.getErrorType(), task.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Exception occurred while executing command", e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorType("SYSTEM_ERROR");
            task.setErrorMessage("系统异常：" + e.getMessage());
            tasks.put(taskId, task);
            cleanupTask(taskId);
        }
    }

    private static String[] getAlterPackStrings(String fullFileName, String authCode) {
        //输出授权码
        log.info("授权码为：{}", authCode);
        String nameWithoutExtension = fullFileName.substring(0, fullFileName.lastIndexOf('.'));
        
        // 判断授权码是否为空，构建不同的命令
        String authCodeParam = (authCode != null && !authCode.trim().isEmpty()) 
                ? " -o " + authCode 
                : "";
        
        String command = String.format(
                "cd /home/package && " +
                        "unzip '%s' -d '%s' && " +
                        "cd '%s' && " +
                        // 查找并移动嵌套的子目录内容到当前目录
                        "if [ $(ls -d */ 2>/dev/null | wc -l) -eq 1 ]; then " +
                        "    dir_name=$(ls -d */); " +
                        "    mv \"$dir_name\"* . 2>/dev/null || true; " +
                        "    rmdir \"$dir_name\" 2>/dev/null || true; " +
                        "fi && " +
                        "cd .. && " +
                        "rm '%s' && " +
                        "cd '%s' && " +
                        // 原有的文件处理逻辑
                        "find model -type f \\( -name '*.rknn' -o -name '*.engine' -o -name '*.bmodel' \\) -delete 2>/dev/null || true && " +
                        "for file in *.{rknn,engine,bmodel,txt}; do " +
                        "    if [ -f \"$file\" ]; then " +
                        "        [ \"$file\" = 'classes.txt' ] && mv \"$file\" model/ 2>/dev/null || true; " +
                        "        [[ \"$file\" =~ \\.(rknn|engine|bmodel)$ ]] && mv \"$file\" model/ 2>/dev/null || true; " +
                        "    fi " +
                        "done && " +
                        "ls /home/package/%s/model && " +
                        "%s%s",
                fullFileName, nameWithoutExtension,  // unzip到目标目录
                nameWithoutExtension,                // 进入该目录处理嵌套
                fullFileName,                        // 删除原始ZIP文件
                nameWithoutExtension,                // 再次进入处理后的目录
                nameWithoutExtension, RUN_BASH_COMMAND, authCodeParam
        );
        return new String[]{"/bin/bash", "-c", command};
    }
}
