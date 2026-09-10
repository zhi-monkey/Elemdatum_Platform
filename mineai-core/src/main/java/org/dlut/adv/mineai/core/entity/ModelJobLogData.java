package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
public class ModelJobLogData implements Serializable {

    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的ModelJob
     */
    @ManyToOne
    @JoinColumn(name = "model_job_id")
    private ModelJob modelJob;

    /**
     * 训练进度
     */
    @Column(precision = 5, scale = 1)  // 总共5位，小数点后1位 (如：100.0)
    private BigDecimal progress;

    /**
     * epoch详细数据，存储为JSON格式
     */
    @Column(columnDefinition = "LONGTEXT")
    private String epochDetail;

    /**
     * 最终指标 - 新召回率
     */
    @Column(precision = 5, scale = 4)  // 总共5位，小数点后4位 (如：0.1234)
    private BigDecimal recall;

    /**
     * 最终指标 - 新准确率
     */
    @Column(precision = 5, scale = 4)  // 总共5位，小数点后4位 (如：0.1234)
    private BigDecimal accuracy;
}