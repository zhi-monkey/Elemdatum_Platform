

package org.dubhe.biz.base.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * @description 字典
 * @date 2021-01-19
 */
@Data
public class DictVO implements Serializable {

    private static final long serialVersionUID = -1176729960392375726L;
    private Long id;

    private String name;

    private String remark;

    private List<DictDetailVO> dictDetails;

    private Timestamp createTime;
}
