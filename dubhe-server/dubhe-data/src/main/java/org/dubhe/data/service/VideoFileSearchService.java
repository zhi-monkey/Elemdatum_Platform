package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.VideoFileSearchDTO;
import org.dubhe.data.domain.entity.VideoDataset;
import org.dubhe.data.domain.vo.VideoFileSearchResultVO;

import java.util.List;

/**
 * @description 视频文件检索服务接口
 * @date 2026-08-27
 */
public interface VideoFileSearchService {

    /**
     * 跨数据集多条件检索视频文件
     *
     * @param query 检索条件
     * @return 分页结果
     */
    IPage<VideoFileSearchResultVO> search(VideoFileSearchDTO query);

    /**
     * 查询全部视频数据集（检索数据集下拉用）
     *
     * @return 视频数据集列表
     */
    List<VideoDataset> listDatasets();
}
