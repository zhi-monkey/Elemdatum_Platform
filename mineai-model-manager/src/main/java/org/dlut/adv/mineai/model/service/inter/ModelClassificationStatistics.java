package org.dlut.adv.mineai.model.service.inter;

/**
 * @package: org/dlut/adv/mineai/model/service/inter
 * @author: chystart
 * @create: 2024-6-14 21:24
 * @description: 算法分类统计返回类型
 **/
public interface ModelClassificationStatistics {

    /**
     * 算法分类统计名称
     *
     * @return
     */
    String getModelClassificationStatisticsName();

    /**
     * 算法分类统计数量
     *
     * @return
     */
    Long getModelClassificationStatisticsCount();
}
