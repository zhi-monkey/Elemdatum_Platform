package org.dlut.adv.mineai.model.domain.vo;

import lombok.Data;
import org.dlut.adv.mineai.core.entity.Model;
import java.util.Date;

/**
 * @package: org.dlut.adv.mineai.model.domain.vo
 * @author: chystart
 * @create: 2024-05-29 20:26
 * @description: 算法商城中的算法信息返回
 **/
@Data
public class ModelVO {

    /**
     * 模型id
     */
    private long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型发布时间
     */
    private Date releaseTime;

    /**
     * 模型描述信息
     */
    private String description;

}
