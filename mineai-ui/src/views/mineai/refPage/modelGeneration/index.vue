<template>
  <div>
    <BasicTable @register="registerTable" rowKey="id">
      <template #expandedRowRender="{ record }">
        <SubTable
          :id="record.id"
          :url="jobUrl"
          :train-model-version-image="
            record.model
              ? record.model.trainModelVersion.url
              : record.modelStore
              ? record.modelStore.trainModelVersion.url
              : ''
          "
          :convert-model-version-image="
            record.model && record.model.convertModelVersion
              ? record.model.convertModelVersion.url
              : record.modelStore && record.modelStore.convertModelVersion
              ? record.modelStore.convertModelVersion.url
              : ''
          "
          :url-prefix="MaBackendUrlEnum.MODEL_MANAGER"
          :columns="jobColumns"
          @success="handleSuccess"
        />
      </template>
      <template #toolbar>
        <Butwton type="primary" @click="handleCreate">新增任务</Butwton>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:edit-outlined',
              tooltip: '编辑',
              onClick: editGeneration.bind(null, record),
              ifShow: record.status !== 1 && record.status !== 2 && record.status !== 17,
            },
            {
              icon: 'ant-design:right-circle-outlined',
              tooltip: '开始训练',
              color: 'success',
              onClick: CreateTrainJob.bind(null, record),
              ifShow: record.status !== 1 && record.status !== 10,
            },
            {
              icon: 'ant-design:down-square-outlined',
              tooltip: '查看训练日志',
              onClick: goTrainJobDetails.bind(null, record),
              ifShow:
                record.status === 1 ||
                record.status === 6 ||
                record.status === 4 ||
                record.status === 10 ||
                record.status === 14,
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除标准化任务',
              color: 'error',
              popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
              ifShow:
                record.status !== 1 &&
                record.status !== 2 &&
                record.status !== 10 &&
                record.status !== 14,
            },
            {
              icon: 'ant-design:pause-circle-outlined',
              tooltip: '停止子任务',
              popConfirm: { title: '是否停止子任务', confirm: handleStop.bind(null, record) },
              ifShow: record.status === 1 || record.status === 2,
            },
          ]"
          :stopButtonPropagation="true"
        />
      </template>
    </BasicTable>
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import SubTable from './SubTable.vue';
  import { Button, Tag } from 'ant-design-vue';
  import { columns, searchFormSchema } from './modelGeneration';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { h, onMounted, onUnmounted, Ref, ref } from 'vue';
  import { router } from '/@/router';
  // import { downloadByUrl } from '/@/utils/file/download';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  import { mockApiResponse } from '/@/views/mineai/refPage/modelGeneration/mockdata'; // 引入假数据

  const jobColumns = [
    {
      title: 'id',
      dataIndex: 'id',
      width: 20,
      align: 'center',
    },
    {
      title: '模型名称',
      dataIndex: 'name',
      width: 100,
      align: 'center',
    },
    {
      title: '作业状态',
      dataIndex: 'status',
      width: 60,
      align: 'center',
      customRender: ({ record }) => {
        let text;
        let color;
        if (record.jobType === 1) {
          if (record.status === 2) {
            text = '训练中';
            color = 'yellow';
          } else if (record.status === 1) {
            text = '训练成功';
            color = 'green';
          } else if (record.status === 0) {
            text = '作业未激活';
            color = 'grey';
          } else if (record.status === -1) {
            text = '训练失败';
            color = 'red';
          } else if (record.status === -2) {
            text = '取消状态';
            color = 'grey';
          }
        }
        if (record.jobType === 2) {
          if (record.status === 2) {
            text = '质检中';
            color = 'yellow';
          } else if (record.status === 1) {
            text = '质检成功';
            color = 'green';
          } else if (record.status === 0) {
            text = '作业未激活';
            color = 'grey';
          } else if (record.status === -1) {
            text = '质检失败';
            color = 'red';
          } else if (record.status === -2) {
            text = '取消状态';
            color = 'grey';
          }
        }
        if (record.jobType === 3) {
          if (record.status === 2) {
            text = '转换中';
            color = 'yellow';
          } else if (record.status === 1) {
            text = '转换成功';
            color = 'green';
          } else if (record.status === 0) {
            text = '作业未激活';
            color = 'grey';
          } else if (record.status === -1) {
            text = '转换失败';
            color = 'red';
          } else if (record.status === -2) {
            text = '取消状态';
            color = 'grey';
          }
        }
        return h(Tag, { color: color }, () => text);
      },
    },
    { title: '任务开始时间', dataIndex: 'createTime', width: 80, align: 'center' },
    { title: '任务结束时间', dataIndex: 'lastJobTime', width: 80, align: 'center' },
    {
      title: '操作',
      dataIndex: 'action',
      width: 80,
      align: 'center',
      slots: { customRender: 'action' },
    },
  ];

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const userId = ref();
  const go = useGo();
  const mgTrainUpdateList = ref([]);
  const mgTrainWaitList = ref([]);
  const timerTrain: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const timerTrainWait: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const { createMessage } = useMessage();
  const jobUrl = 'modelJob/findModelJobsByModelGenerationId?modelGenerationId=';

  const [registerTable, { reload, updateTableDataRecord }] = useTable({
    title: '标准化训练任务管理',
    canResize: false,
    expandRowByClick: true,
    rowKey: 'id',
    api: async (params) => {
      console.log(mockApiResponse);
      return mockApiResponse;
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: async (v) => {
      // GET请求被解析为 VBen表格Table的行后，做一些操作
      const ids = v.map((m) => m.id);
      if (ids.length > 0) {
        //获取所有正在训练
        mgTrainUpdateList.value = v.filter((m) => m.status === 1);
        //获取所有延时训练任务等待中
        mgTrainWaitList.value = v.filter((m) => m.status === 12);
        if (mgTrainUpdateList.value.length > 0) {
          clearInterval(timerTrain.value);
          timerTrain.value = setInterval(() => updateMGTrainStatus(), 3 * 1000);
        }
        if (mgTrainWaitList.value.length > 0) {
          clearInterval(timerTrainWait.value);
          timerTrainWait.value = setInterval(() => updateMGTrainWaitStatus(), 3 * 1000);
        }
      }
      return v;
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
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  //跳转至详情日志页面
  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.latestJobId}_1`);
  }

  //训练任务的状态更新
  async function updateMGTrainStatus() {
    // 状态变更完成，关闭定时器
    if (mgTrainUpdateList.value.length === 0) {
      clearInterval(timerTrain.value);
      return;
    }
    //查询状态是否发生变化
    const result = await getMGStatus({
      mgIds: mgTrainUpdateList.value.map((e) => {
        return e.id;
      }),
    });
    let updateList: any[] = [];
    // 用于保存需要更新的行
    Object.keys(result).forEach((e) => {
      //判断训练中
      if (result[e] === 1) {
        updateList.push({
          id: Number(e),
          status: Number(result[e]),
        });
        mgTrainUpdateList.value.splice(
          mgTrainUpdateList.value.findIndex((m) => m.id === e),
          1,
        );
      }
    });
    if (updateList.length > 0) {
      // 更新该行 api
      updateList.forEach((e) => {
        updateTableDataRecord(e.id, e);
      });
    }
  }

  //等待训练任务状态更新
  async function updateMGTrainWaitStatus() {
    // 状态变更完成，关闭定时器
    if (mgTrainWaitList.value.length === 0) {
      clearInterval(timerTrainWait.value);
      return;
    }

    //查询状态是否发生变化
    const result = await getMGStatus({
      mgIds: mgTrainWaitList.value.map((e) => {
        return e.id;
      }),
    });
    let updateList: any[] = [];
    // 用于保存需要更新的行
    Object.keys(result).forEach((e) => {
      //判断质检中
      if (result[e] === 1) {
        updateList.push({
          id: Number(e),
          status: Number(result[e]),
        });
        mgTrainWaitList.value.splice(
          mgTrainWaitList.value.findIndex((m) => m.id === e),
          1,
        );
      }
    });
    if (updateList.length > 0) {
      // 更新该行 api
      updateList.forEach((e) => {
        updateTableDataRecord(e.id, e);
      });
    }
  }

  const getMGStatus = async (params) => {
    return await maHttp.get(
      {
        url: 'modelGeneration/getMGStatus',
        params,
        headers: {},
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  };

  //刷新状态
  const loading = ref(true);

  onMounted(async () => {
    loading.value = false;
    let isAdmin = false;
    userData.roles.forEach((role) => {
      if (role.id === 1) {
        isAdmin = true;
      }
    });
    if (!isAdmin) {
      userId.value = userData.id;
    }
  });

  onUnmounted(() => {
    //退出时关闭定时器
    clearInterval(timerTrain.value);
    clearInterval(timerTrainWait.value);
  });

  function handleSuccess() {
    reload();
  }

  function handleCreate() {
    router.push({
      path: '/maRef/generationCreate', //页面路径
    });
  }

  function editGeneration(record: Recordable) {
    router.push({
      path: '/maRef/generationEdit', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelGenerationName: record.name,
        modelGenerationId: record.id,
      },
    });
  }

  //创建训练作业
  async function CreateTrainJob(record: Recordable) {
    await maHttp
      .get(
        {
          url: 'modelJob/createJob',
          params: {
            modelGenerationId: record.id,
            modelWorkingMode: 1,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(
        () => {
          createMessage.success('训练作业创建成功');
          reload();
        },
        () => {
          createMessage.error('训练作业创建失败！');
        },
      );
  }

  function handleDelete(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelGeneration/deleteModelGeneration',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('标准化任务删除成功！');
        reload();
      });
  }

  function handleStop(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelJob/stopJob',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('子任务停止成功！');
        reload();
      });
  }

  // function handleDownloadTrainLog(record: Recordable) {
  //   maHttp
  //     .get(
  //       {
  //         url: 'modelGeneration/getTrainLogPath',
  //         params: {
  //           modelGenerationId: record.id,
  //           limit: 100,
  //           start: 1,
  //         },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then((v: string) => {
  //       let url = getFileDownloadUrl(v);
  //       let file = v.split('/').pop();
  //       downloadByUrl({
  //         url: url,
  //         target: '_self',
  //         fileName: file,
  //       });
  //     });
  // }
  // function handleDownloadTestLog(record: Recordable) {
  //   maHttp
  //     .get(
  //       {
  //         url: 'modelGeneration/testJobLogStorage',
  //         params: {
  //           modelGenerationId: record.id,
  //           limit: 100,
  //           start: 1,
  //         },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then((v: string) => {
  //       let url = getFileDownloadUrl(v);
  //       let file = v.split('/').pop();
  //       downloadByUrl({
  //         url: url,
  //         target: '_self',
  //         fileName: file,
  //       });
  //     });
  // }

  // function goTestJobDetails(testJob: Recordable, anotherTestJob: Recordable) {
  //   if (anotherTestJob == null) {
  //     go(`/maTrainingCenter/testJobDetails/${testJob.id}_${nullJobId}`);
  //   } else {
  //     go(`/maTrainingCenter/testJobDetails/${testJob.id}_${anotherTestJob.id}`);
  //   }
  // }

  // function handleSuccess() {
  //   createMessage.success('成功！');
  //   reload();
  // }
  //
  // function getFileDownloadUrl(filePath) {
  //   return (
  //     MaBackendUrlEnum.MODEL_MANAGER +
  //     'storage/getFileStreamNew?' +
  //     'fileName=' +
  //     encodeURIComponent(filePath) +
  //     '&datasetName=' +
  //     encodeURIComponent('/')
  //   );
  // }

  // function handleDownloadWeightFileList(record: Recordable) {
  //   maHttp
  //     .get(
  //       {
  //         url: 'modelGeneration/getWeightPath',
  //         params: {
  //           modelGenerationId: record.id,
  //         },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then((v: string[]) => {
  //       for (let i = 0; i < v.length; i++) {
  //         let url = getFileDownloadUrl(v[i]);
  //         let file = v[i].split('/').pop();
  //         downloadByUrl({
  //           url: url,
  //           target: '_self',
  //           fileName: file,
  //         });
  //       }
  //     });
  // }
</script>
