package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.PcFileSearchDTO;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.domain.vo.PcFileSearchResultVO;

import java.util.List;

/**
 * @description 点云文件检索服务接口
 * @date 2026-08-27
 */
public interface PcFileSearchService {

    /**
     * 跨数据集多条件检索点云文件
     *
     * @param query 检索条件
     * @return 分页结果
     */
    IPage<PcFileSearchResultVO> search(PcFileSearchDTO query);

    /**
     * 查询全部点云数据集（检索数据集下拉用）
     *
     * @return 点云数据集列表
     */
    List<PcDataset> listDatasets();
}
