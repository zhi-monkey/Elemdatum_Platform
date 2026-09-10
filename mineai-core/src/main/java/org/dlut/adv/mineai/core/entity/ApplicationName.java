package org.dlut.adv.mineai.core.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author wangwenjiao
 */
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table
@Getter
@Setter
@Data
public class ApplicationName implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String applicationName;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isDelete = false; // 标记是否已删除

}