<template>
  <a-modal
    v-model:visible="visible"
    title="日志"
    :width="'90vw'"
    :footer="null"
    @cancel="handleCancel"
    :bodyStyle="{
      height: '80vh',
      maxHeight: '80vh',
      overflow: 'auto',
      padding: '12px',
    }"
  >
    <div class="log-container">
      <!-- 工具栏 -->
      <div class="log-toolbar">
        <div class="toolbar-left">
          <a-button
            :type="isStreaming ? 'default' : 'primary'"
            size="small"
            @click="toggleStreaming"
            :icon="h(CaretRightOutlined)"
          >
            {{ isStreaming ? '停止' : '开始' }}
          </a-button>
          <a-button
            size="small"
            @click="refreshLogs"
            :icon="h(ReloadOutlined)"
            :loading="refreshing"
          >
            刷新
          </a-button>
          <a-button size="small" @click="clearLogs" :icon="h(DeleteOutlined)"> 清空 </a-button>
        </div>
        <div class="toolbar-right">
          <span class="status-text">
            <a-badge
              :status="isStreaming ? 'processing' : 'default'"
              :text="isStreaming ? '实时日志中...' : '已停止'"
            />
          </span>
          <span class="log-count">{{ displayedLogLines.length }} 行</span>
          <span class="line-range"> 行号: 1-{{ displayedLogLines.length }} </span>
        </div>
      </div>

      <!-- 日志显示区域 -->
      <div class="log-content" ref="logContentRef" @scroll="handleScroll">
        <!-- 加载更多按钮 - 非实时模式下显示 -->
        <div v-if="showLoadMoreButton" class="load-more-container">
          <a-button
            size="small"
            type="primary"
            @click="loadMoreLogs"
            :loading="loadingMore"
            :disabled="!hasMoreHistory"
            :icon="h(CaretUpOutlined)"
          >
            {{ hasMoreHistory ? '加载更多' : '已加载全部' }}
          </a-button>
        </div>

        <div class="log-lines">
          <div
            v-for="(log, index) in displayedLogLines"
            :key="log.id"
            class="log-line"
            :class="getLogLevelClass(log.level)"
          >
            <span class="log-line-number">{{ index + 1 }}</span>
            <span class="log-timestamp">{{ log.timestamp }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="displayedLogLines.length === 0" class="empty-logs">
            <div class="empty-text">暂无日志数据</div>
            <div class="empty-hint">点击"开始"按钮获取实时日志</div>
          </div>
        </div>
      </div>

      <!-- 底部信息栏 -->
      <div class="log-footer">
        <span class="footer-info">自动滚动: {{ isAutoScrolling ? '开启' : '关闭' }}</span>
        <span class="footer-info">编码: UTF-8</span>
        <span class="footer-info">最后更新: {{ lastUpdateTime }}</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { ref, nextTick, onUnmounted, h, onMounted } from 'vue';
  import { Modal as AModal, Button as AButton, Badge as ABadge } from 'ant-design-vue';
  import {
    CaretRightOutlined,
    PauseOutlined,
    ReloadOutlined,
    DeleteOutlined,
    CaretUpOutlined,
  } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  interface LogEntry {
    id: string;
    timestamp: string;
    level: 'info' | 'warn' | 'error' | 'debug';
    message: string;
  }

  interface Props {
    visible: boolean;
    jobId: number;
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void;
  }

  const props = defineProps<Props>();
  const emit = defineEmits<Emits>();

  const logContentRef = ref<HTMLElement>();
  const isStreaming = ref(false);
  const refreshing = ref(false);
  const loadingMore = ref(false);
  const displayedLogLines = ref<LogEntry[]>([]);
  const lastUpdateTime = ref('--');
  const showLoadMoreButton = ref(false);
  const isAutoScrolling = ref(true);
  const hasMoreHistory = ref(true);

  let streamingTimer: NodeJS.Timeout | null = null;

  // 安全的哈希函数
  const simpleHash = (str: string): string => {
    let hash = 0;
    if (str.length === 0) return hash.toString();

    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash = hash & hash;
    }

    return Math.abs(hash).toString();
  };

  // 生成日志唯一ID
  const generateLogId = (message: string, timestamp: string): string => {
    const randomSuffix = Math.random().toString(36).substr(2, 9);
    const messageHash = simpleHash(message + timestamp).toString();
    return `${messageHash}-${randomSuffix}`.replace(/[^a-zA-Z0-9\-]/g, '');
  };

  // 解析日志级别
  const parseLogLevel = (message: string): 'info' | 'warn' | 'error' | 'debug' => {
    const lowerMessage = message.toLowerCase();
    if (lowerMessage.includes('error') || lowerMessage.includes('错误')) return 'error';
    if (lowerMessage.includes('warn') || lowerMessage.includes('警告')) return 'warn';
    if (lowerMessage.includes('debug') || lowerMessage.includes('调试')) return 'debug';
    return 'info';
  };

  // 强制滚动到底部
  const forceScrollToBottom = () => {
    nextTick(() => {
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
        isAutoScrolling.value = true;
      }
    });
  };

  const scrollToBottom = () => {
    if (!isAutoScrolling.value) return;
    nextTick(() => {
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
      }
    });
  };

  const handleScroll = () => {
    if (!logContentRef.value) return;

    // 实时模式下保持自动滚动开启，不受手动滚动影响
    if (isStreaming.value) {
      isAutoScrolling.value = true;
      return;
    }

    // 非实时模式下才根据滚动位置判断是否自动滚动
    const element = logContentRef.value;
    const isNearBottom = element.scrollTop + element.clientHeight >= element.scrollHeight - 50;
    isAutoScrolling.value = isNearBottom;
  };

  // 更新加载更多按钮显示状态
  const updateLoadMoreButtonVisibility = () => {
    // 只在非实时状态下显示加载更多按钮
    showLoadMoreButton.value =
      !isStreaming.value && displayedLogLines.value.length > 0 && hasMoreHistory.value;
  };

  // 获取日志数据的通用方法
  const fetchLogs = async (lastTimestamp?: string): Promise<LogEntry[]> => {
    // 如果 jobId 无效，则不调用接口
    if (!props.jobId || props.jobId <= 0) {
      return [];
    }
    try {
      const params: any = {
        jobId: props.jobId,
        maxLines: 800,
      };

      // 如果有lastTimestamp，添加到参数中
      if (lastTimestamp) {
        params.lastTimestamp = lastTimestamp;
      }

      const dashboardData = await maHttp.get(
        {
          url: 'modelJob/getTrainingRawLogs',
          params,
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (!dashboardData || !dashboardData.rawLogs) {
        return [];
      }

      const rawLogs = dashboardData.rawLogs || [];
      console.log('Fetched logs:', rawLogs);

      return rawLogs.map((logEntry: any) => {
        const [timestamp, message] = Object.entries(logEntry)[0] as [string, string];
        return {
          id: generateLogId(message, timestamp),
          timestamp,
          level: parseLogLevel(message),
          message,
        };
      });
    } catch (error) {
      console.error('Error fetching logs:', error);
      return [
        {
          id: generateLogId(`加载日志失败: ${error}`, new Date().toISOString()),
          timestamp: new Date().toLocaleString('zh-CN'),
          level: 'error' as const,
          message: `加载日志失败: ${error}`,
        },
      ];
    }
  };

  // 组件挂载后自动获取一次日志（不开启实时）
  onMounted(() => {
    // 只有当 jobId 有效时才获取日志
    if (props.jobId && props.jobId > 0) {
      refreshLogs();
    }
  });

  // 开始实时获取日志
  const startStreaming = async () => {
    isStreaming.value = true;
    isAutoScrolling.value = true; // 实时模式下强制开启自动滚动

    // 立即获取一次最新日志
    const newLogs = await fetchLogs();
    displayedLogLines.value = newLogs;
    hasMoreHistory.value = newLogs.length === 800; // 如果返回800条，可能还有更多历史
    forceScrollToBottom();
    lastUpdateTime.value = new Date().toLocaleString('zh-CN');
    updateLoadMoreButtonVisibility();

    // 启动定时器，每5秒获取一次
    streamingTimer = setInterval(async () => {
      try {
        const newLogs = await fetchLogs();
        displayedLogLines.value = newLogs;
        // 实时模式下强制滚动到底部
        forceScrollToBottom();
        lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      } catch (error) {
        console.error('Error in streaming:', error);
      }
    }, 5000);
  };

  // 停止实时获取
  const stopStreaming = () => {
    isStreaming.value = false;
    if (streamingTimer) {
      clearInterval(streamingTimer);
      streamingTimer = null;
    }
    updateLoadMoreButtonVisibility();
  };

  // 切换实时获取状态
  const toggleStreaming = () => {
    if (isStreaming.value) {
      stopStreaming();
    } else {
      startStreaming();
    }
  };

  // 刷新日志（只调用一次）
  const refreshLogs = async () => {
    refreshing.value = true;
    try {
      const newLogs = await fetchLogs();
      displayedLogLines.value = newLogs;
      hasMoreHistory.value = newLogs.length === 800; // 如果返回800条，可能还有更多历史
      forceScrollToBottom();
      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      updateLoadMoreButtonVisibility();
    } finally {
      refreshing.value = false;
    }
  };

  // 清空所有日志
  const clearLogs = () => {
    displayedLogLines.value = [];
    lastUpdateTime.value = '--';
    isAutoScrolling.value = true;
    hasMoreHistory.value = true;
    updateLoadMoreButtonVisibility();
  };

  // 加载更多历史日志
  const loadMoreLogs = async () => {
    if (
      loadingMore.value ||
      !hasMoreHistory.value ||
      isStreaming.value ||
      displayedLogLines.value.length === 0
    ) {
      return;
    }

    loadingMore.value = true;

    try {
      // 获取当前显示内容的第一条的时间戳
      const firstLogTimestamp = displayedLogLines.value[0]?.timestamp;

      if (!firstLogTimestamp) {
        hasMoreHistory.value = false;
        updateLoadMoreButtonVisibility();
        return;
      }

      // 使用第一条日志的时间戳获取更早的日志
      const olderLogs = await fetchLogs(firstLogTimestamp);

      if (olderLogs.length === 0) {
        hasMoreHistory.value = false;
        updateLoadMoreButtonVisibility();
        return;
      }

      // 将新获取的历史日志添加到当前显示列表的前面
      displayedLogLines.value = [...olderLogs, ...displayedLogLines.value];

      // 如果返回的日志数量少于800条，说明没有更多历史了
      hasMoreHistory.value = olderLogs.length === 800;

      // 保持滚动位置，让用户看到之前看到的内容
      nextTick(() => {
        if (logContentRef.value) {
          const lineHeight = 24;
          const addedHeight = olderLogs.length * lineHeight;
          logContentRef.value.scrollTop = addedHeight;
        }
      });

      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      updateLoadMoreButtonVisibility();
    } catch (error) {
      console.error('Error loading more logs:', error);
      hasMoreHistory.value = false;
      updateLoadMoreButtonVisibility();
    } finally {
      loadingMore.value = false;
    }
  };

  const getLogLevelClass = (level: string) => {
    return `log-line-${level}`;
  };

  const handleCancel = () => {
    stopStreaming();
    emit('update:visible', false);
  };

  onUnmounted(() => {
    stopStreaming();
  });
</script>

<style scoped lang="less">
  .log-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #1e1e1e;
    border-radius: 4px;
    overflow: hidden;
  }

  .log-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #2d2d2d;
    border-bottom: 1px solid #3d3d3d;

    .toolbar-left {
      display: flex;
      gap: 8px;
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 16px;
      color: #ccc;
      font-size: 12px;

      .status-text {
        color: #ccc;
      }

      .log-count {
        color: #888;
      }

      .line-range {
        color: #888;
        font-family: monospace;
      }
    }
  }

  .log-content {
    flex: 1;
    overflow-y: auto;
    background: #0d1117;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    line-height: 1.5;
    position: relative;

    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #161b22;
    }

    &::-webkit-scrollbar-thumb {
      background: #30363d;
      border-radius: 4px;

      &:hover {
        background: #484f58;
      }
    }
  }

  .load-more-container {
    position: sticky;
    top: 0;
    left: 0;
    right: 0;
    z-index: 10;
    padding: 8px;
    background: rgba(13, 17, 23, 0.9);
    border-bottom: 1px solid #30363d;
    display: flex;
    justify-content: center;
    margin-bottom: 8px;
  }

  .log-lines {
    padding: 8px;
  }

  .log-line {
    display: flex;
    padding: 2px 0;
    border-left: 3px solid transparent;
    padding-left: 8px;
    color: #c9d1d9;
    min-height: 20px;

    &:hover {
      background: rgba(56, 139, 253, 0.1);
    }

    &.log-line-error {
      border-left-color: #f85149;
      background: rgba(248, 81, 73, 0.05);
    }

    &.log-line-warn {
      border-left-color: #d29922;
      background: rgba(210, 153, 34, 0.05);
    }

    &.log-line-info {
      border-left-color: #388bfd;
    }

    &.log-line-debug {
      border-left-color: #7c3aed;
      color: #8b949e;
    }
  }

  .log-line-number {
    color: #6e7681;
    margin-right: 8px;
    min-width: 60px;
    font-size: 11px;
    text-align: right;
    font-family: monospace;
  }

  .log-timestamp {
    color: #6e7681;
    margin-right: 8px;
    min-width: 140px;
    font-size: 11px;
  }

  .log-message {
    white-space: pre-wrap;
    flex: 1;
    word-break: break-word;
  }

  .empty-logs {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 300px;
    color: #6e7681;

    .empty-text {
      font-size: 16px;
      margin-bottom: 8px;
    }

    .empty-hint {
      font-size: 12px;
      color: #484f58;
    }
  }

  .log-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 12px;
    background: #2d2d2d;
    border-top: 1px solid #3d3d3d;
    font-size: 11px;
    color: #8b949e;

    .footer-info {
      &:not(:last-child) {
        margin-right: 16px;
      }
    }
  }

  .log-toolbar :deep(.ant-btn) {
    border-color: #3d3d3d;
    background: #404040;
    color: #ccc;

    &:hover {
      border-color: #1890ff;
      background: #1890ff;
      color: #fff;
    }

    &.ant-btn-primary {
      background: #1890ff;
      border-color: #1890ff;
      color: #fff;

      &:hover {
        background: #40a9ff;
        border-color: #40a9ff;
      }
    }
  }

  .log-toolbar :deep(.ant-badge-status-text) {
    color: #ccc;
  }
</style>
