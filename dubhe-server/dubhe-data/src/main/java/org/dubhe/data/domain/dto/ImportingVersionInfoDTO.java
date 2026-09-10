package org.dubhe.data.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.data.domain.entity.Label;

import java.util.List;

/**
 * @description 导入数据集版本信息
 * @author mingming
 * @date 2025/08/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportingVersionInfoDTO {
    private List<Label> mutualLabels;
    private Integer imgCount;
}
