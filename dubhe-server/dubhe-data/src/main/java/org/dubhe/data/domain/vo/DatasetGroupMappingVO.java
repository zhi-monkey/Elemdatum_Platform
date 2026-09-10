package org.dubhe.data.domain.vo;

import lombok.Data;
import org.dubhe.biz.base.vo.DatasetVO;

import java.util.List;

/**
 * @author mingming
 * @date 2025/09/15
 */
@Data
public class DatasetGroupMappingVO {
    private DatasetGroupVO group;
    private List<DatasetVO> datasets;
}