package org.dlut.adv.mineai.model.repository;

import feign.Param;
import org.dlut.adv.mineai.core.entity.Scene;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.OrderBy;
import java.util.List;


/**
 * @author wangwenjiao
 */
@Repository
public interface SceneRepo extends PagingAndSortingRepository<Scene, Long>, JpaSpecificationExecutor<Scene> {


    // 查询所有未删除的场景数据
//    @Query("SELECT s FROM Scene s WHERE s.isDelete = :isDelete")
//    List<Scene> findAllByIsDelete(@Param("isDelete") int isDelete);

    // 查询所有场景数据
    @Query("SELECT s FROM Scene s ")
    List<Scene> findAll();


    @Query("SELECT s FROM Scene s WHERE s.isDelete = 0")
    List<Scene> findAllNotDelete();

    @OrderBy("id DESC")
    List<Scene> findScenesByIsDelete(int isDelete);

    @Query(nativeQuery = true, value = "select * from model where is_delete= ?1 order by id desc limit ?2")
    List<Scene> findSceneListByDataNumAndDesc(int isDelete, int num);

    //通过id精确查询
    @Query("SELECT a FROM Scene a WHERE a.id = :id")
    Scene findSceneBySceneId(@Param("id") Long id);
    // 通过场景名称精确查询
    @Query("SELECT s FROM Scene s WHERE s.name = :name")
    List<Scene> findBySceneName(@Param("name") String name);

    @Query("SELECT s FROM Scene s WHERE s.name = :name AND s.isDelete = 0")
    List<Scene> findBySceneNameAndNotDeleted(@Param("name") String name);

    // 模糊查询场景名称
    @Query("SELECT s FROM Scene s WHERE s.name LIKE %:name%")
    List<Scene> findBySceneNameContaining(@Param("name") String name);
    /**
     * 通过instruction找Scene
     */
    Scene findSceneByInstruction(String instruction);

}
