package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author 25741
 */
@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class HardwareParams implements Serializable {
    //一次作业
    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    private String name;
    /**
     * 使用的资源:内存大小（单位为MB）；使用的处理器数量；GPU显存大小  创建的时候设置
     */
    private String memory;

    private String cpus;

    private String gpus;

    private String gpuMemory;

    private String vGpuCores;

    private Long createUserId;

    private String description;

    private String title;

    @Column(name = "is_default")
    private Integer isDefault = 0;
}
