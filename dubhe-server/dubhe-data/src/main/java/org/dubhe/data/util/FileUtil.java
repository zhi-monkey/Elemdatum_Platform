/**
 * Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */

package org.dubhe.data.util;


import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.SymbolConstant;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.constant.DatatypeEnum;
import org.dubhe.data.constant.ErrorEnum;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.service.DatasetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @description 文件工具
 * @date 2020-04-10
 */
@Component
public class FileUtil {

    @Value("${data.files.rootPath:dataset/}")
    private String datasetRootPath;
    @Resource
    @Lazy
    private DatasetService datasetService;

    @Value("${storage.file-store-root-path}")
    private String nfs;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 允许上传的文件格式
     */
    private static final String ALLOW_FILE_TYPE = ".json";

    /**
     * 获取数据集根路径
     *
     * @param datasetId 数据集ID
     * @return String 数据集根路径
     */
    public String getDatasetAbsPath(Long datasetId) {
        if (datasetId == null) {
            return datasetRootPath;
        }
        Dataset dataset = datasetService.getOneById(datasetId);
        if (dataset != null && StringUtils.isNotBlank(dataset.getUri())) {
            return dataset.getUri();
        }
        return datasetRootPath + datasetId;
    }

    /**
     * Returns the storage root for a newly created standard dataset.
     */
    public String getDatasetStoragePath(Long datasetId, Integer dataType) {
        return datasetRootPath + getDatasetCode(datasetId, dataType);
    }

    /**
     * Returns a human-readable identifier without changing the numeric database primary key.
     * 注意：此方法返回的编号用于前端显示（如 video_1），与 MinIO 存储路径（dataset/video/1）不同
     */
    public static String getDatasetCode(Long datasetId, Integer dataType) {
        if (datasetId == null) {
            return null;
        }
        String prefix;
        if (DatatypeEnum.VIDEO.getValue().equals(dataType)) {
            prefix = "video_";
        } else if (DatatypeEnum.TEXT.getValue().equals(dataType)) {
            prefix = "text_";
        } else if (DatatypeEnum.TABLE.getValue().equals(dataType)) {
            prefix = "table_";
        } else if (DatatypeEnum.AUDIO.getValue().equals(dataType)) {
            prefix = "audio_";
        } else if (DatatypeEnum.POINT_CLOUD.getValue().equals(dataType)) {
            prefix = "pc_";
        } else if (Integer.valueOf(6).equals(dataType)) {
            prefix = "video_";
        } else if (Integer.valueOf(7).equals(dataType)) {
            prefix = "multi_";
        } else {
            prefix = "image_";
        }
        return prefix + datasetId;
    }

    /**
     * 获取读标注文件路径 changed=1则到annotation读取否则去versionFile下读取
     *
     * @param datasetId   数据集ID
     * @param fileName    文件名称
     * @param versionName 版本名称
     * @param change      是否发生改变（true发生改变，false未改变）
     * @return 读取文件的路径
     */
    public String getReadAnnotationAbsPath(Long datasetId, String fileName, String versionName, boolean change) {
        return StringUtils.isBlank(versionName) ?
                getDatasetAbsPath(datasetId) + "/" + Constant.DATASET_ANNOTATION_PATH + fileName :
                change ?
                        getDatasetAbsPath(datasetId) + Constant.VERSION_PATH_NAME + versionName + "/" + Constant.DATASET_ANNOTATION_PATH + fileName :
                        getDatasetAbsPath(datasetId) + "/" + Constant.DATASET_ANNOTATION_PATH + fileName;

    }

    /**
     * 获取写标注文件路径
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名称
     * @return 写文件的路径
     */
    public String getWriteAnnotationAbsPath(Long datasetId, String fileName) {
        return getDatasetAbsPath(datasetId) + "/" + Constant.DATASET_ANNOTATION_PATH + fileName;
    }


    /**
     * 获取源文件绝对路径或源文件的文件夹绝对路径
     *
     * @param datasetId    数据集ID
     * @param fileName     文件名称
     * @param needFileName 是否需要文件名
     * @return 源文件绝对路径或源文件的文件夹绝对路径
     */
    public String getOriginFileAbsPath(Long datasetId, String fileName, boolean needFileName) {
        return nfs + bucketName + "/" + getDatasetAbsPath(datasetId) + "/" + (needFileName ? Constant.DATASET_ORIGIN_PATH + fileName : Constant.DATASET_ORIGIN_NAME);
    }

    /**
     * 获取oringin和annotation的父目录
     */
    public String getDatasetIdAbsPath(Long datasetId) {
        return nfs + bucketName + "/" + getDatasetAbsPath(datasetId);
    }

    /**
     * 获取NFS根路径
     */
    public String getNfsPath() {
        return nfs;
    }

    /**
     * bucketname下的文件路径
     */
    public String getBucketNameFilePath(Long datasetId, String fileName, boolean needFileName) {
        return bucketName + "/" + getDatasetAbsPath(datasetId) + "/" + (needFileName ? Constant.DATASET_ORIGIN_PATH + fileName : Constant.DATASET_ORIGIN_NAME);
    }

