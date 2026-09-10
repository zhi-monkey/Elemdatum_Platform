package org.dlut.adv.mineai.model.repository;

import feign.Param;
import org.dlut.adv.mineai.core.entity.ApplicationName;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wangwenjiao
 */
@Repository
public interface ApplicationNameRepo extends PagingAndSortingRepository<ApplicationName, Long>, JpaSpecificationExecutor<ApplicationName> {

    // 查询所有应用数据
    @Query("SELECT a FROM ApplicationName a")
    List<ApplicationName> findAll();

    // 通过id精确查询
    @Query("SELECT a FROM ApplicationName a WHERE a.id = :id")
    ApplicationName findApplicationNameById(@Param("id") Long id);

    // 通过name精确查询id
    @Query("SELECT a.id FROM ApplicationName a WHERE a.applicationName = :name")
    Long findIdByApplicationName(@Param("name") String name);

    // 通过应用名称精确查询
    @Query("SELECT a FROM ApplicationName a WHERE a.applicationName = :applicationName")
    List<ApplicationName> findByApplicationName(@Param("applicationName") String applicationName);

    // 模糊查询应用名称
    @Query("SELECT a FROM ApplicationName a WHERE a.applicationName LIKE %:applicationName%")
    List<ApplicationName> findByApplicationNameContaining(@Param("applicationName") String applicationName);

    //    @Select("SELECT * FROM application_name WHERE application_name = #{applicationName}")
    @Query(value = "SELECT DISTINCT a FROM ApplicationName a WHERE a.applicationName = :applicationName")
    ApplicationName findApplicationByApplicationName(String applicationName);

    // 根据应用名称和 isDelete 字段查询
    List<ApplicationName> findByApplicationNameAndIsDelete(String applicationName, Boolean isDelete);

    // 根据应用名称（模糊查询）和 isDelete 字段查询
    List<ApplicationName> findByApplicationNameContainingAndIsDelete(String applicationName, Boolean isDelete);

    @Query("SELECT a FROM ApplicationName a WHERE a.applicationName LIKE ?1 AND a.isDelete = ?2")
    List<ApplicationName> findApplicationNameListByApplicationNameLike(String applicationNamePattern, boolean deleteStatus);


}

