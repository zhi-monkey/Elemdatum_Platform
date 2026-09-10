package org.dubhe.data.domain.dto;

import lombok.Data;


@Data
public class ImageStatisticsDTO {
    private int totalImages;
    private int unlabeledImages;
    private int labeledImages;

}
