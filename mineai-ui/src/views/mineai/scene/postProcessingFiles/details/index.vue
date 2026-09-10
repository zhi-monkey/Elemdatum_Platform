<template>
  <PageWrapper style="margin: 0 16px 0 16px" title="后处理文件预设组详情" @back="goBack">
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary">导出/导入后处理文件预设包</a-button>
        <a-button type="primary" @click="handleFileStatusModal">查看文件状态</a-button>

        <a-button type="primary" @click="handleCreate">新增文件</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
            },
          ]"
        />
      </template>
    </BasicTable>
    <FileStatusModal @register="registerModal" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton } from '/@/components/Button';
  import { PageWrapper } from '/@/components/Page';
  import FileStatusModal from './fileStatusModal.vue';
  import { useModal } from '/@/components/Modal';
  import { router } from '/@/router';

  const { createMessage } = useMessage();

  const [registerTable, { reload }] = useTable({
    title: '后处理文件预设组详情',
    api: () => {
      return Promise.resolve({
        items: [
          { id: 1, fileName: '文件1', fileUsage: 'PACKAGE', fileSize: '15MB' },
          { id: 2, fileName: '文件2', fileUsage: 'START_SHELL', fileSize: '5MB' },
          { id: 3, fileName: '文件3', fileUsage: 'CONFIG', fileSize: '2MB' },
          { id: 4, fileName: '文件4', fileUsage: 'LOG', fileSize: '10MB' },
          { id: 5, fileName: '文件5', fileUsage: 'PACKAGE', fileSize: '20MB' }, // 重复的文件用途
        ],
        total: 5,
      });
    },
    columns: [
      {
        title: '文件ID',
        dataIndex: 'id',
        width: 80,
      },
      {
        title: '文件名',
        dataIndex: 'fileName',
      },
      {
        title: '文件用途',
        dataIndex: 'fileUsage',
        customRender: ({ text }) => {
          const usageMap = {
            PACKAGE: '打包',
            START_SHELL: '启动脚本',
            CONFIG: '配置文件',
            LOG: '日志文件',
          };
          return usageMap[text] || text;
        },
      },
      {
        title: '文件大小',
        dataIndex: 'fileSize',
      },
    ],
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  const requiredFiles = [
    { name: '打包', required: true },
    { name: '启动脚本', required: true },
    { name: '配置文件', required: true },
    { name: '日志文件', required: true },
    { name: '监控文件', required: false }, // 缺少的文件
  ];

  const [registerModal, { openModal }] = useModal();

  function handleCreate() {
    createMessage.success('新增文件功能尚未实现');
  }

  function handleEdit(record) {
    createMessage.success(`编辑功能尚未实现: ${record.id}`);
  }

  function handleDelete(record) {
    createMessage.success(`删除成功: ${record.id}`);
    reload();
  }

  function goBack() {
    router.go(-1);
  }

  function handleFileStatusModal() {
    openModal(true, null);
  }
</script>
