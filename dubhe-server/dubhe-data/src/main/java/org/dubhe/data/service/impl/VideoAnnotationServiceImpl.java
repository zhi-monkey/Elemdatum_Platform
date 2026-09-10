package org.dubhe.data.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.data.dao.LabelTemplateVideoMapper;
import org.dubhe.data.dao.VideoAnnotationTaskMapper;
import org.dubhe.data.dao.VideoAnnotationTrackMapper;
import org.dubhe.data.dao.VideoDatasetFileMapper;
import org.dubhe.data.dao.VideoTrackKeyframeMapper;
import org.dubhe.data.domain.dto.VideoAnnotationTrackCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeUpdateDTO;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.domain.entity.LabelTemplateVideo;
import org.dubhe.data.domain.entity.VideoAnnotationTask;
import org.dubhe.data.domain.entity.VideoAnnotationTrack;
import org.dubhe.data.domain.entity.VideoDatasetFile;
import org.dubhe.data.domain.entity.VideoTrackKeyframe;
import org.dubhe.data.domain.vo.VideoAnnotationTrackVO;
import org.dubhe.data.domain.vo.VideoAnnotationVO;
import org.dubhe.data.domain.vo.VideoTrackKeyframeVO;
import org.dubhe.data.service.LabelService;
import org.dubhe.data.service.VideoAnnotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频 BBox 跟踪标注服务实现。
 */
@Service
public class VideoAnnotationServiceImpl implements VideoAnnotationService {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_ANNOTATING = "ANNOTATING";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String TRACK_ACTIVE = "ACTIVE";
    private static final String TRACK_FINISHED = "FINISHED";
    private static final String SOURCE_MANUAL = "manual";

    @Autowired
    private VideoAnnotationTaskMapper taskMapper;
    @Autowired
    private VideoAnnotationTrackMapper trackMapper;
    @Autowired
    private VideoTrackKeyframeMapper keyframeMapper;
    @Autowired
    private VideoDatasetFileMapper fileMapper;
    @Autowired
    private UserContextService userContextService;
    @Autowired
    private LabelService labelService;
    @Autowired
    private LabelTemplateVideoMapper labelTemplateVideoMapper;

    @Override
    @Transactional
    public VideoAnnotationVO openOrCreate(Long datasetId, Long fileId) {
        VideoDatasetFile file = requireFile(datasetId, fileId);
        VideoAnnotationTask task = findTask(fileId);
        if (task == null) {
            task = new VideoAnnotationTask()
                    .setVideoFileId(fileId)
                    .setDatasetId(datasetId)
                    .setAnnotatorId(userContextService.getCurUserId())
                    .setStatus(STATUS_READY)
                    .setAnnotationVersion(1);
            task.setDeleted(false);
            task.setCreateUserId(userContextService.getCurUserId());
            taskMapper.insert(task);
        }
        return load(datasetId, fileId);
    }

    @Override
    public VideoAnnotationVO load(Long datasetId, Long fileId) {
        VideoDatasetFile file = requireFile(datasetId, fileId);
        VideoAnnotationTask task = findTask(fileId);
        VideoAnnotationVO vo = new VideoAnnotationVO();
        vo.setFileId(file.getId());
        vo.setDatasetId(datasetId);
        vo.setFileName(file.getName());
        vo.setWidth(file.getWidth());
        vo.setHeight(file.getHeight());
        vo.setFps(file.getFps());
        vo.setFrameCount(file.getFrameCount());
        vo.setDuration(file.getDuration());
        vo.setUrl(file.getUrl());
        vo.setConvertedUrl(file.getConvertedUrl());
        if (task != null) {
            vo.setTaskId(task.getId());
            vo.setTaskStatus(task.getStatus());
            vo.setLastFrameIndex(task.getLastFrameIndex());
            vo.setTracks(loadTracks(task.getId()));
        } else {
            vo.setTracks(new ArrayList<>());
        }
        return vo;
    }

