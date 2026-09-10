package org.dlut.adv.mineai.model.repository;


import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.OrderBy;
import javax.transaction.Transactional;
import java.util.List;

@Repository
    public interface ModelExploreRepo extends PagingAndSortingRepository<ModelExplore, Long>, JpaSpecificationExecutor<ModelExplore> {
        List<ModelExplore> findAll();

    @OrderBy("id DESC")
    List<ModelExplore> findModelExploresByIsDelete(int isDelete);

    @Query(nativeQuery = true, value = "select * from model_explore where is_delete= ?1 order by id desc limit ?2")
    List<ModelExplore> findModelExploreExploreListByDataNumAndDesc(int isDelete, int num);

    @OrderBy("id DESC")
    List<ModelExplore> findModelExploresByIsDeleteAndModelStatus(int isDelete, int modelStatus);

    /**
     * 根据isDelete查询所有ModelExplore,结果降序排列
     *
     * @param isDelete
     * @return
     */
    List<ModelExplore> findModelExploresByIsDeleteOrderByIdDesc(int isDelete);

    /**
     * 通过modelName找model
     */
    ModelExplore findModelExploreByModelName(String name);

    /**
     * 通过modelDescription找model
     */
    ModelExplore findModelExploreByDescription(String description);


    List<ModelExplore> findModelExploreByModelNameAndCreatorId(String name,long creatorId);

    /**
     * @param
     * @param isDelete
     * @return
     */
    @Query(value = "select m from ModelExplore m where m.isDelete = :isDelete")
    List<ModelExplore> findModelExploresIsNotDelete(int isDelete);

    ModelExplore findModelExploreById(long id);

    @Query("select distinct mv from ModelExplore me join me.trainModelVersion mv where me.isDelete = ?1")
    List<ModelVersion> findtrainModelVersionByModelExplore(int isDelete);

    @Query("select distinct mv from ModelExplore me join me.deployModelVersion mv where me.isDelete = ?1")
    List<ModelVersion> finddeployModelVersionByModelExplore(int isDelete);

    @Query("select distinct mv from ModelExplore me join me.convertModelVersion mv where me.isDelete = ?1")
    List<ModelVersion> findconvertModelVersionByModelExplore(int isDelete);

    @Transactional
    @Modifying
    @Query("update ModelExplore me set me.isDelete = 1 where me.id = ?1")
    void softDeleteById(Long modelExploreId);
}