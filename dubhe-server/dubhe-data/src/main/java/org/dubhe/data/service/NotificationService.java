package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.domain.vo.NotificationVO;

import java.util.Date;

public interface NotificationService extends IService<Notification> {

    Long create(Long toUserId, Integer notificationType, String operationType, String payload);

    // 支持枚举的重载方法
    Long create(Long toUserId, Integer notificationType, NotificationOperationTypeEnum operationType, String payload);

    boolean markRead(Long id, Long userId);

    int markAllRead(Long userId);

    Page<Notification> pageList(Long userId, Integer readStatus, Integer notificationType, String operationType, Page<Notification> page);

    Page<NotificationVO> pageListVO(Long userId, Integer readStatus, Integer notificationType, String operationType, Page<NotificationVO> page);

    int unreadCount(Long userId);


    void safeInsertAsync(Notification notification);
    void createNotification(Long toUserId, Integer notificationType, String operationType, String payload);
}
