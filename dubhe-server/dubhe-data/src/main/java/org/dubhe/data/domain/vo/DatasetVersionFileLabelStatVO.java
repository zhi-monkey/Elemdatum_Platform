package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DatasetVersionFileLabelStatVO
 * @Date：2023/12/18 21:41
 * @Filename：DatasetVersionFileLabelStatVO
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatasetVersionFileLabelStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer annotatedFiles;

    private Integer unannotatedFiles;

    private Integer allFiles;

}
