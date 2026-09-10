package org.dlut.adv.mineai.model.service.impl;

import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.model.repository.ChipRepository;
import org.dlut.adv.mineai.model.service.ChipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

;

@Service
@Transactional
public class ChipServiceImpl implements ChipService {

    @Autowired
    private ChipRepository chipRepository; // 注入 repository

    @Override
    public List<Chip> findAll() {
        return chipRepository.findAll(); // 查询所有芯片信息，支持分页
    }

    @Override
    public Chip getChipById(Long chipId) {
        return chipRepository.findChipById(chipId);
    }

    //    @Override
//    public Optional<Chip> findById(Long id) {
//        return chipRepository.findById(id); // 根据 ID 查询芯片
//    }
    @Override
    public Page<Chip> findChipsByType(String chipType, Pageable pageable) {
    return chipRepository.findByChipTypeContaining(chipType, pageable);
}

    @Override
    public Integer chipCount() {
        return Math.toIntExact(chipRepository.count());
    }


    @Override
    public Chip findByChipType(String chipType) {
        return chipRepository.findByChipType(chipType);
    }

    @Override
    public Chip createOrUpdateChip(Chip chip) {
        return chipRepository.save(chip); // 保存芯片信息
    }

    @Override
    public Chip save(Chip chip) {
        return null;
    }

//    @Override
//    public Chip save(Chip chip) {
//        return chipRepository.save(chip); // 保存芯片信息
//    }
//    @Override
//    public Chip save(Chip chip) {
//        if (chip.getId() != null && chipRepository.existsById(chip.getId())) {
//        // 如果芯片已经存在，则更新芯片类型
//            chipRepository.updateChipTypeById(chip.getId(), chip.getChipType());
//            return chip;
//        } else {
////         如果芯片不存在，则保存新芯片
//            return chipRepository.save(chip);
//    }
//}
//    @Override
//    public Chip saveChip(@RequestBody Chip chip) {
//        Optional<Chip> existingChip = chipRepository.findChipById(chip.getId());
//
//        if (existingChip.isPresent()) {
//        // 更新现有记录
//            int updatedRows = chipRepository.updateChipTypeById(chip.getId(), chip.getChipType());
//        if (updatedRows > 0) {
//            return chipRepository.findChipById(chip.getId()).orElseThrow(() -> new NoSuchElementException("Chip not found with id: " + chip.getId()));
//        } else {
//            throw new RuntimeException("更新失败");
//        }
//         } else {
//        // 插入新记录
//            chipRepository.insertChip(chip.getId(), chip.getChipType());
//            return chipRepository.findChipById(chip.getId()).orElseThrow(() -> new NoSuchElementException("Chip not found with id: " + chip.getId()));
//    }
//}


    @Override
    public void deleteById(Long id) {
        chipRepository.deleteById(id); // 删除芯片信息
    }


    public List<ChipOption> getAllChipOptions() {
        List<Chip> chips = chipRepository.findAll();
        return chips.stream()
                .map(chip -> new ChipOption(chip.getChipType(), chip.getChipType())) // 将芯片类型转换为选项格式
                .collect(Collectors.toList());
    }

    public static class ChipOption {
        private String label;
        private String value;

        public ChipOption(String label, String value) {
            this.label = label;
            this.value = value;
        }

        // Getter 和 Setter 方法
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    @Override
    public Chip findById(Long id) {
        Optional<Chip> chipOptional = chipRepository.findById(id);
        return chipOptional.get();
    }
}