<template>
  <PageWrapper title="传输任务中心" contentFullHeight fixedHeight>
    <template #footer>
      <Tabs :active-key="activeType" @change="activeType = $event">
        <tab-pane key="upload" tab="上传任务">
          <div class="transfer-page">
        <BasicTable @register="registerTable">
          <template #taskName="{ record }">
            <a class="task-name" @click.stop="openDetail(record)">{{ record.taskName }}</a>
          </template>
          <template #datasetType="{ record }">
            <a-tag>{{ typeLabel(record.datasetType) }}</a-tag>
          </template>
          <template #progress="{ record }">
            <div class="progress-cell">
              <Progress
                :percent="progressPercent(record)"
                :show-info="false"
                size="small"
                :status="
                  record.status === 'FAILED'
                    ? 'exception'
                    : record.status === 'COMPLETED'
                    ? 'success'
                    : 'active'
                "
              />
              <span>{{ progressPercent(record) }}%</span>
            </div>
          </template>
          <template #status="{ record }">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
            <div v-if="record.stage && record.status === 'PROCESSING'" class="stage-label">{{
              stageLabel(record.stage)
            }}</div>
          </template>
          <template #action="{ record }">
            <TableAction
              :actions="[
              { icon: 'ant-design:eye-outlined', tooltip: '查看详情', onClick: () => openDetail(record) },
              ...(record.status === 'FAILED' ? [{ icon: 'ant-design:redo-outlined', tooltip: '重试', onClick: () => retry(record) }] : []),
              ...(record.status === 'PROCESSING' ? [{ icon: 'ant-design:close-circle-outlined', tooltip: '取消', color: 'error' as const, onClick: () => cancel(record) }] : []),
              ...(record.status !== 'PROCESSING' ? [{ icon: 'ant-design:delete-outlined', tooltip: '删除', color: 'error' as const, onClick: () => remove(record) }] : []),
            ]"
            />
          </template>
        </BasicTable>
          </div>
        </tab-pane>
        <tab-pane key="export" tab="导出任务">
          <TransferExportTaskList />
        </tab-pane>
      </Tabs>
    </template>
    <Modal
      v-if="activeType === 'upload'"
      v-model:visible="detailVisible"
      title="导入任务详情"
      width="520px"
      :footer="null"
      :destroy-on-close="true"
    >
      <Spin :spinning="detailLoading">
        <template v-if="selectedTask">
          <div class="detail-head">
            <div class="detail-title">{{ selectedTask.taskName }}</div>
            <a-tag :color="statusColor(selectedTask.status)">{{
              statusLabel(selectedTask.status)
            }}</a-tag>
          </div>
          <Progress
            :percent="progressPercent(selectedTask)"
            :show-info="false"
            :status="
              selectedTask.status === 'FAILED'
                ? 'exception'
                : selectedTask.status === 'COMPLETED'
                ? 'success'
                : 'active'
            "
          />
          <div class="detail-progress">{{ progressPercent(selectedTask) }}%</div>
          <a-descriptions :column="1" bordered size="small" class="detail-info">
            <a-descriptions-item label="任务编号">#{{ selectedTask.id }}</a-descriptions-item>
            <a-descriptions-item label="数据类型">{{
              typeLabel(selectedTask.datasetType)
            }}</a-descriptions-item>
            <a-descriptions-item label="处理阶段">{{
              stageLabel(selectedTask.stage)
            }}</a-descriptions-item>
            <a-descriptions-item label="文件进度"
              >{{ selectedTask.successFiles || 0 }} 成功，{{
                selectedTask.failedFiles || 0
              }}
              失败，共 {{ selectedTask.totalFiles || 0 }} 个</a-descriptions-item
            >
            <a-descriptions-item v-if="selectedTask.errorMessage" label="错误信息"
              ><span class="error-text">{{ selectedTask.errorMessage }}</span></a-descriptions-item
            >
          </a-descriptions>
          <a-divider>执行日志</a-divider>
          <a-timeline>
            <a-timeline-item v-for="(event, index) in selectedTask.events || []" :key="index">
              <div>{{ event.message }}</div>
              <div class="event-time">{{ event.createTime }}</div>
            </a-timeline-item>
          </a-timeline>
        </template>
      </Spin>
    </Modal>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { PageWrapper } from '/@/components/Page';
  import { Modal, Progress, Spin, Tabs, TabPane } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import {
    cancelImportTransferTask,
    deleteImportTransferTasks,
    getImportTransferTask,
    getImportTransferTasks,
    retryImportTransferTask,
  } from '/@/api/mineai/importTransferTask';
  import TransferExportTaskList from './export.vue';

  const { createMessage, createConfirm } = useMessage();
  const route = useRoute();
  const activeType = ref(route.query.type === 'export' ? 'export' : 'upload');
  const detailVisible = ref(false);
  const detailLoading = ref(false);
  const selectedTask = ref<any>();
  let refreshTimer: ReturnType<typeof setInterval> | undefined;
  let refreshInFlight = false;

  const columns = [
    {
      title: '任务编号',
      dataIndex: 'id',
      key: 'id',
      width: 110,
    },
    {
      title: '任务',
      dataIndex: 'taskName',
      key: 'taskName',
      slots: { customRender: 'taskName' },
      width: 260,
    },
    {
      title: '数据类型',
      dataIndex: 'datasetType',
      key: 'datasetType',
      slots: { customRender: 'datasetType' },
      width: 110,
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      slots: { customRender: 'progress' },
      width: 250,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      slots: { customRender: 'status' },
      width: 120,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      customRender: ({ text }) => formatToDateTime(text),
    },
  ];

  const [registerTable, { reload: reloadTable, getPaginationRef, setPagination, setTableData }] =
    useTable({
      api: async (params) => {
        const result: any = await getImportTransferTasks({
          current: params.page,
          size: params.pageSize,
        });
        return { items: result?.result || [], total: result?.page?.total || 0 };
      },
      columns,
      showTableSetting: false,
      showIndexColumn: false,
      bordered: true,
      rowKey: 'id',
      actionColumn: {
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
        width: 150,
        fixed: 'right',
      },
    });

  const typeLabel = (type?: string) =>
    (({ IMAGE: '图片', VIDEO: '视频', POINT_CLOUD: '点云', MULTI: '多模态' } as any)[type || ''] ||
    type ||
    '-');
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
  const stageLabel = (value?: string) =>
    (({ UPLOAD: '上传文件', VALIDATE: '校验文件', PROCESS: '处理数据', DONE: '已完成' } as any)[
      value || ''
    ] ||
    value ||
    '-');
  const progressPercent = (task?: { progress?: number | string }) => {
    const progress = Number(task?.progress);
    return Number.isFinite(progress) ? Math.min(100, Math.max(0, Math.round(progress))) : 0;
  };

  async function openDetail(record: any) {
    detailVisible.value = true;
    detailLoading.value = true;
    try {
      const task: any = await getImportTransferTask(record.id);
      selectedTask.value = task?.data || task;
    } catch (_) {
      createMessage.error('任务详情加载失败');
      detailVisible.value = false;
    } finally {
      detailLoading.value = false;
    }
  }

  async function refreshTasksSilently() {
    if (refreshInFlight) return;

    refreshInFlight = true;
    try {
      const pagination = getPaginationRef();
      const result: any = await getImportTransferTasks({
        current: typeof pagination === 'object' ? pagination.current || 1 : 1,
        size: typeof pagination === 'object' ? pagination.pageSize || 10 : 10,
      });
      setTableData(result?.result || []);
      setPagination({ total: result?.page?.total || 0 });
    } catch (_) {
      // 定时刷新失败不打断当前列表，下一轮继续尝试。
    } finally {
      refreshInFlight = false;
    }
  }

  onMounted(async () => {
    const taskId = Number(route.query.taskId);
    if (activeType.value === 'upload' && Number.isSafeInteger(taskId) && taskId > 0) {
      openDetail({ id: taskId });
    }
    refreshTimer = setInterval(refreshTasksSilently, 5000);
  });
  onUnmounted(() => {
    if (refreshTimer) clearInterval(refreshTimer);
  });
  async function cancel(record: any) {
    await cancelImportTransferTask(record.id);
    createMessage.success('任务已取消');
    reloadTable();
  }
  async function retry(record: any) {
    await retryImportTransferTask(record.id);
    createMessage.success('任务已重新提交');
    reloadTable();
  }
  async function remove(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '删除任务记录',
      content: '删除后仅移除任务记录，不影响已上传的数据。',
      onOk: async () => {
        await deleteImportTransferTasks([record.id]);
        createMessage.success('已删除');
        reloadTable();
      },
    });
  }
</script>

<style lang="less" scoped>
  .transfer-page {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 0 12px;
  }
  .task-name {
    font-weight: 500;
  }
  .stage-label,
  .event-time {
    color: #999;
    font-size: 12px;
    margin-top: 3px;
  }
  .progress-cell {
    min-width: 210px;
  }
  .progress-cell :deep(.ant-progress) {
    margin-bottom: 0;
  }
  .progress-cell span,
  .detail-progress {
    color: #777;
    font-size: 12px;
  }
  .detail-progress {
    margin-top: 4px;
    text-align: right;
  }
  .detail-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }
  .detail-title {
    font-size: 16px;
    font-weight: 600;
  }
  .detail-info {
    margin-top: 16px;
  }
  .error-text {
    color: #d4380d;
    word-break: break-word;
  }
</style>
