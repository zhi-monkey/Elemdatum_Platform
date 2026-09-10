package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelConfigRepo extends PagingAndSortingRepository<ModelConfig, Long>,JpaSpecificationExecutor<ModelConfig>{
    /**
     * 根据id 查询ModelConfig
     *
     * @param id ModelConfigId
     * @return ModelConfig 超参配置
     */
    ModelConfig findModelConfigById(long id);
    /**
     * 根据field 查询ModelConfig
     *
     * @param field 超参名称
     * @return ModelConfig 超参配置
     */
    ModelConfig findModelConfigByField(String field);

}
