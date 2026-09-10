<template>
  <PageWrapper @back="goBack" :contentStyle="{ margin: '0' }">
    <template #title>数据处理任务</template>
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate">新建数据处理任务</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:play-circle-outlined',
                tooltip: '启动',
                onClick: handleStart.bind(null, record),
                disabled: record.status === 'running',
              },
              {
                icon: 'ant-design:pause-circle-outlined',
                tooltip: '暂停',
                onClick: handlePause.bind(null, record),
                disabled: record.status !== 'running',
              },
              {
                icon: 'ant-design:info-circle-outlined',
                tooltip: '详情',
                onClick: handleDetail.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                tooltip: '编辑',
                onClick: handleEdit.bind(null, record),
                disabled: record.status === 'running',
              },
              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除',
                color: 'error',
                disabled: record.status === 'running',
                popConfirm: {
                  title: '是否确认删除',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>

    <CreateProcessTaskModal @register="registerCreateModal" @success="handleSuccess" />
    <EditProcessTaskModal @register="registerEditModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { Tag as ATag, Button as AButton } from 'ant-design-vue';
  import BasicTable from '/@/components/Table/src/BasicTable.vue';
  import { TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import PageWrapper from '/@/components/Page/src/PageWrapper.vue';
  import CreateProcessTaskModal from './CreateProcessTaskModal.vue';
  import EditProcessTaskModal from './EditProcessTaskModal.vue';

  const router = useRouter();
  const { createMessage } = useMessage();

  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerEditModal, { openModal: openEditModal }] = useModal();

  // 模拟数据
  const mockData = [
    {
      id: 1,
      taskName: '图像数据增强任务',
      taskType: 'builtin',
      typeName: '平台内置算子',
      operatorName: '数据增强',
      datasetName: '猫狗分类数据集',
      status: 'completed',
      createTime: '2026-09-01 10:30:00',
      updateTime: '2026-09-01 11:45:00',
    },
    {
      id: 2,
      taskName: '视频帧提取任务',
      taskType: 'template',
      typeName: '任务编排模板',
      operatorName: '视频处理流程',
      datasetName: '监控视频数据集',
      status: 'running',
      createTime: '2026-09-05 14:20:00',
      updateTime: '2026-09-08 09:15:00',
    },
    {
      id: 3,
      taskName: '自定义数据清洗',
      taskType: 'custom',
      typeName: '自定义算子',
      operatorName: '数据清洗脚本v2',
      datasetName: '文本数据集A',
      status: 'failed',
      createTime: '2026-09-07 16:00:00',
      updateTime: '2026-09-07 16:25:00',
    },
    {
      id: 4,
      taskName: '批量标注任务',
      taskType: 'builtin',
      typeName: '平台内置算子',
      operatorName: '自动标注',
      datasetName: '车辆检测数据集',
      status: 'pending',
      createTime: '2026-09-08 08:00:00',
      updateTime: '2026-09-08 08:00:00',
    },
  ];

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      width: 180,
    },
    {
      title: '任务类型',
      dataIndex: 'typeName',
      width: 150,
    },
    {
      title: '算子/模板名称',
      dataIndex: 'operatorName',
      width: 180,
    },
    {
      title: '绑定数据集',
      dataIndex: 'datasetName',
      width: 180,
    },
    {
      title: '状态',
      key: 'status',
      dataIndex: 'status',
      width: 120,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 180,
    },
  ];

  const [registerTable, { reload }] = useTable({
    api: async () => {
      // 模拟接口返回
      return {
        items: mockData,
        total: mockData.length,
      };
    },
    columns,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: true,
    actionColumn: {
      width: 220,
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
    },
  });

  const goBack = () => {
    router.go(-1);
  };

  function getStatusColor(status: string) {
    const colorMap = {
      pending: 'default',
      running: 'processing',
      completed: 'success',
      failed: 'error',
      paused: 'warning',
    };
    return colorMap[status] || 'default';
  }

  function getStatusText(status: string) {
    const textMap = {
      pending: '待执行',
      running: '运行中',
      completed: '已完成',
      failed: '失败',
      paused: '已暂停',
    };
    return textMap[status] || status;
  }

  function handleCreate() {
    openCreateModal(true, {
      isUpdate: false,
    });
  }

  function handleStart(record) {
    createMessage.success(`启动任务：${record.taskName}`);
    reload();
  }

  function handlePause(record) {
    createMessage.success(`暂停任务：${record.taskName}`);
    reload();
  }

  function handleDetail(record) {
    createMessage.info(`查看任务详情：${record.taskName}`);
  }

  function handleEdit(record) {
    openEditModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record) {
    createMessage.success(`删除任务：${record.taskName}`);
    reload();
  }

  function handleSuccess() {
    createMessage.success('操作成功');
    reload();
  }
</script>

<style scoped lang="less">
  .vben-page-wrapper {
    padding: 0;
  }
</style>
