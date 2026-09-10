<!--<script src="deviceDetailApi.tsx"></script>-->
<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar v-if="hasPermission([RoleEnum.Message_Write])">
        <Button type="primary" @click="handleCreate">新增设备固件</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
              disabled: !hasPermission([RoleEnum.Message_Write]),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
              disabled: !hasPermission([RoleEnum.Message_Write]),
            },
          ]"
        />
      </template>
    </BasicTable>
    <ApplicationModal @register="createAndUpdateModal" @success="handleFormSubmit" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from '/@/components/Button';
  import ApplicationModal from '/src/views/mineai/application/message/deviceDetail/addDeviceModel.vue';
  import { useModal } from '/@/components/Modal';
  import {
    deviceColumns,
    searchFormSchema,
  } from '/@/views/mineai/application/message/deviceDetail/device.data';
  import { deleteDeviceManager } from '/@/views/mineai/application/message/deviceDetail/deviceDetailApi.tsx';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { usePermission } from '/@/hooks/web/usePermission';

  const { createMessage } = useMessage();
  const { hasPermission } = usePermission();
  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();

  const [registerTable, { reload }] = useTable({
    title: '设备固件管理',
    api: (params) => {
      return maHttp
        .get(
          {
            url: `device/dynamicFindDevices`,
            method: 'POST',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
            params,
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
          v.items = v.content;
          v.total = v.totalElements;
          return v;
        })
        .catch((error) => {
          console.error('API 调用失败:', error);
          createMessage.error(`加载数据失败: ${error.message}`);
        });
    },
    columns: deviceColumns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      showResetButton: true,
    },
    useSearchForm: true,
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
    },
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
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

  function handleDelete(record: Recordable) {
    // 调用 deleteChipManager 函数进行删除
    deleteDeviceManager(record.id).then(() => {
      createMessage.success('删除成功！');
      reload(); // 删除成功后重新加载表格数据
    });
    // .catch((error) => {
    //   createMessage.error(`该设备已被绑定，无法删除: ${error.message}`);
    // });
  }

  // 处理表单提交后的操作
  function handleFormSubmit() {
    reload();
  }
</script>
