<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增模型接收地址</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '详情',
              onClick: () => openDetailModal(record),
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: () => handleEdit(record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: {
                title: '确认删除该模型接收地址吗？',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <InferenceDeviceModal @register="registerModal" @success="reload" />
    <InferenceDeviceDetailModal
      v-model:visible="detailModalVisible"
      :record="currentDetailRecord"
      v-if="detailModalVisible"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { Button } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { deleteInferenceDevice, InferenceDeviceItem, pageInferenceDevice } from './api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import InferenceDeviceModal from './inferenceDeviceModal.vue';
  import InferenceDeviceDetailModal from './InferenceDeviceDetailModal.vue';
  import { inferenceDeviceColumns, inferenceDeviceSearchFormSchema } from './inferenceDeviceData';

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const detailModalVisible = ref(false);
  const currentDetailRecord = ref<InferenceDeviceItem | null>(null);

  const [registerTable, { reload }] = useTable({
    title: '模型接收地址列表',
    api: async (params) => {
      return await pageInferenceDevice(params);
    },
    columns: inferenceDeviceColumns,
    formConfig: {
      labelWidth: 110,
      showAdvancedButton: false,
      schemas: inferenceDeviceSearchFormSchema,
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

  function openDetailModal(record: InferenceDeviceItem) {
    currentDetailRecord.value = record;
    detailModalVisible.value = true;
  }

  async function handleDelete(record: Recordable) {
    await deleteInferenceDevice(record.id);
    createMessage.success('删除成功');
    reload();
  }
</script>
