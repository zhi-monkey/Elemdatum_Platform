<template>
  <div>
    <BasicTable @register="registerTable" @expand="handleExpand" rowKey="id">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增数据收集服务器</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:camera-outlined',
              tooltip: '同步摄像机',
              onClick: () => handleSyncCameras(record),
            },
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
                title: '确认删除该服务器吗？',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>

      <template #expandedRowRender="{ record }">
        <Table
          :columns="cameraColumns"
          :data-source="cameraMap[record.id] || []"
          :loading="cameraLoadingMap[record.id]"
          rowKey="id"
          size="small"
          :pagination="false"
        >
          <template #status="{ record: camera }">
            <Tag :color="getCameraStatusColor(camera.status)">
              {{ getCameraStatusText(camera.status) }}
            </Tag>
          </template>
          <template #collectProgress="{ record: camera }">
            <span v-if="parseCameraDescription(camera.description)">
              {{ parseCameraDescription(camera.description)?.cltnum ?? '-' }} 张
            </span>
            <span v-else>-</span>
          </template>
        </Table>
      </template>
    </BasicTable>

    <HttpServerModal @register="registerModal" @success="reload" />
  </div>
</template>

<script setup lang="ts">
  import { reactive } from 'vue';
  import { Table, Tag } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Button } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    deleteHttpCameraServer,
    listHttpCameras,
    pageHttpCameraServer,
    syncHttpCameras,
  } from '../api';
  import type { HttpCameraItem } from '../api';
  import HttpServerModal from './httpServerModal.vue';
  import { httpServerColumns, httpServerSearchFormSchema } from './httpServerData';

  const [registerModal, { openModal }] = useModal();
  const { createMessage } = useMessage();

  const cameraMap = reactive<Record<number, HttpCameraItem[]>>({});
  const cameraLoadingMap = reactive<Record<number, boolean>>({});

  const cameraColumns = [
    {
      title: '流地址',
      dataIndex: 'streamUrl',
      width: 320,
      customRender: ({ record }) => record.streamUrl || record.cameraId || '-',
    },
    { title: '名称', dataIndex: 'name', width: 200 },
    {
      title: '收集进度',
      dataIndex: 'description',
      slots: { customRender: 'collectProgress' },
      width: 120,
    },
    { title: '状态', dataIndex: 'status', slots: { customRender: 'status' }, width: 110 },
    { title: '最近在线时间', dataIndex: 'lastSeenTime', width: 200 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '数据收集服务器列表',
    api: async (params) => {
      return await pageHttpCameraServer(params);
    },
    columns: httpServerColumns,
    formConfig: {
      labelWidth: 110,
      showAdvancedButton: false,
      schemas: httpServerSearchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 180,
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
    await deleteHttpCameraServer(record.id);
    createMessage.success('删除成功');
    reload();
  }

  function getCameraStatusText(status?: string) {
    const s = (status || '').toUpperCase();
    if (s === 'ONLINE') return '在线';
    if (s === 'OFFLINE') return '离线';
    if (s === 'INACTIVE') return '未工作';
    if (s === 'DELETED') return '已删除';
    return '未知';
  }

  function getCameraStatusColor(status?: string) {
    const s = (status || '').toUpperCase();
    if (s === 'ONLINE') return 'success';
    if (s === 'DELETED') return 'error';
    if (s === 'INACTIVE' || s === 'OFFLINE') return 'warning';
    return 'default';
  }

  function parseCameraDescription(description?: string) {
    if (!description) return null;
    const result: Record<string, string> = {};
    description.split(',').forEach((item) => {
      const [key, value] = item.split('=');
      if (key && value !== undefined) {
        result[key.trim().toLowerCase()] = value.trim().toLowerCase();
      }
    });

    if (!('cltnum' in result)) {
      return null;
    }

    return {
      cltnum: result.cltnum,
    };
  }

  async function loadCameras(serverId: number) {
    cameraLoadingMap[serverId] = true;
    try {
      cameraMap[serverId] = (await listHttpCameras(serverId)) || [];
    } catch (e) {
      createMessage.warning('获取摄像机列表失败');
    } finally {
      cameraLoadingMap[serverId] = false;
    }
  }

  function handleExpand(expanded: boolean, record: Recordable) {
    if (expanded && record.id) {
      loadCameras(record.id);
    }
  }

  async function handleSyncCameras(record: Recordable) {
    const key = `sync-${record.id}`;
    try {
      createMessage.loading({ content: '同步摄像机中...', key });
      const result = await syncHttpCameras(record.id);
      await loadCameras(record.id);
      createMessage.success({
        key,
        duration: 4,
        content: `同步完成：新增 ${result.addedCount}，更新 ${result.updatedCount}，离线 ${result.offlineCount}`,
      });
    } catch (e) {
      createMessage.error({ key, content: '同步失败，请先检查服务器连通性' });
    }
  }
</script>
