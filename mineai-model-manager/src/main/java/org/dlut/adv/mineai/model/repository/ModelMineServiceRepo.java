package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.MineService;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author haoxiaoyang
 */
@Repository
public interface ModelMineServiceRepo extends PagingAndSortingRepository<MineService, Long>, JpaSpecificationExecutor<MineService> {

    List<MineService> findDistinctByModelIsNotNullAndIsDelete(int isDelete);

    MineService findDistinctByModelContainingAndIsDelete(Model model, int isDelete);

    MineService findMineServiceByName(String name);


    MineService save(MineService mineService);

    List<MineService> findAll();

    @Query(value = "select m.name from MineService m ")
    List<String> getNamesOfMineService();

    @Query(value = "select * from mine_service", nativeQuery = true)
    List<MineService> getMineServiceList();

    /**
     * 根据controllerId查询MineServiceId
     *
     * @param controllerId
     * @return List<Long>
     */
    @Query(value = "select a.id from mine_service a where a.id in (select mine_service_id from controller_mine_service where controller_id = :controllerId)", nativeQuery = true)
    List<Long> findMineServiceIdByControllerId(long controllerId);

    /**
     * 根据controllerId查询MineService
     *
     * @param controllerId
     * @return List<MineService>
     */
    @Query(value = "select * from mine_service a where a.id in (select mine_service_id from controller_mine_service where controller_id = :controllerId)", nativeQuery = true)
    List<MineService> findMineServiceByControllerId(long controllerId);

    /**
     * 根据MineServiceId 解绑 Controller
     *
     * @param mineServiceId
     * @return List<MineService>
     */
    @Transactional
    @Modifying
    @Query(value = "delete from controller_mine_service where mine_service_id=:mineServiceId", nativeQuery = true)
    void unbindControllerByMineServiceId(long mineServiceId);

    MineService findMineServiceById(Long id);

    MineService findMineServiceByModelId(long modelVersionId);

}
