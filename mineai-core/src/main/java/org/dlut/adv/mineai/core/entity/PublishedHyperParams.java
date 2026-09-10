package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import javax.persistence.*;

/**
 * @package: org.dlut.adv.mineai.core.entity
 * @author: chystart
 * @create: 2024-10-23 16:02
 * @description: 发布相关超惨信息存储
 **/
@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PublishedHyperParams {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 超参名称
     */
    private String paramName;

    /**
     * 超参范围
     */
    private String paramRange;

    /**
     * 参数的描述信息
     */
    private String paramDescription;

    /**
     * 超参用途：
     *  1：训练
     *  2：转换
     *  3：其他 ->
     */
    private Integer paramUsage;
    public static final Integer PARAM_TYPE_TRAIN = 1;
    public static final Integer PARAM_TYPE_CONVERT = 2;
    public static final Integer PARAM_TYPE_OTHER = 3;

    /**
     * 参数的类型：
     *  string
     *
     */
    private String paramType;

    /**
     * 训练填写的参数值
     */
    private String trainDefaultValue;

    /**
     * 镜像中的默认值
     */
    private String imageDefaultValue;
}