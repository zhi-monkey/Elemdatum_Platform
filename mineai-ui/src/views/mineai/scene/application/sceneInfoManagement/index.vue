<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px" :contentStyle="{ margin: '0' }">
    <template #title>场景管理</template>
    <div>
      <BasicTable @register="registerTable">
        <template #toolbar>
          <a-button type="primary" @click="handleCreate">新增场景类型</a-button>
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
    title: '场景管理',
    api: async () => {
      return Promise.resolve({
        items: [
          {
            id: 1,
            classificationName: '排水',
            createTime: '2024-06-15 13:41:04.000000',
            updateTime: '2024-06-15 13:41:04.000000',
          },
          {
            id: 2,
            classificationName: '辅运',
            createTime: '2024-06-16 09:22:11.000000',
            updateTime: '2024-06-16 09:22:11.000000',
          },
          {
            id: 3,
            classificationName: '主运',
            createTime: '2024-06-17 15:33:22.000000',
            updateTime: '2024-06-17 15:33:22.000000',
          },
          {
            id: 4,
            classificationName: '综采',
            createTime: '2024-06-18 08:14:05.000000',
            updateTime: '2024-06-18 08:14:05.000000',
          },
          {
            id: 5,
            classificationName: '钻场',
            createTime: '2024-06-19 11:25:36.000000',
            updateTime: '2024-06-19 11:25:36.000000',
          },
          {
            id: 6,
            classificationName: '固定场所',
            createTime: '2024-06-20 17:49:12.000000',
            updateTime: '2024-06-20 17:49:12.000000',
          },
          {
            id: 7,
            classificationName: '其他',
            createTime: '2024-06-21 06:31:27.000000',
            updateTime: '2024-06-21 06:31:27.000000',
          },
          {
            id: 8,
            classificationName: '掘进',
            createTime: '2024-06-22 13:55:44.000000',
            updateTime: '2024-06-22 13:55:44.000000',
          },
          {
            id: 9,
            classificationName: '上井',
            createTime: '2024-06-23 09:18:01.000000',
            updateTime: '2024-06-23 09:18:01.000000',
          },
          {
            id: 10,
            classificationName: '识别',
            createTime: '2024-06-24 15:42:19.000000',
            updateTime: '2024-06-24 15:42:19.000000',
          },
          {
            id: 11,
            classificationName: '碳放水',
            createTime: '2024-06-25 20:07:35.000000',
            updateTime: '2024-06-25 20:07:35.000000',
          },
          {
            id: 12,
            classificationName: '排水',
            createTime: '2024-06-26 03:29:51.000000',
            updateTime: '2024-06-26 03:29:51.000000',
          },
          {
            id: 13,
            classificationName: '辅运',
            createTime: '2024-06-27 11:54:08.000000',
            updateTime: '2024-06-27 11:54:08.000000',
          },
          {
            id: 14,
            classificationName: '主运',
            createTime: '2024-06-28 07:21:24.000000',
            updateTime: '2024-06-28 07:21:24.000000',
          },
          {
            id: 15,
            classificationName: '综采',
            createTime: '2024-06-29 14:38:40.000000',
            updateTime: '2024-06-29 14:38:40.000000',
          },
          {
            id: 16,
            classificationName: '钻场',
            createTime: '2024-06-30 22:01:57.000000',
            updateTime: '2024-06-30 22:01:57.000000',
          },
          {
            id: 17,
            classificationName: '固定场所',
            createTime: '2024-07-01 05:25:13.000000',
            updateTime: '2024-07-01 05:25:13.000000',
          },
          {
            id: 18,
            classificationName: '其他',
            createTime: '2024-07-02 12:48:29.000000',
            updateTime: '2024-07-02 12:48:29.000000',
          },
          {
            id: 19,
            classificationName: '掘进',
            createTime: '2024-07-03 19:11:45.000000',
            updateTime: '2024-07-03 19:11:45.000000',
          },
          {
            id: 20,
            classificationName: '上井',
            createTime: '2024-07-04 01:35:02.000000',
            updateTime: '2024-07-04 01:35:02.000000',
          },
        ],
        total: 20,
      });
      // return await maHttp
      //   .get(
      //     {
      //       url: 'modelClassifications',
      //       headers: {
      //         // @ts-ignore
      //         ignoreCancelToken: true,
      //       },
      //     },
      //     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      //   )
      //   .then((v) => {
      //     //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      //     v.items = v.content;
      //     v.total = v.totalElements;
      //     return v;
      //   });
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
