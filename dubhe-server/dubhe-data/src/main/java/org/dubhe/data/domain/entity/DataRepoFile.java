package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("data_repo_file")
@ApiModel(value = "DataRepoFile对象", description = "文件信息")
public class DataRepoFile{

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "文件名")
    private String name;

    @ApiModelProperty(value = "数据集id")
    private Long datasetId;

    @ApiModelProperty(value = "资源访问路径")
    private String url;

    @ApiModelProperty(value = "创建用户ID")
    private Long createUserId;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "0正常，1已删除")
    private Boolean deleted;

    @ApiModelProperty(value = "文件类型 0-图片,1-视频,2-其他")
    private Integer fileType;

    @ApiModelProperty(value = "图片宽")
    private Integer width;

    @ApiModelProperty(value = "图片高")
    private Integer height;

    @ApiModelProperty(value = "资源拥有者ID")
    private Long originUserId;

}
