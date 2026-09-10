package org.dubhe.data.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @package: org.dubhe.data.domain.dto
 * @author: chystart
 * @create: 2024-03-13 16:36
 * @description:
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTypeCountDTO {
    private Integer imageCount;
    private Integer videoCount;
    private Integer otherCount;
}
