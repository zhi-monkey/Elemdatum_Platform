package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatasetCardInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private Integer annotateType;

    private String currentVersionName;

    private Long labeledImageCount;

    private Long unLabeledImageCount;

    private Long totalImageCount;

    private Integer totalVideoCount;

    private Integer unExtractedVideoCount;

    private Integer extractedVideoCount;

    private Integer progress;

    private Integer status;

    private Integer module;

    private Integer remainTime;

}
