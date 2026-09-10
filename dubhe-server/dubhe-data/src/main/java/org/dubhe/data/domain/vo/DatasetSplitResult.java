package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel
public class DatasetSplitResult {
    private String trainImagesPath;
    private String testImagesPath;
    private String valImagesPath;
    private String trainAnnotationsPath;
    private String testAnnotationsPath;
    private String valAnnotationsPath;
}
