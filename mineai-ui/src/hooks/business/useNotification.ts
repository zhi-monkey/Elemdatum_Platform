import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { getUnreadCount, pageFormatted } from '/@/api/sys/notification';

const unreadCount = ref(0);
const previousUnreadCount = ref(0);
let timer: number | null = null;
let onNewMessageCallback: ((newCount: number) => void) | null = null;
let isInitialized = false;
const shownNotificationIds = new Set<number>(); // 记录已显示过的通知ID

export function useNotification(pollSeconds = 60) {
  const hasUnread = computed(() => unreadCount.value > 0);

  // 获取已显示的通知ID集合
  function getShownNotificationIds() {
    return shownNotificationIds;
  }

  // 添加已显示的通知ID
  function addShownNotificationId(id: number) {
    shownNotificationIds.add(id);
  }

  async function refreshUnread() {
    try {
      const newCount = await getUnreadCount();

      // 检测新消息（只在已初始化后才触发通知，避免首次加载时弹出）
      if (isInitialized && newCount > previousUnreadCount.value && previousUnreadCount.value >= 0) {
        const newMessageCount = newCount - previousUnreadCount.value;
        // 触发新消息回调
        if (onNewMessageCallback) {
          onNewMessageCallback(newMessageCount);
        }
      }

      // 更新未读数
      previousUnreadCount.value = unreadCount.value;
      unreadCount.value = newCount;

      // 首次调用后标记为已初始化
      if (!isInitialized) {
        isInitialized = true;
      }
    } catch {
      // 静默失败即可
    }
  }

  function startPolling() {
    stopPolling();
    timer = window.setInterval(refreshUnread, pollSeconds * 1000);
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  function setOnNewMessage(callback: (newCount: number) => void) {
    onNewMessageCallback = callback;
  }

  onMounted(async () => {
    // 首次加载时，获取所有现有的未读消息ID并标记为已显示
    // 这样只有在此之后新增的消息才会触发弹窗
    try {
      // 先获取未读消息总数
      const count = await getUnreadCount();
      if (count > 0) {
        // 使用实际的未读数来获取所有未读消息
        const result = await pageFormatted({
          current: 1,
          size: count,
          readStatus: 0, // 只获取未读消息
        });
        if (result && result.records) {
          result.records.forEach((item) => {
            shownNotificationIds.add(item.id);
          });
        }
      }
    } catch (error) {
      console.error('Failed to initialize shown notification IDs:', error);
    }

    await refreshUnread();
    startPolling();
  });

  onBeforeUnmount(stopPolling);

  return {
    unreadCount,
    hasUnread,
    refreshUnread,
    startPolling,
    stopPolling,
    setOnNewMessage,
    getShownNotificationIds,
    addShownNotificationId,
  };
}
