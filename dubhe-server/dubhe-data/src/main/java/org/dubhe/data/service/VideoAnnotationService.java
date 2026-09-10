package org.dubhe.data.service;

import org.dubhe.data.domain.dto.VideoAnnotationTrackCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeUpdateDTO;
import org.dubhe.data.domain.vo.VideoAnnotationTrackVO;
import org.dubhe.data.domain.vo.VideoAnnotationVO;
import org.dubhe.data.domain.vo.VideoTrackKeyframeVO;

import java.util.List;

/**
 * 视频 BBox 跟踪标注服务。
 */
public interface VideoAnnotationService {

    /**
     * 创建/打开视频标注任务，并返回完整标注数据。
     */
    VideoAnnotationVO openOrCreate(Long datasetId, Long fileId);

    /**
     * 加载标注数据（视频元信息 + tracks + keyframes）。
     */
    VideoAnnotationVO load(Long datasetId, Long fileId);

    /**
     * 创建 Track（首次画框 = 建 Track + 首个 Keyframe）。
     */
    VideoAnnotationTrackVO createTrack(VideoAnnotationTrackCreateDTO dto);

    /**
     * 新增关键帧。
     */
    VideoTrackKeyframeVO createKeyframe(VideoTrackKeyframeCreateDTO dto);

    /**
     * 修改关键帧。
     */
    VideoTrackKeyframeVO updateKeyframe(Long keyframeId, VideoTrackKeyframeUpdateDTO dto);

    /**
     * 删除关键帧。
     */
    void deleteKeyframe(Long keyframeId);

    /**
     * 结束 Track。
     */
    void finishTrack(Long trackId);

    /**
     * 删除 Track（连同其关键帧逻辑删除）。
     */
    void deleteTrack(Long trackId);

    /**
     * 提交标注（校验 + 状态 -> SUBMITTED）。
     */
    void submit(Long fileId);

    /**
     * 审核通过。
     */
    void review(Long taskId);

    /**
     * 审核驳回（退回重标）。
     */
    void reject(Long taskId);

    /**
     * 任务列表（按数据集，供审核入口 / 进度回显）。
     */
    List<VideoAnnotationVO> listTasks(Long datasetId);

    /**
     * 导出标注（一期仅内部 JSON）。
     */
    Object export(Long fileId, String format);
}
