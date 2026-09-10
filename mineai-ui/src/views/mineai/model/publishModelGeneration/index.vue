<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar> </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:send-outlined',
              tooltip: '发布',
              color: 'success',
              onClick: handleRelease.bind(null, record),
            },
            {
              icon: 'ant-design:close-circle-outlined',
              tooltip: '拒绝发布',
              color: 'error',
              onClick: handleReject.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './modelGeneration';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted, ref } from 'vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { createMessage } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '发布任务管理',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'modelGeneration/dynamicFindPublishingModelGenerationPage',
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
      width: 40,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  //刷新状态
  const loading = ref(true);

  function handleRelease(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelGeneration/publishModelGeneration',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('发布成功！');
        reload();
      });
  }
  function handleReject(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelGeneration/rejectPublishModelGeneration',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('该训练任务驳回成功！');
        reload();
      });
  }
  onMounted(async () => {
    loading.value = false;
  });
</script>
