<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar v-if="hasPermission([RoleEnum.Message_Write])">
        <Button type="primary" @click="handleCreate">新增场景</Button>
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
              popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
              disabled: !hasPermission([RoleEnum.Message_Write]) || !record.canDelete,
            },
          ]"
        />
      </template>
    </BasicTable>
    <SceneModal @register="createAndUpdateModal" @success="handleFormSubmit" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useRouter } from 'vue-router';
  import { addAndUpdateScene, deleteScene, findScene } from './SceneManagementApi';
  import { computed, ComputedRef, onMounted } from 'vue';
  import SceneModal from '/@/views/mineai/application/message/sceneDetail/sceneModal.vue';
  import {
    columns,
    searchFormSchema,
  } from '/@/views/mineai/application/message/sceneDetail/modelTypeData';
  import { RoleEnum } from '/@/enums/roleEnum';

  const router = useRouter();
  const [registerModal, { openModal }] = useModal();
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();

  // 表格配置及数据加载
  const [registerTable, { reload }] = useTable({
    title: '场景列表',
    api: async (param) => {
      //console.log(param);
      // 调用 API 获取数据
      const res = await findScene(param.page, param.pageSize, param.order, param.name, param.id);
      // 返回数据格式化为表格需要的格式
      return {
        items: res.content,
        total: res.totalElements,
      };
    },
    /*columns: columns,*/
    beforeFetch: (v) => {
      // 将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      console.log('afterFetch', itemList);
      return itemList;
      // 过滤掉isDeleted = 1的数据
      //return itemList.filter(item => item.isDelete === 1);
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
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  //新增场景
  function handleCreate() {
    openCreateAndUpdateModal(true, {
      isUpdate: false,
    });
  }

  // 编辑场景
  function handleEdit(record: Recordable) {
    openCreateAndUpdateModal(true, {
      record,
      isUpdate: true,
    });
  }

  //删除操作
  function handleDelete(record: Recordable) {
    // 调用 deleteSceneManager 函数进行删除
    deleteScene(record.id).then(() => {
      createMessage.success('删除成功！');
      reload(); // 删除成功后重新加载表格数据
    });
    // .catch((error) => {
    //   createMessage.error(`该场景已被绑定，无法删除: ${error.message}`);
    // });
  }

  // 处理表单提交后的操作
  function handleFormSubmit(data: any, isUpdate: boolean) {
    addAndUpdateScene(data).then(() => {
      createMessage.success(isUpdate ? '更新成功！' : '新增成功！');
      reload(); // 刷新表格数据
    });
    // .catch((error) => {
    //   createMessage.error(`操作失败: ${error.message}`);
    // });
  }

  onMounted(() => {
    reload(); // 重新加载表格
  });
</script>
