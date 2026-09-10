package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：BatchFileInfoDTO
 * @Date：2024/1/2 21:01
 * @Filename：BatchFileInfoDTO
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "BatchFileInfo dto", description = "批量获取文件信息")
public class BatchFileInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "文件不能为空")
    private List<Long> fileIds;

    private Long datasetId;
}
