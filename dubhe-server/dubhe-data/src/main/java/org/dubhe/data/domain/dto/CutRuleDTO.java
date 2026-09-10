package org.dubhe.data.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package: org.dubhe.data.domain.dto
 * @author: chystart
 * @create: 2024-04-06 15:39
 * @description: 切分规则实体类
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CutRuleDTO {

    /**
     * 测试集比例
     */
    private Integer testSet;

    /**
     * 训练集比例
     */
    private Integer trainSet;

    /**
     * 验证集比例
     */
    private Integer validationSet;
}
