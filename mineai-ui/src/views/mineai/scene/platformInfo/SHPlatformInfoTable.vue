<template>
  <BasicTable @register="registerTable">
    <template #toolbar>
      <Button type="primary" @click="handleCreate">新增软硬件平台</Button>
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
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from '/@/components/Button';

  const { createMessage } = useMessage();

  const [registerTable, { reload }] = useTable({
    title: '软硬件平台信息',
    api: () => {
      return Promise.resolve({
        items: [
          { id: 1, softwarePlatform: 'Pytorch', hardwarePlatform: 'GPU' },
          { id: 2, softwarePlatform: 'RKNN', hardwarePlatform: 'RK3588' },
          { id: 3, softwarePlatform: 'TensorFlow', hardwarePlatform: 'TPU' },
        ],
        total: 3,
      });
    },
    columns: [
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
      },
      {
        title: '软件平台',
        dataIndex: 'softwarePlatform',
      },
      {
        title: '硬件平台',
        dataIndex: 'hardwarePlatform',
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

  function handleCreate() {
    createMessage.success('新增平台功能尚未实现');
  }

  function handleEdit(record) {
    createMessage.success(`编辑功能尚未实现: ${record.id}`);
  }

  function handleDelete(record) {
    createMessage.success(`删除成功: ${record.id}`);
    reload();
  }
</script>
