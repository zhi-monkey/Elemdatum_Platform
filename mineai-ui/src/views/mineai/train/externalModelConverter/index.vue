<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增设备</Button>
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
    <ApplicationModal @register="createAndUpdateModal" @success="handleFormSubmit" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from '/@/components/Button';
  import ApplicationModal from '/src/views/mineai/train/externalModelConverter/addExternalModelConverterModel.vue';
  import { useModal } from '/@/components/Modal';
  import {
    deviceConverterColumns,
    searchFormSchema,
  } from '/@/views/mineai/train/externalModelConverter/data';
  import {
    deleteConverterDeviceManager,
    findExternalModelConverter,
  } from '/src/views/mineai/train/externalModelConverter/externalModelConverterDetailApi.tsx';
  import { onMounted, ref } from 'vue';

  const { createMessage } = useMessage();

  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();

  let formattedItems = ref([]);
  const [registerTable, { reload }] = useTable({
    title: '设备管理',
    api: async (params) => {
      // 调用 API 获取数据
      const res = await findExternalModelConverter(params);
      formattedItems.value = res.content.map((item) => {
        return {
          ...item, // 保留原有的字段
          onlineStatus: item.onlineStatus ? '在线' : '离线', // 根据 onlineStatus 设置值
        };
      });
      console.log(formattedItems.value);
      // 返回数据格式化为表格需要的格式
      return {
        items: formattedItems.value,
        total: res.totalElements,
      };
    },
    columns: deviceConverterColumns,
    formConfig: {
      labelWidth: 130,
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
    deleteConverterDeviceManager(record.id)
      .then(() => {
        createMessage.success('删除成功！');
        reload(); // 删除成功后重新加载表格数据
      })
      .catch((error) => {
        createMessage.error(`删除失败: ${error.message}`);
      });
  }

  // 处理表单提交后的操作
  function handleFormSubmit() {
    reload();
  }

  onMounted(() => {
    reload(); // 重新加载表格
  });
</script>
