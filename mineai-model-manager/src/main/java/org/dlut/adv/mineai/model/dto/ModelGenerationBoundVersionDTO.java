package org.dlut.adv.mineai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dlut.adv.mineai.core.vo.DatasetGroupVO;
import org.dlut.adv.mineai.core.vo.DatasetVO;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;

import java.util.List;

/**
 * 模型生成任务绑定版本等信息DTO
 * @author mingming
 * @date 2025/09/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelGenerationBoundVersionDTO {
    private DatasetGroupVO datasetGroup;
    private DatasetVO dataset;
    private List<DatasetVersionVO> datasetVersions;
}