    /**
     * dataset下的文件路径
     */
    public String getDatasetFilePath(Long datasetId, String fileName, boolean needFileName) {
        return getDatasetAbsPath(datasetId) + "/" + (needFileName ? Constant.DATASET_ORIGIN_PATH + fileName : Constant.DATASET_ORIGIN_NAME);
    }

    /**
     * 获取源文件绝对路径或源文件的文件夹绝对路径
     *
     * @param url 相对路径
     * @return 源文件绝对路径或源文件绝对路径
     */
    public String getOriginFileAbsPath(String url) {
        return nfs + url;
    }

    /**
     * 获取数据集指定文件标注地址(支持多版本)
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名称
     * @return String 数据集指定文件标注地址(支持多版本)
     */
    public String getAnnotationAbsPath(Long datasetId, String fileName) {
        Dataset dataset = datasetService.getOneById(datasetId);
        return getAnnotationDirAbsPath(datasetId) +
                (org.springframework.util.StringUtils.isEmpty(dataset.getCurrentVersionName()) ? "" : dataset.getCurrentVersionName() + "/")
                + fileName;
    }

    /**
     * 获取数据集标注文件地址
     *
     * @param datasetId 数据集id
     * @return String   数据集标注文件地址
     */
    public String getAnnotationDirAbsPath(Long datasetId) {
        return getDatasetAbsPath(datasetId) + "/" + Constant.DATASET_ANNOTATION_PATH;
    }


    /**
     * 获取标注文件绝对路径（带nfs）
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名称
     * @return 源文件绝对路径
     */
    public String getNfsReadAnnotationAbsPath(Long datasetId, String fileName, String versionName, boolean change) {
        return nfs + bucketName + "/" + getReadAnnotationAbsPath(datasetId, fileName, versionName, change);
    }

    /**
     * 写标注文件的文件夹绝对路径
     *
     * @param datasetId 数据集ID
     * @return 当前数据集写标注文件的文件夹绝对路劲
     */
    public String getNfsWriteAnnotationAbsPath(Long datasetId) {
        return nfs + bucketName + "/" + getDatasetAbsPath(datasetId) + "/" + Constant.DATASET_ANNOTATION_NAME;
    }


