package org.dlut.adv.mineai.core.vo;

import lombok.Data;
import java.util.Date;

@Data

public class NotificationVO {


    private Long id;

    private Integer notificationType;


    private String operationType;


    private String message;


    private Integer readStatus;

    private Date createTime;


    private String payload;


    private String url;


}
