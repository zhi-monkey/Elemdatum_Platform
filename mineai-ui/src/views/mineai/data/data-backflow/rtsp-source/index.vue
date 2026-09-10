<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增摄像头视频流</Button>
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
                title: '确认删除该摄像头视频流吗？',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <RtspSourceModal @register="registerModal" @success="reload" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Button } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { deleteRtspSource, pageRtspSource } from './api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import RtspSourceModal from './rtspSourceModal.vue';
  import { rtspColumns, rtspSearchFormSchema } from './rtspSourceData';

  const [registerModal, { openModal }] = useModal();
  const { createMessage } = useMessage();

  const [registerTable, { reload }] = useTable({
    title: '摄像头视频流列表',
    api: async (params) => {
      return await pageRtspSource(params);
    },
    columns: rtspColumns,
    formConfig: {
      labelWidth: 110,
      showAdvancedButton: false,
      schemas: rtspSearchFormSchema,
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
    await deleteRtspSource(record.id);
    createMessage.success('删除成功');
    reload();
  }
</script>
