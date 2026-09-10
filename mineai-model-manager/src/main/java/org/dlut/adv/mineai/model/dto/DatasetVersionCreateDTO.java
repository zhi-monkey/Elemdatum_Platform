package org.dlut.adv.mineai.model.dto;



import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @description 数据集版本
 * @date 2020-05-14
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DatasetVersionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    @NotNull(message = "数据集ID不能为空")
    private Long datasetId;

    /**
     * 版本名称
     */
    private String versionName;

    /**
     * 版本说明
     */
    private String versionNote;


    private Integer ofRecord;

    /**
     * 版本数据集格式
     */
    @ApiModelProperty(value = "格式")
    @NotNull(message = "版本格式不能为空")
    private String format;

    /**
     * 文件状态列表
     */
    @ApiModelProperty(value = "文件状态列表")
    private List<Integer> fileStatus;

    /**
     * 标签id列表
     */
    @ApiModelProperty(value = "标签id列表")
    private List<Long> labels;

    @ApiModelProperty(value = "标签映射")
    private List<LabelMappingDTO> labelMappings;


    public @interface Create {
    }


}
