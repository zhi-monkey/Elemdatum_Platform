package org.dlut.adv.mineai.model.dto;

import lombok.Data;

/**
 * @author mingming
 * @date 2024/09/27
 */
@Data
public class SaveZipUrlDTO {
    private Long modelApplicationId;
    private String appZipPath;
}
