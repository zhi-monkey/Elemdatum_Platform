<template>
  <div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:down-square-outlined',
              tooltip: '作业详情',
              color: 'success',
              onClick: goJobDetails.bind(null, record),
              ifShow: () => {
                return record.jobType !== 3;
              },
            },
            {
              icon: 'ant-design:stop-outlined',
              tooltip: '作业取消',
              popConfirm: { title: '是否确认取消', confirm: deleteJob.bind(null, record) },
              color: 'error',
              ifShow: () => {
                return record.jobType !== 3;
              },
            },
            {
              icon: 'ant-design:form-outlined',
              tooltip: '作业编辑',
              color: 'warning',
              onClick: handleEdit.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
    <JobModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  export default { name: 'JobList' };
</script>
<script lang="ts" setup>
  import { useGo } from '/@/hooks/web/usePage';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { columns, searchFormSchema } from './jobData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import JobModal from './JobModal.vue';
  import { onActivated, onMounted, onUnmounted, ref } from 'vue';

  const go = useGo();
  const [registerModal, { openModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    title: '作业管理',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'modelJob/jobList',
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
      width: 80,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  //刷新状态
  const loading = ref(true);
  let dataTimer;

  onMounted(async () => {
    await reload();
    loading.value = false;
    dataTimer = setInterval(reload, 10000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
  });

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  const { createMessage } = useMessage();

  function deleteJob(record: Recordable) {
    maHttp
      .post(
        {
          url: 'modelJob/abortJob',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('取消成功！');
        reload();
      });
  }

  onActivated(() => {
    reload();
  });

  function goJobDetails(record: Recordable) {
    go(`/maTrainingCenter/jobDetails/${record.id}_${record.jobType}`);
  }

  function handleSuccess() {
    reload();
  }
</script>
