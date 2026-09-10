package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.data.domain.dto.VideoDatasetCreateDTO;
import org.dubhe.data.domain.dto.VideoDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.VideoDatasetUploadUrlDTO;
import org.dubhe.data.domain.entity.VideoDataset;
import org.dubhe.data.domain.vo.VideoDatasetDetailVO;
import org.dubhe.data.domain.vo.VideoDatasetFileVO;
import org.dubhe.data.domain.vo.VideoDatasetUploadUrlVO;

public interface VideoDatasetService {
    VideoDataset create(VideoDatasetCreateDTO dto);

    VideoDatasetUploadUrlVO createUploadUrl(Long datasetId, VideoDatasetUploadUrlDTO dto);

    VideoDatasetFileVO commitFile(Long datasetId, VideoDatasetFileCommitDTO dto);

    IPage<VideoDatasetFileVO> listFiles(Long datasetId, Page<VideoDatasetFileVO> page);

    /**
     * 获取视频数据集详情（video 命名空间，与图片数据集数字 id 隔离）
     *
     * @param datasetId 视频数据集 id
     * @return 详情 VO（含文件抽帧统计）
     */
    VideoDatasetDetailVO getDetail(Long datasetId);
}
