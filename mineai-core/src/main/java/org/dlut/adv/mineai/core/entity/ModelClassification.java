package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @package: org.dlut.adv.mineai.core.entity
 * @author: chystart
 * @create: 2024-06-13 08:41
 * @description: 算法分类实体
 **/
@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model_classification")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class ModelClassification implements Serializable {

    // 算法分类实体id
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 分类名称（唯一约束）
    @Column(name = "classification_name")
    private String classificationName;

    // 算法分类创建时间
    @CreatedDate
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    // 算法分类更新时间
    @LastModifiedDate
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}