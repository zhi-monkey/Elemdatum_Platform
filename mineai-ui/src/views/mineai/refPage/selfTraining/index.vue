<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar v-if="hasPermission([RoleEnum.SystemUser])">
        <a-button type="primary" @click="handleCreate"> 新增自迭代训练</a-button>
      </template>
      <template #action="{ record }" v-if="hasPermission([RoleEnum.SystemUser])">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '训练详情',
              onClick: gotoApplicationDetail.bind(null, record),
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
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <!--    <ApplicationModal @register="createAndUpdateModal" />-->
  </div>
</template>
<script lang="ts" setup>
  import { onActivated } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import { columns, searchFormSchema, simpleColumns, simpleSearchFormSchema } from './model.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { useRouter } from 'vue-router';
  import { computed, ComputedRef } from 'vue';

  const router = useRouter(); // 获取 router 实例
  const go = useGo();
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '自迭代训练列表',
    api: () => {
      return Promise.resolve({
        items: [
          {
            id: 1,
            name: '131213',
            dataset: '数据集1',
            algorithm: '算法1',
            status: 'training',
            time: '2023-09-01 08:29:45',
          },
          {
            id: 2,
            name: '123123',
            dataset: '数据集1',
            algorithm: '算法2',
            status: 'waitingForApproval',
            time: '2023-09-02 08:29:45',
          },
          {
            id: 3,
            name: 'test_gudie_0701',
            dataset: '数据集1',
            algorithm: '算法1',
            status: 'training',
            time: '2023-09-03 08:29:45',
          },
          {
            id: 4,
            name: 'asdasd',
            dataset: '数据集2',
            algorithm: '算法2',
            status: 'collecting',
            time: '2023-09-04 08:29:45',
          },
          {
            id: 5,
            name: 'test_guide_0001',
            dataset: '数据集3',
            algorithm: '算法2',
            status: 'completed',
            time: '2023-09-11 08:29:45',
          },
          {
            id: 6,
            name: 'test0622_1',
            dataset: '数据集4',
            algorithm: '算法3',
            status: 'testing',
            time: '2023-09-01 08:29:45',
          },
          {
            id: 7,
            name: 'test',
            dataset: '数据集5',
            algorithm: '算法2',
            status: 'completed',
            time: '2023-07-01 08:29:45',
          },
          {
            id: 8,
            name: '0622test',
            dataset: '数据集6',
            algorithm: '算法5',
            status: 'converting',
            time: '2023-08-29 08:29:45',
          },
          {
            id: 9,
            name: 'peo_test_gu_0001',
            dataset: '数据集7',
            algorithm: '算法6',
            status: 'waitingToBeIssued',
            time: '2023-09-22 08:29:45',
          },
          {
            id: 10,
            name: 'pro_GUIDE_test_001',
            dataset: '数据集8',
            algorithm: '算法4',
            status: 'completed',
            time: '2023-09-02 08:29:45',
          },
        ],
        total: 10,
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
    columns: userPermission.value ? columns : simpleColumns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: userPermission.value ? searchFormSchema : simpleSearchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 60,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      ifShow: userPermission.value,
    },
  });

  // 跳转算法类型管理页面
  /*
function goManageModelType() {
go('/maScene/sceneInfoManagement');
}
*/

  function handleCreate() {
    //console.log("新增自迭代训练任务");
    router.push({
      path: '/maTrainingCenter/selfTrainingCreate', // 页面路径
    });
  }

  /*function handleEdit(record: Recordable) {
openCreateAndUpdateModal(true, {
  record,
  isUpdate: true,
});
}*/
  //点击编辑操作
  function handleEdit() {
    console.log('编辑');
    router.push({
      path: '/maRef/selfTrainingEdit', // 页面路径
    });
  }

  //查看训练详情
  const gotoApplicationDetail = () => {
    /*go('/maScene/applicationDetail');*/
    console.log('查看训练详情');
    router.push({
      path: '/maRef/selfTrainingDetails', // 页面路径
    });
  };

  /*function handleDelete(record: Recordable) {
maHttp
  .post(
    {
      url: 'model/deleteModel',
      params: record,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  )
  .then(() => {
    createMessage.success('删除成功！');
    reload();
  });
}*/
  function handleDelete() {
    console.log('删除');
  }

  onActivated(() => {
    reload();
  });
</script>
