package org.dlut.adv.mineai.model.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class GuidedAndLabelsVO {
    // 判断是不是引导式
    private Boolean isDatasetGuided;
    // 拿到所有标签信息
    private List<LabelDTO> labels;
    // 数据集标注类型：102-目标检测，103-语义分割
    private Integer annotateType;
}
