package org.dubhe.biz.file.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSONObject;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteError;
import io.minio.messages.Item;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang.StringUtils;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.file.dto.FileDTO;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @description Minio工具类
 * @date 2020-05-09
 */
@Service
public class MinioUtil {

    @Value("${minio.url}")
    private String url;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${storage.file-store-root-path}")
    private String nfs;

    private MinioClient client;


    private static final int NUM_THREADS = 30;
    private final ConcurrentHashMap<String, Boolean> bucketExistCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            client = new MinioClient(url, accessKey, secretKey);
        } catch (InvalidEndpointException e) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "MinIO endpoint invalid. e, {}", e);
        } catch (InvalidPortException e) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "MinIO endpoint port invalid. e, {}", e);
        }
    }

    /**
     * 写文件
     *
     * @param bucket       桶名称
     * @param fullFilePath 文件存储的全路径，包括文件名，非'/'开头. e.g. dataset/12/annotation/test.txt
     * @param content      file content. can not be null
     */
    public void writeString(String bucket, String fullFilePath, String content) throws Exception {
        ensureBucketExistsCached(bucket);
        try (InputStream inputStream = IoUtil.toUtf8Stream(content)) {
            PutObjectOptions options = new PutObjectOptions(inputStream.available(), MagicNumConstant.NEGATIVE_ONE);
            client.putObject(bucket, fullFilePath, inputStream, options);
        }
    }

    /**
     * 写入二进制文件
     *
     * @param bucket       桶名称
     * @param fullFilePath 文件存储的全路径，包括文件名，非'/'开头
     * @param content      文件二进制内容
     */
    public void writeBytes(String bucket, String fullFilePath, byte[] content) throws Exception {
        ensureBucketExistsCached(bucket);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            PutObjectOptions options = new PutObjectOptions(content.length, MagicNumConstant.NEGATIVE_ONE);
            client.putObject(bucket, fullFilePath, inputStream, options);
        }
    }

    private void ensureBucketExistsCached(String bucket) throws Exception {
        Boolean cached = bucketExistCache.get(bucket);
        if (cached != null && cached) {
            return;
        }
        boolean isExist = client.bucketExists(bucket);
        if (!isExist) {
            client.makeBucket(bucket);
        }
        bucketExistCache.put(bucket, true);
    }

    /**
     * 读取文件
     *
     * @param bucketName   桶
     * @param fullFilePath 文件存储的全路径，包括文件名，非'/'开头. e.g. dataset/12/annotation/test.txt
     * @return String
     */
    public String readString(String bucketName, String fullFilePath) throws Exception {
        try (InputStream is = client.getObject(bucketName, fullFilePath)) {
            return IoUtil.read(is, Charset.defaultCharset());
        }
    }

    /**
     * 文件删除
     *
     * @param bucketName   桶
     * @param fullFilePath 文件存储的全路径，包括文件名，非'/'开头. e.g. dataset/12/annotation/test.txt
     */
    public void del(String bucketName, String fullFilePath) throws Exception {
        Iterable<Result<Item>> items = client.listObjects(bucketName, fullFilePath);
        Set<String> files = new HashSet<>();
        for (Result<Item> item : items) {
            files.add(item.get().objectName());
        }
        Iterable<Result<DeleteError>> results = client.removeObjects(bucketName, files);
        for (Result<DeleteError> result : results) {
            result.get();
        }
    }

    /**
     * Deletes exactly one object without listing objects by prefix.
     */
    public void deleteObject(String bucketName, String objectName) throws Exception {
        client.removeObject(bucketName, objectName);
    }
    /**
     * 删除MinIO中的一个"目录"（即删除所有具有特定前缀的对象）。
     * 此方法会递归删除，比原有的 del 方法更适合删除整个目录。
     *
     * @param bucketName 桶名称
     * @param prefix     要删除的对象的前缀 (例如 "dataset/123/")。为确保准确性，建议前缀以'/'结尾。
     */
    public void deleteDirectory(String bucketName, String prefix) {
        try {
            // 1. 递归查找所有具有指定前缀的对象
            Iterable<Result<Item>> objects = client.listObjects(bucketName, prefix, true);

            // 2. 将找到的对象名称收集到列表中
            List<String> objectNames = new ArrayList<>();
            for (Result<Item> itemResult : objects) {
                objectNames.add(itemResult.get().objectName());
            }

            // 3. 如果没有找到对象，则记录日志并直接返回
            if (objectNames.isEmpty()) {
                LogUtil.info(LogEnum.BIZ_DATASET, "No objects found with prefix '[{}]' in bucket '{}'. Nothing to delete.", prefix, bucketName);
                return;
            }

            LogUtil.info(LogEnum.BIZ_DATASET, "Attempting to delete {} objects with prefix '[{}]' in bucket '{}'.", objectNames.size(), prefix, bucketName);

            // 4. 调用批量删除接口
            Iterable<Result<DeleteError>> results = client.removeObjects(bucketName, objectNames);

            // 5. 检查并记录每一个删除错误，而不是遇到第一个错误就中断
            List<String> errorMessages = new ArrayList<>();
            for (Result<DeleteError> result : results) {
                try {
                    DeleteError error = result.get();
                    String objectName = error.objectName();
                    String errorMessage = "Error deleting object " + objectName + ": " + error.message();

                    // 将错误信息添加到列表
                    errorMessages.add(errorMessage);

                    // 6. 如果删除失败，尝试使用文件系统删除
                    String filePath = nfs + bucketName + "/" + objectName;
                    Path path = Paths.get(filePath);

                    // 检查文件是否存在
                    if (Files.exists(path)) {
                        // 尝试删除文件
                        Files.delete(path);
                        LogUtil.info(LogEnum.BIZ_DATASET, "Successfully deleted file at path: {}", filePath);

                        // 如果文件系统删除成功，移除对应的错误消息
                        errorMessages.remove(errorMessage);
                        LogUtil.info(LogEnum.BIZ_DATASET, "Successfully removed error message for object: {}", objectName);
                    } else {
                        LogUtil.error(LogEnum.BIZ_DATASET, "File at path '{}' does not exist.", filePath);
                    }

                } catch (Exception e) {
                    errorMessages.add("An unrecoverable error occurred during deletion check: " + e.getMessage());
                }
            }

            if (!errorMessages.isEmpty()) {
                errorMessages.forEach(msg -> LogUtil.error(LogEnum.BIZ_DATASET, msg));
                // 如果有错误，抛出异常，让调用方知道操作未完全成功
                throw new BusinessException("Errors occurred while deleting objects with prefix: " + prefix);
            } else {
                LogUtil.info(LogEnum.BIZ_DATASET, "Successfully deleted all {} objects with prefix '[{}]' from bucket '{}'.", objectNames.size(), prefix, bucketName);
            }

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to delete directory with prefix '[{}]' from bucket '{}'.", prefix, bucketName, e);
            // 将异常包装后重新抛出，以便上层服务能感知到失败
            throw new BusinessException("Failed to delete directory from MinIO: " + e.getMessage(), e);
        }
    }

    public void delFolder(String bucketName, String folderPrefix) {
        // 1) 规范化“文件夹”前缀（确保以 / 结尾，避免误删同前缀对象）
        String prefix = folderPrefix == null ? "" : folderPrefix.trim();
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        final int BATCH_SIZE = 1000; // 可按需要调整

        long listed = 0;        // 成功从 list 里取到的条目数
        long enqueued = 0;      // 加入删除批次的数量
        long deletedOk = 0;     // 近似成功删除数量（会在最后用 deleteErrors 修正）
        long listErrors = 0;    // 列举阶段的异常次数
        long deleteErrors = 0;  // 删除阶段的错误数量（按失败项计数）

        List<String> batch = new ArrayList<>(BATCH_SIZE);

        try {
            Iterable<Result<Item>> items = client.listObjects(bucketName, prefix, true);

            for (Result<Item> r : items) {
                try {
                    Item it = r.get(); // 这一项异常会抛出
                    listed++;
                    batch.add(it.objectName());
                    enqueued++;
                } catch (Exception e) {
                    listErrors++;
                    System.err.printf("[minio] list error at prefix=%s : %s%n", prefix, e.toString());
                    // 继续后续条目
                }

                if (batch.size() >= BATCH_SIZE) {
                    deleteErrors += deleteBatchStrings(bucketName, batch);
                    deletedOk += batch.size();
                    batch.clear();
                }
            }

            // 最后一批
            if (!batch.isEmpty()) {
                deleteErrors += deleteBatchStrings(bucketName, batch);
                deletedOk += batch.size();
                batch.clear();
            }
        } catch (Exception e) {
            // 极少见：listObjects 本身抛出无法开始迭代
            System.err.printf("[minio] listObjects failed: bucket=%s, prefix=%s, err=%s%n",
                    bucketName, prefix, e.toString());
        }

        deletedOk = Math.max(0, deletedOk - deleteErrors);
        System.out.printf(
                "[minio] delFolder done. bucket=%s, prefix=%s, listed=%d, enqueued=%d, deletedOk~=%d, listErrors=%d, deleteErrors=%d%n",
                bucketName, prefix, listed, enqueued, deletedOk, listErrors, deleteErrors
        );
    }

    private long deleteBatchStrings(String bucketName, List<String> objectNames) {
        long errors = 0L;
        try {
            Iterable<Result<DeleteError>> results = client.removeObjects(bucketName, objectNames);
            for (Result<DeleteError> r : results) {
                try {
                    // 失败项
                    DeleteError de = r.get();
                    errors++;
                    System.err.printf(
                            "[minio] delete error: object=%s, message=%s%n",
                            de.objectName(), de.message()
                    );
                } catch (Exception e) {
                    // 返回流里某条解析失败
                    errors++;
                    System.err.printf("[minio] delete result error: %s%n", e.toString());
                }
            }
        } catch (Exception e) {
            // 整批请求失败（网络/鉴权等），这时整批都可能未删
            errors += objectNames.size();
            System.err.printf("[minio] removeObjects batch failed: %s (batchSize=%d)%n",
                    e.toString(), objectNames.size());
        }
        return errors;
    }

    /**
     * 批量删除文件
     *
     * @param bucketName  桶
     * @param objectNames 对象名称
     */
    public void delFiles(String bucketName, List<String> objectNames) throws Exception {
        Iterable<Result<DeleteError>> results = client.removeObjects(bucketName, objectNames);
        for (Result<DeleteError> result : results) {
            result.get();
        }
    }

    /**
     * 获取对象名称
     *
     * @param bucketName 桶名称
     * @param prefix     前缀
     * @return List<String> 对象名称列表
     * @throws Exception
     */
    public List<String> getObjects(String bucketName, String prefix) throws Exception {
        List<String> fileNames = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(bucketName, prefix);
        for (Result<Item> result : results) {
            Item item = result.get();
            fileNames.add(item.objectName());
        }
        return fileNames;
    }

    /**
     * 获取路径下文件数量
     *
     * @param bucketName 桶名称
     * @param prefix     前缀
     * @return InputStream 文件流
     * @throws Exception
     */
    public int getCount(String bucketName, String prefix) throws Exception {
        int count = NumberConstant.NUMBER_0;
        Iterable<Result<Item>> results = client.listObjects(bucketName, prefix);
        for (Result<Item> result : results) {
            count++;
        }
        return count;
    }

    /**
     * 获取文件流
     *
     * @param bucketName 桶
     * @param objectName 对象名称
     * @return InputStream 文件流
     * @throws Exception
     */
    public InputStream getObjectInputStream(String bucketName, String objectName) throws Exception {
        return client.getObject(bucketName, objectName);
    }

    /**
     * 文件夹复制
     *
     * @param bucketName  桶
     * @param sourceFiles 源文件
     * @param targetDir   目标文件夹
     */

    public void copyDir(String bucketName, List<String> sourceFiles, String targetDir) {
        long startTime = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "开始复制文件，总文件数: {}", sourceFiles.size());

        // 使用固定线程池，限制并发数
        int threadCount = Math.min(30, sourceFiles.size()); // 最多30个线程
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            // 提交所有复制任务
            List<CompletableFuture<Void>> futures = sourceFiles.stream()
                    .map(sourceFile -> CompletableFuture.runAsync(() -> {
                        try {
                            String sourceObjectName = sourceFile;
                            String targetObjectName = targetDir + "/" + StringUtils.substringAfterLast(sourceObjectName, "/");

                            // 直接使用copyObject，避免读取到内存
                            client.copyObject(bucketName, targetObjectName, null, null, bucketName, sourceObjectName, null, null);
                        } catch (Exception e) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file copy exception, {}", e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LogUtil.info(LogEnum.BIZ_DATASET, "文件复制完成 - 优化版本，总耗时: {} ms ({} 秒), 平均每文件: {} ms",
                duration, duration / 1000.0, sourceFiles.size() > 0 ? duration / sourceFiles.size() : 0);
    }

    /**
     * minio拷贝操作
     *
     * @param bucketName 桶名
     * @param sourceFile 需要复制的标注文件名
     * @param targetFile 目标文件路径
     */
    public void copyObject(String bucketName, String sourceFile, String targetFile) {
        CopyConditions copyConditions = new CopyConditions();
        try {
            client.copyObject(bucketName, targetFile, null, null, bucketName, sourceFile, null, copyConditions);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file copy failed, {}", e);
            throw new RuntimeException("文件复制失败");
        }

    }

    /**
     * 获取文件列表
     *
     * @param bucketName 桶
     * @param prefix     前缀
     * @param recursive  是否递归查询
     * @return List<FileDTO> 文件列表
     */
    public List<FileDTO> fileList(String bucketName, String prefix, boolean recursive) {
        List<FileDTO> result = new ArrayList<>();
        Iterable<Result<Item>> items = client.listObjects(bucketName, prefix, false);
        for (Result<Item> resultItem : items) {
            try {
                Item item = resultItem.get();
                FileDTO fileDto = FileDTO.builder().dir(item.isDir()).size(item.size()).path(item.objectName())
                        .name(item.objectName().substring(item.objectName().lastIndexOf("/") + 1, item.objectName().length()))
                        .build();
                if (!item.isDir()) {
                    fileDto.setLastModified(Date.from(item.lastModified().toInstant()));
                }
                result.add(fileDto);
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "get file list error {}", e);
            }
        }
        return result;
    }

    /**
     * 生成一个给HTTP PUT请求用的presigned URL。浏览器/移动端的客户端可以用这个URL进行上传，
     * 即使其所在的存储桶是私有的。这个presigned URL可以设置一个失效时间，默认值是7天
     *
     * @param bucketName 存储桶名称
     * @param objectName 存储桶里的对象名称
     * @param expires    失效时间（以秒为单位），默认是7天，不得大于七天
     * @return String
     */
    public String getEncryptedPutUrl(String bucketName, String objectName, Integer expires) {
        if (StringUtils.isEmpty(objectName)) {
            throw new BusinessException("object name cannot be empty");
        }
        try {
            return client.presignedPutObject(bucketName, objectName, expires);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, e.getMessage());
            throw new BusinessException("MinIO an error occurred, please contact the administrator");
        }
    }

    /**
     * 生成一个给 HTTP GET 请求用的 presigned URL。浏览器/移动端的客户端可以用这个 URL 下载对象，
     * 即使其所在的存储桶是私有的。这个 presigned URL 可以设置一个失效时间，默认值是 7 天。
     *
     * @param bucketName 存储桶名称
     * @param objectName 存储桶里的对象名称
     * @param expires    失效时间（以秒为单位），默认是 7 天，不得大于七天
     * @return String
     */
    public String getEncryptedGetUrl(String bucketName, String objectName, Integer expires) {
        if (StringUtils.isEmpty(objectName)) {
            throw new BusinessException("object name cannot be empty");
        }
        try {
            return client.presignedGetObject(bucketName, objectName, expires);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, e.getMessage());
            throw new BusinessException("MinIO an error occurred, please contact the administrator");
        }
    }

    /**
     * 生成给HTTP PUT请求用的presigned URLs。浏览器/移动端的客户端可以用这个URL进行上传，
     * 即使其所在的存储桶是私有的。这个presigned URL可以设置一个失效时间，默认值是7天
     *
     * @param bucketName  存储桶名称
     * @param objectNames 存储桶里的对象名称
     * @param expires     失效时间（以秒为单位），默认是7天，不得大于七天
     * @return String
     */
    public JSONObject getEncryptedPutUrls(String bucketName, String objectNames, Integer expires) {
        List<String> filePaths = JSONObject.parseObject(objectNames, List.class);
        List<String> urls = new ArrayList<>();
        filePaths.stream().forEach(filePath -> {
            if (StringUtils.isEmpty(filePath)) {
                throw new BusinessException("filePath cannot be empty");
            }
            try {
                urls.add(client.presignedPutObject(bucketName, filePath, expires));
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, e.getMessage());
                throw new BusinessException("MinIO an error occurred, please contact the administrator");
            }
        });
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("preUrls", urls);
        jsonObject.put("bucketName", bucketName);
        return jsonObject;
    }

    //通过文件URL查询文件大小和类型
    public Map<String, Object> getFileDetails(String bucketName, String objectName) {
        try {
            ObjectStat objectStat = client.statObject(bucketName, objectName);

            Map<String, Object> details = new HashMap<>();
            details.put("size", objectStat.length());
            details.put("type", objectStat.contentType());
            details.put("etag", objectStat.etag());

            return details;
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "Error getting file details. e, {}", e);
            return null;
        }
    }


    public Map<String, Long> getFilesSizes(Map<String, String> bucketAndObjectNames) {
        Map<String, Long> sizes = new HashMap<>();
        for (Map.Entry<String, String> entry : bucketAndObjectNames.entrySet()) {
            try {
                ObjectStat objectStat = client.statObject(entry.getValue(), entry.getKey());
                sizes.put(entry.getKey(), objectStat.length());
            } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "Error getting file details. e, {}", e);
            }
        }
        return sizes;
    }

    private static final int BUFFER_SIZE = 2048;

    public List<String> unTar(InputStream in, MinioClient minioClient, String bucketName, String parentDir) {
        List<String> fileNames = new ArrayList<>();
        try {
            TarArchiveInputStream tarIn = new TarArchiveInputStream(in, BUFFER_SIZE);
            TarArchiveEntry entry = null;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                if (entry.isFile()) {
                    // 设置分片大小，例如 5MB
                    long partSize = 5 * 1024 * 1024;
                    // 使用putObject上传一个文件到存储桶中。
                    PutObjectOptions options = new PutObjectOptions(-1, partSize);
                    minioClient.putObject(bucketName, parentDir + entry.getName(), tarIn, options);
                    fileNames.add(parentDir + entry.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileNames;
    }


    public List<String> unZip(InputStream in, MinioClient minioClient, String bucketName, String parentDir) {
        List<String> fileNames = new ArrayList<>();
        try {
            ZipArchiveInputStream is = new ZipArchiveInputStream(new BufferedInputStream(in, BUFFER_SIZE), CharsetUtil.defaultCharset().name(), false, true);
            ZipArchiveEntry entry = null;
            while ((entry = is.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    try {
                        // 设置分片大小，例如 5MB
                        long partSize = 5 * 1024 * 1024;
                        // 使用putObject上传一个文件到存储桶中。
                        PutObjectOptions options = new PutObjectOptions(-1, partSize);
                        minioClient.putObject(bucketName, parentDir + entry.getName(), is, options);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fileNames.add(parentDir + entry.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    public List<String> unZipAndUpload(InputStream in, MinioClient minioClient, String bucketName, String parentDir) {
        List<String> fileNames = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(8); // 并发度可调
        List<Future<?>> futures = new ArrayList<>();
        int maxInMemory = 5 * 1024 * 1024; // 5MB

        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(in, 8192), Charset.defaultCharset().name(), false, true)) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    String objectName = parentDir + entry.getName();
                    int size = (int) entry.getSize();
                    if (size > 0 && size < maxInMemory) {
                        // 小文件，直接读进内存并异步上传
                        byte[] data = cn.hutool.core.io.IoUtil.readBytes(zis, size);
                        futures.add(executor.submit(() -> {
                            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                                PutObjectOptions options = new PutObjectOptions(data.length, 5 * 1024 * 1024);
                                minioClient.putObject(bucketName, objectName, bais, options);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }));
                    } else {
                        // 大文件，直接主线程流式上传（避免OOM）
                        PutObjectOptions options = new PutObjectOptions(-1, 5 * 1024 * 1024);
                        minioClient.putObject(bucketName, objectName, zis, options);
                    }
                    fileNames.add(objectName);
                }
            }
            // 等待所有异步任务完成
            for (Future<?> future : futures) {
                try { future.get(); } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        return fileNames;
    }



    /**
     * 解压方法
     *
     * @param file 解压文件
     * @return
     * @throws Exception
     */
    public List<String> decompress(File file, String bucketName, String parentDir) throws Exception {
        List<String> ret = new ArrayList<>();
        try {
            String fileName = file.getName();
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".zip")) {
                ret = unZipAndUpload(new FileInputStream(file), client, bucketName, parentDir);
            } else if (lowerName.endsWith(".tar")) {
                ret = unTar(new FileInputStream(file), client, bucketName, parentDir);
            }
            //删除压缩包
            del(bucketName, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        return ret;
    }

    /**
     * Reads an archive directly from MinIO and uploads its extracted entries back to MinIO.
     * The object name must not include the bucket name.
     */
    public List<String> decompress(String bucketName, String objectName, String parentDir) throws Exception {
        if (StringUtils.isBlank(objectName)) {
            throw new BusinessException("archive object name cannot be empty");
        }

        String lowerName = objectName.toLowerCase(Locale.ROOT);
        try (InputStream inputStream = client.getObject(bucketName, objectName)) {
            if (lowerName.endsWith(".zip")) {
                return unZipAndUpload(inputStream, client, bucketName, parentDir);
            }
            if (lowerName.endsWith(".tar")) {
                return unTar(inputStream, client, bucketName, parentDir);
            }
            throw new BusinessException("unsupported archive format: " + objectName);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "decompress MinIO object failed, bucket:{}, object:{}", bucketName, objectName, e);
            throw new Exception(e.getMessage(), e);
        }
    }


    public boolean zipAndUploadToMinio(String bucketName, List<String> minioFilePaths, String zipFileName, String targetDir) {
        // 临时文件用于存储 ZIP 压缩包
        File tempZipFile = null;
        try {
            tempZipFile = File.createTempFile("tempZip", ".zip");

            try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 遍历所有路径，支持文件和目录
                for (String path : minioFilePaths) {
                    // 判断是文件还是目录
                    if (path.endsWith("/")) {
                        // 如果是目录，递归遍历所有文件
                        Iterable<Result<Item>> results = client.listObjects(bucketName, path, true);
                        for (Result<Item> result : results) {
                            //System.out.println("path = " + path);
                            Item item = result.get();
                            if (!item.isDir()) {
                                addFileToZip(bucketName, item.objectName(), zos,targetDir);
                            }
                        }
                    } else {
                        // 如果是文件，直接添加
                        addFileToZip(bucketName, path, zos,targetDir);
                    }
                }
            }

            // 上传压缩包到 MinIO
            String minioZipPath = targetDir + "/" + zipFileName;
            try (InputStream zipInputStream = new FileInputStream(tempZipFile)) {
                PutObjectOptions options = new PutObjectOptions(zipInputStream.available(), -1);
                client.putObject(bucketName, minioZipPath, zipInputStream, options);
                LogUtil.info(LogEnum.BIZ_DATASET, "ZIP file successfully uploaded to MinIO: " + minioZipPath);
            }
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to create ZIP and upload to MinIO: {}", e.getMessage());
            return false;
        } finally {
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
        }
        return true;
    }
    /**
     * 将 MinIO 文件添加到 ZIP 文件中，并裁剪掉 versionFilePath 前缀
     *
     * @param bucketName       MinIO 桶名称
     * @param filePath         MinIO 中完整路径
     * @param zos              ZIP 输出流
     * @param versionFilePath  你希望 ZIP 中从哪里开始的相对路径，例如 dataset/398/versionFile/V0001/YOLO
     */
    private void addFileToZip(String bucketName, String filePath, ZipOutputStream zos, String versionFilePath) {
        try (InputStream inputStream = client.getObject(bucketName, filePath)) {
            // 相对路径：去掉 versionFilePath 开头部分
            String relativePath = filePath.substring(versionFilePath.length() + 1);
            zos.putNextEntry(new ZipEntry(relativePath));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            LogUtil.info(LogEnum.BIZ_DATASET, "File added to ZIP: " + relativePath);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to add file to ZIP: {}", e.getMessage());
        }
    }

    /**
     * 把 MinIO 里的文件列表打包成 ZIP 并上传（ZIP 内每个条目只保留源文件名，不含路径）
     *
     * @param bucketName     MinIO 桶名称
     * @param minioFilePaths MinIO 对象名列表（不带前导 /）
     * @param zipFileName    ZIP 文件名
     * @param targetDir      ZIP 上传目标目录
     */
    public boolean zipFilesToMinio(String bucketName, List<String> minioFilePaths, String zipFileName, String targetDir) {
        File tempZipFile = null;
        try {
            tempZipFile = File.createTempFile("export", ".zip");
            try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                for (String path : minioFilePaths) {
                    try (InputStream inputStream = client.getObject(bucketName, path)) {
                        String fileName = path.substring(path.lastIndexOf('/') + 1);
                        zos.putNextEntry(new ZipEntry(fileName));
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "Failed to add file to ZIP: {}", e.getMessage());
                    }
                }
            }
            String minioZipPath = targetDir + "/" + zipFileName;
            try (InputStream zipInputStream = new FileInputStream(tempZipFile)) {
                PutObjectOptions options = new PutObjectOptions(zipInputStream.available(), -1);
                client.putObject(bucketName, minioZipPath, zipInputStream, options);
                LogUtil.info(LogEnum.BIZ_DATASET, "ZIP file successfully uploaded to MinIO: " + minioZipPath);
            }
            return true;
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to create ZIP and upload to MinIO: {}", e.getMessage());
            return false;
        } finally {
            if (tempZipFile != null && tempZipFile.exists()) {
                tempZipFile.delete();
            }
        }
    }




    public boolean zipYoloLayoutToMinio(
            String bucketName,
            String annoDirPrefix,          // 必须以 "/" 结尾，例如 ".../annotations/"
            String yamlObjectKey,          // 单个对象键，例如 ".../YOLO/data.yaml"
            List<String> imageObjectKeys,  // 图片对象键列表（都在同一目录）
            String zipFileName,
            String targetDir               // 例如 ".../YOLO"
    ) {
        File tempZipFile = null;
        String phase = "初始化";
        try {
            tempZipFile = File.createTempFile("yolo-", ".zip");

            try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 1) data.yaml：放在 zip 根目录，文件名固定为 data.yaml
                if (yamlObjectKey != null && !yamlObjectKey.isEmpty()) {
                    phase = "读取data.yaml: " + yamlObjectKey;
                    try (InputStream in = client.getObject(bucketName, yamlObjectKey)) {
                        putStreamAsEntry(zos, in, "data.yaml");
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET,
                                "读取 data.yaml 失败: bucket={}, key={}, exception={}",
                                bucketName, yamlObjectKey, e.getMessage(), e);
                        throw e;
                    }
                }

                // 2) annotations：根据图片列表推断标注文件路径（避免使用有问题的 listObjects）
                // YOLO 格式中，每张图片对应一个同名的 .txt 标注文件
                if (annoDirPrefix != null && !annoDirPrefix.isEmpty() && imageObjectKeys != null && !imageObjectKeys.isEmpty()) {
                    String normAnnoPrefix = ensureTrailingSlash(annoDirPrefix);
                    phase = "根据图片推断标注文件";

                    for (String imageKey : imageObjectKeys) {
                        // 从图片路径提取文件名（不含扩展名）
                        String imageFileName = fileNameOf(imageKey);
                        String[] baseExt = splitBaseExt(imageFileName);
                        String baseName = baseExt[0];

                        // 推断对应的标注文件路径：annotations/xxx.txt
                        String annotationKey = normAnnoPrefix + baseName + ".txt";

                        phase = "读取标注文件: " + annotationKey;
                        try {
                            // 尝试读取标注文件（可能不存在，例如未标注的图片）
                            if (doesObjectExistSilent(bucketName, annotationKey)) {
                                try (InputStream in = client.getObject(bucketName, annotationKey)) {
                                    String zipEntry = "annotations/" + baseName + ".txt";
                                    putStreamAsEntry(zos, in, zipEntry);
                                }
                            } else {
                                LogUtil.warn(LogEnum.BIZ_DATASET, "标注文件不存在，跳过: {}", annotationKey);
                            }
                        } catch (Exception e) {
                            // 单个标注文件失败不影响整体导出
                            LogUtil.warn(LogEnum.BIZ_DATASET, "读取标注文件失败，跳过: {}, error: {}", annotationKey, e.getMessage());
                        }
                    }
                }

                // 3) images：所有图片对象键来自同一目录，直接以文件名写入 images/
                if (imageObjectKeys != null && !imageObjectKeys.isEmpty()) {
                    Set<String> seen = new HashSet<>(); // 可选：防重名
                    for (String key : imageObjectKeys) {
                        phase = "读取图片: " + key;
                        String fileName = fileNameOf(key);
                        if (!seen.add(fileName)) {
                            // 发生重名时，追加短哈希避免覆盖
                            String[] baseExt = splitBaseExt(fileName);
                            String suffix = Integer.toHexString(Objects.hash(key)).substring(0, 6);
                            fileName = baseExt[0] + "_" + suffix + baseExt[1];
                        }
                        String zipEntry = "images/" + fileName;
                        try (InputStream in = client.getObject(bucketName, key)) {
                            putStreamAsEntry(zos, in, zipEntry);
                        }
                    }
                }
            }

            // 4) 上传 ZIP 到 MinIO: targetDir/zipFileName
            String minioZipPath = ensureNoTrailingSlash(targetDir) + "/" + zipFileName;
            phase = "上传ZIP: " + minioZipPath;
            try (InputStream zipIn = new FileInputStream(tempZipFile)) {
                // 长度未知时使用 -1；分片大小设置为 10MB（可按需调整）
                PutObjectOptions opts = new PutObjectOptions(-1, 10 * 1024 * 1024);
                client.putObject(bucketName, minioZipPath, zipIn, opts);
                LogUtil.info(LogEnum.BIZ_DATASET, "ZIP uploaded to MinIO: " + minioZipPath);
            }
            return true;

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET,
                    "MinIO导出失败: phase={}, bucket={}, annoPrefix={}, yamlKey={}, zipName={}, targetDir={}, exception={}",
                    phase, bucketName, annoDirPrefix, yamlObjectKey, zipFileName, targetDir, e.getMessage(), e);
            return false;

        } finally {
            if (tempZipFile != null && tempZipFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempZipFile.delete();
            }
        }
    }

    private void putStreamAsEntry(ZipOutputStream zos, InputStream in, String entryPath) throws IOException {
        String normalized = entryPath.replace("\\", "/");

        // 先确保目录条目存在（兼容部分解压器）
        int slash = normalized.lastIndexOf('/');
        if (slash > 0) {
            String dir = normalized.substring(0, slash + 1);
            ZipEntry dirEntry = new ZipEntry(dir);
            dirEntry.setTime(System.currentTimeMillis());
            try {
                zos.putNextEntry(dirEntry);
                zos.closeEntry();
            } catch (java.util.zip.ZipException ignore) {
                // 目录已存在，忽略
            }
        }

        ZipEntry fileEntry = new ZipEntry(normalized);
        fileEntry.setTime(System.currentTimeMillis());
        zos.putNextEntry(fileEntry);

        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            zos.write(buf, 0, len);
        }
        zos.closeEntry();
    }

    private String ensureTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.endsWith("/") ? s : (s + "/");
    }

    private String ensureNoTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** 从对象键取文件名 */
    private String fileNameOf(String key) {
        if (key == null || key.isEmpty()) return "";
        int idx = key.lastIndexOf('/');
        return (idx >= 0) ? key.substring(idx + 1) : key;
    }

    /** 拆分文件名为 [basename, ext]，ext 包含 '.' 或为空字符串 */
    private String[] splitBaseExt(String fileName) {
        if (fileName == null) return new String[]{"", ""};
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            return new String[]{fileName.substring(0, dot), fileName.substring(dot)};
        }
        return new String[]{fileName, ""};
    }


    /**
     * 返回文件路径列表
     *
     * @param bucketName 桶名称
     * @param folderPath 源文件路径
     * @return 返回文件路径列表
     * @throws Exception
     */
    public List<String> listObjectsInFolder(String bucketName, String folderPath) throws Exception {
        List<String> objectNames = new ArrayList<>();

        String prefix = folderPath.endsWith("/") ? folderPath : folderPath + "/";
        Iterable<Result<Item>> results = client.listObjects(bucketName, prefix, true);
        ;
        for (io.minio.Result<io.minio.messages.Item> result : results) {
            objectNames.add(result.get().objectName());
        }
        return objectNames;
    }



    /**
     * 从指定的文件夹中读取所有图片文件,并按照指定的比例将它们切分并存储到不同的文件夹中
     *
     * @param bucketName            桶名称
     * @param sourceImgFolderPath   源图片文件夹路径
     * @param sourceLabelFolderPath 源标签文件夹路径
     * @param trainFolderPathImg    训练图片文件夹路径
     * @param testFolderPathImg     测试图片文件夹路径
     * @param valFolderPathImg       验证图片文件夹路径
     * @param trainFolderPathLabel  训练标签文件夹路径
     * @param testFolderPathLabel   测试标签文件夹路径
     * @param valFolderPathLabel     验证标签文件夹路径
     * @param trainRatio            训练集比例
     * @param testRatio             测试集比例
     * @param valRatio              验证集比例
     * @throws Exception
     */
    public void splitImageFiles(String bucketName, String sourceImgFolderPath, String sourceLabelFolderPath,
                                String trainFolderPathImg, String testFolderPathImg, String valFolderPathImg,
                                String trainFolderPathLabel, String testFolderPathLabel, String valFolderPathLabel,
                                double trainRatio, double testRatio, double valRatio) throws Exception {
        long startTime = System.nanoTime();
        LogUtil.info(LogEnum.BIZ_DATASET, "开始切分数据集，参数如下：");
        LogUtil.info(LogEnum.BIZ_DATASET, "bucketName: {}", bucketName);
        LogUtil.info(LogEnum.BIZ_DATASET, "sourceImgFolderPath: {}", sourceImgFolderPath);
        LogUtil.info(LogEnum.BIZ_DATASET, "sourceLabelFolderPath: {}", sourceLabelFolderPath);
        LogUtil.info(LogEnum.BIZ_DATASET, "trainFolderPathImg: {}", trainFolderPathImg);
        LogUtil.info(LogEnum.BIZ_DATASET, "testFolderPathImg: {}", testFolderPathImg);
        LogUtil.info(LogEnum.BIZ_DATASET, "valFolderPathImg: {}", valFolderPathImg);
        LogUtil.info(LogEnum.BIZ_DATASET, "trainFolderPathLabel: {}", trainFolderPathLabel);
        LogUtil.info(LogEnum.BIZ_DATASET, "testFolderPathLabel: {}", testFolderPathLabel);
        LogUtil.info(LogEnum.BIZ_DATASET, "valFolderPathLabel: {}", valFolderPathLabel);
        LogUtil.info(LogEnum.BIZ_DATASET, "trainRatio: {}, testRatio: {}, valRatio: {}", trainRatio, testRatio, valRatio);


        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        try {
            // 获取图片文件和标签文件
            List<String> imageFiles = listObjectsInFolder(bucketName, sourceImgFolderPath);
            List<String> annotationsFiles = listObjectsInFolder(bucketName, sourceLabelFolderPath);

            if (imageFiles.isEmpty()) {
                throw new IllegalArgumentException("Source image folder is empty or does not exist.");
            }

            Map<String, String> annotationMap = new HashMap<>();
            for (String annotationFile : annotationsFiles) {
                annotationMap.put(getBaseName(annotationFile), annotationFile);
            }

            // 随机排列图片文件
            Collections.shuffle(imageFiles);

            // 根据图片文件匹配标签文件，并按图片顺序排列标签文件
            List<String> sortedImageFiles = new ArrayList<>();
            List<String> sortedAnnotationFiles = new ArrayList<>();
            for (String imageFile : imageFiles) {
                String imageBaseName = getBaseName(imageFile);
                String matchingAnnotation = annotationMap.get(imageBaseName);

                // 如果找到了匹配的标签文件
                if (matchingAnnotation != null) {
                    sortedImageFiles.add(imageFile);
                    sortedAnnotationFiles.add(matchingAnnotation);
                } else {
                    // 可以选择打印一个警告，告知哪些图片没有找到对应的标签
                    LogUtil.info(LogEnum.MINIO,"Warning: No matching annotation found for image: {}", imageFile);
                }
            }

            // 计算数据集划分数量
            int totalFiles = sortedImageFiles.size();
            int trainCount = (int) (Math.round(totalFiles * trainRatio));
            int testCount = (int) (Math.round(totalFiles * testRatio));
            int valCount = totalFiles - trainCount - testCount;

            // 划分数据集
            List<String> trainImages = sortedImageFiles.subList(0, trainCount);
            List<String> trainLabels = sortedAnnotationFiles.subList(0, trainCount);

            List<String> testImages = sortedImageFiles.subList(trainCount, trainCount + testCount);
            List<String> testLabels = sortedAnnotationFiles.subList(trainCount, trainCount + testCount);

            List<String> valImages = sortedImageFiles.subList(trainCount + testCount, totalFiles);
            List<String> valLabels = sortedAnnotationFiles.subList(trainCount + testCount, totalFiles);

            if (trainImages.isEmpty()) { trainImages = imageFiles; trainLabels = sortedAnnotationFiles; }
            if (testImages.isEmpty()) { testImages = imageFiles; testLabels = sortedAnnotationFiles; }
            if (valImages.isEmpty()) { valImages = imageFiles; valLabels = sortedAnnotationFiles; }

            // 复制图片和标签文件到对应的文件夹
            LogUtil.info(LogEnum.MINIO,"Submitting {} train images and {} labels to be copied.", trainImages.size(), trainLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, trainImages, trainFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, trainLabels, trainFolderPathLabel, executor));

            LogUtil.info(LogEnum.MINIO,"Submitting {} test images and {} labels to be copied.", testImages.size(), testLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, testImages, testFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, testLabels, testFolderPathLabel, executor));

            LogUtil.info(LogEnum.MINIO,"Submitting {} validation images and {} labels to be copied.", valImages.size(), valLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, valImages, valFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, valLabels, valFolderPathLabel, executor));

            LogUtil.info(LogEnum.MINIO,"Waiting for {} total file copy operations to complete...", futures.size());
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            if (!executor.isShutdown()) {
                LogUtil.info(LogEnum.MINIO,"正在关闭 splitImageFiles 的线程池...");
                executor.shutdown();
            }

            long endTime = System.nanoTime();
            long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            LogUtil.info(LogEnum.MINIO,"Method splitImageFiles executed in {} ms", durationInMillis);
        }
    }

    /**
     * 获取文件的基本名称（去除路径和扩展名）
     */
    private String getBaseName(String filePath) {
        String fileName = new File(filePath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    /**
     * 高效地将指定的文件列表复制到目标文件夹，使用MinIO服务器端复制功能。
     *
     * @param bucketName       存储桶名称
     * @param filePaths        要复制的文件的完整路径列表 (例如: ["source/images/cat.jpg", "source/images/dog.jpg"])
     * @param targetFolderPath 目标文件夹路径 (例如: "train/images")，不需要以 "/" 结尾
     * @param executor         The ExecutorService to submit tasks to.
     * @throws Exception 如果复制过程中发生错误
     */
    private List<Future<?>> copyFilesToFolder(String bucketName, List<String> filePaths, String targetFolderPath, ExecutorService executor) throws Exception {
        // 如果文件列表为空，直接返回，不做任何操作。
        if (filePaths == null || filePaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<Future<?>> futures = new ArrayList<>();

        for (String sourceObjectPath : filePaths) {
            Future<?> future = executor.submit(() -> {
                try {
                    String baseFileName = sourceObjectPath.substring(sourceObjectPath.lastIndexOf('/') + 1);
                    String targetObjectPath = targetFolderPath + "/" + baseFileName;

                    client.copyObject(bucketName,targetObjectPath,null,null,bucketName,sourceObjectPath,null,null);
                } catch (Exception e) {
                    LogUtil.info(LogEnum.MINIO,"Failed to copy object '{}' to '{}'", sourceObjectPath, targetFolderPath, e);
                    throw new RuntimeException("Failed to copy object: " + sourceObjectPath, e);
                }
            });
            futures.add(future);
        }
        return futures;
    }



    /**
     * 检查MinIO中指定的对象是否存在（静默版本）
     * 发生异常时返回false而不抛出异常
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return true表示对象存在，false表示对象不存在或检查失败
     */
    public boolean doesObjectExistSilent(String bucketName, String objectName) {
        try {
            client.statObject(bucketName, objectName);
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().errorCode().code())) {
                return false;
            }
            // 其他错误也返回false，但记录日志
            LogUtil.warn(LogEnum.BIZ_DATASET, "Check object existence failed, bucket: {}, object: {}, error: {}",
                    bucketName, objectName, e.getMessage());
            return false;
        } catch (Exception e) {
            // 所有异常都返回false，但记录日志
            LogUtil.warn(LogEnum.BIZ_DATASET, "Check object existence failed, bucket: {}, object: {}, error: {}",
                    bucketName, objectName, e.getMessage());
            return false;
        }
    }



}
