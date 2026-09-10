package org.dlut.adv.mineai.model.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author mingming
 * @date 2024/10/18
 */
@Data
@AllArgsConstructor
public class ModelJobParams {

    private Long modelJobId;

    private String paramsKey;

    /**
     * 上次训练的的参数
     */
    private String params;

    /**
     * 参数范围(1,)/{true,false}
     */
    private String msg;

    private short required;

    private String inputDescription;
    /**
     * 类型: numeric/string/enum
     */
    private String type;
    /**
     *  镜像模板默认值，如果params没有，再用这个
     */
    private String defaultValue;
}
