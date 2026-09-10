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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.XmlUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.data.domain.bo.FileAnnotationBO;
import org.dubhe.data.domain.dto.FileCreateDTO;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.service.LabelService;
import org.dubhe.data.service.impl.FileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description oneflow文本格式转换
 * @date 2020-07-16
 */
@Component
public class ConversionUtil {

    private static final int ARRAY_LENGTH = MagicNumConstant.FOUR;

    private static final String TXT_FILE_FORMATS = ".txt";

    private static final String JPEG_FILE_FORMATS = "JPEG";

    @Value("${minio.bucketName}")
    private String bucket;

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private LabelService labelService;

    /**
     * 格式转换
     *
     * @param x      横坐标
     * @param y      纵坐标
     * @param w      宽度
     * @param h      高度
     * @param width  图片宽
     * @param height 图片高
     * @return double[]
     */
    private static double[] bboxCocoYolo(double x, double y, double w, double h, int width, int height) {
        double[] newBbox = new double[ARRAY_LENGTH];
        newBbox[0] = (x + 0.5 * w) / width;
        newBbox[1] = (y + 0.5 * h) / height;
        newBbox[2] = w / width;
        newBbox[3] = h / height;
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            BigDecimal bd = BigDecimal.valueOf(newBbox[i]);
            newBbox[i] = bd.setScale(MagicNumConstant.SIX, RoundingMode.HALF_UP).doubleValue();
        }
        return newBbox;
    }

    public static JSONObject buildCOCOCommon() {
        JSONObject cocoObject = new JSONObject();
        cocoObject.put("info", buildCOCOInfo());
        cocoObject.put("licenses", buildCOCOLicenses());
        return cocoObject;
    }

    private static JSONObject buildCOCOInfo() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        JSONObject info = new JSONObject();
        info.put("description", "Exported from AI platform");
        info.put("url", "");
        info.put("version", "");
        info.put("contributor", "");
        info.put("year", calendar.get(Calendar.YEAR));
        info.put("date_created", format.format(calendar.getTime()));
        return info;
    }

    private static JSONArray buildCOCOLicenses() {
        JSONObject license = new JSONObject();
        license.put("id", 0);
        license.put("name", "placeholder license");
        license.put("url", "");
        JSONArray licenseArray = new JSONArray();
        licenseArray.add(license);
        return licenseArray;
    }

    public static String buildYOLOLabelsString(List<Label> labels) {
        List<String> labelNames = Lists.newArrayList();
        for (Label label : labels) {
            labelNames.add(label.getName());
        }
        return Strings.join(labelNames, '\n');
    }

    public static String buildYoloAnnotation(Integer categoryIndex, JSONArray bboxArray, int width, int height) {
        BigDecimal[] bbox = new BigDecimal[ARRAY_LENGTH];
        for (int j = 0; j < ARRAY_LENGTH; j++) {
            bbox[j] = new BigDecimal(bboxArray.get(j).toString());
        }
        double[] newBbox = bboxCocoYolo(bbox[0].doubleValue(), bbox[1].doubleValue(),
                bbox[2].doubleValue(), bbox[3].doubleValue(), width, height);

        return categoryIndex + " " + newBbox[0] + " " + newBbox[1] + " " + newBbox[2] + " " + newBbox[3] + "\n";
    }

    public static JSONObject xmlToJsonObject(String xmlPath) {
        // 读取 XML 文件内容到字符串
        String xmlContent = FileUtil.readUtf8String(xmlPath);
        // 使用Hutool的XmlUtil将XML转换为Map
        Map<String, Object> map = XmlUtil.xmlToMap(xmlContent);
        // 将Map转换为Fastjson的JsonObject
        return JSONObject.parseObject(JSONObject.toJSONString(map));
    }

    public static List<JSONObject> txtToJsonObjectList(String txtPath) {
        java.io.File file = new File(txtPath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Stream<String> lines = Files.lines(Paths.get(txtPath))) {
            return lines.filter(str -> str != null && !str.isEmpty() && !str.trim().isEmpty())
                    .map(line -> {
                        JSONObject re = new JSONObject();
                        List<String> strList = Arrays.asList(line.split("\\s+"));
                        re.put("labelId", Long.valueOf(strList.get(0)));
                        
                        // 解析坐标点，支持目标检测（4个值）和语义分割（任意多个值）
                        List<Double> coordinates = new ArrayList<>();
                        for (int i = 1; i < strList.size(); i++) {
                            coordinates.add(Double.parseDouble(strList.get(i)));
                        }
                        re.put("coordinates", coordinates);
                        
                        // 为了向后兼容，如果是4个值（目标检测），也保留newBbox字段
                        if (coordinates.size() == 4) {
                            re.put("newBbox", coordinates);
                        }
                        return re;
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 随机生成颜色字符串
     */
    public static String generateRandomColor() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("#");
        for (int i = 0; i < 6; i++) {
            int colorPart = random.nextInt(16);
            if (colorPart < 10) {
                sb.append(colorPart);
            } else {
                sb.append((char) ('A' + (colorPart - 10)));
            }
        }
        return sb.toString();
    }

    public static String buildVOCAnnotation(String jsonStr, Map<Long, String> labelMaps, FileAnnotationBO sourceFile) {
        // 构建标注xml
        Document newXml = XmlUtil.createXml();
        // 创建根节点 annotation
        Element rootElement = newXml.createElement("annotation");
        // 创建annotation子节点 filename
        Element folderElement = newXml.createElement("folder");
        folderElement.setTextContent("annotations");
        rootElement.appendChild(folderElement);
        // 创建annotation子节点 filename
        Element filenameElement = newXml.createElement("filename");
        filenameElement.setTextContent(org.dubhe.biz.base.utils.StringUtils.substringAfterLast(sourceFile.getFileUrl(), "/"));
        rootElement.appendChild(filenameElement);
        // 创建annotation子节点size
        Element sizeElement = newXml.createElement("size");
        // 创建size子节点width
        Element widthElement = newXml.createElement("width");
        widthElement.setTextContent(sourceFile.getFileWidth().toString());
        sizeElement.appendChild(widthElement);
        // 创建size子节点height
        Element heightElement = newXml.createElement("height");
        heightElement.setTextContent(sourceFile.getFileHeight().toString());
        sizeElement.appendChild(heightElement);
        // 创建size子节点depth
        Element depthElement = newXml.createElement("depth");
        depthElement.setTextContent("3");
        sizeElement.appendChild(depthElement);
        rootElement.appendChild(sizeElement);
        // 每个array中保存的是每个框的标注信息
        JSONArray jsonArray = JSON.parseArray(jsonStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            // 创建annotation子节点object
            Element objectElement = newXml.createElement("object");
            // 每个框的标注信息
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            // 语义分割发布时跳过
            String categoryIdStr = jsonObject.getString("category_id");
            if (NumberUtil.isNumber(categoryIdStr)) {
                Long labelId = Long.parseLong(categoryIdStr);
                if (!labelMaps.containsKey(labelId)) {
                    continue;
                }
            }
            // 标注框标注类型
            String filteredLabel = labelMaps.get(Long.parseLong(categoryIdStr));
            // object节点子节点 name
            Element nameElement = newXml.createElement("name");
            nameElement.setTextContent(filteredLabel);
            objectElement.appendChild(nameElement);
            // object节点子节点 difficult
            Element difficultElement = newXml.createElement("difficult");
            difficultElement.setTextContent("0");
            objectElement.appendChild(difficultElement);
            // 每个标注的bbox
            JSONArray bboxArray = (JSONArray) jsonObject.get("bbox");
            BigDecimal[] bbox = buildVocBbox(bboxArray);
            // object节点子节点 bndbox
            Element bndboxElement = newXml.createElement("bndbox");
            // bndbox子节点 xmin
            Element xminElement = newXml.createElement("xmin");
            xminElement.setTextContent(bbox[0].toString());
            bndboxElement.appendChild(xminElement);
            // bndbox子节点 ymin
            Element yminElement = newXml.createElement("ymin");
            yminElement.setTextContent(bbox[1].toString());
            bndboxElement.appendChild(yminElement);
            // bndbox子节点 xmax
            Element xmaxElement = newXml.createElement("xmax");
            xmaxElement.setTextContent(bbox[2].toString());
            bndboxElement.appendChild(xmaxElement);
            // bndbox子节点 xmin
            Element ymaxElement = newXml.createElement("ymax");
            ymaxElement.setTextContent(bbox[3].toString());
            bndboxElement.appendChild(ymaxElement);
            // object节点插入 bndbox节点
            objectElement.appendChild(bndboxElement);
            // root节点插入object节点
            rootElement.appendChild(objectElement);
        }

        return XmlUtil.toStr(rootElement).replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "");

    }

    /**
     * 创建 YOLO 格式的标注文件
     *
     * @param jsonStr
     * @param labels
     * @param sourceFile
     * @return
     */
    public static String buildYoloAnnotationLabelFile(String jsonStr, List<Label> labels, FileAnnotationBO sourceFile) {

        List<Long> categoryIds = Lists.newArrayList();

        //标签
        List<String> labelNames = Lists.newArrayList();
        for (Label label : labels) {
            labelNames.add(label.getName());
            categoryIds.add(label.getId());
        }

        JSONArray jsonArray = JSON.parseArray(jsonStr);
        StringBuilder annotations = new StringBuilder();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Integer categoryIndex = null;
            String categoryIdStr = jsonObject.getString("category_id");
            if (NumberUtil.isNumber(categoryIdStr)) {
                Long categoryId = Long.parseLong(categoryIdStr);
                if (categoryIds.contains(categoryId)) {
                    categoryIndex = categoryIds.indexOf(categoryId);
                }
            }
            if (categoryIndex == null) {
                continue;
            }
            JSONArray bboxArray = (JSONArray) jsonObject.get("bbox");
            String annotation = ConversionUtil.buildYoloAnnotation(categoryIndex, bboxArray, sourceFile.getFileWidth(), sourceFile.getFileHeight());
            annotations.append(annotation);
        }
        return annotations.toString();
    }

    // 根据bbox 构建voc xmin xmax ymin ymax
    public static BigDecimal[] buildVocBbox(JSONArray bboxArray) {
        BigDecimal[] bbox = new BigDecimal[ARRAY_LENGTH];
        BigDecimal x1 = new BigDecimal(bboxArray.get(0).toString());
        BigDecimal width = new BigDecimal(bboxArray.get(2).toString());
        BigDecimal x2 = x1.add(width);
        BigDecimal y1 = new BigDecimal(bboxArray.get(1).toString());
        BigDecimal height = new BigDecimal(bboxArray.get(3).toString());
        BigDecimal y2 = y1.add(height);

        bbox[0] = x1.compareTo(x2) > 0 ? x2 : x1;
        bbox[1] = y1.compareTo(y2) > 0 ? y2 : y1;
        bbox[2] = x1.compareTo(x2) > 0 ? x1 : x2;
        bbox[3] = y1.compareTo(y2) > 0 ? y1 : y2;
        return bbox;
    }

    /**
     * 将annotation信息转换为txt
     *
     * @param path      图片文件路径
     * @param datasetId 数据集ID
     */
    public void txtConversion(String path, Long datasetId) {
        long startTime = System.currentTimeMillis();

        List<String> imagePaths = new ArrayList<>();
        try {
            long t1 = System.currentTimeMillis();
            imagePaths = minioUtil.getObjects(bucket, path);
            LogUtil.info(LogEnum.BIZ_DATASET, "getObjects耗时:{}ms, 数量:{}",
                    System.currentTimeMillis() - t1, imagePaths.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "getObjects is failed:{}", e);
            return;
        }

        Map<String, Integer> labelMap = new HashMap<>();
        try {
            long t2 = System.currentTimeMillis();
            String labelIdsPath = path.replace("/origin", "/annotation/");
            String labelIdsString = minioUtil.readString(bucket, labelIdsPath + "labelsIds.text");
            Map<Integer, String> idLabelMap = JSONObject.parseObject(labelIdsString, Map.class);
            labelMap = idLabelMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            LogUtil.info(LogEnum.BIZ_DATASET, "读取标签映射耗时:{}ms", System.currentTimeMillis() - t2);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
        }

        // 【优化1】预先收集所有需要处理的文件名，用于批量查询数据库
        List<String> fileNamesToProcess = new ArrayList<>();
        List<String> validImagePaths = new ArrayList<>(); // 【新增】保存有效的图片路径

        for (String imagePath : imagePaths) {
            if (!imagePath.endsWith(TXT_FILE_FORMATS)) {
                String imageName = StringUtils.substringAfterLast(imagePath, "/");
                String annName = StringUtils.substringBeforeLast(imageName, ".");
                fileNamesToProcess.add(annName);
                validImagePaths.add(imagePath); // 【新增】保存对应的图片路径
            }
        }

        // 【优化2】批量查询数据库，避免N+1查询问题
        Map<String, FileCreateDTO> fileInfoMap = new HashMap<>();
        if (!fileNamesToProcess.isEmpty()) {
            long batchDbStart = System.currentTimeMillis();
            try {
                List<FileCreateDTO> fileInfoList = fileService.getBaseMapper().selectWidthAndHeightBatch(fileNamesToProcess, datasetId);
                fileInfoMap = fileInfoList.stream().collect(Collectors.toMap(FileCreateDTO::getName, Function.identity()));
                LogUtil.info(LogEnum.BIZ_DATASET, "批量查询数据库耗时:{}ms, 查询数量:{}",
                        System.currentTimeMillis() - batchDbStart, fileInfoList.size());
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "批量查询数据库失败:{}", e);
                return;
            }
        }

        // 【优化3】使用线程池进行并行处理
        int threadCount = Math.min(8, Runtime.getRuntime().availableProcessors()); // 限制最大线程数为8
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        LogUtil.info(LogEnum.BIZ_DATASET, "使用{}个线程进行并行处理", threadCount);

        // 线程安全的计数器和统计变量
        AtomicLong totalReadTime = new AtomicLong(0);
        AtomicLong totalWriteTime = new AtomicLong(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger currentIndex = new AtomicInteger(0);

        // 预计算路径，避免在线程中重复计算
        final String annPath = StringUtils.substringBeforeLast(path, "/") + "/" + "annotation" + "/";
        final Map<String, Integer> finalLabelMap = labelMap; // final引用供lambda使用
        final Map<String, FileCreateDTO> finalFileInfoMap = fileInfoMap; // final引用供lambda使用

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 【优化4】并行处理每个文件
        for (int i = 0; i < validImagePaths.size(); i++) {
            final int index = i;
            final String imagePath = validImagePaths.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String imageName = StringUtils.substringAfterLast(imagePath, "/");
                    String annName = StringUtils.substringBeforeLast(imageName, ".");

                    // 从预先查询的Map中获取文件信息
                    FileCreateDTO fileCreateDTO = finalFileInfoMap.get(annName);
                    if (fileCreateDTO == null) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "文件{}在数据库中不存在，跳过处理", annName);
                        return;
                    }

                    int width = fileCreateDTO.getWidth();
                    int height = fileCreateDTO.getHeight();

                    JSONArray objects = null;
                    try {
                        long readStart = System.currentTimeMillis();
                        objects = JSONObject.parseArray((minioUtil.readString(bucket, annPath + annName)));
                        totalReadTime.addAndGet(System.currentTimeMillis() - readStart);
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
                        return;
                    }

                    // 保持原有的字符串处理逻辑，不做过度优化
                    StringBuffer content = new StringBuffer();
                    for (Object object : objects) {
                        JSONObject jsonObject = (JSONObject) object;
                        String categoryName = jsonObject.getString("category_id");
                        Integer categoryId = finalLabelMap.get(categoryName);
                        JSONArray jsonArray = (JSONArray) jsonObject.get("bbox");
                        BigDecimal[] bbox = new BigDecimal[ARRAY_LENGTH];
                        for (int j = 0; j < ARRAY_LENGTH; j++) {
                            bbox[j] = new BigDecimal(jsonArray.get(j).toString());
                        }
                        double[] newBbox = bboxCocoYolo(bbox[0].doubleValue(), bbox[1].doubleValue(), bbox[2].doubleValue(), bbox[3].doubleValue(), width, height);
                        if (categoryId == null) {
                            continue;
                        }
                        String caId = String.valueOf(categoryId - 1);
                        String newX = String.valueOf(newBbox[0]);
                        String newY = String.valueOf(newBbox[1]);
                        String newW = String.valueOf(newBbox[2]);
                        String newH = String.valueOf(newBbox[3]);
                        content.append(caId).append(" ").append(newX).append(" ").append(newY).append(" ").append(newW).append(" ")
                                .append(newH).append("\n");
                    }

                    try {
                        long writeStart = System.currentTimeMillis();
                        minioUtil.writeString(bucket, path + "/" + annName + TXT_FILE_FORMATS, content.toString());
                        totalWriteTime.addAndGet(System.currentTimeMillis() - writeStart);
                        processedCount.incrementAndGet();
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "write to file failed:{}", e);
                    }

                    // 每100个输出一次进度（线程安全）
                    int current = currentIndex.incrementAndGet();
                    if (current % 100 == 0) {
                        LogUtil.info(LogEnum.BIZ_DATASET, "已处理:{}/{}", current, validImagePaths.size());
                    }

                } catch (Exception e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "处理文件时发生异常:{}", e);
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "等待线程完成时发生异常:{}", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        int finalProcessedCount = processedCount.get();
        LogUtil.info(LogEnum.BIZ_DATASET, "转换完成 - 总耗时:{}ms, 处理数量:{}, 平均读取:{}ms, 平均写入:{}ms, 使用线程数:{}",
                totalTime, finalProcessedCount,
                finalProcessedCount > 0 ? totalReadTime.get() / finalProcessedCount : 0,
                finalProcessedCount > 0 ? totalWriteTime.get() / finalProcessedCount : 0,
                threadCount);
    }


    //生成yolo标注文件以及classes.txt
    public void txtConversionForExport(String path, Long datasetId,List<String> pics) {
        long startTime = System.currentTimeMillis();

        List<String> imagePaths = new ArrayList<>();
        try {
            long t1 = System.currentTimeMillis();
            imagePaths = pics;
            LogUtil.info(LogEnum.BIZ_DATASET, "getObjects耗时:{}ms, 数量:{}",
                    System.currentTimeMillis() - t1, imagePaths.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "getObjects is failed:{}", e);
            return;
        }


        Map<Integer, String> idLabelMap = new HashMap<>();
        Map<String, Integer> yoloIndexMap = new HashMap<>();
        try {
            long t2 = System.currentTimeMillis();
            String labelIdsPath = path + "/annotation/";

            // 读取 labelsIds.text（id -> name）
            String labelIdsString = minioUtil.readString(bucket, labelIdsPath + "labelsIds.text");
            // 注意：直接 parseObject 到 Map<Integer, String>
            idLabelMap = JSONObject.parseObject(labelIdsString, Map.class);

            // 读取 labels.text（按逗号顺序作为 YOLO 下标）
            String labelsTextRaw = minioUtil.readString(bucket, labelIdsPath + "labels.text");
            // 清洗：去掉换行、空白；按逗号分隔；过滤空项
            List<String> yoloLabels = Arrays.stream(labelsTextRaw.replace("\r", " ").replace("\n", " ")
                            .split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // 建立 labelName -> yoloIndex 的映射（0,1,2,...）
            for (int i = 0; i < yoloLabels.size(); i++) {
                yoloIndexMap.put(yoloLabels.get(i), i);
            }
            try {
                final String yoloDir = path + "/temp/annotations/";
                final String classesKey = yoloDir + "classes.txt";

                String classesContent = String.join("\n", yoloLabels);
                minioUtil.writeString(bucket, classesKey, classesContent);

                LogUtil.info(LogEnum.BIZ_DATASET, "已生成 classes.txt，类别数:{}，对象键:{}", yoloLabels.size(), classesKey);

            } catch (Exception ex) {
                LogUtil.error(LogEnum.BIZ_DATASET, "写入 classes.txt 失败:{}", ex);
            }

            // 同时保留你之前的 labelMap（name -> id），如果后面还想用
            Map<Integer, String> idLabelMapTmp = idLabelMap;
            Map<String, Integer> labelMap = idLabelMapTmp.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

            LogUtil.info(LogEnum.BIZ_DATASET, "读取标签映射与YOLO下标耗时:{}ms, 类别数:{}",
                    System.currentTimeMillis() - t2, yoloLabels.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "读取标签映射失败:{}", e);
        }


        // 【优化1】预先收集所有需要处理的文件名，用于批量查询数据库
        List<String> fileNamesToProcess = new ArrayList<>();
        List<String> validImagePaths = new ArrayList<>(); // 【新增】保存有效的图片路径

        for (String imagePath : imagePaths) {
            if (!imagePath.endsWith(TXT_FILE_FORMATS)) {
                String imageName = StringUtils.substringAfterLast(imagePath, "/");
                String annName = StringUtils.substringBeforeLast(imageName, ".");
                fileNamesToProcess.add(annName);
                validImagePaths.add(imagePath); // 【新增】保存对应的图片路径
            }
        }

        // 【优化2】批量查询数据库，避免N+1查询问题
        Map<String, FileCreateDTO> fileInfoMap = new HashMap<>();
        if (!fileNamesToProcess.isEmpty()) {
            long batchDbStart = System.currentTimeMillis();
            try {
                List<FileCreateDTO> fileInfoList = fileService.getBaseMapper().selectWidthAndHeightBatch(fileNamesToProcess, datasetId);
                fileInfoMap = fileInfoList.stream().collect(Collectors.toMap(FileCreateDTO::getName, Function.identity()));
                LogUtil.info(LogEnum.BIZ_DATASET, "批量查询数据库耗时:{}ms, 查询数量:{}",
                        System.currentTimeMillis() - batchDbStart, fileInfoList.size());
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "批量查询数据库失败:{}", e);
                return;
            }
        }

        // 【优化3】使用线程池进行并行处理
        int threadCount = Math.min(8, Runtime.getRuntime().availableProcessors()); // 限制最大线程数为8
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        LogUtil.info(LogEnum.BIZ_DATASET, "使用{}个线程进行并行处理", threadCount);

        // 线程安全的计数器和统计变量
        AtomicLong totalReadTime = new AtomicLong(0);
        AtomicLong totalWriteTime = new AtomicLong(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger currentIndex = new AtomicInteger(0);

        // 预计算路径，避免在线程中重复计算
        final String annPath = StringUtils.substringBeforeLast(path, "/") + "/" + "annotation" + "/";
        final Map<String, FileCreateDTO> finalFileInfoMap = fileInfoMap; // final引用供lambda使用
        final Map<Integer, String> finalIdLabelMap = idLabelMap;
        final Map<String, Integer> finalYoloIndexMap = yoloIndexMap;


        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 【优化4】并行处理每个文件
        for (int i = 0; i < validImagePaths.size(); i++) {
            final int index = i;
            final String imagePath = validImagePaths.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String imageName = StringUtils.substringAfterLast(imagePath, "/");
                    String annName = StringUtils.substringBeforeLast(imageName, ".");

                    // 从预先查询的Map中获取文件信息
                    FileCreateDTO fileCreateDTO = finalFileInfoMap.get(annName);
                    if (fileCreateDTO == null) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "文件{}在数据库中不存在，跳过处理", annName);
                        return;
                    }

                    int width = fileCreateDTO.getWidth();
                    int height = fileCreateDTO.getHeight();

                    JSONArray objects = null;
                    try {
                        long readStart = System.currentTimeMillis();
                        objects = JSONObject.parseArray((minioUtil.readString(bucket, annPath + annName)));
                        totalReadTime.addAndGet(System.currentTimeMillis() - readStart);
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
                        return;
                    }

                    // 保持原有的字符串处理逻辑，不做过度优化
                    StringBuffer content = new StringBuffer();
                    for (Object object : objects) {
                        JSONObject jsonObject = (JSONObject) object;
                        JSONArray jsonArray = (JSONArray) jsonObject.get("bbox");
                        BigDecimal[] bbox = new BigDecimal[ARRAY_LENGTH];
                        for (int j = 0; j < ARRAY_LENGTH; j++) {
                            bbox[j] = new BigDecimal(jsonArray.get(j).toString());
                        }
                        double[] newBbox = bboxCocoYolo(bbox[0].doubleValue(), bbox[1].doubleValue(), bbox[2].doubleValue(), bbox[3].doubleValue(), width, height);


                        String rawCategory = jsonObject.getString("category_id");

// 统一得到 labelName
                        String labelName = rawCategory;
                        if (StringUtils.isNumeric(rawCategory)) {
                            // 如果是数字 id，则用 idLabelMap 反查到标签名
                            try {
                                Integer cid = Integer.valueOf(rawCategory);
                                String mappedName = finalIdLabelMap.get(cid);
                                if (mappedName != null) {
                                    labelName = mappedName;
                                }
                            } catch (NumberFormatException ignore) {}
                        }

// 用 labels.text 的顺序得到 YOLO 下标
                        Integer yoloIdx = finalYoloIndexMap.get(labelName);
                        if (yoloIdx == null) {
                            // 找不到就跳过，避免写入错误类别
                            LogUtil.warn(LogEnum.BIZ_DATASET, "标签'{}'未在labels.text中找到，文件:{} 跳过该对象", labelName, annName);
                            continue;
                        }

                        String caId = String.valueOf(yoloIdx);
                        String newX = String.valueOf(newBbox[0]);
                        String newY = String.valueOf(newBbox[1]);
                        String newW = String.valueOf(newBbox[2]);
                        String newH = String.valueOf(newBbox[3]);
                        content.append(caId).append(" ").append(newX).append(" ").append(newY).append(" ").append(newW).append(" ")
                                .append(newH).append("\n");
                    }

                    try {
                        long writeStart = System.currentTimeMillis();
                        minioUtil.writeString(bucket, path + "/temp/annotations/" + annName + TXT_FILE_FORMATS, content.toString());
                        totalWriteTime.addAndGet(System.currentTimeMillis() - writeStart);
                        processedCount.incrementAndGet();
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "write to file failed:{}", e);
                    }

                    // 每100个输出一次进度（线程安全）
                    int current = currentIndex.incrementAndGet();
                    if (current % 100 == 0) {
                        LogUtil.info(LogEnum.BIZ_DATASET, "已处理:{}/{}", current, validImagePaths.size());
                    }

                } catch (Exception e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "处理文件时发生异常:{}", e);
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "等待线程完成时发生异常:{}", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        int finalProcessedCount = processedCount.get();
        LogUtil.info(LogEnum.BIZ_DATASET, "转换完成 - 总耗时:{}ms, 处理数量:{}, 平均读取:{}ms, 平均写入:{}ms, 使用线程数:{}",
                totalTime, finalProcessedCount,
                finalProcessedCount > 0 ? totalReadTime.get() / finalProcessedCount : 0,
                finalProcessedCount > 0 ? totalWriteTime.get() / finalProcessedCount : 0,
                threadCount);
    }


    public void writeYOLOCommon(String targetDir, List<Label> labels, List<Long> labelIds) {
        // obj.data
        StringBuilder objData = new StringBuilder();
        objData.append("classes=").append(labels.size()).append("\n");
        objData.append("train = data/train.txt").append("\n");
        objData.append("names = data/obj.names").append("\n");
        objData.append("backup = backup").append("\n");

        //标签
        List<String> labelNames = Lists.newArrayList();
        for (Label label : labels) {
            labelNames.add(label.getName());
            labelIds.add(label.getId());
        }

        try {
            minioUtil.writeString(bucket, targetDir + "obj.names", Strings.join(labelNames, '\n'));
            minioUtil.writeString(bucket, targetDir + "obj.data", objData.toString());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write exception, {}", e);
        }
    }

    public void writeYOLOCommonNew(String targetDir, List<Label> labels, List<Long> categoryIds, Map<Long, Long> labelMapping) {
        // 使用 Set 来去重标签
        Set<String> labelNamesSet = new LinkedHashSet<>();
        Set<Long> uniqueCategoryIds = new LinkedHashSet<>(); // LinkedHashSet 保持插入顺序

        // 获取 labelMapping 中的所有目标 ID（values）
        Set<Long> targetLabelIds = new HashSet<>(labelMapping.values());

        // 筛选出 ID 在 targetLabelIds 中的 label
        List<Label> filteredLabels = labels.stream()
                .filter(label -> targetLabelIds.contains(label.getId()))
                .collect(Collectors.toList());

        for (Label label : filteredLabels) {
            if (labelMapping.containsKey(label.getId())) {
                String mappedLabelName = labelService.findLabelByIds(Collections.singletonList(labelMapping.get(label.getId()))).get(0).getName();
                labelNamesSet.add(mappedLabelName); // 加入 Set 来确保唯一性

                Long mappedCategoryId = labelMapping.get(label.getId());
                uniqueCategoryIds.add(mappedCategoryId); // 添加映射后的标签 ID，自动去重
            }
        }

        categoryIds.clear();
        // 将 Set 转换为 List 以保证顺序
        categoryIds.addAll(uniqueCategoryIds);
        List<String> labelNames = new ArrayList<>(labelNamesSet);


        // 创建 namesMap 用于 YAML 配置
        Map<Integer, String> namesMap = new HashMap<>();
        for (int i = 0; i < labelNames.size(); i++) {
            namesMap.put(i, labelNames.get(i));
        }

        // 创建顶层的 Map，用于生成 YAML 文件
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("names", namesMap);

        // 将 Map 转换为 YAML 字符串
        DumperOptions dumperOptions = new DumperOptions();
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);
        String yamlString = yaml.dumpAsMap(dataMap);

        try {
            // 写入过滤后的 obj.names 和 data.yaml 文件
            minioUtil.writeString(bucket, targetDir + "annotations/" + "classes.txt", Strings.join(labelNames, '\n'));
            minioUtil.writeString(bucket, targetDir + "/" + "data.yaml", yamlString);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO文件写入异常, {}", e);
        }
    }




}
