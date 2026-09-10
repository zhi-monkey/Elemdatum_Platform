<template>
  <a-modal
    v-model:visible="visible"
    title="转换日志"
    :width="'85vw'"
    :footer="null"
    @cancel="handleCancel"
    :bodyStyle="{
      height: '75vh',
      maxHeight: '75vh',
      overflow: 'auto',
      padding: '12px',
    }"
  >
    <div class="log-container">
      <!-- 工具栏 -->
      <div class="log-toolbar">
        <div class="toolbar-left">
          <a-button size="small" @click="refreshLogs" :loading="refreshing">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>
        <div class="toolbar-right">
          <span class="log-count">{{ logLines.length }} 行</span>
          <span class="last-update">最后更新: {{ lastUpdateTime }}</span>
        </div>
      </div>

      <!-- 日志显示区域 -->
      <div class="log-content" ref="logContentRef">
        <div class="log-lines">
          <div v-for="(log, index) in logLines" :key="index" class="log-line">
            <span class="log-line-number">{{ index + 1 }}</span>
            <span class="log-message">{{ log }}</span>
          </div>
          <div v-if="logLines.length === 0" class="empty-logs">
            <div class="empty-text">暂无日志数据</div>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { nextTick, ref, watch } from 'vue';
  import { Button as AButton, Modal as AModal } from 'ant-design-vue';
  import { ReloadOutlined } from '@ant-design/icons-vue';

  interface Props {
    visible: boolean;
    logs: string[];
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void;

    (e: 'refresh'): void;
  }

  const props = defineProps<Props>();
  const emit = defineEmits<Emits>();

  const logContentRef = ref<HTMLElement>();
  const refreshing = ref(false);
  const logLines = ref<string[]>([]);
  const lastUpdateTime = ref('--');

  const scrollToBottom = () => {
    nextTick(() => {
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
      }
    });
  };

  // 监听logs变化
  watch(
    () => props.logs,
    (newLogs) => {
      logLines.value = newLogs || [];
      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      scrollToBottom();
    },
    { immediate: true },
  );

  const refreshLogs = async () => {
    refreshing.value = true;
    emit('refresh');
    setTimeout(() => {
      refreshing.value = false;
    }, 500);
  };

  const handleCancel = () => {
    emit('update:visible', false);
  };
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

      .log-count {
        color: #888;
      }

      .last-update {
        color: #888;
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

  .log-lines {
    padding: 8px;
  }

  .log-line {
    display: flex;
    padding: 2px 0;
    border-left: 3px solid #388bfd;
    padding-left: 8px;
    color: #c9d1d9;
    min-height: 20px;

    &:hover {
      background: rgba(56, 139, 253, 0.1);
    }
  }

  .log-line-number {
    color: #6e7681;
    margin-right: 12px;
    min-width: 50px;
    font-size: 11px;
    text-align: right;
    font-family: monospace;
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
  }
</style>
