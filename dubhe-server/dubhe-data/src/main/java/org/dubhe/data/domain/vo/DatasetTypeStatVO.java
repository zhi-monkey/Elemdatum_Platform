package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DatasetTypeStatVO
 * @Date：2023/12/18 22:05
 * @Filename：DatasetTypeStatVO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatasetTypeStatVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer detection;

    private Integer segmentation;

    private Integer all;

    private List<String> dateList;

    private List<String> detectionNumList;

    private List<String> segmentationNumList;

}
