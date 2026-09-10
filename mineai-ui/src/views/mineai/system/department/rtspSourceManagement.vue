<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增RTSP流</Button>
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
                title: '确认删除该RTSP流吗？',
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
  import {
    dynamicFindRtspSourcePage,
    deleteRtspSource,
  } from '/@/views/mineai/system/api/rtspSource';
  import { useMessage } from '/@/hooks/web/useMessage';
  import RtspSourceModal from './rtspSourceModal.vue';
  import { rtspColumns, rtspSearchFormSchema } from './rtspSourceData';

  const [registerModal, { openModal }] = useModal();
  const { createMessage } = useMessage();

  const [registerTable, { reload }] = useTable({
    title: 'RTSP数据源列表',
    api: async (params) => {
      return await dynamicFindRtspSourcePage(params);
    },
    beforeFetch: (v) => {
      v.page -= 1;
      return v;
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
