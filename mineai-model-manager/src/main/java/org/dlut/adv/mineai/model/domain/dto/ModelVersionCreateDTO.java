package org.dlut.adv.mineai.model.domain.dto;


import lombok.Data;
import org.dlut.adv.mineai.core.dto.UserSmallDTO;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class ModelVersionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本信息描述
     */
    private String description;

    /**
     * 镜像名词
     */
    private String showName;

    private String level;


    private Boolean isTrainable;


    private Boolean isInspectable;


    private Boolean isInferable;


    private String conversionPlatform;


    private String url;

    private Timestamp createTime;
}
