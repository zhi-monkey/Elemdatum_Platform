<template>
  <div class="export-page">
    <BasicTable @register="registerTable">
      <template #taskName="{ record }">
        <a @click.stop="openDetail(record)">{{ record.taskName }}</a>
      </template>
      <template #datasetType="{ record }"
        ><a-tag>{{ typeLabel(record.datasetType) }}</a-tag></template
      >
      <template #progress="{ record }">
        <div class="progress-cell"
          ><Progress :percent="percent(record)" :show-info="false" size="small" /><span
            >{{ percent(record) }}%</span
          ></div
        >
      </template>
      <template #status="{ record }"
        ><a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag
        ><div class="stage">{{ stageLabel(record.stage) }}</div></template
      >
      <template #action="{ record }"><TableAction :actions="actions(record)" /></template>
    </BasicTable>
    <Modal
      v-model:visible="detailVisible"
      title="导出任务详情"
      width="520px"
      :footer="null"
      :destroy-on-close="true"
    >
      <Spin :spinning="detailLoading" v-if="selectedTask">
        <div class="detail-head"
          ><strong>{{ selectedTask.taskName }}</strong
          ><a-tag :color="statusColor(selectedTask.status)">{{
            statusLabel(selectedTask.status)
          }}</a-tag></div
        >
        <Progress
          :percent="percent(selectedTask)"
          :show-info="false"
          :status="progressStatus(selectedTask.status)"
        />
        <a-descriptions :column="1" bordered size="small" class="detail-info">
          <a-descriptions-item label="任务编号">#{{ selectedTask.id }}</a-descriptions-item>
          <a-descriptions-item label="数据集版本"
            >{{ selectedTask.datasetId }} / {{ selectedTask.versionName }}</a-descriptions-item
          >
          <a-descriptions-item label="数据类型">{{
            typeLabel(selectedTask.datasetType)
          }}</a-descriptions-item>
          <a-descriptions-item label="导出格式">{{ selectedTask.format }}</a-descriptions-item>
          <a-descriptions-item label="文件进度"
            >{{ selectedTask.successFiles || 0 }} 成功，共
            {{ selectedTask.totalFiles || 0 }} 个</a-descriptions-item
          >
          <a-descriptions-item v-if="selectedTask.errorMessage" label="错误信息"
            ><span class="error-text">{{ selectedTask.errorMessage }}</span></a-descriptions-item
          >
        </a-descriptions>
        <a-divider>执行日志</a-divider>
        <a-timeline
          ><a-timeline-item v-for="(event, index) in selectedTask.events || []" :key="index"
            ><div>{{ event.message }}</div
            ><div class="stage">{{ event.createTime }}</div></a-timeline-item
          ></a-timeline
        >
      </Spin>
    </Modal>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Modal, Progress, Spin } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import { downloadByData } from '/@/utils/file/download';
  import {
    getTransferExportTasks,
    getTransferExportTask,
    cancelTransferExportTask,
    deleteTransferExportTasks,
    downloadTransferExportTask,
  } from '/@/api/mineai/transferExportTask';

  const { createMessage, createConfirm } = useMessage();
  const route = useRoute();
  const detailVisible = ref(false);
  const detailLoading = ref(false);
  const selectedTask = ref<any>();
  const columns = [
    { title: '任务编号', dataIndex: 'id', width: 100 },
    { title: '任务', dataIndex: 'taskName', slots: { customRender: 'taskName' }, width: 260 },
    {
      title: '数据类型',
      dataIndex: 'datasetType',
      slots: { customRender: 'datasetType' },
      width: 100,
    },
    { title: '版本', dataIndex: 'versionName', width: 110 },
    { title: '格式', dataIndex: 'format', width: 120 },
    { title: '进度', dataIndex: 'progress', slots: { customRender: 'progress' }, width: 190 },
    { title: '状态', dataIndex: 'status', slots: { customRender: 'status' }, width: 120 },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      customRender: ({ text }) => formatToDateTime(text),
    },
  ];
  const [registerTable, { reload, getPaginationRef, setTableData, setPagination }] = useTable({
    api: async (params) => {
      const r: any = await getTransferExportTasks({ current: params.page, size: params.pageSize });
      return { items: r?.result || [], total: r?.page?.total || 0 };
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
  const typeLabel = (v?: string) =>
    (({ IMAGE: '图片', VIDEO: '视频', POINT_CLOUD: '点云', MULTI: '多模态' } as any)[v || ''] ||
    v ||
    '-');
  const statusLabel = (v?: string) =>
    ((
      {
        PROCESSING: '进行中',
        COMPLETED: '已完成',
        FAILED: '失败',
        CANCELLED: '已取消',
        PENDING: '等待中',
      } as any
    )[v || ''] ||
    v ||
    '-');
  const statusColor = (v?: string) =>
    ((
      {
        PROCESSING: 'blue',
        COMPLETED: 'green',
        FAILED: 'red',
        CANCELLED: 'default',
        PENDING: 'orange',
      } as any
    )[v || ''] || 'default');
  const stageLabel = (v?: string) =>
    (({ PREPARE: '准备中', PACKAGE: '打包中', UPLOAD: '上传文件', DONE: '已完成' } as any)[
      v || ''
    ] ||
    v ||
    '-');
  const percent = (r: any) => Math.min(100, Math.max(0, Math.round(Number(r?.progress) || 0)));
  const progressStatus = (s: string) =>
    s === 'FAILED' ? 'exception' : s === 'COMPLETED' ? 'success' : 'active';
  function actions(record: any) {
    return [
      { icon: 'ant-design:eye-outlined', tooltip: '查看详情', onClick: () => openDetail(record) },
      ...(record.status === 'COMPLETED'
        ? [
            {
              icon: 'ant-design:download-outlined',
              tooltip: '下载',
              onClick: () => download(record),
            },
          ]
        : []),
      ...(record.status === 'PROCESSING'
        ? [
            {
              icon: 'ant-design:close-circle-outlined',
              tooltip: '取消',
              color: 'error' as const,
              onClick: () => cancel(record),
            },
          ]
        : []),
      ...(record.status !== 'PROCESSING'
        ? [
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error' as const,
              onClick: () => remove(record),
            },
          ]
        : []),
    ];
  }
  async function openDetail(r: any) {
    detailVisible.value = true;
    detailLoading.value = true;
    try {
      const v: any = await getTransferExportTask(r.id);
      selectedTask.value = v?.data || v;
    } catch (_) {
      createMessage.error('任务详情加载失败');
      detailVisible.value = false;
    } finally {
      detailLoading.value = false;
    }
  }
  async function cancel(r: any) {
    await cancelTransferExportTask(r.id);
    createMessage.success('任务已取消');
    reload();
  }
  async function remove(r: any) {
    createConfirm({
      iconType: 'warning',
      title: '删除任务记录',
      content: '删除后仅移除任务记录，不影响已生成的文件。',
      onOk: async () => {
        await deleteTransferExportTasks([r.id]);
        createMessage.success('已删除');
        reload();
      },
    });
  }
  async function download(r: any) {
    try {
      const response: any = await downloadTransferExportTask(r.id);
      downloadByData(response.data, `${r.datasetId}_${r.format}_${r.versionName}.zip`);
    } catch (_) {
      createMessage.error('下载失败');
    }
  }
  let timer: ReturnType<typeof setInterval> | undefined;
  let refreshInFlight = false;

  async function refreshSilently() {
    if (refreshInFlight) return;
    refreshInFlight = true;
    try {
      const pagination = getPaginationRef();
      const r: any = await getTransferExportTasks({
        current: typeof pagination === 'object' ? pagination.current || 1 : 1,
        size: typeof pagination === 'object' ? pagination.pageSize || 10 : 10,
      });
      setTableData(r?.result || []);
      setPagination({ total: r?.page?.total || 0 });
    } catch (_) {
      // 静默刷新失败不影响界面
    } finally {
      refreshInFlight = false;
    }
  }

  onMounted(() => {
    const id = Number(route.query.taskId);
    if (Number.isSafeInteger(id) && id > 0) openDetail({ id });
    timer = setInterval(refreshSilently, 5000);
  });
  onUnmounted(() => {
    if (timer) clearInterval(timer);
  });
</script>

<style lang="less" scoped>
  .export-page {
    height: 100%;
    padding: 0 12px;
  }
  .progress-cell {
    min-width: 160px;
  }
  .progress-cell span {
    color: #777;
    font-size: 12px;
  }
  .stage {
    color: #999;
    font-size: 12px;
    margin-top: 3px;
  }
  .detail-head {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
  }
  .detail-info {
    margin-top: 16px;
  }
  .error-text {
    color: #d4380d;
    word-break: break-word;
  }
</style>
