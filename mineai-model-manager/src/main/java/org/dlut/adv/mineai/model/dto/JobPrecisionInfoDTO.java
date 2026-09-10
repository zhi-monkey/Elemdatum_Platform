package org.dlut.adv.mineai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于返回模型精度相关信息
 * @author mingming
 * @date 2025/08/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor()
public class JobPrecisionInfoDTO {
    private Double accuracy;
    private Double loss;
    private Integer imgCount;
}
