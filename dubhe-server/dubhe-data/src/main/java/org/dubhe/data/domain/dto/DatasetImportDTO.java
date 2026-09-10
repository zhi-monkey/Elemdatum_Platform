package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel
public class DatasetImportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据集Id")
    @NotNull(message = "数据集ID不能为空")
    private Long datasetId;

    @ApiModelProperty(value = "数据集压缩包地址，/nfs下的相对路径")
    @NotNull(message = "请上传压缩包!")
    private String archiveUrl;

    @ApiModelProperty(value = "数据集类型: VOC, YOLO, CreateML")
    @NotNull(message = "请指定上传的数据集类型!")
    private String datasetType;

    @ApiModelProperty(value = "数据集版本号")
    private String currentVersionName;

    @ApiModelProperty(value = "传输任务ID，由服务端创建")
    private Long transferTaskId;


}
