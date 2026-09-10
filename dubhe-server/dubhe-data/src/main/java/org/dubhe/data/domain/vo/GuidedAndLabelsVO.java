package org.dubhe.data.domain.vo;

import lombok.Data;
import org.dubhe.data.domain.dto.LabelDTO;
import org.dubhe.data.domain.dto.LabelZipDTO;

import java.util.List;

@Data
public class GuidedAndLabelsVO {
    private Boolean isDatasetGuided;
    // 拿到所有标签信息
    private List<LabelZipDTO> labels;
    // 数据集标注类型：102-目标检测，103-语义分割
    private Integer annotateType;
}
