package org.dlut.adv.mineai.core.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @description 数据集版本Vo
 * @date 2020-05-21
 */
@Data
public class DatasetVersionVO implements Serializable {

    private Long id;

    private Long datasetId;

    private Integer dataType;

    private Integer annotateType;

    private String name;

    private String versionName;

    private String versionSource;

    private String fullName;

    private Date createTime;

    private String versionNote;

    private Boolean isCurrent;

    private Integer status;

    private Integer fileCount;

    private ProgressVO progressVO;

    private String versionUrl;

    private String versionOfRecordUrl;

    private Integer dataConversion;

    private Integer imageCounts;

    private Boolean presetFlag;

    private Integer isOfRecord;

    private String format;

    private Long createUserId;

    private Map<String, Integer> labelCountMap;

    private Long totalFileSize;

    private Integer importableImageCount;

    private short isPublic;
    public final static short IS_PUBLIC = 1;
    public final static short IS_NOT_PUBLIC = 0;
}
