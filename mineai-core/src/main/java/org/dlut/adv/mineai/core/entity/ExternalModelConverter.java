package org.dlut.adv.mineai.core.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author wwj
 */
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table
@Getter
@Setter
@Data
public class ExternalModelConverter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ip;

    private String description;

    private int port;

    private boolean onlineStatus;

    @ManyToMany
    @JoinTable(
            name = "external_model_converter_chip", // 中间表名
            joinColumns = @JoinColumn(name = "converter_id"), // 当前类在中间表中的外键列名
            inverseJoinColumns = @JoinColumn(name = "chip_id") // 关联类在中间表中的外键列名
    )
    private List<Chip> chips;

//    // 定时 ping 功能
//    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//    public ExternalModelConverter() {
//        startPingTask();
//    }
//
//    // 定时 ping 方法
//    private void startPingTask() {
//        ExternalModelConverter converter = this;
//
//        scheduler.scheduleAtFixedRate(() -> {
//            converter.onlineStatus = pingDevice(converter.ip); // 使用局部变量
//            System.out.println("Ping status for " + converter.ip + ": " + converter.onlineStatus);
//        }, 0, 5, TimeUnit.SECONDS); // 每5秒钟 ping 一次
//    }
//
//    // Ping 方法
//    private boolean pingDevice(String ipAddress) {
//        try {
////            Process process = Runtime.getRuntime().exec("ping -c 1 " + ipAddress); // Linux/Mac
//            Process process = Runtime.getRuntime().exec("ping -n 1 " + ipAddress); // Windows
//            return process.waitFor() == 0; // 如果返回值为0则表示在线
////            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false; // 出现异常则认为设备不在线
//        }
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalModelConverter that = (ExternalModelConverter) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public boolean getOnlineStatus() {
        return this.onlineStatus;
    }
}