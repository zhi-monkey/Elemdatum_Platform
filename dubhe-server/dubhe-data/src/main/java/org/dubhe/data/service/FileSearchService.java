package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.FileSearchDTO;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.FileSearchResultVO;

import java.util.List;

/**
 * @description 图片多条件检索服务接口
 * @date 2026-08-26
 */
public interface FileSearchService {

    /**
     * 跨数据集多条件检索图片
     *
     * @param query 检索条件
     * @return 分页结果
     */
    IPage<FileSearchResultVO> search(FileSearchDTO query);

    /**
     * 查询全部现存标签
     *
     * @return 标签列表
     */
    List<LabelTemplate> listAllLabels();
}
