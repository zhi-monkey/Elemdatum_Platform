package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationStatistics;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import javax.persistence.OrderBy;
import java.util.List;
import java.util.Map;

/**
 * @author haoxiaoyang
 */
@Repository
public interface ModelRepo extends PagingAndSortingRepository<Model, Long>, JpaSpecificationExecutor<Model> {
    @Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.publishedHyperParamsList")
    List<Model> findAll();

    @OrderBy("id DESC")
    List<Model> findModelsByIsDelete(int isDelete);

    @Query(nativeQuery = true, value = "select * from model where is_delete= ?1 order by id desc limit ?2")
    List<Model> findModelListByDataNumAndDesc(int isDelete, int num);

    /**
     * 根据isDelete查询所有model,结果降序排列
     *
     * @param isDelete
     * @return
     */
    List<Model> findModelsByIsDeleteOrderByIdDesc(int isDelete);

    @Query(value = "select a.id from Model a where a.id in ( select m.model.id from MonitorModelConfig m where m.monitor.id = :monitorId) and a.isDelete = :isDelete")
    List<Long> findModelIdsByMonitorId(long monitorId ,int isDelete);

    /**
     * 通过modelName找model
     */
    Model findModelByModelName(String name);

    /**
     * 通过modelDescription找model
     */
    Model findModelByDescription(String description);

    /**
     * 通过modelEnglishName找model
     */
    Model findModelByModelEnglishName(String modelEnglishNames);

    /**
     * @param
     * @param isDelete
     * @return
     */
    @Query(value = "select m from Model m where m.isDelete = :isDelete")
    List<Model> findModelsIsNotDelete(int isDelete);

    @Query(value = "select a.id from Model a where a.id in ( select m.model.id from MonitorModelConfig m where m.monitor.id = :monitorId) and a.isDelete = :isDelete")
    List<Long> findModelIdByMonitorId(long monitorId, int isDelete);

    @Query(value = "select a from Model a where a.id in ( select m.model.id from MonitorModelConfig m  where m.monitor.id = :monitorId) and a.isDelete = :isDelete ")
    List<Model> findModelsByMonitorId(long monitorId, int isDelete);

    @Query(value = "select a from Monitor a where a.id in ( select m.monitor.id from MonitorModelConfig m  where m.model.id = :modelId) and a.isDelete = :isDelete ")
    List<Monitor> findMonitorsByModelId(long modelId, int isDelete);

    Model findModelById(long id);

    /**
     * 根据name模糊查询所有模型，用于删除
     *
     * @param modelName 模糊modelName
     * @param isDelete  是否删除
     * @return List<Model> 模型列表
     */
    @Query(nativeQuery = true, value = "select * from model m where m.model_name like CONCAT (?1, '%') and m.is_delete= ?2")
    List<Model> findModelListByModelNameLike(String modelName, int isDelete);

    @Query(nativeQuery = true, value = "select * from model m where m.model_english_name like CONCAT (?1, '%') and m.is_delete= ?2")
    List<Model> findModelListByModelEnglishNameLike(String modelEnglishName, int isDelete);

    @Query(nativeQuery = true, value = "select * from model m where m.description like CONCAT (?1, '%') and m.is_delete= ?2")
    List<Model> findModelListByDescriptionLike(String description, int isDelete);

    List<Model> findModelByModelNameLikeAndIsDelete(String modelName, int isDelete);

    @Query(value = "select * from model a where a.id in (select model_id from mine_service_model where mine_service_id = :mineServiceId) and a.is_delete =:isDelete", nativeQuery = true)
    List<Model> findModelByMineServiceId(long mineServiceId, int isDelete);

    @Query(value = "select stream_url from monitor_model_config where model_id = :model_id and monitor_id = :monitorId", nativeQuery = true)
    String findStreamUrlByModelIdAndMonitorId(long model_id, long monitorId);

    @Query(value = "select id from monitor_model_config where model_id = :model_id and monitor_id = :monitorId", nativeQuery = true)
    Long findCustomConfigIdByModelIdAndMonitorId(long model_id, long monitorId);

    @Query(value = "select video_name from monitor_model_config where id = :monitor_model_config_id", nativeQuery = true)
    String findVideoNameByCustomConfigId(long monitor_model_config_id);

    @Query(value = "select name from scene s where s.id = :sceneId", nativeQuery = true)
    String findSceneNameBySceneId(long sceneId);


    @Query(value = "select * from monitor_model_config_custom_config where monitor_model_config_id = :monitor_model_config_id", nativeQuery = true)
    List<Map<String, Object>> findMonitorModelConfigByCustomConfigId(long monitor_model_config_id);

    @Query(value = "select mc.classification_name as modelClassificationStatisticsName, count(mmc.model_id) as modelClassificationStatisticsCount " +
                   "from model_model_classification as mmc " +
                   "inner join model_classification as mc " +
                   "on mmc.model_classification_id = mc.id " +
                   "group by mmc.model_classification_id ", nativeQuery = true)
    List<ModelClassificationStatistics> findModelClassificationCount();

    @Query(value = "SELECT mg.model_explore_id FROM model m " +
            "JOIN model_generation mg ON mg.id = m.model_generation_id " +
            "WHERE model_id = :modelId", nativeQuery = true)
    ModelExplore findModelExploreByModelId(Long modelId);

    @Query(value = "select count(*) from model m where m.is_delete = ?1", nativeQuery = true)
    Long countModelByIsDelete(int isDelete);
}
