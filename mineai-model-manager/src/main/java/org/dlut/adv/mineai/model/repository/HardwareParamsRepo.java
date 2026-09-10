package org.dlut.adv.mineai.model.repository;
import org.dlut.adv.mineai.core.entity.HardwareParams;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author daixiaowei
 */
@Repository
public interface HardwareParamsRepo extends PagingAndSortingRepository<HardwareParams, Long>, JpaSpecificationExecutor<ModelJob> {

    List<HardwareParams> findAll();

    HardwareParams findHardwareParamsById(long id);

    HardwareParams findHardwareParamsByName(String name);

    /**
     * 根据用户ID或角色查询硬件参数
     * @param userId 用户ID
     * @return 硬件参数列表
     */
    @Query(value = "SELECT hp.* " +
            "FROM hardware_params hp " +
            "LEFT JOIN user u ON hp.create_user_id = u.id " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE (hp.create_user_id = :userId OR hp.create_user_id IS NULL) " +
            "   OR r.name IN ('管理员', '管理人员')" +
            "ORDER BY hp.is_default DESC, hp.id ",
            nativeQuery = true)
    List<HardwareParams> findByCreateUserIdOrAdminRole(@Param("userId") Long userId);

    /**
     * 根据用户ID或角色统计硬件参数数量
     * @param userId 用户ID
     * @return 硬件参数数量
     */
    @Query(value = "SELECT COUNT(hp.id) " +
            "FROM hardware_params hp " +
            "LEFT JOIN user u ON hp.create_user_id = u.id " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE (hp.create_user_id = :userId OR hp.create_user_id IS NULL) " +
            "   OR r.name IN ('管理员', '管理人员')",
            nativeQuery = true)
    long countByCreateUserIdOrAdminRole(@Param("userId") Long userId);

    /**
     * 根据用户ID或角色查询硬件参数（分页）
     * @param userId 用户ID
     * @return 硬件参数列表
     */
    @Query(value = "SELECT hp.* " +
            "FROM hardware_params hp " +
            "LEFT JOIN user u ON hp.create_user_id = u.id " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE (hp.create_user_id = :userId OR hp.create_user_id IS NULL) " +
            "   OR r.name IN ('管理员', '管理人员') " +
            "ORDER BY hp.is_default DESC, hp.id " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<HardwareParams> findByCreateUserIdOrAdminRolePage(@Param("userId") Long userId,
                                                           @Param("offset") int offset,
                                                           @Param("limit") int limit);

    /**
     * 根据用户ID或角色以及is_default字段分页查询硬件参数
     * @param userId 用户ID
     * @param isDefault 是否为默认配置 (1: 默认配置, 0: 自定义配置)
     * @return 硬件参数列表
     */
    @Query(value = "SELECT hp.* " +
            "FROM hardware_params hp " +
            "LEFT JOIN user u ON hp.create_user_id = u.id " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE ((hp.create_user_id = :userId OR hp.create_user_id IS NULL) " +
            "   OR r.name IN ('管理员', '管理人员')) " +
            "AND hp.is_default = :isDefault " +
            "ORDER BY hp.id " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<HardwareParams> findByCreateUserIdOrAdminRolePageByIsDefault(@Param("userId") Long userId,
                                                                      @Param("isDefault") int isDefault,
                                                                      @Param("offset") int offset,
                                                                      @Param("limit") int limit);

    /**
     * 根据用户ID或角色以及is_default字段统计硬件参数数量
     * @param userId 用户ID
     * @param isDefault 是否为默认配置 (1: 默认配置, 0: 自定义配置)
     * @return 硬件参数数量
     */
    @Query(value = "SELECT COUNT(hp.id) " +
            "FROM hardware_params hp " +
            "LEFT JOIN user u ON hp.create_user_id = u.id " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE ((hp.create_user_id = :userId OR hp.create_user_id IS NULL) " +
            "   OR r.name IN ('管理员', '管理人员')) " +
            "AND hp.is_default = :isDefault",
            nativeQuery = true)
    long countByCreateUserIdOrAdminRoleByIsDefault(@Param("userId") Long userId,
                                                   @Param("isDefault") int isDefault);

    /**
     * 查询哪些 ID 被引用（在 related_table 中有关联记录）
     * @param ids 要检查的 ID 列表
     * @return 被引用的 ID 列表
     */
    @Query(value = "SELECT DISTINCT hp.name, CAST(hp.id AS SIGNED) as id FROM hardware_params hp " +
            "WHERE hp.id IN (:ids) " +
            "AND EXISTS (SELECT 1 FROM model_generation rt WHERE rt.hardware_params_id = hp.id)",
            nativeQuery = true)
    List<Object[]> findReferencedIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM hardware_params WHERE id IN (:ids)", nativeQuery = true)
    void deleteAllByIdInBatch(@Param("ids") List<Long> ids);
}
