package org.dubhe.data.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.VideoDatasetFileMapper;
import org.dubhe.data.domain.entity.VideoDatasetFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 视频媒体解析：从 MinIO 拉取视频，用 javacv 解析元信息（宽高/帧率/帧数/时长/编码）写回 video_dataset_file；
 * 若编码为 H.265(hevc)，则转码成 H.264 存 MinIO，写 converted_url，保证浏览器可播放。
 * 元信息解析与转码分离：转码失败不影响元信息写入（前端退回播原文件）。
 */
@Slf4j
@Service
public class VideoMediaProbeService {

    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private VideoDatasetFileMapper fileMapper;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Async
    public void probe(VideoDatasetFile file) {
        File tempFile = null;
        File convertedFile = null;
        try {
            tempFile = File.createTempFile("video_probe_", "." + safeExt(file.getFileExt()));
            try (InputStream in = minioUtil.getObjectInputStream(bucketName, file.getObjectKey());
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }

            int width;
            int height;
            int frameCount;
            int videoCodecId;
            double fps;
            double duration;
            String codec;
            FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(tempFile);
            try {
                grabber.start();
                width = grabber.getImageWidth();
                height = grabber.getImageHeight();
                fps = grabber.getFrameRate();
                frameCount = grabber.getLengthInVideoFrames();
                long durationMicros = grabber.getLengthInTime();
                duration = durationMicros / 1000000.0;
                codec = grabber.getVideoCodecName();
                videoCodecId = grabber.getVideoCodec();
            } finally {
                grabber.stop();
                grabber.close();
            }
            if (codec == null || codec.trim().isEmpty()) {
                codec = videoCodecId == 173 ? "hevc" : videoCodecId == 27 ? "h264" : String.valueOf(videoCodecId);
            }

            // 先写元信息（即使转码失败，帧数等信息也已落库，media_status=READY）
            fileMapper.updateById(new VideoDatasetFile()
                    .setId(file.getId())
                    .setWidth(width)
                    .setHeight(height)
                    .setFps(fps)
                    .setFrameCount(frameCount)
                    .setDuration(duration)
                    .setCodec(codec)
                    .setMediaStatus("READY"));
            log.info("视频媒体解析完成 fileId={} {}x{} fps={} frames={} codec={}", file.getId(), width, height, fps, frameCount, codec);

            // H.265 转码成 H.264（失败不影响元信息，前端退回播原文件）
            if (videoCodecId == 173) {
                try {
                    convertedFile = File.createTempFile("video_convert_", ".mp4");
                    String convertedObjectKey = file.getObjectKey().replaceAll("\\.[^.]+$", "_h264.mp4");
                    transcode(tempFile, convertedFile, width, height, fps);
                    minioUtil.writeBytes(bucketName, convertedObjectKey, Files.readAllBytes(convertedFile.toPath()));
                    fileMapper.updateById(new VideoDatasetFile().setId(file.getId()).setConvertedUrl(convertedObjectKey));
                    log.info("视频 H.265 转码完成 fileId={} -> {}", file.getId(), convertedObjectKey);
                } catch (Exception e) {
                    log.error("视频 H.265 转码失败 fileId={}", file.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("视频媒体解析失败 fileId={}", file.getId(), e);
            fileMapper.updateById(new VideoDatasetFile().setId(file.getId()).setMediaStatus("FAILED"));
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (convertedFile != null && convertedFile.exists()) {
                convertedFile.delete();
            }
        }
    }

    private void transcode(File src, File dest, int width, int height, double fps) throws Exception {
        FFmpegFrameGrabber g = FFmpegFrameGrabber.createDefault(src);
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(dest, width, height, 0);
        try {
            g.start();
            recorder.setVideoCodecName("libx264");
            recorder.setFormat("mp4");
            recorder.setFrameRate(fps);
            recorder.setVideoBitrate(2000000);
            recorder.start();
            Frame frame;
            while ((frame = g.grab()) != null) {
                if (frame.image != null) {
                    recorder.record(frame);
                }
            }
        } finally {
            try {
                g.stop();
            } catch (Exception ignored) {
            }
            try {
                g.close();
            } catch (Exception ignored) {
            }
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String safeExt(String ext) {
        return ext == null || ext.trim().isEmpty() ? "mp4" : ext.trim();
    }
}
