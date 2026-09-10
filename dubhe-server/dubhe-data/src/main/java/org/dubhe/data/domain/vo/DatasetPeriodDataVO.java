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
 * @name：DatasetPeriodDataVO
 * @Date：2023/12/18 20:57
 * @Filename：DatasetPeriodDataVO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatasetPeriodDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer thisMonth;

    private Integer thisWeek;

    private Integer today;

    private Integer total;

    private List<String> dateList;

    private List<String> numList;

}
