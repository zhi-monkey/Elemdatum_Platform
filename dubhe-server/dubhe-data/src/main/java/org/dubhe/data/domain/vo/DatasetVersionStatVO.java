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
 * @name：DatasetVersionStatVO
 * @Date：2023/12/19 14:50
 * @Filename：DatasetVersionStatVO
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetVersionStatVO implements Serializable {

    private Integer publishedDatasetNum;

    private Integer unpublishedDatasetNum;

    private Integer allDatasetNum;
}
