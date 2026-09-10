<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> 新增算法</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:edit-outlined',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              disabled: record.isBoundWithGeneration === 2,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
  </div>
  <Modal @register="registerModal" @success="handleSuccess" />
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import {
    columns,
    getConvertMVList,
    getDeployMVList,
    getTrainMVList,
    searchFormSchema,
  } from './algorithm.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import Modal from './Modal/Modal.vue';

  const [registerModal, { openModal }] = useModal();
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '算法列表',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'modelExplore/dynamicFindUnReleasedModelPage',
            params,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
          v.items = v.content;
          v.total = v.totalElements;
          //console.log(v);
          return v;
        });
    },
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
    // columns: userPermission.value ? columns : simpleColumns,
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
    actionColumn: {
      width: 80,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      // ifShow: userPermission.value,
    },
  });

  async function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  async function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record: Recordable) {
    maHttp
      .delete(
        {
          url: `modelExplore/deleteModelExplore/${record.id}`,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('算法删除成功！');
        reload();
      });
  }

  onMounted(async () => {
    await getTrainMVList();
    await getDeployMVList();
    await getConvertMVList();
  });

  const handleSuccess = async (isEdit) => {
    // 根据isEdit标志判断是编辑还是新增，显示不同的提示信息
    if (isEdit) {
      createMessage.success('算法修改成功！');
    } else {
      createMessage.success('算法新增成功！');
    }
    // 刷新页面
    await reload();
  };

  onActivated(() => {
    reload();
  });
</script>
