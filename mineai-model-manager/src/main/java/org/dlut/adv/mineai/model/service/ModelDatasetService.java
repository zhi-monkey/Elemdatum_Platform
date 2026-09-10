package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.Constant;
import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.core.vo.DatasetVO;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ModelDatasetService {
    @Value("${minio.bucketName}")
    private String bucketName;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUtils dubheUtils;

    @Resource
    @Lazy
    private ModelGenerationService modelGenerationService;

    @Resource
    private MinioUtils minioUtils;

    public DatasetVersionVO selectDatasetVersionById(@PathVariable Long datasetVersionId) {
        DatasetVersionVO datasetVersionVO = dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), datasetVersionId).getData();
        datasetVersionVO.setFullName(datasetVersionVO.getName() + ' ' + datasetVersionVO.getVersionName());
        String format = datasetVersionVO.getFormat();
        datasetVersionVO.setVersionUrl(datasetVersionVO.getVersionUrl() + "/" + ("COCO".equals(format) ? "CreateML" : format));
        return datasetVersionVO;
    }

    /**
     * 根据版本id获取datasetVersionVo
     *
     * @param ids
     * @return
     */
    public List<DatasetVersionVO> findDatasetVersionsByIds(List<Long> ids) {
        return dubheDataFeign.findDatasetVersionsByIds(dubheUtils.getAuthorization(), ids).getData();
    }

    public DataResponseBody<List<DatasetVO>> getAllDatasets() {
        return dubheDataFeign.getAllDatasets(dubheUtils.getAuthorization());
    }

    public DataResponseBody<List<DatasetVO>> getDatasetByDataType(Integer datasetType) {
        return dubheDataFeign.getDatasetByDataType(dubheUtils.getAuthorization(), datasetType);
    }

    public List<DatasetVersionVO> datasetVersionList() {
        List<DatasetVersionVO> datasetVersionVOS = dubheDataFeign.datasetVersionList(dubheUtils.getAuthorization()).getData();
        if (datasetVersionVOS != null) {
            datasetVersionVOS.forEach(datasetVersionVO -> {
                        datasetVersionVO.setFullName(datasetVersionVO.getName() + ' ' + datasetVersionVO.getVersionName());
                        String format = datasetVersionVO.getFormat();
                        datasetVersionVO.setVersionUrl(datasetVersionVO.getVersionUrl() + "/" + ("COCO".equals(format) ? "CreateML" : format));
                    }
            );
            datasetVersionVOS = datasetVersionVOS.stream().filter(datasetVersionVO -> datasetVersionVO.getVersionName().startsWith(Constant.DATASET_VERSION_PREFIX)).collect(Collectors.toList());
        }
        return datasetVersionVOS;
    }

    public List<DatasetVersionVO> datasetFullNameList() {
        List<DatasetVersionVO> datasetVersionVOS = dubheDataFeign.datasetNameAndVersionList(dubheUtils.getAuthorization()).getData();
        if (datasetVersionVOS != null) {
            datasetVersionVOS.forEach(datasetVersionVO -> {
                String fullName = datasetVersionVO.getName();
                if (datasetVersionVO.getVersionName() != null && !datasetVersionVO.getVersionName().isEmpty()) {
                    fullName += " " + datasetVersionVO.getVersionName();
                }
                datasetVersionVO.setFullName(fullName);
            });
        }
        return datasetVersionVOS;
    }

    public List<Long> publishSplit(Long versionId, Double splitSize) {
        return dubheDataFeign.publishSplit(dubheUtils.getCurrentAuthorization(), versionId, splitSize).getData();
    }

    public DatasetVersionVO getDatasetVersionByNameAndVersion(String datasetName, String versionName) {
        return dubheDataFeign.getDatasetVersionByNameAndVersion(dubheUtils.getAuthorization(), datasetName, versionName).getData();
    }

    /**
     * 数据集的逻辑切分
     *
     * @param splitRatio        切分比例
     * @param modelGenerationId 生产任务id
     */
    public void datasetCut(String splitRatio, long modelGenerationId, long latestJobId) {
        log.info("splitRatio: {}, modelGenerationId: {}", splitRatio, modelGenerationId);
        long startTime = System.nanoTime();
        final String normalizedSplitRatio = normalizeSplitRatio(splitRatio);
        String[] split = normalizedSplitRatio.split("-");
        // cut radio: [0] train [1] test [2] val
        List<Integer> datasetCutRadio = Arrays.stream(split).map(Integer::parseInt).collect(Collectors.toList());
        Integer total = datasetCutRadio.stream().reduce(0, Integer::sum);
        String cutRootRadio = datasetCutRadio.get(0) + "-" + datasetCutRadio.get(1) + "-" + datasetCutRadio.get(2);
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        Long trainDatasetVersionId = modelGeneration.getTrainDataset();
        DatasetVersionVO datasetVersion = dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), trainDatasetVersionId).getData();
        try {
            List<String> getSplitDir = minioUtils.listObjectsInFolder(bucketName, datasetVersion.getVersionUrl() + "/" + datasetVersion.getFormat())
                    .stream()
                    .filter(s -> s.contains(normalizedSplitRatio))
                    .collect(Collectors.toList());
            if (getSplitDir.isEmpty()) {
                final int threadPoolSize = 30;
                ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

                try {
                    log.info("未找到指定比例的切分目录，开始新的切分流程...");

                    switch (datasetVersion.getFormat()) {
                        case "YOLO": {
                            log.info("处理YOLO格式数据集的切分...");
                            List<Future<?>> copyFutures = new ArrayList<>();

                            copyFutures.addAll(minioUtils.copyDirParallel(bucketName, datasetVersion.getVersionUrl() + "/YOLO/annotations", datasetVersion.getVersionUrl() + "/YOLO/annotations-copy", executor));
                            copyFutures.addAll(minioUtils.copyDirParallel(bucketName, datasetVersion.getVersionUrl() + "/YOLO/images", datasetVersion.getVersionUrl() + "/YOLO/images-copy", executor));

                            log.info("等待 {} 个YOLO文件预复制任务完成...", copyFutures.size());
                            for (Future<?> future : copyFutures) {
                                future.get();
                            }
                            log.info("YOLO文件预复制成功。开始执行切分...");
                            long endTime = System.nanoTime();
                            long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                            log.info("Method datasetCut preparation executed in {} ms", durationInMillis);

                            minioUtils.splitImageFiles(bucketName,
                                    datasetVersion.getVersionUrl() + "/YOLO/images-copy",
                                    datasetVersion.getVersionUrl() + "/YOLO/annotations-copy",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/train/images",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/test/images",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/validation/images",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/train/annotations",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/test/annotations",
                                    datasetVersion.getVersionUrl() + "/YOLO/" + cutRootRadio + "/validation/annotations",
                                    datasetCutRadio.get(0) * 1.0 / total,
                                    datasetCutRadio.get(1) * 1.0 / total,
                                    datasetCutRadio.get(2) * 1.0 / total);
                            break;
                        }

                        case "COCO":
                            log.info("处理COCO格式数据集的切分...");
                            minioUtils.splitJsonFile(bucketName,
                                    datasetVersion.getVersionUrl() + "/CreateML/annotations/coco_info.json",
                                    datasetVersion.getVersionUrl() + "/CreateML/" + cutRootRadio + "/train/annotations/train.json",
                                    datasetVersion.getVersionUrl() + "/CreateML/" + cutRootRadio + "/test/annotations/test.json",
                                    datasetVersion.getVersionUrl() + "/CreateML/" + cutRootRadio + "/validation/annotations/validation.json",
                                    datasetCutRadio.get(0) * 1.0 / total,
                                    datasetCutRadio.get(1) * 1.0 / total,
                                    datasetCutRadio.get(2) * 1.0 / total);
                            break;

                        case "VOC": {
                            log.info("处理VOC格式数据集的切分...");
                            List<Future<?>> copyFutures = new ArrayList<>();

                            copyFutures.addAll(minioUtils.copyDirParallel(bucketName, datasetVersion.getVersionUrl() + "/VOC/annotations", datasetVersion.getVersionUrl() + "/YOLO/annotations-copy", executor));
                            copyFutures.addAll(minioUtils.copyDirParallel(bucketName, datasetVersion.getVersionUrl() + "/VOC/images", datasetVersion.getVersionUrl() + "/YOLO/images-copy", executor));

                            log.info("等待 {} 个VOC文件预复制任务完成...", copyFutures.size());
                            for (Future<?> future : copyFutures) {
                                future.get();
                            }
                            log.info("VOC文件预复制成功。开始执行切分...");
                            long endTime = System.nanoTime();
                            long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                            log.info("Method datasetCut preparation executed in {} ms", durationInMillis);

                            minioUtils.splitImageFiles(bucketName,
                                    datasetVersion.getVersionUrl() + "/VOC/images-copy",
                                    datasetVersion.getVersionUrl() + "/VOC/annotations-copy",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/train/images",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/test/images",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/validation/images",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/train/annotations",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/test/annotations",
                                    datasetVersion.getVersionUrl() + "/VOC/" + cutRootRadio + "/validation/annotations",
                                    datasetCutRadio.get(0) * 1.0 / total,
                                    datasetCutRadio.get(1) * 1.0 / total,
                                    datasetCutRadio.get(2) * 1.0 / total);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("切分数据集过程中发生错误: ", e);
                    // 如果是子线程的错误，会被包装在ExecutionException中，最终被捕获
                    throw new RuntimeException("切分数据集出现错误~", e);
                } finally {
                    if (!executor.isShutdown()) {
                        log.info("正在关闭 datasetCut 的线程池...");
                        executor.shutdown();
                    }
                }
            } else {
                log.info("指定比例 {} 的切分目录已存在，跳过切分。", normalizedSplitRatio);
            }
        } catch (Exception e) {
            throw new RuntimeException("路径查询存在错误~~", e);
        }
        modelGeneration.setTestDataset(trainDatasetVersionId);
        modelGeneration.setValDataset(trainDatasetVersionId);
        modelGeneration.setLatestJobId(latestJobId);
        System.out.println("modelGeneration.getLatestJobId() = " + modelGeneration.getLatestJobId());
        modelGenerationService.saveModelGeneration(modelGeneration);
    }

    private String normalizeSplitRatio(String splitRatio) {
        String defaultSplitRatio = "80-10-10";
        String rawSplitRatio = splitRatio == null || splitRatio.trim().isEmpty() ? "8-1-1" : splitRatio;
        String[] split = rawSplitRatio.split("-");
        if (split.length != 3) {
            return defaultSplitRatio;
        }

        try {
            List<Double> parts = Arrays.stream(split)
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            double total = parts.stream().reduce(0.0, Double::sum);
            if (total <= 0 || parts.stream().anyMatch(item -> item <= 0)) {
                return defaultSplitRatio;
            }
            if (Math.abs(total - 10.0) < 0.000001) {
                parts = parts.stream().map(item -> item * 10).collect(Collectors.toList());
            } else if (Math.abs(total - 100.0) >= 0.000001) {
                parts = parts.stream().map(item -> item / total * 100).collect(Collectors.toList());
            }

            List<Long> roundedParts = parts.stream().map(Math::round).collect(Collectors.toList());
            if (roundedParts.stream().anyMatch(item -> item < 1)) {
                return defaultSplitRatio;
            }
            return String.format(
                    Locale.ROOT,
                    "%d-%d-%d",
                    roundedParts.get(0),
                    roundedParts.get(1),
                    roundedParts.get(2)
            );
        } catch (NumberFormatException e) {
            return defaultSplitRatio;
        }
    }
}


