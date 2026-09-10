package org.dlut.adv.mineai.core.entity;



import lombok.Data;

import java.util.Date;
@Data
public class Notification {

    public static class NotificationType {
        public static final Integer INFO = 0;
        //public static final Integer SUCCESS = 1;
        public static final Integer WARNING = 2;
        public static final Integer ERROR = 3;
    }


    private Long id;


    private Long toUserId;


    private Integer notificationType;


    private String operationType;



    private String payload;

    private Integer readStatus;


    private Date createTime;


    private Date updateTime;


    private Boolean deleted;
}