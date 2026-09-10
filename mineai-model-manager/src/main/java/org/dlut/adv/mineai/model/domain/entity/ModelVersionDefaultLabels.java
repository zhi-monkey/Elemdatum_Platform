package org.dlut.adv.mineai.model.domain.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 该类用于存放镜像与默认模型对应的标签名称的对应关系
 * @author mingming
 * @date 2025/06/11
 */
@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@ToString
@Data
public class ModelVersionDefaultLabels {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long modelVersionId;
    private String labelName;
}
