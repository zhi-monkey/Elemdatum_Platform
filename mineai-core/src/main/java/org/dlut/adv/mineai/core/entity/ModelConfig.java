package org.dlut.adv.mineai.core.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * @author haoxiaoyang
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
public class ModelConfig {

    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true)
    private long id;
    //id是唯一的

    /**
     * 变量字段名
     */
    String field;

    /**
     * 前端页面显示名称，默认与field取值相同
     */
    String label;

    /**
     * 默认值
     */
    String defaultNum;

    /**
     * 是否必填，默认为false
     */
    @Column(columnDefinition = "boolean default false NOT NULL")
    Boolean required;

    /**
     * 范围最小值
     */
    Integer min;

    /**
     * 范围最大值
     */
    Integer max;

    /**
     * 不满足校验时的提示，默认为“[label]范围需在[min]-[max]之间！
     */
    String msg;

    String type;

    String inputDescription;
}