    @Override
    @Transactional
    public VideoAnnotationTrackVO createTrack(VideoAnnotationTrackCreateDTO dto) {
        VideoAnnotationTask task = requireTask(dto.getTaskId());
        ensureEditable(task);
        VideoAnnotationTrack track = new VideoAnnotationTrack()
                .setTaskId(dto.getTaskId())
                .setTrackNo(dto.getTrackNo())
                .setLabelId(dto.getLabelId())
                .setStartFrame(dto.getFrameIndex())
                .setEndFrame(dto.getFrameIndex())
                .setStatus(TRACK_ACTIVE)
                .setSource(SOURCE_MANUAL);
        track.setDeleted(false);
        track.setCreateUserId(userContextService.getCurUserId());
        trackMapper.insert(track);

        VideoTrackKeyframe keyframe = new VideoTrackKeyframe()
                .setTrackId(track.getId())
                .setFrameIndex(dto.getFrameIndex())
                .setX(dto.getX()).setY(dto.getY())
                .setWidth(dto.getWidth()).setHeight(dto.getHeight())
                .setOutside(false).setOccluded(false)
                .setSource(SOURCE_MANUAL);
        keyframe.setDeleted(false);
        keyframe.setCreateUserId(userContextService.getCurUserId());
        keyframeMapper.insert(keyframe);

        updateLastFrame(task, dto.getFrameIndex());
        markAnnotating(task);
        return toTrackVO(track, java.util.Collections.singletonList(keyframe));
    }

    @Override
    @Transactional
    public VideoTrackKeyframeVO createKeyframe(VideoTrackKeyframeCreateDTO dto) {
        VideoAnnotationTrack track = requireTrack(dto.getTrackId());
        VideoAnnotationTask task = requireTask(track.getTaskId());
        ensureEditable(task);
        VideoTrackKeyframe keyframe = new VideoTrackKeyframe()
                .setTrackId(dto.getTrackId())
                .setFrameIndex(dto.getFrameIndex())
                .setX(dto.getX()).setY(dto.getY())
                .setWidth(dto.getWidth()).setHeight(dto.getHeight())
                .setOutside(dto.getOutside() != null && dto.getOutside())
                .setOccluded(dto.getOccluded() != null && dto.getOccluded())
                .setSource(SOURCE_MANUAL);
        keyframe.setDeleted(false);
        keyframe.setCreateUserId(userContextService.getCurUserId());
        keyframeMapper.insert(keyframe);

        // 维护 Track 的存在区间
        int start = track.getStartFrame() == null ? dto.getFrameIndex() : Math.min(track.getStartFrame(), dto.getFrameIndex());
        int end = track.getEndFrame() == null ? dto.getFrameIndex() : Math.max(track.getEndFrame(), dto.getFrameIndex());
        trackMapper.updateById(new VideoAnnotationTrack()
                .setId(track.getId()).setStartFrame(start).setEndFrame(end));
        updateLastFrame(task, dto.getFrameIndex());
        markAnnotating(task);
        return toKeyframeVO(keyframe);
    }

    @Override
    @Transactional
    public VideoTrackKeyframeVO updateKeyframe(Long keyframeId, VideoTrackKeyframeUpdateDTO dto) {
        VideoTrackKeyframe keyframe = requireKeyframe(keyframeId);
        VideoAnnotationTrack track = requireTrack(keyframe.getTrackId());
        VideoAnnotationTask task = requireTask(track.getTaskId());
        ensureEditable(task);
        keyframe.setX(dto.getX()).setY(dto.getY())
                .setWidth(dto.getWidth()).setHeight(dto.getHeight())
                .setOutside(dto.getOutside() != null && dto.getOutside())
                .setOccluded(dto.getOccluded() != null && dto.getOccluded());
        keyframeMapper.updateById(keyframe);
        markAnnotating(task);
        return toKeyframeVO(keyframe);
    }

    @Override
    @Transactional
    public void deleteKeyframe(Long keyframeId) {
        VideoTrackKeyframe keyframe = requireKeyframe(keyframeId);
        VideoAnnotationTrack track = requireTrack(keyframe.getTrackId());
        VideoAnnotationTask task = requireTask(track.getTaskId());
        ensureEditable(task);
        keyframeMapper.deleteById(keyframeId);
        markAnnotating(task);
    }

    @Override
    @Transactional
    public void finishTrack(Long trackId) {
        VideoAnnotationTrack track = requireTrack(trackId);
        trackMapper.updateById(new VideoAnnotationTrack().setId(trackId).setStatus(TRACK_FINISHED));
    }

