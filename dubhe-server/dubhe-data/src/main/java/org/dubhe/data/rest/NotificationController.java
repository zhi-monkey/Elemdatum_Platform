package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.utils.JwtUtils;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.Notification;
import org.dubhe.data.domain.vo.NotificationVO;
import org.dubhe.data.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "通用：站内通知")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "分页查询我的通知")
    @GetMapping
    public DataResponseBody<Page<Notification>> page(Page<Notification> page,
                                                     @RequestParam(required = false) Integer readStatus,
                                                     @RequestParam(required = false) Integer notificationType,
                                                     @RequestParam(required = false) String operationType) {
        Long userId = JwtUtils.getCurUserId();
        return new DataResponseBody<>(notificationService.pageList(userId, readStatus, notificationType, operationType, page));
        }

    @ApiOperation(value = "获取我的未读数量")
    @GetMapping("/unread-count")
    public DataResponseBody<Integer> unreadCount() {
        Long userId = JwtUtils.getCurUserId();
        return new DataResponseBody<>(notificationService.unreadCount(userId));
    }

    @ApiOperation(value = "标记单条为已读")
    @PostMapping("/{id}/read")
    public DataResponseBody<Boolean> markRead(@PathVariable Long id) {
        Long userId = JwtUtils.getCurUserId();
        return new DataResponseBody<>(notificationService.markRead(id, userId));
    }

    @ApiOperation(value = "标记全部为已读")
    @PostMapping("/read-all")
    public DataResponseBody<Integer> markAllRead() {
        Long userId = JwtUtils.getCurUserId();
        return new DataResponseBody<>(notificationService.markAllRead(userId));
    }

    @ApiOperation(value = "创建通知（内部/管理使用）")
    @PostMapping
    public DataResponseBody<Long> create(@RequestParam Long toUserId,
                                         @RequestParam Integer notificationType,
                                         @RequestParam String operationType,
                                         @RequestBody String payload) {
        return new DataResponseBody<>(notificationService.create(toUserId, notificationType, operationType, payload));
    }


    @ApiOperation(value = "分页查询我的通知（格式化）")
    @GetMapping("/formatted")
    public DataResponseBody<Page<NotificationVO>> pageFormatted(
            Page<NotificationVO> page,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) Integer notificationType,
            @RequestParam(required = false) String operationType) {
        Long userId = JwtUtils.getCurUserId();
        return new DataResponseBody<>(notificationService.pageListVO(userId, readStatus, notificationType, operationType, page));
    }


    @ApiOperation(value = "创建通知异步版。服务内调用")
    @PostMapping("/createAsync")
    public DataResponseBody createAsync(@RequestParam Long toUserId,
                                         @RequestParam Integer notificationType,
                                         @RequestParam String operationType,
                                         @RequestBody String payload) {
        try {
            notificationService.createNotification(toUserId, notificationType, operationType, payload);
        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, "创建消息失败");
        }

        return new DataResponseBody<>(ResponseCode.SUCCESS, "创建消息成功");
    }

}


