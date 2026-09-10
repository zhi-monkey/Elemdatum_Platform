

package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.biz.db.annotation.Query;

/**
 * @description 数据集二进制转换条件查询
 * @date 2020-06-08
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetVersionCriteriaVO {

    @Query(type = Query.Type.EQ, propName = "deleted")
    private int deleted;

    @Query(type = Query.Type.EQ, propName = "data_conversion")
    private int dataConversion;

}
