

package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.SymbolConstant;
import org.dubhe.biz.db.annotation.Query;
import org.dubhe.biz.db.base.PageQueryBase;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @description 数据集版本查询
 * @date 2020-05-25
 */
@Data
public class DatasetVersionQueryCriteriaDTO extends PageQueryBase implements Serializable{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("数据集ID")
    @Query(propName = "dataset_id", type = Query.Type.EQ)
    @NotNull(message = "数据集ID不能为空")
    @Min(value = NumberConstant.NUMBER_0, message = "数据集ID不能小于0")
    private Long datasetId;

    @Query(propName = "deleted", type = Query.Type.EQ)
    @ApiModelProperty(hidden = true,value = SymbolConstant.ZERO)
    private int deleted;

    @ApiModelProperty("是否公开")
    @Query(propName = "is_public", type = Query.Type.EQ)
    private Boolean isPublic;

    /**
     * 用于引导式过滤标签的
     */
    private List<String> labelList;

    private Boolean isIncludePublishing =  false;

    @ApiModelProperty("格式（YOLO、Segment-YOLO、COCO等）")
    @Query(propName = "format", type = Query.Type.EQ)
    private String format;

}
