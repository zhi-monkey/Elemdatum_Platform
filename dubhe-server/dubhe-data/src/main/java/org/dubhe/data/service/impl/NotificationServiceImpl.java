package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.dao.NotificationMapper;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.domain.vo.NotificationVO;
import org.dubhe.data.service.NotificationService;
import org.dubhe.data.service.formatter.NotificationFormatter;
import org.dubhe.data.service.formatter.NotificationFormatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    private Logger logger = LoggerFactory.getLogger(NotificationService.class);
    @Autowired
    private NotificationFormatterFactory formatterFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long toUserId, Integer notificationType, String operationType, String payload) {
        // 验证操作类型是否有效
        if (!NotificationOperationTypeEnum.isValid(operationType)) {
            throw new BusinessException("无效的操作类型: " + operationType);
        }

        Notification n = new Notification();
        n.setToUserId(toUserId);
        n.setNotificationType(notificationType);
        n.setOperationType(operationType);
        n.setPayload(payload);
        n.setReadStatus(0);
        Date now = new Date();
        n.setCreateTime(now);
        n.setUpdateTime(now);
        n.setDeleted(false);
        save(n);
        return n.getId();
    }

    // 重载方法：支持直接传入枚举
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long toUserId, Integer notificationType, NotificationOperationTypeEnum operationType, String payload) {
        return create(toUserId, notificationType, operationType.getCode(), payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long id, Long userId) {
        LambdaUpdateWrapper<Notification> uw = new LambdaUpdateWrapper<>();
        uw.eq(Notification::getId, id)
                .eq(Notification::getToUserId, userId)
                .eq(Notification::getReadStatus, 0)
                .set(Notification::getReadStatus, 1)
                .set(Notification::getUpdateTime, new Date());
        return update(uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long userId) {
        LambdaUpdateWrapper<Notification> uw = new LambdaUpdateWrapper<>();
        uw.eq(Notification::getToUserId, userId)
                .eq(Notification::getReadStatus, 0)
                .set(Notification::getReadStatus, 1)
                .set(Notification::getUpdateTime, new Date());
        return baseMapper.update(null, uw);
    }

    @Override
    public Page<Notification> pageList(Long userId, Integer readStatus, Integer notificationType, String operationType, Page<Notification> page) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getToUserId, userId);
        if (readStatus != null) {
            qw.eq(Notification::getReadStatus, readStatus);
        }
        if (notificationType != null) {
            qw.eq(Notification::getNotificationType, notificationType);
        }
        if (operationType != null && !operationType.isEmpty()) {
            qw.eq(Notification::getOperationType, operationType);
        }
        qw.orderByDesc(Notification::getCreateTime);
        return page(page, qw);
    }

    @Override
    public Page<NotificationVO> pageListVO(Long userId, Integer readStatus, Integer notificationType, String operationType, Page<NotificationVO> page) {
        // 查询原始数据
        Page<Notification> notificationPage = new Page<>(page.getCurrent(), page.getSize());
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getToUserId, userId);
        if (readStatus != null) {
            qw.eq(Notification::getReadStatus, readStatus);
        }
        if (notificationType != null) {
            qw.eq(Notification::getNotificationType, notificationType);
        }
        if (operationType != null && !operationType.isEmpty()) {
            qw.eq(Notification::getOperationType, operationType);
        }
        // 未读排在上面
        qw.orderByAsc(Notification::getReadStatus);
        // 时间最新的在上面
        qw.orderByDesc(Notification::getCreateTime);

        Page<Notification> result = page(notificationPage, qw);

        // 转换为VO并格式化消息
        Page<NotificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(
                result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList())
        );

        return voPage;
    }

    /**
     * 转换为VO并格式化消息
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);

        // 使用格式化器格式化消息
        NotificationFormatter formatter = formatterFactory
                .getFormatter(notification.getOperationType());

        String message = formatter.format(notification.getPayload());
        String url  = formatter.getUrl(notification);
        vo.setMessage(message);
        vo.setUrl(url);

        return vo;
    }

    @Override
    public int unreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getToUserId, userId)
                .eq(Notification::getReadStatus, 0));
    }

    @Override
    @Async
    public void safeInsertAsync(Notification notification) {

        try {
            save(notification);
        } catch (Exception e) {
            logger.error("Error occurred while inserting dataset operation event: ", e);
        }
    }

    @Override
    public void createNotification(Long toUserId, Integer notificationType, String operationType, String payload) {
        if (!NotificationOperationTypeEnum.isValid(operationType)) {
            throw new BusinessException("无效的操作类型: " + operationType);
        }

        Notification n = new Notification();
        n.setToUserId(toUserId);
        n.setNotificationType(notificationType);
        n.setOperationType(operationType);
        n.setPayload(payload);
        n.setReadStatus(0);
        Date now = new Date();
        n.setCreateTime(now);
        n.setUpdateTime(now);
        n.setDeleted(false);
        this.safeInsertAsync(n);
    }
}