    /**
     * 通过本地文件访问json并读取
     *
     * @param file 读取文件
     * @return 读取的文件内容
     */
    public static String readFile(MultipartFile file) {
        StringBuffer lastStr = new StringBuffer();
        BufferedReader reader = null;
        try {
            Reader br = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
            reader = new BufferedReader(br);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                lastStr.append(tempString);
            }
        } catch (IOException e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Class FileUtil method readFile error , error info is :{}", e);
            throw new BusinessException(ErrorEnum.NET_ERROR);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "Class FileUtil method readFile error , error info is :{}", e);
                }
            }
        }
        return lastStr.toString();
    }

    /**
     * 文件格式/大小/属性校验
     *
     * @param file json文件
     */
    public static void checkoutFile(MultipartFile file) {
        if (Objects.isNull(file)) {
            throw new BusinessException(ErrorEnum.FILE_ABSENT);
        }
        String fileName = file.getOriginalFilename();
        if (Objects.isNull(fileName)) {
            throw new BusinessException(ErrorEnum.LABELGROUP_FILE_NAME_NOT_EXIST);
        }
        String lastFileName = fileName.substring(fileName.lastIndexOf("."));
        if (!ALLOW_FILE_TYPE.equals(lastFileName)) {
            throw new BusinessException(ErrorEnum.LABELGROUP_JSON_FILE_ERROR);
        } else if (file.getSize() > NumberConstant.NUMBER_1024 * NumberConstant.NUMBER_1024 * NumberConstant.NUMBER_5) {
            throw new BusinessException(ErrorEnum.LABELGROUP_JSON_FILE_SIZE_ERROR);
        }

    }

    /**
     * 拼接文件名称和数据集ID
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名称
     * @return 拼接后名称
     */
    public static String spliceFileNameAndDatasetId(Long datasetId, String fileName) {
        return new StringBuffer(fileName).append(SymbolConstant.HYPHEN).append(datasetId.toString()).toString();
    }

    /**
     * 拼接文件名称和数据集版本
     *
     * @param version  数据集版本号
     * @param fileName 文件名称
     * @return 拼接后名称
     */
    public static String spliceFileNameAndVersion(String version, String fileName) {
        return new StringBuffer(fileName).append(SymbolConstant.HYPHEN).append(version).toString();
    }


    /**
     * 截取文件名称和数据集ID
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名称
     * @return 截取后名称
     */
    public static String interceptFileNameAndDatasetId(Long datasetId, String fileName) {
        return StringUtils.substringBeforeLast(fileName, SymbolConstant.HYPHEN + datasetId);
    }

    /**
     * 截取文件名称和数据集版本号
     *
     * @param version  数据集版本
     * @param fileName 文件名称
     * @return 截取后名称
     */
    public static String interceptFileNameAndVersion(String version, String fileName) {
        return StringUtils.substringBeforeLast(fileName, SymbolConstant.HYPHEN + version);
    }




    public boolean createYoloZipDirectly(
            String annoDirPrefix,
            String yamlObjectKey,
            List<String> imageObjectKeys,
            String zipFilePath,
            List<String> labelNames) {

        String nfsBasePath = nfs + bucketName + "/";
        zipFilePath = nfsBasePath + zipFilePath;
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 1) 添加 data.yaml 文件到 zip 根目录
            if (yamlObjectKey != null && !yamlObjectKey.isEmpty()) {
                System.out.println("nfsBasePath + yamlObjectKey = " + nfsBasePath + yamlObjectKey);
                File yamlFile = new File(nfsBasePath + yamlObjectKey);
                if (yamlFile.exists() && yamlFile.isFile()) {
                    try (FileInputStream fis = new FileInputStream(yamlFile)) {
                        putFileAsEntry(zos, fis, "data.yaml");
                    }
                } else {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "data.yaml文件不存在: {}", yamlFile.getAbsolutePath());
                }
            }

            // 2) 添加 annotations 目录下的所有文件
            if (annoDirPrefix != null && !annoDirPrefix.isEmpty()) {
                String annoDir = nfsBasePath + ensureNoTrailingSlash(annoDirPrefix);
                System.out.println("annoDir = " + annoDir);
                File annotationDir = new File(annoDir);
                if (annotationDir.exists() && annotationDir.isDirectory()) {
                    addDirectoryToZip(zos, annotationDir, annoDir, "annotations/");
                } else {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "annotations目录不存在: {}", annotationDir.getAbsolutePath());
                }
            }

            // 2.1) 添加 classes.txt 到 annotations 目录
            if (labelNames != null && !labelNames.isEmpty()) {
                String classesContent = String.join("\n", labelNames);
                // 确保末尾有换行符
                if (!classesContent.endsWith("\n")) {
                    classesContent += "\n";
                }
                try (ByteArrayInputStream bais = new ByteArrayInputStream(classesContent.getBytes(StandardCharsets.UTF_8))) {
                    putFileAsEntry(zos, bais, "annotations/classes.txt");
                }
                LogUtil.info(LogEnum.BIZ_DATASET, "Added classes.txt with {} labels", labelNames.size());
            }

            // 3) 添加指定的图片文件到 images 目录
            if (imageObjectKeys != null && !imageObjectKeys.isEmpty()) {
                Set<String> seenFileNames = new HashSet<>();
                for (String imageKey : imageObjectKeys) {
                    //System.out.println("nfsBasePath + imageKey = " + nfsBasePath + imageKey);
                    File imageFile = new File(nfsBasePath + imageKey);
                    if (imageFile.exists() && imageFile.isFile()) {
                        String fileName = imageFile.getName();

                        // 处理文件名冲突
                        if (seenFileNames.contains(fileName)) {
                            String[] baseExt = splitBaseExt(fileName);
                            String suffix = Integer.toHexString(Objects.hash(imageKey)).substring(0, 6);
                            fileName = baseExt[0] + "_" + suffix + baseExt[1];
                        }
                        seenFileNames.add(fileName);

                        String zipEntry = "images/" + fileName;
                        try (FileInputStream fis = new FileInputStream(imageFile)) {
                            putFileAsEntry(zos, fis, zipEntry);
                        }
                    } else {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "图片文件不存在: {}", imageFile.getAbsolutePath());
                    }
                }
            }

            LogUtil.info(LogEnum.BIZ_DATASET, "YOLO ZIP created successfully: {}", zipFilePath);
            return true;

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to create YOLO ZIP directly: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 递归添加目录到ZIP
     */
    private void addDirectoryToZip(ZipOutputStream zos, File dir, String basePath, String zipPrefix) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                addDirectoryToZip(zos, file, basePath, zipPrefix + file.getName() + "/");
            } else {
                // 计算相对路径
                String relativePath = file.getAbsolutePath().substring(basePath.length());
                if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                    relativePath = relativePath.substring(1);
                }
                relativePath = relativePath.replace("\\", "/"); // 统一使用 / 分隔符

                String zipEntry = zipPrefix + relativePath;
                try (FileInputStream fis = new FileInputStream(file)) {
                    putFileAsEntry(zos, fis, zipEntry);
                }
            }
        }
    }

    /**
     * 将文件流添加到ZIP条目
     */
    private void putFileAsEntry(ZipOutputStream zos, InputStream inputStream, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            zos.write(buffer, 0, length);
        }
        zos.closeEntry();
    }

    /**
     * 确保路径没有尾随斜杠
     */
    private String ensureNoTrailingSlash(String path) {
        if (path != null && (path.endsWith("/") || path.endsWith("\\"))) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 分离文件名和扩展名
     */
    private String[] splitBaseExt(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return new String[]{fileName, ""};
        }
        return new String[]{
                fileName.substring(0, lastDotIndex),
                fileName.substring(lastDotIndex)
        };
    }





}
