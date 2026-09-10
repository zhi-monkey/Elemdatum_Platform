<template>
  <div :class="prefixCls">
    <Popover
      title=""
      trigger="click"
      :overlayClassName="`${prefixCls}__overlay`"
      @visibleChange="handleVisibleChange"
    >
      <Badge :count="unreadCount" :numberStyle="numberStyle" :overflowCount="99">
        <BellOutlined />
      </Badge>
      <template #content>
        <div :class="`${prefixCls}__content`">
          <div :class="`${prefixCls}__header`">
            <span :class="`${prefixCls}__title`">
              <BellOutlined :class="`${prefixCls}__header-icon`" />
              消息通知
            </span>
            <a
              v-if="notifications.length > 0"
              :class="`${prefixCls}__clear`"
              @click="handleMarkAllRead"
            >
              全部已读
            </a>
          </div>
          <div :class="`${prefixCls}__filter`">
            <RadioGroup v-model:value="readStatusFilter" @change="handleFilterChange" size="small">
              <RadioButton :value="undefined">全部</RadioButton>
              <RadioButton :value="0">未读</RadioButton>
              <RadioButton :value="1">已读</RadioButton>
            </RadioGroup>
          </div>
          <div :class="`${prefixCls}__list`">
            <Spin :spinning="loading">
              <Empty
                v-if="notifications.length === 0 && !loading"
                :description="
                  readStatusFilter === 0
                    ? '暂无未读消息'
                    : readStatusFilter === 1
                    ? '暂无已读消息'
                    : '暂无消息'
                "
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
                :class="`${prefixCls}__empty`"
              />
              <NoticeList
                v-else
                :list="notifications"
                :pageSize="10"
                @title-click="onNoticeClick"
              />
            </Spin>
          </div>
          <div :class="`${prefixCls}__footer`">
            <Pagination
              v-model:current="currentPage"
              v-model:pageSize="pageSize"
              :total="total"
              :showSizeChanger="false"
              :showQuickJumper="false"
              size="small"
              simple
              @change="handlePageChange"
              :class="`${prefixCls}__pagination`"
            />
          </div>
        </div>
      </template>
    </Popover>
    <Modal
      v-model:visible="detailModalVisible"
      title="消息详情"
      :width="700"
      :class="`${prefixCls}__modal`"
    >
      <template #footer>
        <div style="display: flex; justify-content: right; align-items: center">
          <Button
            v-if="selectedNotification?.url"
            type="primary"
            @click="handleGoToTask"
            style="background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border: none"
          >
            前往详情
          </Button>
          <div v-else></div>
          <Button @click="detailModalVisible = false">关闭</Button>
        </div>
      </template>
      <div v-if="selectedNotification" :class="`${prefixCls}__detail`">
        <div :class="`${prefixCls}__detail-item`">
          <span :class="`${prefixCls}__detail-label`">消息内容:</span>
          <span :class="`${prefixCls}__detail-value`">{{ selectedNotification.title }}</span>
        </div>
        <div :class="`${prefixCls}__detail-item`">
          <span :class="`${prefixCls}__detail-label`">消息类型:</span>
          <span :class="`${prefixCls}__detail-value`">
            <Tag :color="getTypeColor(selectedNotification.type)">
              {{ getNotificationType(selectedNotification.type) }}
            </Tag>
          </span>
        </div>
        <div :class="`${prefixCls}__detail-item`">
          <span :class="`${prefixCls}__detail-label`">创建时间:</span>
          <span :class="`${prefixCls}__detail-value`">{{
            selectedNotification.fullDatetime || selectedNotification.datetime
          }}</span>
        </div>
        <div :class="`${prefixCls}__detail-item`">
          <span :class="`${prefixCls}__detail-label`">读取状态:</span>
          <span :class="`${prefixCls}__detail-value`">
            <Tag :color="selectedNotification.readStatus === 0 ? 'orange' : 'green'">
              {{ selectedNotification.readStatus === 0 ? '未读' : '已读' }}
            </Tag>
          </span>
        </div>
        <!--        <div :class="`${prefixCls}__detail-item`" style="display: block">-->
        <!--          <span :class="`${prefixCls}__detail-label`">完整Payload:</span>-->
        <!--          <pre :class="`${prefixCls}__detail-payload`">{{-->
        <!--            formatPayload(selectedNotification.payload)-->
        <!--          }}</pre>-->
        <!--        </div>-->
        <div v-if="selectedNotification.url" :class="`${prefixCls}__detail-item`">
          <span :class="`${prefixCls}__detail-label`">跳转链接:</span>
          <span :class="`${prefixCls}__detail-value`" style="color: #3b82f6; word-break: break-all">
            {{ selectedNotification.url }}
          </span>
        </div>
      </div>
    </Modal>
  </div>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import {
    Badge,
    Button,
    Empty,
    Modal,
    Pagination,
    Popover,
    Radio,
    Spin,
    Tag,
  } from 'ant-design-vue';
  import { BellOutlined } from '@ant-design/icons-vue';
  import NoticeList from './NoticeList.vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useNotification } from '/@/hooks/business/useNotification';
  import {
    markAllRead as markAllReadApi,
    markRead,
    type NotificationVO,
    pageFormatted,
    getLatestUnread,
  } from '/@/api/sys/notification';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import { useGo } from '/@/hooks/web/usePage';
  import { h } from 'vue';
  import { useRouter } from 'vue-router';

  export default defineComponent({
    components: {
      Popover,
      BellOutlined,
      Badge,
      NoticeList,
      Empty,
      Spin,
      Radio: Radio,
      RadioGroup: Radio.Group,
      RadioButton: Radio.Button,
      Pagination,
      Modal,
      Tag,
      Button,
    },
    setup() {
      const { prefixCls } = useDesign('header-notify');
      const { createMessage, notification } = useMessage();
      const {
        unreadCount,
        refreshUnread,
        setOnNewMessage,
        getShownNotificationIds,
        addShownNotificationId,
      } = useNotification(30);

      const notifications = ref<any[]>([]);
      const loading = ref(false);
      const currentPage = ref(1);
      const pageSize = ref(10);
      const total = ref(0);
      const readStatusFilter = ref<number | undefined>(undefined);
      const detailModalVisible = ref(false);
      const selectedNotification = ref<any>(null);
      const go = useGo();
      const router = useRouter();

      // 格式化时间
      function formatTime(time: string) {
        if (!time) return '';
        try {
          // 如果后端返回的是本地时间字符串（没有时区信息），需要特殊处理
          // 方案1：如果后端返回格式是 "YYYY-MM-DD HH:mm:ss"，直接替换空格为T
          let dateStr = time;
          if (time.includes(' ') && !time.includes('T')) {
            dateStr = time.replace(' ', 'T');
          }

          // 创建日期对象并加上8小时的时区偏移
          const date = new Date(dateStr);
          const offsetDate = new Date(date.getTime() + 8 * 60 * 60 * 1000);

          const now = new Date();
          const diff = now.getTime() - offsetDate.getTime();
          const minute = 60 * 1000;
          const hour = 60 * minute;
          const day = 24 * hour;

          if (diff < 0) {
            // 如果时间在未来，可能是时区问题，直接格式化显示
            return formatToDateTime(offsetDate);
          }

          if (diff < minute) {
            return '刚刚';
          } else if (diff < hour) {
            return `${Math.floor(diff / minute)}分钟前`;
          } else if (diff < day) {
            return `${Math.floor(diff / hour)}小时前`;
          } else if (diff < 7 * day) {
            return `${Math.floor(diff / day)}天前`;
          } else {
            return formatToDateTime(offsetDate);
          }
        } catch {
          return time;
        }
      }

      // 格式化完整日期时间
      function formatFullDateTime(time: string) {
        if (!time) return '';
        try {
          let dateStr = time;
          if (time.includes(' ') && !time.includes('T')) {
            dateStr = time.replace(' ', 'T');
          }
          const date = new Date(dateStr);
          const offsetDate = new Date(date.getTime());
          return formatToDateTime(offsetDate);
        } catch {
          return time;
        }
      }

      // 转换 API 数据为 NoticeList 组件需要的格式
      function transformNotification(item: NotificationVO) {
        return {
          id: String(item.id),
          avatar: '',
          title: item.message,
          datetime: formatTime(item.createTime),
          fullDatetime: formatFullDateTime(item.createTime),
          type: String(item.notificationType),
          description: '',
          extra: '',
          color: '',
          payload: item.payload,
          operationType: item.operationType,
          readStatus: item.readStatus,
          url: item.url,
        };
      }

      // 获取消息类型名称
      function getNotificationType(type: string) {
        const typeMap: Record<string, string> = {
          '0': 'INFO',
          '2': 'WARNING',
          '3': 'ERROR',
        };
        return typeMap[type] || type;
      }

      // 获取消息类型颜色
      function getTypeColor(type: string) {
        const colorMap: Record<string, string> = {
          '0': 'blue',
          '1': 'green',
          '2': 'orange',
          '3': 'red',
        };
        return colorMap[type] || 'default';
      }

      // 格式化 payload
      function formatPayload(payload: string | undefined) {
        if (!payload) return '无';
        try {
          return JSON.stringify(JSON.parse(payload), null, 2);
        } catch {
          return payload;
        }
      }

      // 加载通知列表
      async function loadNotifications() {
        try {
          loading.value = true;
          const result = await pageFormatted({
            current: currentPage.value,
            size: pageSize.value,
            readStatus: readStatusFilter.value,
          });

          if (result) {
            const transformed = (result.records || []).map(transformNotification);
            notifications.value = transformed;
            total.value = result.total || 0;
          }
        } catch (error) {
          console.error('Failed to load notifications:', error);
        } finally {
          loading.value = false;
        }
      }

      // 下拉框显示/隐藏
      function handleVisibleChange(visible: boolean) {
        if (visible) {
          currentPage.value = 1;
          loadNotifications();
        }
      }

      // 筛选条件变化
      function handleFilterChange() {
        currentPage.value = 1;
        loadNotifications();
      }

      // 分页变化
      function handlePageChange() {
        loadNotifications();
      }

      // 点击通知项
      async function onNoticeClick(record: any) {
        selectedNotification.value = record;
        detailModalVisible.value = true;

        // 如果是未读消息，标记为已读
        if (record.readStatus === 0) {
          try {
            await markRead(Number(record.id));
            refreshUnread();
            loadNotifications();
          } catch (error) {
            console.error('Failed to mark as read:', error);
          }
        }
      }

      // 标记全部已读
      async function handleMarkAllRead() {
        try {
          await markAllReadApi();
          createMessage.success('已全部标记为已读');
          refreshUnread();
          loadNotifications();
        } catch (error) {
          createMessage.error('操作失败');
        }
      }

      // 前往任务界面
      function handleGoToTask() {
        if (selectedNotification.value?.url) {
          const targetUrl = selectedNotification.value.url;
          // 关闭弹窗
          detailModalVisible.value = false;
          // 跳转到指定 URL
          if (targetUrl === 'mineai/system/user/index') {
            router.push({ name: 'SystemUserList' }).catch(console.error);
            return;
          }
          go(targetUrl);
        }
      }

      // 显示新消息通知弹窗
      async function showNewMessageNotification(newCount: number) {
        try {
          // 获取最新的未读消息
          const result = await getLatestUnread(Math.min(newCount * 2, 50));
          if (!result || !result.records || result.records.length === 0) {
            return;
          }

          const shownIds = getShownNotificationIds();

          // 过滤出未显示过的消息
          const newMessages = result.records.filter((item: NotificationVO) => {
            return !shownIds.has(item.id);
          });

          if (newMessages.length === 0) {
            return;
          }

          // 只显示最新的newCount条消息
          const messagesToShow = newMessages.slice(0, newCount);

          // 将这些消息ID标记为已显示
          messagesToShow.forEach((item: NotificationVO) => {
            addShownNotificationId(item.id);
          });

          const batchSize = 3; // 每批最多显示3条
          const batchCount = Math.ceil(messagesToShow.length / batchSize); // 总共需要几批

          // 分批显示消息
          for (let batchIndex = 0; batchIndex < batchCount; batchIndex++) {
            const batchDelay = batchIndex * 5500; // 每批间隔5.5秒（5秒显示时间 + 0.5秒缓冲）
            const startIndex = batchIndex * batchSize;
            const endIndex = Math.min(startIndex + batchSize, messagesToShow.length);
            const batchMessages = messagesToShow.slice(startIndex, endIndex);

            // 显示这一批的消息
            batchMessages.forEach((item: NotificationVO, indexInBatch: number) => {
              setTimeout(() => {
                const notificationType = getNotificationType(String(item.notificationType));
                const typeColor = getTypeColor(String(item.notificationType));

                // 根据消息类型选择通知方法
                let notifyMethod = notification.info;
                if (item.notificationType === 3) {
                  notifyMethod = notification.error;
                } else if (item.notificationType === 2) {
                  notifyMethod = notification.warning;
                } else if (item.notificationType === 1) {
                  notifyMethod = notification.success;
                }

                notifyMethod({
                  message: h('div', { style: 'font-weight: 600; font-size: 14px;' }, [
                    h(
                      'span',
                      {
                        style: `color: ${
                          typeColor === 'blue'
                            ? '#3b82f6'
                            : typeColor === 'orange'
                            ? '#f97316'
                            : typeColor === 'red'
                            ? '#ef4444'
                            : '#10b981'
                        }; margin-right: 8px;`,
                      },
                      `[${notificationType}]`,
                    ),
                    '新消息通知',
                  ]),
                  description: h('div', { style: 'margin-top: 8px;' }, [
                    h(
                      'div',
                      { style: 'margin-bottom: 4px; color: #e2e8f0; line-height: 1.5;' },
                      item.message,
                    ),
                    h(
                      'div',
                      { style: 'font-size: 12px; color: #94a3b8; margin-top: 8px;' },
                      formatTime(item.createTime),
                    ),
                  ]),
                  duration: 5,
                  placement: 'topRight',
                  class: 'notification-closable',
                  style: {
                    width: '380px',
                    background: 'linear-gradient(135deg, #1a2332 0%, #151d28 100%)',
                    border: '1px solid rgba(59, 130, 246, 0.3)',
                    borderRadius: '8px',
                    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
                  },
                });
              }, batchDelay + indexInBatch * 300); // 批次延迟 + 批内延迟300ms
            });
          }
        } catch (error) {
          console.error('Failed to show new message notification:', error);
        }
      }

      // 设置新消息回调
      setOnNewMessage((newCount: number) => {
        showNewMessageNotification(newCount);
      });

      return {
        prefixCls,
        notifications,
        loading,
        unreadCount,
        Empty,
        currentPage,
        pageSize,
        total,
        readStatusFilter,
        detailModalVisible,
        selectedNotification,
        onNoticeClick,
        handleVisibleChange,
        handleMarkAllRead,
        handleFilterChange,
        handlePageChange,
        handleGoToTask,
        getNotificationType,
        getTypeColor,
        formatPayload,
        numberStyle: {},
        showNewMessageNotification,
      };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-header-notify';

  .@{prefix-cls} {
    display: inline-flex;
    align-items: center;
    height: 100%;
    padding: 0 16px 0 12px;
    cursor: pointer;
    position: relative;
    overflow: visible !important;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
    }

    &__overlay {
      .ant-popover-inner {
        padding: 0;
        background: #1a2332;
        border-radius: 12px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
        overflow: hidden;
      }

      .ant-popover-arrow {
        display: none;
      }
    }

    &__content {
      width: 420px;
      max-height: 620px;
      background: linear-gradient(180deg, #1a2332 0%, #151d28 100%);
      border-radius: 12px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    &__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid rgba(59, 130, 246, 0.15);
      background: linear-gradient(135deg, rgba(30, 58, 138, 0.3) 0%, rgba(59, 130, 246, 0.1) 100%);
      backdrop-filter: blur(10px);
    }

    &__filter {
      padding: 16px 24px;
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(10px);
      display: flex;
      justify-content: center;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 200;
      border-bottom: 1px solid rgba(59, 130, 246, 0.1);
      flex-shrink: 0;
      min-height: 60px;
      overflow: visible;

      // 修复 RadioGroup 布局问题
      .ant-radio-group {
        display: flex;
        width: 100%;
        max-width: 280px;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
        background: rgba(30, 41, 59, 0.8);
        border: 1px solid rgba(59, 130, 246, 0.2);
      }

      .ant-radio-button-wrapper {
        flex: 1;
        min-width: 0;
        background: transparent;
        border: none !important;
        color: #94a3b8;
        padding: 8px 4px;
        font-size: 13px;
        font-weight: 500;
        transition: all 0.3s ease;
        position: relative;
        text-align: center;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        display: flex;
        align-items: center;
        justify-content: center;
        height: 32px;
        line-height: 1;

        // 移除 Ant Design 默认的分隔线
        &::before {
          display: none !important;
        }

        // 自定义按钮间的分隔线
        &:not(:last-child)::after {
          content: '';
          position: absolute;
          right: 0;
          top: 25%;
          height: 50%;
          width: 1px;
          background: rgba(59, 130, 246, 0.15);
          z-index: 1;
        }

        &:hover:not(.ant-radio-button-wrapper-checked) {
          color: #60a5fa;
          background: rgba(59, 130, 246, 0.15);
        }

        &-checked {
          background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
          border-color: transparent !important;
          color: #fff !important;
          box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
          z-index: 2;

          // 选中状态下隐藏分隔线
          &::after {
            display: none;
          }

          &:hover {
            background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%) !important;
          }
        }

        // 确保内部文字正确显示
        .ant-radio-button {
          display: none;
        }

        // 修复内部 span 元素的样式
        > span:not(.ant-radio-button) {
          display: block;
          width: 100%;
          text-align: center;
          padding: 0 8px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          line-height: 1.2;
        }

        // 第一个按钮的圆角
        &:first-child {
          border-top-left-radius: 6px !important;
          border-bottom-left-radius: 6px !important;
        }

        // 最后一个按钮的圆角
        &:last-child {
          border-top-right-radius: 6px !important;
          border-bottom-right-radius: 6px !important;
        }
      }

      // 修复禁用状态
      .ant-radio-button-wrapper-disabled {
        background: rgba(15, 23, 42, 0.5) !important;
        color: #64748b !important;
        cursor: not-allowed;

        &:hover {
          background: rgba(15, 23, 42, 0.5) !important;
          color: #64748b !important;
        }
      }
    }

    &__header-icon {
      margin-right: 8px;
      font-size: 18px;
      color: #3b82f6;
      animation: bellRing 2s ease-in-out infinite;
    }

    &__title {
      font-size: 17px;
      font-weight: 600;
      color: #e2e8f0;
      display: flex;
      align-items: center;
      letter-spacing: 0.3px;
    }

    &__clear {
      font-size: 13px;
      color: #3b82f6;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 6px;
      background: rgba(59, 130, 246, 0.1);
      transition: all 0.3s ease;
      font-weight: 500;

      &:hover {
        color: #60a5fa;
        background: rgba(59, 130, 246, 0.2);
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }
    }

    &__list {
      flex: 1;
      overflow-y: auto;
      overflow-x: hidden;
      background: #151d28;
      padding: 8px 16px 8px 0;
      position: relative;
      z-index: 1;

      &::-webkit-scrollbar {
        width: 8px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.2);
        border-radius: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
        border-radius: 4px;
        transition: background 0.3s ease;

        &:hover {
          background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%);
        }
      }
    }

    &__empty {
      padding: 60px 0;
      color: #64748b;

      .ant-empty-description {
        color: #64748b;
        font-size: 14px;
      }

      .ant-empty-image {
        opacity: 0.5;
      }
    }

    &__footer {
      padding: 16px 24px;
      text-align: center;
      border-top: 1px solid rgba(59, 130, 246, 0.15);
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(10px);
      display: flex;
      justify-content: center;
      align-items: center;
      flex-shrink: 0;
      position: sticky;
      bottom: 0;
      z-index: 200;
    }

    &__pagination {
      .ant-pagination-simple-pager {
        color: #e2e8f0;
        margin: 0 12px;

        input {
          background: rgba(30, 41, 59, 0.8);
          border: 1px solid rgba(59, 130, 246, 0.3);
          color: #e2e8f0;
          border-radius: 6px;
          padding: 4px 8px;
          text-align: center;
          transition: all 0.3s ease;

          &:hover {
            border-color: #3b82f6;
            background: rgba(59, 130, 246, 0.1);
          }

          &:focus {
            border-color: #3b82f6;
            box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
            background: rgba(59, 130, 246, 0.15);
          }
        }
      }

      .ant-pagination-prev,
      .ant-pagination-next {
        .ant-pagination-item-link {
          background: rgba(30, 41, 59, 0.8);
          border: 1px solid rgba(59, 130, 246, 0.3);
          color: #94a3b8;
          border-radius: 6px;
          transition: all 0.3s ease;

          &:hover {
            background: rgba(59, 130, 246, 0.2);
            border-color: #3b82f6;
            color: #60a5fa;
            transform: translateY(-1px);
          }
        }

        &.ant-pagination-disabled .ant-pagination-item-link {
          background: rgba(15, 23, 42, 0.5);
          border-color: rgba(59, 130, 246, 0.1);
          color: #475569;
          cursor: not-allowed;
          opacity: 0.5;

          &:hover {
            transform: none;
          }
        }
      }

      .ant-pagination-slash {
        color: #64748b;
        margin: 0 8px;
      }
    }

    &__modal {
      .ant-modal-content {
        background: linear-gradient(180deg, #1a2332 0%, #151d28 100%);
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
      }

      .ant-modal-header {
        background: linear-gradient(
          135deg,
          rgba(30, 58, 138, 0.3) 0%,
          rgba(59, 130, 246, 0.1) 100%
        );
        border-bottom: 1px solid rgba(59, 130, 246, 0.2);
        border-radius: 12px 12px 0 0;
        padding: 20px 24px;
        backdrop-filter: blur(10px);

        .ant-modal-title {
          color: #e2e8f0;
          font-weight: 600;
          font-size: 16px;
        }
      }

      .ant-modal-body {
        background: #151d28;
        padding: 32px 36px;
      }

      .ant-modal-footer {
        padding: 16px 24px;
        border-top: 1px solid rgba(30, 64, 175, 0.5);
        background: rgba(15, 23, 42, 0.98);
      }
    }

    &__detail {
      color: #e2e8f0;
      padding: 20px;
    }

    &__detail-item {
      margin-bottom: 24px;
      padding: 0;
      display: flex;
      align-items: flex-start;

      &:last-child {
        margin-bottom: 0;
      }
    }

    &__detail-label {
      font-weight: 600;
      color: #94a3b8;
      min-width: 96px;
      margin-right: 20px;
      font-size: 14px;
      line-height: 1.6;
      flex-shrink: 0;
    }

    &__detail-value {
      flex: 1;
      color: #e2e8f0;
      word-break: break-word;
      font-size: 14px;
      line-height: 1.6;
    }

    &__detail-payload {
      margin-top: 12px; // 增加与标签的间距
      padding: 20px; // 增加内边距
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid rgba(59, 130, 246, 0.25);
      border-radius: 8px;
      color: #94a3b8;
      font-size: 12px;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      line-height: 1.6;
      overflow-x: auto;
      white-space: pre-wrap;
      word-wrap: break-word;
      max-height: 400px;
      overflow-y: auto;

      &::-webkit-scrollbar {
        width: 8px;
        height: 8px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.3);
        border-radius: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
        border-radius: 4px;

        &:hover {
          background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%);
        }
      }
    }

    .ant-badge {
      font-size: 18px;
      line-height: 1;
      display: inline-flex;
      align-items: center;
      position: relative;

      .ant-badge-count {
        box-shadow: 0 0 0 2px #1a2332;
        position: absolute;
        top: -2px;
        right: -4px;
        transform: none;
        z-index: 10;
        min-width: 18px;
        height: 18px;
        padding: 0 5px;
        font-size: 11px;
        line-height: 18px;
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        border-radius: 9px;
        font-weight: 600;
        animation: badgePulse 2s ease-in-out infinite;
      }

      svg {
        width: 1em;
        height: 1em;
        color: #94a3b8;
        transition: all 0.3s ease;
      }

      &:hover svg {
        color: #3b82f6;
        transform: scale(1.1);
      }
    }

    .ant-spin {
      color: #3b82f6;
    }

    .ant-spin-dot-item {
      background-color: #3b82f6;
    }
  }

  @keyframes bellRing {
    0%,
    100% {
      transform: rotate(0deg);
    }
    10%,
    30% {
      transform: rotate(-10deg);
    }
    20%,
    40% {
      transform: rotate(10deg);
    }
    50% {
      transform: rotate(0deg);
    }
  }

  @keyframes badgePulse {
    0%,
    100% {
      transform: scale(1);
      box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
    }
    50% {
      transform: scale(1.05);
      box-shadow: 0 0 0 4px rgba(239, 68, 68, 0);
    }
  }

  // 自定义 NoticeList 样式
  .@{prefix-cls}__overlay {
    .ant-list-item {
      padding: 16px 24px !important;
      margin-right: 8px !important;
      border-bottom: 1px solid rgba(59, 130, 246, 0.1) !important;
      background: transparent !important;
      transition: all 0.3s ease !important;
      cursor: pointer;
      position: relative;
      overflow: visible !important;

      .ant-list-item-meta {
        overflow: visible !important;
      }

      .ant-list-item-meta-content {
        overflow: visible !important;
      }

      .unread-indicator {
        display: block !important;
        visibility: visible !important;
        opacity: 1 !important;
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 3px;
        height: 100%;
        background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
        transform: scaleY(0);
        transition: transform 0.3s ease;
      }

      &:hover {
        background: rgba(59, 130, 246, 0.08) !important;
        transform: translateX(4px);

        &::before {
          transform: scaleY(1);
        }
      }

      &:active {
        transform: translateX(2px);
      }

      &:last-child {
        border-bottom: none !important;
      }
    }

    .ant-list-item-meta-title {
      color: #e2e8f0 !important;
      font-size: 14px !important;
      font-weight: 500 !important;
      margin-bottom: 6px !important;
      line-height: 1.5 !important;
    }

    .ant-list-item-meta-description {
      color: #94a3b8 !important;
      font-size: 12px !important;
    }

    .ant-list-item-meta-avatar {
      margin-right: 16px !important;
    }
  }

  // 自定义通知弹窗样式 - 允许关闭但不延长显示时间
  .notification-closable {
    // 允许鼠标交互（可以点击关闭按钮）
    pointer-events: auto !important;

    // 关闭按钮样式
    .ant-notification-notice-close {
      pointer-events: auto !important;
      opacity: 1 !important;
      cursor: pointer !important;

      &:hover {
        opacity: 0.8 !important;
      }
    }

    // 防止鼠标悬停延长显示时间
    // Ant Design 的通知组件在鼠标悬停时会暂停倒计时
    // 我们通过 CSS 无法完全禁用这个行为，但可以让内容区域不响应鼠标事件
    .ant-notification-notice-content {
      pointer-events: none !important;
    }

    // 确保关闭按钮可以交互
    .ant-notification-notice-close {
      pointer-events: auto !important;
    }
  }
</style>
