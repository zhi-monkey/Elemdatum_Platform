package org.dlut.adv.mineai.core.entity;

import  lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class MineService implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String name;

    /**
     * description:服务中文名称
     */
    private String mineServiceName;

    private int port;

    @OneToMany
    private List<Model> model;

    /**
     * 是否删除（0为启用 1为删除）默认为启用0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;
}
