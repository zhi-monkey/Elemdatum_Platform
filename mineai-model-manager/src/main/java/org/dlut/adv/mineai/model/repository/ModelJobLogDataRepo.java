package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.ModelJobLogData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ModelJobLogDataRepo extends PagingAndSortingRepository<ModelJobLogData, Long>, JpaSpecificationExecutor<ModelJobLogData> {
    /**
     * 根据ModelJob查找对应的日志数据
     * @param modelJob 模型任务对象
     * @return ModelJobLogData 日志数据
     */
    ModelJobLogData findByModelJob(ModelJob modelJob);
}
