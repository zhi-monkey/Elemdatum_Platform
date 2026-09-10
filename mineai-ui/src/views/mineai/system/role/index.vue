<template>
  <div>
    <BasicTable @register="registerTable" @selection-change="selectionChange">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate">新增角色</a-button>
        <a-button type="primary" @click="handleGroupDelete" :disabled="canDelete">
          批量删除</a-button
        >
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑角色',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: record.isBound ? '角色被绑定无法删除' : '删除角色',
              color: 'error',
              disabled: record.isBound
                ? true
                : record.id === ADMIN_ROLE_ID || record.id === REGISTER_ROLE_ID,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
            {
              icon: 'ant-design:menu-outlined',
              tooltip: '编辑菜单',
              onClick: handleEditMenu.bind(null, record),
            },
            {
              icon: 'ant-design:apartment-outlined',
              tooltip: '编辑权限',
              onClick: handleEditPermission.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
    <RoleModal @register="registerModal" @success="handleSuccess" />
    <RoleDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import RoleModal from './RoleModal.vue';
  import { useModal } from '/@/components/Modal';

  import { columns, searchFormSchema } from './role.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton, Modal } from 'ant-design-vue';
  import { list, del, get } from '/@/views/mineai/system/api/role';
  import { list as perList } from '/@/views/mineai/system/api/permission';
  import RoleDrawer from '/@/views/mineai/system/role/RoleDrawer.vue';
  import { useDrawer } from '/@/components/Drawer';
  import { createVNode, onUnmounted, ref, Ref } from 'vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const [registerModal, { openModal }] = useModal();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    title: '角色列表',
    api: async (params) => {
      const v = await list(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      const v1 = await perList(params);
      v.items.forEach((role) => {
        const rolePermission = v1.result
          .filter((permission) => {
            return permission.authCode == role.name;
          })
          .map((permission) => permission.permissions);
        role.permissions = rolePermission[0];
      });
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
      showResetButton: true,
    },
    indexColumnProps: {
      width: 20,
    },
    clearSelectOnPageChange: true,
    rowSelection: {
      columnWidth: 20,
    },
    useSearchForm: true,
    showTableSetting: false,
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
  const ADMIN_ROLE_ID = 1; // 管理员角色id
  const REGISTER_ROLE_ID = 2; // 注册用户角色id
  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);
  const selectionChange = ({ keys, rows }) => {
    if (rows.length > 0 && rows[rows.length - 1].isBound) {
      keys.pop();
      rows.pop();
      return;
    }
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const handleGroupDelete = () => {
    if (selectedKeys.value.length <= 0) {
      return;
    }
    const modal = Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      onOk() {
        // 手动关闭弹窗
        modal.destroy();

        // 异步删除操作
        const ids = selectedRows.value.map((row) => row.id);
        maHttp
          .delete(
            {
              url: 'roles',
              data: { ids },
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
          )
          .then(() => {
            createMessage.success('删除成功！');
            // 清空所有选中的keys
            clearSelectedRowKeys();
            // 刷新页面
            reload();
          })
          .catch((e) => {
            console.error(e);
            createMessage.error('删除失败！');
            reload();
            clearSelectedRowKeys();
          });
      },
    });
  };

  function handleEdit(record: Recordable) {
    get(record.id)
      .then(({ auths }) => {
        const authCode = auths[0]?.authCode || '';
        const description = auths[0]?.description || '';
        const permissions = auths[0]?.permissions || '';
        console.log('test111', get(record.id));

        openModal(true, {
          record: {
            ...record,
            authCode: authCode,
            description: description,
            permissions: permissions,
          },
          isUpdate: true,
        });
      })
      .catch((error) => {
        console.error('Error fetching data:', error);
      });
  }

  function handleEditPermission(record: Recordable) {
    openDrawer(true, {
      record,
      isMenu: false,
    });
  }

  function handleEditMenu(record: Recordable) {
    openDrawer(true, {
      record,
      isMenu: true,
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

  onUnmounted(() => {
    clearSelectedRowKeys();
  });

  function handleSuccess() {
    reload();
  }
</script>
