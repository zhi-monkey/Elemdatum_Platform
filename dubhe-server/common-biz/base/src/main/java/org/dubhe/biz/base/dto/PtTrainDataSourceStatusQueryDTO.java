

package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @description 查询数据集对应训练状态查询条件
 * @date 2020-05-21
 */
@Data
@Accessors(chain = true)
public class PtTrainDataSourceStatusQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集路径
     */
    private List<String> dataSourcePath;

}
