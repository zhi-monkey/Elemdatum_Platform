package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.model.service.impl.ChipServiceImpl;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ChipService {

    /**
     * 找到所有的芯片信息
     *
     * @return
     */
    List<Chip> findAll();

    /**
     * 按照 id 返回 chip 信息
     *
     * @param chipId
     * @return
     */
    Chip getChipById(Long chipId);

    Chip findByChipType(String chipType);

    /**
     * 更新或新建芯片信息
     *
     * @param chip
     * @return
     */
    Chip createOrUpdateChip(Chip chip);

    /**
     * 保存芯片信息
     *
     * @param chip
     * @return
     */
    Chip save(Chip chip);

    /**
     * 按照 芯片 id 删除信息
     *
     * @param id
     */
    void deleteById(Long id);


    List<ChipServiceImpl.ChipOption> getAllChipOptions();

    /**
     * 按照 id 查询芯片信息
     *
     * @param id
     * @return
     */
    Chip findById(Long id);

//    public Chip saveChip(Chip chip);

    org.springframework.data.domain.Page<Chip> findChipsByType(String chipType, Pageable pageable);

    Integer chipCount();

}