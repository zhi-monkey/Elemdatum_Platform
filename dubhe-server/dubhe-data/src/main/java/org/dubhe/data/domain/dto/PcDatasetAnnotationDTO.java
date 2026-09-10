package org.dubhe.data.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 点云 PCD 文件 3D Box 标注保存请求 DTO。
 * 接收前端 Three.js 点云标注页面回写的标注框列表。
 * 持久化复用 pc_dataset_file.pcd_metadata JSON 字段，无需新建表。
 */
@Data
@Accessors(chain = true)
public class PcDatasetAnnotationDTO {

    @NotEmpty(message = "标注框不能为空")
    @Valid
    private List<BoxAnnotation> boxes;

    @Data
    @Accessors(chain = true)
    public static class BoxAnnotation {

        private String id;

        private String label;

        private Long labelId;

        @NotNull(message = "中心坐标不能为空")
        @Size(min = 3, max = 3, message = "中心坐标必须为 x/y/z 三个数值")
        private List<Double> center;

        @NotNull(message = "尺寸不能为空")
        @Size(min = 3, max = 3, message = "尺寸必须为长/宽/高三个数值")
        private List<Double> size;

        @NotNull(message = "航向角不能为空")
        private Double yaw;
    }
}