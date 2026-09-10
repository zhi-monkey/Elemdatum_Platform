<template>
  <div>
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增分析服务器</Button>
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
              popConfirm: {
                title: '确认删除该分析服务器吗？',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>

    <GpuUrlTargetModal @register="registerModal" @success="reload" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Button } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { deleteGpuUrlTarget, pageGpuUrlTarget } from '../api';
  import GpuUrlTargetModal from './gpuUrlTargetModal.vue';
  import { gpuUrlTargetColumns, gpuUrlTargetSearchFormSchema } from './gpuUrlTargetData';

  const [registerModal, { openModal }] = useModal();
  const { createMessage } = useMessage();

  const [registerTable, { reload }] = useTable({
    title: '分析服务器地址列表',
    api: async (params) => await pageGpuUrlTarget(params),
    columns: gpuUrlTargetColumns,
    formConfig: {
      labelWidth: 110,
      showAdvancedButton: false,
      schemas: gpuUrlTargetSearchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 140,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function handleCreate() {
    openModal(true, { isUpdate: false });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { isUpdate: true, record });
  }

  async function handleDelete(record: Recordable) {
    await deleteGpuUrlTarget(record.id);
    createMessage.success('删除成功');
    reload();
  }
</script>
