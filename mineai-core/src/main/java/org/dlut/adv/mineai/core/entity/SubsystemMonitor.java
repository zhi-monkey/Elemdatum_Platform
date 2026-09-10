package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class SubsystemMonitor implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;                 //索引

    private String name;      //子系统名称
    private String location;      //位置
    private String scene;      //场景
    private String algName;      //算法名称
    private String monitorName;      //摄像头名称
    private String monitorUrl;      //原始流
    private String streamUrl;      //推理流

    @Column(columnDefinition = "int default 0 NOT NULL")
    private int status;    //存活状态
    public static final int STATUS_ON = 0;
    public static final int STATUS_OFF = 1;

    @ElementCollection
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, String> customInfo;      //自定义信息

    //重写Object的equals方法
    @Override
    public boolean equals(Object obj) {
        //如果比较的两个对象是同一个对象，则直接返回true
        if (this == obj) {   //this就代表调用equals方法的那个对象，obj就是方法里面的实参
            return true;
        }
        //类型判断
        if (obj instanceof SubsystemMonitor) {  //SubsystemMonitor才比较
            //进行转型
            SubsystemMonitor p = (SubsystemMonitor) obj;
            return this.name.equals(p.name) &&
                    this.location.equals(p.location) &&
                    this.scene.equals(p.scene) &&
                    this.algName.equals(p.algName) &&
                    this.monitorName.equals(p.monitorName) &&
                    this.monitorUrl.equals(p.monitorUrl) &&
                    this.streamUrl.equals(p.streamUrl) &&
                    this.customInfo.equals(p.customInfo);
        }
        //如果不是则直接返回false
        return false;
    }
}
