package org.dlut.adv.mineai.core.entity;
import org.dlut.adv.mineai.core.entity.Chip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID

    private String deviceName; // 设备名称

    private String firmwareVersion; // 固件版本
//
//    @ManyToOne
//    @JoinColumn(name = "chip_id", referencedColumnName = "id") // 指定外键和引用的主键列
//    private Chip chip; // 对应的算力芯片

    @ManyToOne
    @JoinColumn(name = "chip_id", referencedColumnName = "id") // 指定外键和引用的主键列
    private Chip chip; // 对应的算力芯片

//    @Transient // 该注解表示该字段不应映射到数据库列中，仅用于临时计算或显示
//    private String chipType; // 临时字段，用于显示算力芯片类型
//
//    public String getChipType() {
//        return chip != null ? chip.getChipType() : null; // 从关联的 Chip 对象获取 chipType
//    }
}