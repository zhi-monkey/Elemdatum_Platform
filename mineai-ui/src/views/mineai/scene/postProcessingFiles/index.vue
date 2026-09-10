<template>
  <div>
    <PageWrapper style="margin: 0 16px 0 16px" title="后处理文件预设组管理">
      <BasicTable @register="registerTable">
        <template #toolbar>
          <a-button type="primary" @click="handleCreate">新增预设组</a-button>
        </template>
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:info-circle-outlined',
                tooltip: '预设组详情',
                onClick: gotoPresetGroupDetail.bind(null, record),
              },
              {
                icon: 'clarity:note-edit-line',
                tooltip: '编辑',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'clarity:paperclip-line',
                tooltip: '关联算法',
                onClick: () => openAlgorithmDrawer(record),
              },
              {
                icon: 'clarity:paperclip-line',
                tooltip: '关联设备固件组',
                onClick: () => openDeviceFirmwareDrawer(record),
              },

              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除',
                color: 'error',
                popConfirm: {
                  title: '是否确认删除',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </BasicTable>
    </PageWrapper>
    <ApplicationModal @register="createAndUpdateModal" />
    <AlgorithmDrawer :visible="isAlgorithmDrawerVisible" @close="closeAlgorithmDrawer" />
    <DeviceFirmwareDrawer
      :visible="isDeviceFirmwareDrawerVisible"
      @close="closeDeviceFirmwareDrawer"
      @confirm="handleDeviceFirmwareConfirm"
    />
  </div>
</template>

<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, ref } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import ApplicationModal from './applicationModal.vue';
  import { PageWrapper } from '/@/components/Page';
  import AlgorithmDrawer from './AlgorithmDrawer.vue';
  import DeviceFirmwareDrawer from '/@/views/mineai/scene/postProcessingFiles/DeviceFirmwareDrawer.vue';

  const go = useGo();
  const { hasPermission } = usePermission();
  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const isAlgorithmDrawerVisible = ref(false);
  const isDeviceFirmwareDrawerVisible = ref(false);

  const [registerTable, { reload }] = useTable({
    title: '后处理文件预设组列表',
    api: () => {
      return Promise.resolve({
        items: [
          {
            id: 1,
            presetGroupName: '预设组A',
            fileCount: 10,
            algorithm: 'YOLOv5-123456',
            deviceFirmware: [
              { device: 'KBA12C', firmware: 'v1.0' },
              { device: 'KBA13D', firmware: 'v2.1' },
            ],
          },
          {
            id: 2,
            presetGroupName: '预设组B',
            fileCount: 15,
            algorithm: 'YOLOv5-654321',
            deviceFirmware: [{ device: 'KBA14E', firmware: 'v3.0' }],
          },
          // 更多示例数据...
        ],
        total: 2,
      });
    },
    beforeFetch: (v) => {
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      return itemList.map((item) => ({
        ...item,
        deviceFirmwareDisplay: item.deviceFirmware
          .map((df) => `${df.device}-${df.firmware}`)
          .join(', '),
      }));
    },
    columns: [
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
      },
      {
        title: '预设组名称',
        dataIndex: 'presetGroupName',
      },
      {
        title: '文件数量',
        dataIndex: 'fileCount',
      },
      {
        title: '关联算法',
        dataIndex: 'algorithm',
      },
      {
        title: '关联设备-固件组',
        dataIndex: 'deviceFirmwareDisplay',
      },
    ],
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: [],
    },
    useSearchForm: false,
    showTableSetting: true,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 300,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      //ifShow: userPermission.value,
    },
  });

  function handleCreate() {
    openCreateAndUpdateModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openCreateAndUpdateModal(true, {
      record,
      isUpdate: true,
    });
  }

  function openAlgorithmDrawer(record: Recordable) {
    isAlgorithmDrawerVisible.value = true;
    console.log('Open Algorithm Drawer for:', record);
  }

  function closeAlgorithmDrawer() {
    isAlgorithmDrawerVisible.value = false;
  }

  function openDeviceFirmwareDrawer(record: Recordable) {
    isDeviceFirmwareDrawerVisible.value = true;
    console.log('Open DeviceFirmwareDrawer for:', record);
  }

  function closeDeviceFirmwareDrawer() {
    isDeviceFirmwareDrawerVisible.value = false;
  }

  function handleDeviceFirmwareConfirm(selectedIds: number[]) {
    console.log('Selected device-firmware IDs:', selectedIds);
    // 处理选中的设备-固件组合
  }

  const gotoPresetGroupDetail = () => {
    go('/maScene/postProcessingFilesDetails');
  };

  function handleDelete(record: Recordable) {
    createMessage.success(`删除成功: ${record.id}`);
  }

  onActivated(() => {
    reload();
  });
</script>
