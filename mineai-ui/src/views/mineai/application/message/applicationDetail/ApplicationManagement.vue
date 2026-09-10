<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar v-if="hasPermission([RoleEnum.Message_Write])">
        <a-button type="primary" @click="handleCreate"> 新增应用</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
              disabled: !hasPermission([RoleEnum.Message_Write]) || !record.canEdit,
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
              disabled: !hasPermission([RoleEnum.Message_Write]) || !record.canDelete,
            },
          ]"
        />
      </template>
    </BasicTable>
    <ApplicationModal @register="createAndUpdateModal" @success="handleFormSubmit" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onMounted } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import ApplicationModal from './applicationModal.vue';
  import {
    addAndUpdateApplicationName,
    deleteApplicationName,
    findApplicationName,
    findByApplicationName,
  } from '/@/views/mineai/application/message/applicationDetail/ApplicationManagementApi';
  import { useRouter } from 'vue-router';
  import {
    columns,
    searchFormSchema,
  } from '/@/views/mineai/application/message/applicationDetail/model.data';

  const router = useRouter();
  const go = useGo();
  const { hasPermission } = usePermission();
  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '应用列表',
    api: async (param) => {
      // 调用 API 获取数据
      const res = await findApplicationName(
        param.page,
        param.pageSize,
        param.order,
        param.applicationName,
        param.id,
      );
      // 返回数据格式化为表格需要的格式
      return {
        items: res.content,
        total: res.totalElements,
      };
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      return itemList;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    handleSearchInfoFn(info) {
      return info;
    },
    //expandRowByClick: true,
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
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

  //删除操作
  function handleDelete(record: Recordable) {
    // 调用 deleteSceneManager 函数进行删除
    deleteApplicationName(record.id).then(() => {
      createMessage.success('删除成功！');
      reload(); // 删除成功后重新加载表格数据
    });
    // .catch((error) => {
    //   createMessage.error(`该应用已被绑定，无法删除: ${error.message}`);
    // });
  }

  async function handleFormSubmit(data: any, isUpdate: boolean) {
    // 先调用查询接口，检查名字是否重复
    const response = await findByApplicationName(data.applicationName);
    if (response.length === 0) {
      // 名字不存在，可以新增或更新
      addAndUpdateApplicationName(data).then(() => {
        createMessage.success(isUpdate ? '更新成功！' : '新增成功！');
        reload(); // 刷新表格数据
      });
      // .catch((error) => {
      //   createMessage.error(`操作失败: ${error.message}`);
      // });
    } else {
      // 名字重复，提示失败
      createMessage.error('操作失败: 应用名称已存在');
    }
  }

  onMounted(() => {
    reload(); // 重新加载表格
  });
</script>
