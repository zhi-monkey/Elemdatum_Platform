package org.dlut.adv.mineai.model.repository;


import feign.Param;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Map;

/**
 * @author sunzhen
 */
public interface ModelVersionRepo extends PagingAndSortingRepository<ModelVersion, Long>, JpaSpecificationExecutor<ModelVersion> {

    @Query("SELECT m FROM ModelVersion m JOIN FETCH m.chip WHERE m.isDelete = 0")
    List<ModelVersion> findAll();

    @Query("SELECT m FROM ModelVersion m JOIN FETCH m.chip WHERE m.chip.chipType = :chipType")
    List<ModelVersion> findByChipType(@Param("chipType") String chipType);


    /**
     * 根据showname查询所有modelVersion
     *
     * @param showName
     * @return
     */
    List<ModelVersion> findModelVersionsByShowNameAndIsDelete(String showName, int isDelete);

    ModelVersion findModelVersionByShowNameAndIsDelete(String showName, int isDelete);

    ModelVersion findModelVersionByDescriptionAndIsDelete(String description , int isDelete);


    /**
     * description: 通过modelVersion查版本
     */
    ModelVersion findModelVersionByNameAndIsDelete(String name, int isDeleted);

    /**
     * description: 通过id查版本
     *
     * @param id
     * @return
     */
    ModelVersion findModelVersionById(long id);

    ModelVersion findModelVersionByShowNameAndIsInferable(String showName, boolean isInferable);






//    @Query(value = "select a from ModelVersion a where a.id in ( select m.modelVersion.id from MonitorModelConfig m  where m.monitor.id = :monitorId) and a.isDelete = :isDelete ")
//    List<ModelVersion> findModelVersionsByMonitorId(long monitorId, int isDelete);
//
//    @Query(value = "select a from Monitor a where a.id in ( select m.monitor.id from MonitorModelConfig m  where m.modelVersion.id = :modelVersionId) and a.isDelete = :isDelete ")
//    List<Monitor> findMonitorsByModelVersionId(long modelVersionId, int isDelete);
//
//    @Query(value = "select * from modelVersion a where a.id in (select model_version_id from mine_service_model where mine_service_id = :mineServiceId) and a.is_delete =:isDelete", nativeQuery = true)
//    List<ModelVersion> findModelVersionByMineServiceId(long mineServiceId, int isDelete);
//
//    @Query(value = "select stream_url from monitor_model_config where model_version_id = :modelVersionId and monitor_id = :monitorId", nativeQuery = true)
//    String findStreamUrlByModelVersionIdAndMonitorId(long modelVersionId, long monitorId);
//
//    @Query(value = "select id from monitor_model_config where model_version_id = :modelVersionId and monitor_id = :monitorId", nativeQuery = true)
//    Long findCustomConfigIdByModeVersionIdAndMonitorId(long modelVersionId, long monitorId);

    @Query(value = "select video_name from monitor_model_config where id = :monitor_model_config_id", nativeQuery = true)
    String findVideoNameByCustomConfigId(long monitor_model_config_id);

    @Query(value = "select name from scene s where s.id = :sceneId", nativeQuery = true)
    String findSceneNameBySceneId(long sceneId);


    @Query(value = "select * from monitor_model_config_custom_config where monitor_model_config_id = :monitor_model_config_id", nativeQuery = true)
    List<Map<String, Object>> findMonitorModelConfigByCustomConfigId(long monitor_model_config_id);

    /**
     * 根据name模糊查询所有模型，用于删除
     *
     * @param Name     模糊Name
     * @param isDelete 是否删除
     * @return List<ModelVersion> 模型列表
     */
    @Query(nativeQuery = true, value = "select * from model_version m where m.name like CONCAT (?1, '%') and m.is_delete= ?2")
    List<ModelVersion> findModelVersionListByNameLike(String Name, int isDelete);

    /**
     * 根据是否自动标注、是否已删除查询所有modelVersion
     *
     * @param autoLabel 是否用于自动标注
     * @param isDelete 是否删除
     * @return List<ModelVersion> modelVersion列表
     */
    @Query(nativeQuery = true, value = "select * from model_version m where m.is_auto_label = ?1 and m.is_delete= ?2")
    List<ModelVersion> findModelVersionsByAutoLabel(boolean autoLabel, int isDelete);

    /**
     * 查询所有支持自动标注的模型
     *
     * @return List<ModelVersion> modelVersion列表
     */
    @Query(nativeQuery = true, value = "select * from model_version m where m.is_auto_label = 1 and m.is_delete= 0")
    List<ModelVersion> findImageSupportAutoLabel();

    /**
     * 根据标注类型查询支持自动标注的镜像
     *
     * @param annotationType 标注类型（Detection/Segmentation）
     * @return List<ModelVersion> modelVersion列表
     */
    @Query(nativeQuery = true, value = "select * from model_version m where m.is_auto_label = 1 and m.is_delete= 0 and m.annotation_type = ?1")
    List<ModelVersion> findImageSupportAutoLabelByType(String annotationType);

    /**
     * 根据url查询模型
     *
     * @param imageUrl url
     * @return ModelVersion 模型
     */
    @Query(nativeQuery = true, value = "select * from model_version m where m.url = ?1 and m.is_delete = 0")
    ModelVersion findModelVersionByUrl(String imageUrl);
}
