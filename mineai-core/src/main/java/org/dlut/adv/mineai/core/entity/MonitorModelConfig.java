package org.dlut.adv.mineai.core.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author haoxiaoyang
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"monitor_id", "model_id"}))
@Getter
@Setter
public class MonitorModelConfig implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true)
    private long id;//id是唯一的
    /**
     * 绑定模型
     */
    @ManyToOne
    Model model;


    /**
     * 绑定监控设备
     */
    @ManyToOne
    Monitor monitor;


    /**
     * 对摄像头推理后合成的包含标注框视频流推流地址
     */
    String streamUrl;

    /**
     * 模型对摄像头级别自定义的配置，如感兴趣区域、模型参数等
     */
    @ElementCollection
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, String> customConfig;

    @Column(columnDefinition = "LONGTEXT")
    private String viaMetadata;
    //monitorUrl  根据绑定的monitor获取。

    //用户自定义的视频名称
    String videoName;

    //是否开启推理流
    Boolean pushStream;
}
