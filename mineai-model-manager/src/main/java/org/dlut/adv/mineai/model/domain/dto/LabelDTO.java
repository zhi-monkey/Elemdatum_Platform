package org.dlut.adv.mineai.model.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 返回label相关信息
 * @author mingming
 * @date 2025/03/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelDTO {

    private String name;

    private Long id;

    private String color;

    public LabelDTO(String name) {
        this.name = name;
    }
}
