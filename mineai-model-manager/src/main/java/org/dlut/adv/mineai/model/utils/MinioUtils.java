package org.dlut.adv.mineai.model.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * @package: org.dlut.adv.mineai.model.utils
 * @author: chystart
 * @create: 2024-05-26 13:55
 * @description: minio工具类的使用
 **/
@Slf4j
@Service
public class MinioUtils {

    private static MinioClient minioClient;

    @Value("${minio.url}")
    private String url;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Getter
    @Value("${minio.bucketName}")
    private String bucketName;

    private static final int NUM_THREADS = 30;

    /**
     * 初始化客户端。
     */
    @PostConstruct
    public void initMinioClient() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(url)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("初始化MinioClient失败！！！", e);
        }
    }

    /**
     * 获取文件大小 -> byte
     *
     * @param bucketName 桶名称
     * @param filePath   文件路径
     * @return 文件大小，单位字节
     * @throws Exception 如果获取失败抛出异常
     */
    public long getMinioFileSize(String bucketName, String filePath) throws Exception {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build();
            return minioClient.statObject(statObjectArgs).size();
        } catch (Exception e) {
            throw new Exception("获取文件大小失败，文件不存在或无法访问");
        }
    }

    /**
     * 将字符串内容写入到MinIO对象中。
     * 此方法适用于MinIO SDK v8+。
     *
     * @param bucket       桶名称
     * @param fullFilePath 文件存储的全路径，包括文件名 (e.g. "auto-temp/my-job/data.yaml")
     * @param content      要写入的字符串内容, 不能为null.
     * @throws Exception 如果检查存储桶或上传失败
     */
    public void writeString(String bucket, String fullFilePath, String content) throws Exception {
        // 1. 检查存储桶是否存在，如果不存在则创建（与另一个模块的行为保持一致）
        boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket '{}' created successfully.", bucket);
        }
        // 2. 将字符串内容转换为UTF-8编码的字节数组和输入流
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(contentBytes);
        // 3. 使用PutObjectArgs上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(fullFilePath)
                        .stream(inputStream, contentBytes.length, -1)
                        .contentType("text/plain")
                        .build()
        );

        log.info("Successfully wrote string to MinIO object: {}/{}", bucket, fullFilePath);
    }

    /**
     * 对原始 json文件中的 images 对象数组进行拆分
     *
     * @param bucketName              桶名称
     * @param sourceJsonFilePath      原始json文件
     * @param trainTargetJsonFilePath 训练json文件路径
     * @param testTargetJsonFilePath  测试json文件路径
     * @param valTargetJsonFilePath   验证json文件路径
     * @param trainRatio              训练比率
     * @param testRatio               测试比率
     * @param valRadio                验证比率
     * @throws Exception
     */
    public void splitJsonFile(String bucketName, String sourceJsonFilePath, String trainTargetJsonFilePath, String testTargetJsonFilePath, String valTargetJsonFilePath, double trainRatio, double testRatio, double valRadio) throws Exception {
        log.info("参数：bucketName：{}, sourceJsonFilePath: {}, trainTargetJsonFilePath: {}, testTargetJsonFilePath: {}, valTargetJsonFilePath: {}, " +
                "trainRatio: {}, testRatio: {}, valRadio: {}", bucketName, sourceJsonFilePath, trainTargetJsonFilePath, testTargetJsonFilePath, valTargetJsonFilePath, trainRatio, testRatio, valRadio);
        // 读取 JSON 文件
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonData = objectMapper.readValue(minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(sourceJsonFilePath)
                        .build()), Map.class);
        // 切分 images 数组
        List<Map<String, Object>> images = (List<Map<String, Object>>) jsonData.get("images");
        int imagesCount = images.size(); // 4
        int trainSize = (int) (Math.ceil(imagesCount * trainRatio)); // 2
        int testSize = (int) (Math.ceil(imagesCount * testRatio)); // 1
        int valSize = imagesCount - trainSize - testSize; // 1
        // 0 ~ 2
        List<Map<String, Object>> trainImages = IntStream.range(0, trainSize)
                .mapToObj(images::get)
                .collect(Collectors.toList());
        // 2 ~ 3
        List<Map<String, Object>> valImages = IntStream.range(trainSize, trainSize + testSize)
                .mapToObj(images::get)
                .collect(Collectors.toList());
        // 3 ~ 4
        List<Map<String, Object>> testImages = IntStream.range(trainSize + testSize, images.size())
                .mapToObj(images::get)
                .collect(Collectors.toList());
        // 创建新的 JSON 对象
        Map<String, Object> trainJson = new HashMap<>(jsonData);
        trainJson.put("images", trainImages);
        uploadJsonToMinio(minioClient, bucketName, trainTargetJsonFilePath, trainJson);
        Map<String, Object> valJson = new HashMap<>(jsonData);
        valJson.put("images", valImages);
        uploadJsonToMinio(minioClient, bucketName, testTargetJsonFilePath, valJson);
        Map<String, Object> testJson = new HashMap<>(jsonData);
        testJson.put("images", testImages);
        uploadJsonToMinio(minioClient, bucketName, valTargetJsonFilePath, testJson);
    }

    /**
     * @param minioClient
     * @param bucketName
     * @param targetJsonFilePath
     * @param jsonData
     * @throws Exception
     */
    private void uploadJsonToMinio(MinioClient minioClient, String bucketName, String targetJsonFilePath, Map<String, Object> jsonData) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonData);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(targetJsonFilePath)
                        .stream(new ByteArrayInputStream(jsonBytes), jsonBytes.length, -1)
                        .build());
    }

    /**
     * 上传本地文件到MinIO
     *
     * @param localFilePath 本地文件路径
     * @param objectName    上传到MinIO后的对象名
     * @return boolean 上传是否成功
     */
    @SneakyThrows
    public boolean uploadLocalFile(String localFilePath, String objectName) {

        File file = new File(localFilePath);

        // 检查文件是否存在
        if (!file.exists()) {
            log.error("文件不存在: {}", localFilePath);
            return false;
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            // 构建上传参数
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.length(), -1)
                    .build();

            // 上传文件
            minioClient.putObject(putObjectArgs);
            log.info("文件上传成功: {}", objectName);
            return true;
        } catch (IOException e) {
            log.error("文件读取失败: {}", localFilePath, e);
            return false;
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            return false;
        }
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
                    log.warn("Warning: No matching annotation found for image: {}", imageFile);
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
            log.info("Submitting {} train images and {} labels to be copied.", trainImages.size(), trainLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, trainImages, trainFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, trainLabels, trainFolderPathLabel, executor));

            log.info("Submitting {} test images and {} labels to be copied.", testImages.size(), testLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, testImages, testFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, testLabels, testFolderPathLabel, executor));

            log.info("Submitting {} validation images and {} labels to be copied.", valImages.size(), valLabels.size());
            futures.addAll(copyFilesToFolder(bucketName, valImages, valFolderPathImg, executor));
            futures.addAll(copyFilesToFolder(bucketName, valLabels, valFolderPathLabel, executor));

            log.info("Waiting for {} total file copy operations to complete...", futures.size());
            for (Future<?> future : futures) {
                future.get();
            }

            // 合并删除文件,使用一次请求 统一删除
            List<String> allFilesToRemove = new ArrayList<>(imageFiles);
            allFilesToRemove.addAll(annotationsFiles);
            List<DeleteObject> objectsToDelete = allFilesToRemove.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("Error in deleting object {}: {}", error.objectName(), error.message());
            }
        } finally {
            if (!executor.isShutdown()) {
                log.info("正在关闭 splitImageFiles 的线程池...");
                executor.shutdown();
            }

            long endTime = System.nanoTime();
            long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            log.info("Method splitImageFiles executed in {} ms", durationInMillis);
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
     * 返回文件路径列表
     *
     * @param bucketName 桶名称
     * @param folderPath 源文件路径
     * @return 返回文件路径列表
     * @throws Exception
     */
    public List<String> listObjectsInFolder(String bucketName, String folderPath) throws Exception {
        List<String> objectNames = new ArrayList<>();
        Iterable<io.minio.Result<io.minio.messages.Item>> objectsIterator = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(folderPath + "/")
                        .recursive(true)
                        .build()
        );
        for (io.minio.Result<io.minio.messages.Item> result : objectsIterator) {
            objectNames.add(result.get().objectName());
        }
        return objectNames;
    }

    /**
     * @param bucketName       桶名称
     * @param fileNames        文件
     * @param targetFolderPath 目标文件夹路径
     * @throws Exception
     */
    private void moveFilesToFolder(String bucketName, List<String> fileNames, String targetFolderPath) throws Exception {
        //log.info("moveFilesToFolder方法： bucketName ： {}, fileNames: {}, targetFolderPath: {}", bucketName, fileNames.toString(), targetFolderPath);
        for (String fileName : fileNames) {
            String targetObjectName = targetFolderPath + "/" + fileName.substring(fileName.lastIndexOf("/") + 1);
            InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(targetObjectName).stream(stream, -1, 10485760).build());
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(fileName).build());
            stream.close();
        }
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

                    minioClient.copyObject(
                            CopyObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(targetObjectPath)
                                    .source(
                                            CopySource.builder()
                                                    .bucket(bucketName)
                                                    .object(sourceObjectPath)
                                                    .build()
                                    )
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("Failed to copy object '{}' to '{}'", sourceObjectPath, targetFolderPath, e);
                    throw new RuntimeException("Failed to copy object: " + sourceObjectPath, e);
                }
            });
            futures.add(future);
        }
        return futures;
    }

    /**
     * 复制文件
     *
     * @param bucketName       桶名称
     * @param sourceFolderPath 复制的源文件夹的路径
     * @param targetFolderPath 目标夹的路径
     */
    public void copyDir(String bucketName, String sourceFolderPath, String targetFolderPath) throws Exception {
        Iterable<Result<Item>> sourceObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(sourceFolderPath)
                        .recursive(true)
                        .build());
        for (Result<Item> sourceObject : sourceObjects) {
            String sourceObjectName = sourceObject.get().objectName();
            String targetObjectName = targetFolderPath + sourceObjectName.substring(sourceFolderPath.length());
            // copy
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourceObjectName)
                                    .build())
                            .bucket(bucketName)
                            .object(targetObjectName)
                            .build()
            );
        }
    }

    /**
     * 使用多线程并行地将一个文件夹中的所有对象复制到另一个文件夹。
     *
     * @param bucketName       存储桶名称
     * @param sourceFolderPath 源文件夹路径 (e.g., "path/to/source/")
     * @param targetFolderPath 目标文件夹路径 (e.g., "path/to/target/")
     * @param executor         用于执行并行任务的线程池
     * @return 一个包含所有已提交复制任务的 Future 列表
     * @throws Exception 如果列出对象失败
     */
    public List<Future<?>> copyDirParallel(String bucketName, String sourceFolderPath, String targetFolderPath, ExecutorService executor) throws Exception {
        // 确保源和目标路径以'/'结尾，以便正确进行路径替换
        if (!sourceFolderPath.endsWith("/")) {
            sourceFolderPath += "/";
        }
        if (!targetFolderPath.endsWith("/")) {
            targetFolderPath += "/";
        }

        List<Future<?>> futures = new ArrayList<>();

        Iterable<Result<Item>> sourceObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(sourceFolderPath)
                        .recursive(true)
                        .build());

        for (Result<Item> itemResult : sourceObjects) {
            final String sourceObjectName = itemResult.get().objectName();

            // 跳过文件夹本身的对象
            if (sourceObjectName.endsWith("/")) {
                continue;
            }

            final String targetObjectName = targetFolderPath + sourceObjectName.substring(sourceFolderPath.length());

            // 为每个文件复制操作提交一个任务
            Future<?> future = executor.submit(() -> {
                try {
                    minioClient.copyObject(
                            CopyObjectArgs.builder()
                                    .source(CopySource.builder()
                                            .bucket(bucketName)
                                            .object(sourceObjectName)
                                            .build())
                                    .bucket(bucketName)
                                    .object(targetObjectName)
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("Failed to copy object from {} to {}", sourceObjectName, targetObjectName, e);
                    throw new RuntimeException("Copy failed for: " + sourceObjectName, e);
                }
            });
            futures.add(future);
        }

        log.info("Submitted {} copy tasks from '{}' to thread pool.", futures.size(), sourceFolderPath);
        return futures;
    }

    @SneakyThrows
    public void removeObject(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空或空白！！！");
        }
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object("package/" + fileName)
                .build();
        try {
            minioClient.removeObject(removeObjectArgs);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                System.out.println("对象不存在: " + fileName);
            } else {
                System.out.println("删除对象时发生错误: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("发生了其他错误: " + e.getMessage());
        }
    }

    /**
     * 返回指定前缀对象中包含的对象个数
     *
     * @param bucketName 存储桶的名称
     * @param objName    对象前缀
     * @return
     */
    public long containObjectCount(String bucketName, String objName) {
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(objName)
                .build();
        Iterable<Result<Item>> results = null;
        try {
            results = minioClient.listObjects(listObjectsArgs);
        } catch (Exception e) {
            System.err.println("获取参数列表失败：" + e.getMessage());
            return 0;
        }
        try {
            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            result.get();
                            return 1;
                        } catch (Exception e) {
                            System.err.println("Error retrieving item: " + e.getMessage());
                            return 0;
                        }
                    })
                    .reduce(0, Integer::sum);
        } catch (Exception e) {
            System.err.println("其他错误：" + e.getMessage());
            return 0;
        }
    }

    /**
     * 复制文件夹中不是以.txt文件结尾的指定个数对象，文件指定个数到一个文件夹中
     *
     * @param sourceBucket 源文件桶名称
     * @param targetBucket 目标文件桶名称
     * @param sourcePrefix 源对象名称前缀
     * @param targetPrefix 目标对象名称前缀
     */
    public void copyNonTxtFiles(String sourceBucket, String sourcePrefix, String targetBucket, String targetPrefix, int limit) throws Exception {
        System.out.println("limit:" + limit);
        List<Item> nonTxtFiles = new ArrayList<>();
        minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(sourceBucket)
                .prefix(sourcePrefix)
                .build()).forEach(item -> {
            try {
                if (!item.get().objectName().endsWith(".txt")) {
                    nonTxtFiles.add(item.get());
                }
            } catch (Exception e) {
                throw new RuntimeException("列出对象时出错: " + e.getMessage(), e);
            }
        });
        ensureTargetFolderExists(targetBucket, targetPrefix);
        // 前端处理好了，limit 不会大于文件总数的
        for (int i = 0; i < limit; i++) {
            Item item = nonTxtFiles.get(i);
            String sourceObjectName = item.objectName();
            String targetObjectName = targetPrefix + sourceObjectName.substring(sourcePrefix.length());
            try {
                minioClient.copyObject(CopyObjectArgs.builder()
                        .source(CopySource.builder().bucket(sourceBucket).object(sourceObjectName).build())
                        .bucket(targetBucket)
                        .object(targetObjectName)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("复制对象时出错: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 对象后缀处理 + stat检查状态，不存在则创建
     *
     * @param bucket
     * @param prefix
     * @throws Exception
     */
    private void ensureTargetFolderExists(String bucket, String prefix) throws Exception {
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(prefix)
                    .build());
            // 废弃 isExistsObject -> 不存在直接异常
        } catch (Exception e) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(prefix)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                    .contentType("application/octet-stream")
                    .build());
        }
    }


    public Iterable<Result<DeleteError>> removeObjects(String bucket, List<String> allFilesToRemove) throws Exception {
        List<DeleteObject> objectsToDelete = allFilesToRemove.stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());
        return minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(objectsToDelete)
                        .build()
        );
    }

    public void delFolder(String bucketName, String folderPrefix) {
        // 1) 规范化"文件夹"前缀（确保以 / 结尾，避免误删同前缀对象）
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

        List<DeleteObject> batch = new ArrayList<>(BATCH_SIZE);

        try {
            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> r : items) {
                try {
                    Item it = r.get(); // 这一项异常会抛出
                    listed++;
                    batch.add(new DeleteObject(it.objectName()));
                    enqueued++;
                } catch (Exception e) {
                    listErrors++;
                    System.err.printf("[minio] list error at prefix=%s : %s%n", prefix, e.toString());
                    // 继续后续条目
                }

                if (batch.size() >= BATCH_SIZE) {
                    deleteErrors += deleteBatchObjects(bucketName, batch);
                    deletedOk += batch.size();
                    batch.clear();
                }
            }

            // 最后一批
            if (!batch.isEmpty()) {
                deleteErrors += deleteBatchObjects(bucketName, batch);
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

    private long deleteBatchObjects(String bucketName, List<DeleteObject> deleteObjects) {
        long errors = 0L;
        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );

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
            errors += deleteObjects.size();
            System.err.printf("[minio] removeObjects batch failed: %s (batchSize=%d)%n",
                    e.toString(), deleteObjects.size());
        }
        return errors;
    }
}

