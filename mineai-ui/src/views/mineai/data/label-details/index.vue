<template>
  <div>
    <BasicTable
      @register="registerTable"
      @selection-change="selectionChange"
      @change="onPageChange"
    >
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增标签组</Button>
        <Button type="primary" @click="handleDelete" :disabled="canDelete">删除标签组</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
              disabled: record.id === 1,
            },
          ]"
        />
      </template>
    </BasicTable>
    <LabelGroupModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import LabelGroupModal from './LabelGroupModal.vue';
  import { columns, searchFormSchema } from './labelGroup.data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createVNode, Ref, ref } from 'vue';
  import { Modal, Button } from 'ant-design-vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    title: '标签组列表',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'labelGroup/query',
            params,
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
          v.items = v.result;
          v.total = v.page.total;
          return v;
        });
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
        type: 0,
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
    columns: columns,
    rowSelection: {
      type: 'checkbox',
    },
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
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
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      labelGroupId: record.id,
      isUpdate: true,
    });
  }

  function handleSuccess() {
    // 清空所有选中的keys
    clearSelectedRowKeys();
    // 刷新页面
    reload();
  }

  const handleDelete = async () => {
    console.log(selectedKeys);
    Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      async onOk() {
        const ids = selectedRows.value.map((row) => row.id);
        if (
          ids.some((v) => {
            return v === 1;
          })
        ) {
          createMessage.error('不能删除默认标签组！');
          // 清空所有选中的keys
          clearSelectedRowKeys();
          return;
        }

        try {
          await maHttp.delete(
            {
              url: 'labelGroup',
              data: { ids },
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
          createMessage.success('标签组删除成功！');
          // 清空所有选中的keys
          clearSelectedRowKeys();
          // 刷新页面
          await reload();
        } catch (e) {
          console.log('error', e);
        }
      },
    });
  };

  // 分页变化
  function onPageChange() {
    clearSelectedRowKeys();
  }

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);
  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };
</script>
