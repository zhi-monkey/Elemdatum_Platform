package org.dlut.adv.mineai.core.vo;

import lombok.Data;

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