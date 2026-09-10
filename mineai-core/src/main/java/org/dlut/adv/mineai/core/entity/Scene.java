package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

/**
 * @author haoxiaoyang
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
@Data
public class Scene {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备场景名是唯一的
     */
    @Column(nullable = false)
    private String name;

    /**
     * 设备场景描述
     */
    private String instruction;

    /**
     * 部门（0为启用 1为删除）默认为启用0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scene scene = (Scene) o;
        return id.equals(scene.id) && isDelete == scene.isDelete && name.equals(scene.name) && instruction.equals(scene.instruction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, instruction, isDelete);
    }
}