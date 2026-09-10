package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.capture.CaptureExecutionEvent;
import org.dubhe.data.capture.CaptureExecutionStateMachine;
import org.dubhe.data.capture.CaptureExecutionStatus;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.dao.RtspCaptureExecutionMapper;
import org.dubhe.data.dao.RtspCaptureTaskMapper;
import org.dubhe.data.domain.dto.BatchFileCreateDTO;
import org.dubhe.data.domain.dto.FileCreateDTO;
import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.domain.entity.RtspCaptureExecution;
import org.dubhe.data.domain.entity.RtspCaptureTask;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.HttpCameraServerService;
import org.dubhe.data.service.HttpCameraService;
import org.dubhe.data.service.RtspCaptureExecutionRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class RtspCaptureExecutionRunnerServiceImpl implements RtspCaptureExecutionRunnerService {

    private static final Logger log = LoggerFactory.getLogger(RtspCaptureExecutionRunnerServiceImpl.class);
    private static final long NO_NEW_IMAGE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private RtspCaptureExecutionMapper rtspCaptureExecutionMapper;

    @Autowired
    private RtspCaptureTaskMapper rtspCaptureTaskMapper;

    @Autowired
    private HttpCameraService httpCameraService;

    @Autowired
    private HttpCameraServerService httpCameraServerService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private MinioUtil minioUtil;

    @Resource(name = "rtspCaptureExecutionExecutor")
    private Executor rtspCaptureExecutionExecutor;

    @Override
    public void triggerAsync(Long executionId) {
        triggerAsync(executionId, false);
    }

    @Override
    public void triggerAsync(Long executionId, boolean delRaw) {
        CompletableFuture
                .runAsync(() -> runExecution(executionId, delRaw), rtspCaptureExecutionExecutor)
                .exceptionally(ex -> {
                    log.error("异步执行采集任务失败: executionId={}", executionId, ex);
                    return null;
                });
    }

    private void runExecution(Long executionId, boolean delRaw) {
        RtspCaptureExecution execution = rtspCaptureExecutionMapper.selectById(executionId);
        if (execution == null || execution.getIsDeleted() == null || execution.getIsDeleted() != 0) {
            return;
        }
        RtspCaptureTask task = rtspCaptureTaskMapper.selectById(execution.getTaskId());
        if (task == null || task.getIsDeleted() == null || task.getIsDeleted() != 0) {
            failExecution(executionId, "任务不存在或已删除");
            return;
        }

        if (!transition(executionId, CaptureExecutionEvent.START, "开始采集", true, false)) {
            return;
        }

        try {
            if (!"HTTP".equalsIgnoreCase(task.getSourceType())) {
                failExecution(executionId, "当前仅实现HTTP摄像机采集");
                return;
            }
            executeHttpCapture(task, executionId, delRaw);
        } catch (Exception e) {
            log.error("执行采集失败: executionId={}", executionId, e);
            failExecution(executionId, "采集失败: " + e.getMessage());
        }
    }

    private void executeHttpCapture(RtspCaptureTask task, Long executionId, boolean delRaw) throws Exception {
        if (task.getDatasetGroupId() == null || task.getImageQuantity() == null || task.getImageQuantity() <= 0) {
            throw new IllegalArgumentException("任务参数无效：datasetGroupId/imageQuantity");
        }

        RtspCaptureExecution execution = rtspCaptureExecutionMapper.selectById(executionId);
        if (execution == null || execution.getDatasetId() == null) {
            throw new IllegalStateException("执行记录缺少 datasetId，无法写入数据集");
        }

        if (task.getHttpCameraId() == null) {
            throw new IllegalArgumentException("HTTP任务缺少 httpCameraId");
        }
        HttpCamera camera = httpCameraService.findAvailableById(task.getHttpCameraId());
        if (camera == null) {
            throw new IllegalArgumentException("摄像机不存在或已删除");
        }

        int total = task.getImageQuantity();
        int intervalSeconds = task.getCaptureInterval() == null ? 1 : Math.max(task.getCaptureInterval(), 1);
        int captured = 0;
        long lastNewImageTime = System.currentTimeMillis();

        log.info("开始HTTP采集: executionId={}, taskId={}, datasetId={}, streamId={}, interval={}s, target={}",
                executionId, task.getId(), execution.getDatasetId(), camera.getCameraId(), intervalSeconds, total);
        log.info("HTTP采集删除甲方原始图片选项: executionId={}, delRaw={}", executionId, delRaw);

        while (captured < total) {
            if (isCancelled(executionId)) {
                transition(executionId, CaptureExecutionEvent.CANCEL, "任务已取消", false, true);
                return;
            }

            ensureStreamStatusAvailable(camera);
            //采集图片
            java.util.List<byte[]> snapshots = httpCameraServerService
                    .downloadSnapshots(camera.getHttpCameraServerId(), camera.getCameraId(), delRaw);
            log.info("采集轮次拉图: executionId={}, streamId={}, imageBatchCount={}",
                    executionId, camera.getCameraId(), snapshots == null ? 0 : snapshots.size());
            if (snapshots == null || snapshots.isEmpty()) {
                long noNewImageMillis = System.currentTimeMillis() - lastNewImageTime;
                if (noNewImageMillis >= NO_NEW_IMAGE_TIMEOUT_MILLIS) {
                    if (captured > 0) {
                        transition(executionId, CaptureExecutionEvent.FINISH,
                                "连续30分钟无新增图片，按实际采集数量完成: " + captured + "/" + total, false, true);
                    } else {
                        failExecution(executionId, "连续30分钟无新增图片，且实际采集数量为0");
                    }
                    return;
                }
                updateProgress(executionId, captured, total, "暂无新图片，等待下一轮(" + captured + "/" + total + ")");
                log.info("暂无新图片: executionId={}, streamId={}, captured={}/{}", executionId, camera.getCameraId(), captured, total);
                Thread.sleep(intervalSeconds * 1000L);
                continue;
            }
            ArrayList<FileCreateDTO> batchFiles = new ArrayList<>();
            for (byte[] snapshot : snapshots) {
                if (snapshot == null || snapshot.length == 0) {
                    continue;
                }
                if (captured >= total) {
                    break;
                }
                int index = captured + 1;
                String objectName = buildObjectName(execution.getDatasetId(), task.getId(), camera, index);
                minioUtil.writeBytes(bucketName, objectName, snapshot);
                String fileUrl = bucketName + "/" + objectName;
                log.info("上传图片: executionId={}, file={}, bytes={}", executionId, objectName, snapshot.length);

                FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                        .url(fileUrl)
                        .frameInterval(1)
                        .width(readImageWidth(snapshot))
                        .height(readImageHeight(snapshot))
                        .build();
                batchFiles.add(fileCreateDTO);

                captured = index;
                lastNewImageTime = System.currentTimeMillis();
                updateProgress(executionId, captured, total, "采集中 " + captured + "/" + total);
            }

            if (!batchFiles.isEmpty()) {
                BatchFileCreateDTO batchFileCreateDTO = BatchFileCreateDTO.builder()
                        .files(batchFiles)
                        .ifImport(false)
                        .build();
                log.info("采集批次入库开始: executionId={}, datasetId={}, batchFileCount={}, captured={}/{}",
                        executionId, execution.getDatasetId(), batchFiles.size(), captured, total);
                datasetService.uploadFiles(execution.getDatasetId(), batchFileCreateDTO);
                log.info("采集批次入库完成: executionId={}, datasetId={}, batchFileCount={}, captured={}/{}",
                        executionId, execution.getDatasetId(), batchFiles.size(), captured, total);
            }

            if (captured < total) {
                Thread.sleep(intervalSeconds * 1000L);
            }
        }

        transition(executionId, CaptureExecutionEvent.FINISH, "采集完成", false, true);
    }

    private void ensureStreamStatusAvailable(HttpCamera camera) {
        java.util.List<CameraInfo> currentCameras = httpCameraServerService.listCameras(camera.getHttpCameraServerId());
        CameraInfo current = currentCameras.stream()
                .filter(item -> camera.getCameraId().equals(item.getCameraId()))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new IllegalStateException("流不存在或已下线，无法拉取图片");
        }
        String status = String.valueOf(current.getStatus()).toUpperCase();
        if ("ONLINE".equals(status)) {
            return;
        }
        if ("DELETED".equals(status)) {
            throw new IllegalStateException("流已删除，无法拉取图片");
        }
        if ("INACTIVE".equals(status) || "OFFLINE".equals(status)) {
            throw new IllegalStateException("流未工作（未运行或未采集中），无法拉取图片");
        }
        throw new IllegalStateException("流状态不可用，无法拉取图片");
    }

    private String buildObjectName(Long datasetId, Long taskId, HttpCamera camera, int index) {
        String cameraId = camera.getCameraId() == null ? "camera" : camera.getCameraId().replaceAll("[^a-zA-Z0-9_-]", "_");
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return Constant.DATASET_PATH_NAME + datasetId + "/" + Constant.DATASET_ORIGIN_PATH
                + "http_task" + taskId + "_" + cameraId + "_" + index + "_" + random + ".jpg";
    }

    private void updateProgress(Long executionId, int capturedCount, int total, String message) {
        BigDecimal progress = BigDecimal.valueOf(capturedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        rtspCaptureExecutionMapper.update(null, new LambdaUpdateWrapper<RtspCaptureExecution>()
                .eq(RtspCaptureExecution::getId, executionId)
                .set(RtspCaptureExecution::getCapturedCount, capturedCount)
                .set(RtspCaptureExecution::getProgress, progress)
                .set(RtspCaptureExecution::getMessage, message));
    }

    private void failExecution(Long executionId, String message) {
        transition(executionId, CaptureExecutionEvent.FAIL, message, false, true);
    }

    private boolean transition(Long executionId, CaptureExecutionEvent event, String message, boolean setStartTime, boolean setEndTime) {
        RtspCaptureExecution execution = rtspCaptureExecutionMapper.selectById(executionId);
        if (execution == null) {
            return false;
        }
        CaptureExecutionStatus current = CaptureExecutionStatus.fromCode(execution.getStatus());
        CaptureExecutionStatus next = CaptureExecutionStateMachine.transit(current, event);
        if (next == null) {
            log.warn("非法状态流转: executionId={}, current={}, event={}", executionId, current, event);
            return false;
        }

        RtspCaptureExecution update = new RtspCaptureExecution();
        update.setId(executionId);
        update.setStatus(next.getCode());
        update.setMessage(message);
        if (setStartTime) {
            update.setStartTime(LocalDateTime.now());
        }
        if (setEndTime) {
            update.setEndTime(LocalDateTime.now());
            if (CaptureExecutionStatus.SUCCESS == next) {
                update.setProgress(BigDecimal.valueOf(100));
            }
        }
        return rtspCaptureExecutionMapper.updateById(update) > 0;
    }

    private boolean isCancelled(Long executionId) {
        RtspCaptureExecution execution = rtspCaptureExecutionMapper.selectById(executionId);
        return execution != null && execution.getStatus() != null
                && execution.getStatus().equals(CaptureExecutionStatus.CANCELLED.getCode());
    }

    private Integer readImageWidth(byte[] imageBytes) {
        return readImageDimension(imageBytes, true);
    }

    private Integer readImageHeight(byte[] imageBytes) {
        return readImageDimension(imageBytes, false);
    }

    private Integer readImageDimension(byte[] imageBytes, boolean returnWidth) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        ImageInputStream iis = null;
        ImageReader reader = null;
        try {
            iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
            if (iis == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return null;
            reader = readers.next();
            reader.setInput(iis);
            return returnWidth ? reader.getWidth(0) : reader.getHeight(0);
        } catch (Exception e) {
            log.warn("读取图片尺寸失败: {}", e.getMessage());
            return null;
        } finally {
            if (reader != null) reader.dispose();
            if (iis != null) {
                try { iis.close(); } catch (Exception ignored) {}
            }
        }
    }
}
