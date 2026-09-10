<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px" :contentStyle="{ margin: '0' }">
    <template #title>算法类型管理</template>
    <div>
      <BasicTable @register="registerTable">
        <template #toolbar>
          <a-button type="primary" @click="handleCreate">新增算法类型</a-button>
        </template>
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:form-outlined',
                tooltip: '编辑算法类型',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除算法类型',
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
      <ModelTypeModal @register="registerModal" @success="handleSuccess" />
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { columns } from './modelTypeData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ModelTypeModal from './modelTypeModal.vue';
  import { useRouter } from 'vue-router';
  import { Button as AButton } from 'ant-design-vue';

  const router = useRouter();
  const [registerModal, { openModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    title: '算法类型管理',
    api: async () => {
      return await maHttp
        .get(
          {
            url: 'modelClassifications',
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
    columns,
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
    maHttp
      .delete(
        {
          url: `modelClassifications/${record.id}`,
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
  }

  function handleSuccess() {
    reload();
  }

  function goBack() {
    router.go(-1);
  }
</script>
