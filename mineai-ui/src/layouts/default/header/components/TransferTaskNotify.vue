<template>
  <Popover
    trigger="click"
    placement="bottomRight"
    overlayClassName="transfer-task-popover"
    @visibleChange="handleVisibleChange"
  >
    <span class="transfer-task-trigger" title="传输任务">
      <CloudUploadOutlined />
    </span>
    <template #content>
      <div class="transfer-task-panel">
        <div class="panel-title">传输任务</div>
        <Spin :spinning="loading">
          <Empty
            v-if="tasks.length === 0 && !loading"
            description="暂无传输任务"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
          />
          <div v-for="task in tasks" :key="task.id" class="task-item" @click="openTask(task)">
            <div class="task-line">
              <span class="task-name">{{ task.taskName }}</span>
              <Tag :color="statusColor(task.status)">{{ statusLabel(task.status) }}</Tag>
            </div>
            <Progress
              :percent="task.progress || 0"
              :status="
                task.status === 'FAILED'
                  ? 'exception'
                  : task.status === 'COMPLETED'
                  ? 'success'
                  : 'active'
              "
              size="small"
            />
          </div>
        </Spin>
        <div class="panel-footer" @click="openTaskCenter">查看全部</div>
      </div>
    </template>
  </Popover>
</template>

<script lang="ts" setup>
  import { ref, onUnmounted } from 'vue';
  import { CloudUploadOutlined } from '@ant-design/icons-vue';
  import { Empty, Popover, Progress, Spin, Tag } from 'ant-design-vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { getImportTransferTasks } from '/@/api/mineai/importTransferTask';
  import { getTransferExportTasks } from '/@/api/mineai/transferExportTask';
  import { usePermissionStore } from '/@/store/modules/permission';

  const go = useGo();
  const permissionStore = usePermissionStore();
  const tasks = ref<any[]>([]);
  const loading = ref(false);
  const isFirstLoad = ref(true);
  let pollingTimer: ReturnType<typeof setInterval> | null = null;

  const statusLabel = (value?: string) =>
    ((
      {
        PROCESSING: '进行中',
        COMPLETED: '已完成',
        FAILED: '失败',
        CANCELLED: '已取消',
        PENDING: '等待中',
      } as any
    )[value || ''] ||
    value ||
    '-');
  const statusColor = (value?: string) =>
    ((
      {
        PROCESSING: 'blue',
        COMPLETED: 'green',
        FAILED: 'red',
        CANCELLED: 'default',
        PENDING: 'orange',
      } as any
    )[value || ''] || 'default');
  async function loadTasks() {
    if (isFirstLoad.value) {
      loading.value = true;
    }
    try {
      const [imports, exports] = await Promise.all([
        getImportTransferTasks({ current: 1, size: 6 }),
        getTransferExportTasks({ current: 1, size: 6 }),
      ]);
      tasks.value = [
        ...((imports as any)?.result || []).map((task: any) => ({
          ...task,
          transferType: 'import',
        })),
        ...((exports as any)?.result || []).map((task: any) => ({
          ...task,
          transferType: 'export',
        })),
      ]
        .sort((a, b) => Number(new Date(b.createTime || 0)) - Number(new Date(a.createTime || 0)))
        .slice(0, 6);
    } finally {
      if (isFirstLoad.value) {
        loading.value = false;
        isFirstLoad.value = false;
      }
    }
  }

  function startPolling() {
    if (pollingTimer) return;
    pollingTimer = setInterval(() => {
      loadTasks();
    }, 500);
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  }

  function handleVisibleChange(visible: boolean) {
    if (visible) {
      loadTasks();
      startPolling();
    } else {
      stopPolling();
    }
  }

  onUnmounted(() => {
    stopPolling();
  });
  function taskCenterPath(menus = permissionStore.getBackMenuList as any[]): string | undefined {
    for (const menu of menus) {
      if (menu.name === '传输任务中心') return menu.path;
      const childPath = taskCenterPath(menu.children || []);
      if (childPath) return childPath;
    }
    return undefined;
  }
  function openTaskCenter() {
    go(taskCenterPath() || '/maData/transferTask');
  }
  function openTask(task: any) {
    const type = task.transferType || 'import';
    go(`${taskCenterPath() || '/maData/transferTask'}?type=${type}&taskId=${task.id}`);
  }
</script>

<style lang="less" scoped>
  .transfer-task-trigger {
    display: inline-flex;
    align-items: center;
    height: 100%;
    padding: 0 12px;
    font-size: 18px;
    cursor: pointer;
  }
  .transfer-task-panel {
    width: 340px;
  }
  .panel-title {
    padding-bottom: 12px;
    font-size: 15px;
    font-weight: 600;
    border-bottom: 1px solid #f0f0f0;
  }
  .task-item {
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
  }
  .task-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  .task-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .panel-footer {
    padding-top: 12px;
    color: #1677ff;
    text-align: center;
    cursor: pointer;
  }
</style>
