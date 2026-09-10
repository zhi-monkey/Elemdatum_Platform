package org.dlut.adv.mineai.model.repository;

import feign.Param;
import org.dlut.adv.mineai.core.entity.Chip;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ChipRepository extends PagingAndSortingRepository<Chip, Long>, JpaSpecificationExecutor<Chip> {

    @Query("SELECT a FROM Chip a")
    List<Chip> findAll();

    @Query("SELECT c FROM Chip c WHERE (:chipType IS NULL OR c.chipType LIKE %:chipType%)")
    Page<Chip> findByChipTypeContaining(@Param("chipType") String chipType, Pageable pageable);

    /**
     * 按照 chip id 查询芯片信息
     *
     * @param id
     * @return
     */
    @Query("SELECT a FROM Chip a WHERE a.id = :id")
    Chip findChipById(Long id);

    Chip findByChipType(String chipType);

}