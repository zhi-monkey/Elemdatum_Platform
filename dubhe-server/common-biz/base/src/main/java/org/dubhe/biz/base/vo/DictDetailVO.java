
package org.dubhe.biz.base.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @description 字典详情
 * @date 2020-12-23
 */
@Data
public class DictDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典详情id
     */
    private Long id;

    /**
     * 字典id
     */
    private Long dictId;

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典值
     */
    private String value;

    /**
     * 排序
     */
    private String sort;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 修改时间
     */
    private Timestamp updateTime;
}