    @Override
    @Transactional
    public void deleteTrack(Long trackId) {
        VideoAnnotationTrack track = requireTrack(trackId);
        VideoAnnotationTask task = requireTask(track.getTaskId());
        ensureEditable(task);
        trackMapper.deleteById(trackId);
        keyframeMapper.delete(new QueryWrapper<VideoTrackKeyframe>().eq("track_id", trackId));
    }

    @Override
    @Transactional
    public void submit(Long fileId) {
        VideoAnnotationTask task = requireTaskByFile(fileId);
        validateForSubmit(task);
        syncLabelsToDataset(task);
        taskMapper.updateById(new VideoAnnotationTask().setId(task.getId()).setStatus(STATUS_SUBMITTED));
        syncFileStatus(task.getVideoFileId(), STATUS_SUBMITTED);
    }

    /**
     * 提交时把标注用到的 label_template_video 标签同步到 data_label，并建立数据集标签关联（data_dataset_label），
     * 再把 track.label_id 从 label_template_video.id 更新为 data_label.id。
     */
    private void syncLabelsToDataset(VideoAnnotationTask task) {
        List<VideoAnnotationTrack> tracks = listTracks(task.getId());
        Set<Long> templateIds = tracks.stream()
                .map(VideoAnnotationTrack::getLabelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (templateIds.isEmpty()) {
            return;
        }
        List<LabelTemplateVideo> templates = labelTemplateVideoMapper.selectBatchIds(templateIds);
        Map<Long, String> nameByTemplateId = templates.stream()
                .collect(Collectors.toMap(LabelTemplateVideo::getId,
                        t -> t.getAnnotationName() != null ? t.getAnnotationName() : t.getName(),
                        (a, b) -> a));
        Map<Long, Long> dataLabelIdByTemplateId = new HashMap<>();
        for (Long tid : templateIds) {
            String name = nameByTemplateId.get(tid);
            if (name == null) {
                continue;
            }
            Long dataLabelId = labelService.save(new Label().setName(name), task.getDatasetId());
            if (dataLabelId != null) {
                dataLabelIdByTemplateId.put(tid, dataLabelId);
            }
        }
        for (VideoAnnotationTrack track : tracks) {
            Long dataLabelId = dataLabelIdByTemplateId.get(track.getLabelId());
            if (dataLabelId != null) {
                trackMapper.updateById(new VideoAnnotationTrack().setId(track.getId()).setLabelId(dataLabelId));
            }
        }
    }

    @Override
    @Transactional
    public void review(Long taskId) {
        VideoAnnotationTask task = requireTask(taskId);
        if (!STATUS_SUBMITTED.equals(task.getStatus())) {
            throw new BusinessException("仅已提交的任务可审核");
        }
        taskMapper.updateById(new VideoAnnotationTask().setId(taskId).setStatus(STATUS_APPROVED));
        syncFileStatus(task.getVideoFileId(), STATUS_APPROVED);
    }

    @Override
    @Transactional
    public void reject(Long taskId) {
        VideoAnnotationTask task = requireTask(taskId);
        if (!STATUS_SUBMITTED.equals(task.getStatus())) {
            throw new BusinessException("仅已提交的任务可驳回");
        }
        taskMapper.updateById(new VideoAnnotationTask().setId(taskId).setStatus(STATUS_ANNOTATING));
        syncFileStatus(task.getVideoFileId(), STATUS_ANNOTATING);
    }

    @Override
    public List<VideoAnnotationVO> listTasks(Long datasetId) {
        List<VideoDatasetFile> files = fileMapper.selectList(new QueryWrapper<VideoDatasetFile>()
                .eq("dataset_id", datasetId).eq("deleted", 0).orderByAsc("id"));
        Map<Long, VideoAnnotationTask> taskByFile = findTasksByDataset(datasetId).stream()
                .collect(Collectors.toMap(VideoAnnotationTask::getVideoFileId, t -> t, (a, b) -> a));
        List<VideoAnnotationVO> result = new ArrayList<>();
        for (VideoDatasetFile file : files) {
            VideoAnnotationVO vo = new VideoAnnotationVO();
            vo.setFileId(file.getId());
            vo.setDatasetId(datasetId);
            vo.setFileName(file.getName());
            vo.setWidth(file.getWidth());
            vo.setHeight(file.getHeight());
            vo.setFps(file.getFps());
            vo.setFrameCount(file.getFrameCount());
            vo.setDuration(file.getDuration());
            vo.setUrl(file.getUrl());
            vo.setConvertedUrl(file.getConvertedUrl());
            VideoAnnotationTask task = taskByFile.get(file.getId());
            if (task != null) {
                vo.setTaskId(task.getId());
                vo.setTaskStatus(task.getStatus());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public Object export(Long fileId, String format) {
        VideoAnnotationTask task = requireTaskByFile(fileId);
        if ("mot".equalsIgnoreCase(format)) {
            return exportMot(task);
        }
        List<VideoAnnotationTrack> tracks = listTracks(task.getId());
        Map<Long, List<VideoTrackKeyframe>> keyframesByTrack = keyframesByTrack(task.getId());
        List<Map<String, Object>> trackList = new ArrayList<>();
        for (VideoAnnotationTrack track : tracks) {
            List<VideoTrackKeyframe> keyframes = keyframesByTrack.getOrDefault(track.getId(), new ArrayList<>());
            keyframes.sort(Comparator.comparing(VideoTrackKeyframe::getFrameIndex));
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("trackId", track.getId());
            t.put("trackNo", track.getTrackNo());
            t.put("labelId", track.getLabelId());
            t.put("startFrame", track.getStartFrame());
            t.put("endFrame", track.getEndFrame());
            t.put("status", track.getStatus());
            List<Map<String, Object>> kfList = new ArrayList<>();
            for (VideoTrackKeyframe kf : keyframes) {
                Map<String, Object> k = new LinkedHashMap<>();
                k.put("frameIndex", kf.getFrameIndex());
                k.put("x", kf.getX());
                k.put("y", kf.getY());
                k.put("width", kf.getWidth());
                k.put("height", kf.getHeight());
                k.put("outside", kf.getOutside());
                k.put("occluded", kf.getOccluded());
                kfList.add(k);
            }
            t.put("keyframes", kfList);
            trackList.add(t);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileId", fileId);
        payload.put("tracks", trackList);
        return payload;
    }

    /**
     * MOT 格式导出：每行 frame_id, track_id, x, y, w, h（像素，x/y 为左上角）。
     */
    private String exportMot(VideoAnnotationTask task) {
        VideoDatasetFile file = fileMapper.selectById(task.getVideoFileId());
        int width = file != null && file.getWidth() != null ? file.getWidth() : 1;
        int height = file != null && file.getHeight() != null ? file.getHeight() : 1;
        List<VideoAnnotationTrack> tracks = listTracks(task.getId());
        Map<Long, List<VideoTrackKeyframe>> keyframesByTrack = keyframesByTrack(task.getId());
        List<String> lines = new ArrayList<>();
        for (VideoAnnotationTrack track : tracks) {
            List<VideoTrackKeyframe> keyframes = keyframesByTrack.getOrDefault(track.getId(), new ArrayList<>());
            keyframes.sort(Comparator.comparing(VideoTrackKeyframe::getFrameIndex));
            if (keyframes.isEmpty()) {
                continue;
            }
            int trackId = track.getTrackNo() != null ? track.getTrackNo() : 1;
            int start = keyframes.get(0).getFrameIndex();
            int end = keyframes.get(keyframes.size() - 1).getFrameIndex();
            for (int frame = start; frame <= end; frame++) {
                double[] bbox = interpolateBbox(keyframes, frame);
                if (bbox == null) {
                    continue;
                }
                int x = (int) Math.round(bbox[0] * width);
                int y = (int) Math.round(bbox[1] * height);
                int w = (int) Math.round(bbox[2] * width);
                int h = (int) Math.round(bbox[3] * height);
                lines.add(frame + "," + trackId + "," + x + "," + y + "," + w + "," + h);
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 线性插值：返回 [x, y, width, height]（归一化），frame 在区间外返回 null。
     */
    private double[] interpolateBbox(List<VideoTrackKeyframe> keyframes, int frame) {
        int first = keyframes.get(0).getFrameIndex();
        int last = keyframes.get(keyframes.size() - 1).getFrameIndex();
        if (frame < first || frame > last) {
            return null;
        }
        for (VideoTrackKeyframe kf : keyframes) {
            if (kf.getFrameIndex() == frame) {
                if (Boolean.TRUE.equals(kf.getOutside())) {
                    return null;
                }
                return new double[]{kf.getX(), kf.getY(), kf.getWidth(), kf.getHeight()};
            }
        }
        VideoTrackKeyframe prev = keyframes.get(0);
        VideoTrackKeyframe next = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (frame > keyframes.get(i).getFrameIndex() && frame < keyframes.get(i + 1).getFrameIndex()) {
                prev = keyframes.get(i);
                next = keyframes.get(i + 1);
                break;
            }
        }
        if (Boolean.TRUE.equals(prev.getOutside())) {
            return null;
        }
        double t = (frame - prev.getFrameIndex()) / (double) (next.getFrameIndex() - prev.getFrameIndex());
        double x = prev.getX() + t * (next.getX() - prev.getX());
        double y = prev.getY() + t * (next.getY() - prev.getY());
        double w = prev.getWidth() + t * (next.getWidth() - prev.getWidth());
        double h = prev.getHeight() + t * (next.getHeight() - prev.getHeight());
        return new double[]{x, y, w, h};
    }

    // ===== 校验 =====

    private void ensureEditable(VideoAnnotationTask task) {
        // 已删除提交审核功能，标注始终可编辑，不再锁死
    }

    private void validateForSubmit(VideoAnnotationTask task) {
        List<VideoAnnotationTrack> tracks = listTracks(task.getId());
        if (tracks.isEmpty()) {
            throw new BusinessException("请至少创建一条 Track 再提交");
        }
        VideoDatasetFile file = fileMapper.selectById(task.getVideoFileId());
        int frameCount = file != null && file.getFrameCount() != null ? file.getFrameCount() : Integer.MAX_VALUE;
        Map<Long, List<VideoTrackKeyframe>> keyframesByTrack = keyframesByTrack(task.getId());
        for (VideoAnnotationTrack track : tracks) {
            List<VideoTrackKeyframe> keyframes = keyframesByTrack.getOrDefault(track.getId(), new ArrayList<>());
            if (keyframes.isEmpty()) {
                throw new BusinessException("Track #" + track.getTrackNo() + " 缺少关键帧");
            }
            if (track.getStartFrame() != null && track.getEndFrame() != null
                    && track.getStartFrame() > track.getEndFrame()) {
                throw new BusinessException("Track #" + track.getTrackNo() + " 起止帧不合法");
            }
            for (VideoTrackKeyframe kf : keyframes) {
                if (kf.getFrameIndex() != null && (kf.getFrameIndex() < 1 || kf.getFrameIndex() > frameCount)) {
                    throw new BusinessException("Track #" + track.getTrackNo() + " 关键帧 " + kf.getFrameIndex() + " 超出视频帧数范围");
                }
                validateBbox(kf);
            }
        }
    }

    private void validateBbox(VideoTrackKeyframe kf) {
        double x = kf.getX() == null ? -1 : kf.getX();
        double y = kf.getY() == null ? -1 : kf.getY();
        double w = kf.getWidth() == null ? 0 : kf.getWidth();
        double h = kf.getHeight() == null ? 0 : kf.getHeight();
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > 1 || y + h > 1) {
            throw new BusinessException("关键帧(frame " + kf.getFrameIndex() + ") bbox 越界或非法（需在 0~1 归一化范围内）");
        }
    }

    // ===== 内部工具 =====

    private List<VideoAnnotationTrackVO> loadTracks(Long taskId) {
        List<VideoAnnotationTrack> tracks = listTracks(taskId);
        Map<Long, List<VideoTrackKeyframe>> keyframesByTrack = keyframesByTrack(taskId);
        List<VideoAnnotationTrackVO> result = new ArrayList<>();
        for (VideoAnnotationTrack track : tracks) {
            List<VideoTrackKeyframe> keyframes = keyframesByTrack.getOrDefault(track.getId(), new ArrayList<>());
            keyframes.sort(Comparator.comparing(VideoTrackKeyframe::getFrameIndex));
            result.add(toTrackVO(track, keyframes));
        }
        return result;
    }

    private VideoAnnotationTrackVO toTrackVO(VideoAnnotationTrack track, List<VideoTrackKeyframe> keyframes) {
        VideoAnnotationTrackVO vo = new VideoAnnotationTrackVO();
        vo.setId(track.getId());
        vo.setTrackNo(track.getTrackNo());
        vo.setLabelId(track.getLabelId());
        vo.setStartFrame(track.getStartFrame());
        vo.setEndFrame(track.getEndFrame());
        vo.setStatus(track.getStatus());
        vo.setSource(track.getSource());
        vo.setKeyframes(keyframes.stream().map(this::toKeyframeVO).collect(Collectors.toList()));
        return vo;
    }

    private VideoTrackKeyframeVO toKeyframeVO(VideoTrackKeyframe kf) {
        VideoTrackKeyframeVO vo = new VideoTrackKeyframeVO();
        vo.setId(kf.getId());
        vo.setFrameIndex(kf.getFrameIndex());
        vo.setX(kf.getX());
        vo.setY(kf.getY());
        vo.setWidth(kf.getWidth());
        vo.setHeight(kf.getHeight());
        vo.setOutside(kf.getOutside());
        vo.setOccluded(kf.getOccluded());
        return vo;
    }

    private VideoDatasetFile requireFile(Long datasetId, Long fileId) {
        VideoDatasetFile file = fileMapper.selectById(fileId);
        if (file == null || !datasetId.equals(file.getDatasetId())) {
            throw new BusinessException("视频文件不存在");
        }
        return file;
    }

    private VideoAnnotationTask findTask(Long fileId) {
        return taskMapper.selectOne(new QueryWrapper<VideoAnnotationTask>()
                .eq("video_file_id", fileId).eq("deleted", 0).last("LIMIT 1"));
    }

    private VideoAnnotationTask requireTask(Long taskId) {
        VideoAnnotationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("标注任务不存在");
        }
        return task;
    }

    private VideoAnnotationTask requireTaskByFile(Long fileId) {
        VideoAnnotationTask task = findTask(fileId);
        if (task == null) {
            throw new BusinessException("标注任务不存在，请先进入标注页创建");
        }
        return task;
    }

    private VideoAnnotationTrack requireTrack(Long trackId) {
        VideoAnnotationTrack track = trackMapper.selectById(trackId);
        if (track == null) {
            throw new BusinessException("Track 不存在");
        }
        return track;
    }

    private VideoTrackKeyframe requireKeyframe(Long keyframeId) {
        VideoTrackKeyframe keyframe = keyframeMapper.selectById(keyframeId);
        if (keyframe == null) {
            throw new BusinessException("关键帧不存在");
        }
        return keyframe;
    }

    private List<VideoAnnotationTrack> listTracks(Long taskId) {
        return trackMapper.selectList(new QueryWrapper<VideoAnnotationTrack>()
                .eq("task_id", taskId).eq("deleted", 0).orderByAsc("track_no"));
    }

    private List<VideoAnnotationTask> findTasksByDataset(Long datasetId) {
        return taskMapper.selectList(new QueryWrapper<VideoAnnotationTask>()
                .eq("dataset_id", datasetId).eq("deleted", 0));
    }

    private Map<Long, List<VideoTrackKeyframe>> keyframesByTrack(Long taskId) {
        List<VideoAnnotationTrack> tracks = listTracks(taskId);
        if (tracks.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> trackIds = tracks.stream().map(VideoAnnotationTrack::getId).collect(Collectors.toList());
        List<VideoTrackKeyframe> keyframes = keyframeMapper.selectList(new QueryWrapper<VideoTrackKeyframe>()
                .in("track_id", trackIds).eq("deleted", 0));
        return keyframes.stream().collect(Collectors.groupingBy(VideoTrackKeyframe::getTrackId));
    }

    private void updateLastFrame(VideoAnnotationTask task, int frameIndex) {
        taskMapper.updateById(new VideoAnnotationTask().setId(task.getId()).setLastFrameIndex(frameIndex));
    }

    private void markAnnotating(VideoAnnotationTask task) {
        if (STATUS_READY.equals(task.getStatus())) {
            taskMapper.updateById(new VideoAnnotationTask().setId(task.getId()).setStatus(STATUS_ANNOTATING));
            syncFileStatus(task.getVideoFileId(), STATUS_ANNOTATING);
        }
    }

    private void syncFileStatus(Long fileId, String status) {
        fileMapper.updateById(new VideoDatasetFile().setId(fileId).setAnnotationStatus(status));
    }
}
