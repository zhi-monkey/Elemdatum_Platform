<template>
  <div class="export-task">
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <a-button danger :disabled="!selectedIds.length" @click="handleBatchDelete">
          批量删除
        </a-button>
      </template>
      <template #status="{ record }">
        <Tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</Tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <el-dialog v-model="detailVisible" title="导出任务详情" width="560px" append-to-body>
      <el-descriptions v-if="detailRecord" :column="1" border size="small">
        <el-descriptions-item label="任务号">{{ detailRecord.taskId }}</el-descriptions-item>
        <el-descriptions-item label="导出人">
          {{ detailRecord.createUserName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="导出格式">{{ detailRecord.format }}</el-descriptions-item>
        <el-descriptions-item label="数据条数">
          {{ detailRecord.fileCount ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="数据集">
          {{ detailRecord.datasetSummary || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="筛选条件">
          <pre class="log-pre">{{ detailRecord.conditionSummary || '无' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <Tag :color="getStatusColor(detailRecord.status)">
            {{ getStatusText(detailRecord.status) }}
          </Tag>
        </el-descriptions-item>
        <el-descriptions-item label="进度">{{ detailRecord.progress ?? 0 }}%</el-descriptions-item>
        <el-descriptions-item label="有效期">
          {{ detailRecord.expireTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRecord.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRecord.errorMessage" label="失败原因">
          {{ detailRecord.errorMessage }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRecord.log" label="执行日志">
          <pre class="log-pre">{{ detailRecord.log }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicColumn } from '/@/components/Table';
  import {
    getExportTasks,
    downloadExportTask,
    confirmExportTask,
    cancelExportTask,
    deleteExportTasks,
  } from './api';
  import { downloadByData } from '/@/utils/file/download';
  import { Tag, Progress, message, Modal } from 'ant-design-vue';
  import { computed, ref, h } from 'vue';

  const columns: BasicColumn[] = [
    { title: '任务号', dataIndex: 'taskId', ellipsis: true, width: 200 },
    { title: '导出人', dataIndex: 'createUserName', width: 100 },
    { title: '格式', dataIndex: 'format', width: 70 },
    { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 130,
      customRender: ({ record }) => {
        const p = record.progress || 0;
        return h(Progress, { percent: p, size: 'small' });
      },
    },
    { title: '数据条数', dataIndex: 'fileCount', width: 90 },
    { title: '有效期', dataIndex: 'expireTime', width: 160 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作',
      dataIndex: 'action',
      width: 200,
      fixed: 'right',
      slots: { customRender: 'action' },
    },
  ];

  const [registerTable, { reload, getSelectRows }] = useTable({
    api: async (params) => {
      const v: any = await getExportTasks({ current: params.page, size: params.pageSize });
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    columns,
    useSearchForm: false,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    rowSelection: { type: 'checkbox' },
  });

  const detailVisible = ref(false);
  const detailRecord = ref<any>(null);

  const selectedIds = computed(() => {
    const rows = getSelectRows();
    return rows ? rows.map((r) => r.id) : [];
  });

  function getStatusText(status: string) {
    const map: Record<string, string> = {
      PENDING: '等待确认',
      QUEUED: '排队中',
      PROCESSING: '执行中',
      COMPLETED: '已完成',
      FAILED: '失败',
      CANCELLED: '已取消',
    };
    return map[status] || status;
  }

  function getStatusColor(status: string) {
    const map: Record<string, string> = {
      PENDING: 'green',
      QUEUED: 'blue',
      PROCESSING: 'orange',
      COMPLETED: 'gray',
      FAILED: 'red',
      CANCELLED: 'red',
    };
    return map[status] || 'gray';
  }

  function getActions(record: any) {
    const actions: any[] = [
      {
        icon: 'ant-design:info-circle-outlined',
        tooltip: '查看详情',
        onClick: () => showDetail(record),
      },
    ];
    if (record.status === 'PENDING') {
      actions.push({
        icon: 'ant-design:check-circle-outlined',
        tooltip: '确认',
        onClick: () => handleConfirm(record),
      });
      actions.push({
        icon: 'ant-design:close-circle-outlined',
        tooltip: '取消',
        onClick: () => handleCancel(record),
      });
    }
    if (record.status === 'COMPLETED') {
      actions.push({
        icon: 'ant-design:download-outlined',
        tooltip: '下载',
        onClick: () => handleDownload(record),
      });
    }
    return actions;
  }

  function showDetail(record: any) {
    detailRecord.value = record;
    detailVisible.value = true;
  }

  async function handleConfirm(record: any) {
    try {
      await confirmExportTask(record.taskId);
      message.success('已确认，正在导出');
      reload();
    } catch (e) {
      message.error('确认失败');
    }
  }

  async function handleCancel(record: any) {
    try {
      await cancelExportTask(record.taskId);
      message.success('已取消');
      reload();
    } catch (e) {
      message.error('取消失败');
    }
  }

  async function handleDownload(record: any) {
    try {
      const response: any = await downloadExportTask(record.taskId);
      const blob = response.data;
      if (!blob) return;
      const contentDisposition = response.headers?.['content-disposition'];
      let fileName = `export_${record.taskId}`;
      if (contentDisposition) {
        const match =
          contentDisposition.match(/filename\*=UTF-8''(.+)/i) ||
          contentDisposition.match(/filename="?(.+?)"?$/i);
        if (match) fileName = decodeURIComponent(match[1]);
      }
      downloadByData(blob, fileName);
    } catch (e) {
      message.error('下载失败');
    }
  }

  function handleBatchDelete() {
    if (!selectedIds.value.length) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定删除选中的 ${selectedIds.value.length} 个导出任务吗？删除后不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteExportTasks(selectedIds.value);
          message.success('删除成功');
          reload();
        } catch (e) {
          message.error('删除失败');
        }
      },
    });
  }
</script>

<style scoped lang="less">
  .log-pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 12px;
    color: #666;
    max-height: 200px;
    overflow-y: auto;
  }
</style>
