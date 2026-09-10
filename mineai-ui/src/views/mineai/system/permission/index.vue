<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> 创建权限组</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              disabled: record.id === ADMIN_PERMISSION_ID,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <PermissionModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import PermissionModal from './PermissionModal.vue';
  import { useModal } from '/@/components/Modal';

  import { columns, searchFormSchema } from './permission.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton } from 'ant-design-vue';
  import { list, del } from '/@/views/mineai/system/api/permission';

  const ADMIN_PERMISSION_ID = 1; // 管理员权限id
  const [registerModal, { openModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    title: '权限管理',
    api: async (params) => {
      const v = await list(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 Vben表格Table的行后，做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
    },
    columns,
    formConfig: {
      labelWidth: 120,
      schemas: searchFormSchema,
      showAdvancedButton: false,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 80,
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
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  const { createMessage } = useMessage();

  function handleDelete(record: Recordable) {
    del([record.id])
      .then(() => {
        createMessage.success('删除成功！');
        reload();
      })
      .catch((e) => {
        console.error(e);
      });
  }

  function handleSuccess() {
    reload();
  }
</script>
