package org.dlut.adv.mineai.packagem.utils;

import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class MinioUtil {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    private MinioClient minioClient;


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
     * 从MinIO下载文件
     * @param objectPath MinIO对象路径，例如：package/test.zip
     * @param targetDirPath 下载到哪个文件夹，例如：/home/package/
     * @return boolean
     */
    @SneakyThrows
    public boolean downloadObject(String objectPath, String targetDirPath) {
        // 如果目标目录不存在，则创建
        Path targetDir = Paths.get(targetDirPath);
        if (Files.notExists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 从 MinIO 对象路径获取文件名
        Path targetFilePath = targetDir.resolve(Paths.get(objectPath).getFileName().toString());

        // 构建下载参数
        DownloadObjectArgs downloadObjectArgs = DownloadObjectArgs.builder()
                .bucket(bucketName)
                // 传递完整的 MinIO 对象路径
                .object(objectPath)
                // 目标本地文件路径
                .filename(targetFilePath.toString())
                .build();

        // 下载文件
        minioClient.downloadObject(downloadObjectArgs);

        log.info("文件下载成功: {}", targetFilePath.toString());
        return true;
    }

    /**
     * 上传文件到MinIO
     * @param filePath 文件路径
     * @param objectName 上传到MinIO后的对象名
     * @return boolean 上传是否成功
     */
    @SneakyThrows
    public boolean uploadFile(String filePath, String objectName) {

        File file = new File(filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            log.error("文件不存在: {}", filePath);
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
            log.error("文件读取失败: {}", filePath, e);
            return false;
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            return false;
        }
    }


    @SneakyThrows
    public boolean removeObject(String fileName) {

        RemoveObjectArgs.Builder builder = RemoveObjectArgs.builder();
        builder.bucket(bucketName);
        builder.object("package/" + fileName);
        RemoveObjectArgs removeObjectArgs = builder.build();
        this.minioClient.removeObject(removeObjectArgs);

        return true;
    }
}