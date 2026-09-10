package org.dlut.adv.mineai.model.service.inter;

import org.dlut.adv.mineai.core.entity.ModelClassification;
import java.util.List;

public interface ModelClassificationService {

    /**
     * 插入算法分类实体
     *
     * @param modelClassification 算法分类实体
     * @return 算法分类实体
     */
    ModelClassification insertModelClassification(ModelClassification modelClassification);

    /**
     * 删除算法分类
     *
     * @param id 算法分类实体 id
     */
    void deleteModelClassification(Long id);

    /**
     * 修改算法分类实体
     *
     * @param modelClassification 算法分类实体
     * @return 算法分类实体
     */
    ModelClassification updateModelClassification(ModelClassification modelClassification);

    /**
     * 按照算法分类 id 查询算法实体
     *
     * @param id 算法分类实体
     * @return 算法分类实体
     */
    ModelClassification getModelClassification(Long id);

    /**
     * 查询算法分类实体列表
     *
     * @return 所有的算法分类实体集合
     */
    List<ModelClassification> getModelClassifications();

}
