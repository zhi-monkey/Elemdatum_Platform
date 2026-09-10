package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ExternalModelConverter;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import feign.Param;
import java.util.List;

/**
 * @author wwj
 */
@Repository
public interface ExternalModelConverterRepo extends PagingAndSortingRepository<ExternalModelConverter, Long>, JpaSpecificationExecutor<ExternalModelConverter> {

    // 查询所有设备数据
    @Override
    @Query("SELECT e FROM ExternalModelConverter e")
    List<ExternalModelConverter> findAll();

    // 根据设备id查询（精确查询）
    @Query("SELECT e FROM ExternalModelConverter e WHERE e.id = :id")
    ExternalModelConverter findDeviceById(@Param("id") Long id);

    // 根据设备ip查询（精确查询）
    @Query("SELECT e FROM ExternalModelConverter e WHERE e.ip = :ip")
    ExternalModelConverter findDeviceByIp(@Param("ip") String ip);

    // 根据描述信息模糊查询设备
    @Query("SELECT e FROM ExternalModelConverter e WHERE e.description LIKE %:description%")
    List<ExternalModelConverter> findDevicesByDescriptionContaining(@Param("description") String description);

    // 根据设备ip查询并返回设备id
    @Query("SELECT e.id FROM ExternalModelConverter e WHERE e.ip = :ip")
    Long findIdByIp(@Param("ip") String ip);
}
