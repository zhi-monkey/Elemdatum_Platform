package org.dlut.adv.mineai.core.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用于绑定应用任务(对应的算法包)和算法包中的标签
 * @author mingming
 * @date 2025/03/31
 */
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table
@Getter
@Setter
@Data
public class ModelApplicationZipLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long modelApplicationId;

    private Long labelId;
}
