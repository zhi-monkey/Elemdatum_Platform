<template>
  <div>
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增引导任务</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:right-circle-outlined',
              tooltip: '启动训练步骤',
              popConfirm: {
                title: '启动训练步骤',
                confirm: handleTrain.bind(null, record),
              },
              ifShow: () => {
                return record.status === 0 || record.status === 4;
              },
            },
            {
              icon: 'ant-design:right-circle-outlined',
              tooltip: '启动质检步骤',
              popConfirm: {
                title: '启动质检步骤',
                confirm: handleTest.bind(null, record),
              },
              ifShow: () => {
                return record.status === 6 || record.status === 5;
              },
            },
            {
              icon: 'ant-design:retweet-outlined',
              tooltip: '启动转换步骤',
              popConfirm: {
                title: '启动转换步骤',
                confirm: handleConvert.bind(null, record),
              },
              ifShow: () => {
                return record.status === 7;
              },
            },
            {
              icon: 'ant-design:down-square-outlined',
              tooltip: '查看训练日志',
              color: 'warning',
              onClick: goTrainJobDetails.bind(null, record),
              ifShow: () => {
                return (
                  record.status === 6 ||
                  record.status === 7 ||
                  record.status === 8 ||
                  record.status === 14
                );
              },
            },
            {
              icon: 'ant-design:send-outlined',
              tooltip: '查看发布情况',
              color: 'success',
              onClick: handleResult.bind(null, record),
              ifShow: () => {
                return record.status === 7;
              },
            },
          ]"
          :dropDownActions="[
            {
              //icon: 'ant-design:delete-outlined',
              tooltip: '删除任务',
              label: '删除任务',
              popConfirm: {
                title: '是否删除当前任务',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <DeployMentModel @register="registerDeployModal" @success="handleSuccess" />
    <CancelDeployModel @register="registerCancelDeployModal" @success="handleSuccess" />
  </div>
</template>

<script setup lang="ts">
  import BasicTable from '/@/components/Table/src/BasicTable.vue';
  import { TableAction, useTable } from '/@/components/Table';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import {
    columns,
    searchFormSchema,
  } from '/@/views/mineai/model/generationGuideList/generationGuideList.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from 'ant-design-vue';
  import { router } from '/@/router';
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  // import { useModal } from '/@/components/Modal';
  // import { downloadByUrl } from '/@/utils/file/download';
  import { useGo } from '/@/hooks/web/usePage';
  import DeployMentModel from '../modelGeneration/Modal/DeployMentModel.vue';
  import CancelDeployModel from '../modelGeneration/Modal/CancelDeployModel.vue';
  import { useUserStore } from '/@/store/modules/user';

  const go = useGo();
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const userId = ref();
  const mgTrainUpdateList = ref([]);
  const mgTestUpdateList = ref([]);
  // const [registerCancelDeployModal, { openModal: openCancelDeployModal }] = useModal();
  // const [registerDeployModal, { openModal: openDeployModal }] = useModal();
  const timerTrain: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const timerTest: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const { createMessage } = useMessage();
  const [registerTable, { reload, updateTableDataRecord }] = useTable({
    title: '引导任务管理',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'modelGeneration/dynamicFindGuidedModelGenerationPage',
            params: { ...params, userId: userId.value },
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
    afterFetch: async (v) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      const ids = v.map((m) => m.id);
      if (ids.length > 0) {
        //获取所有正在训练
        mgTrainUpdateList.value = v.filter((m) => m.status === 1);
        //获取所有正在质检
        mgTestUpdateList.value = v.filter((m) => m.status === 2);
        if (mgTrainUpdateList.value.length > 0) {
          clearInterval(timerTrain.value);
          timerTrain.value = setInterval(
            () => updateMGTrainStatus(mgTrainUpdateList.value),
            3 * 1000,
          );
        }
        if (mgTestUpdateList.value.length > 0) {
          clearInterval(timerTest.value);
          timerTest.value = setInterval(() => updateMGTestStatus(mgTestUpdateList.value), 3 * 1000);
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

  //训练任务的状态更新
  async function updateMGTrainStatus(mgList) {
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
    Object.keys(result).forEach((e, index) => {
      //判断训练中
      if (result[e] === 6 || result[e] === 4) {
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

  //质检任务状态更新
  async function updateMGTestStatus(mgList) {
    // 状态变更完成，关闭定时器
    if (mgTestUpdateList.value.length === 0) {
      clearInterval(timerTest.value);
      return;
    }

    //查询状态是否发生变化
    const result = await getMGStatus({
      mgIds: mgTestUpdateList.value.map((e) => {
        return e.id;
      }),
    });
    let updateList: any[] = [];
    // 用于保存需要更新的行
    Object.keys(result).forEach((e, index) => {
      //判断训练中
      if (result[e] === 7 || result[e] === 5) {
        updateList.push({
          id: Number(e),
          status: Number(result[e]),
        });
        mgTestUpdateList.value.splice(
          mgTestUpdateList.value.findIndex((m) => m.id === e),
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

  function handleCreate() {
    router.push({
      path: '/maTrainingCenter/generationGuide', //页面路径
      //这里写参数，可以多个参数
    });
  }

  // function compareGeneration(record: Recordable) {
  //   maHttp
  //     .get(
  //       {
  //         url: 'modelGeneration/compareModelGeneration',
  //         params: { modelGenerationId: record.id },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then((v) => {
  //       if (v === '可发布') {
  //         openReleaseModal(true, {
  //           record,
  //           result: '当前模型训练效果比最优模型更好，可以申请发布',
  //           boolResult: true,
  //         });
  //       } else {
  //         openReleaseModal(true, {
  //           record,
  //           result: '当前模型训练效果不如之前的模型，不允许发布',
  //           boolResult: false,
  //         });
  //       }
  //     });
  // }

  function handleTrain(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/generationGuide', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelGenerationId: record.id,
        modelGenerationName: record.name,
        currentStep: 1,
        jobType: 1,
      },
    });
  }

  function handleTest(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/generationGuide', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelGenerationId: record.id,
        modelGenerationName: record.name,
        currentStep: 1,
        jobType: 2,
      },
    });
  }

  function handleConvert(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/generationGuide', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelGenerationId: record.id,
        modelGenerationName: record.name,
        currentStep: 2,
        jobType: 3,
      },
    });
  }

  function handleResult(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/generationGuide', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelGenerationId: record.id,
        modelGenerationName: record.name,
        currentStep: 3,
        jobType: 1,
      },
    });
  }

  // //创建部署
  // function handleDeploy(record: Recordable) {
  //   openDeployModal(true, {
  //     record,
  //   });
  // }
  // function handCancelDeploy(record: Recordable) {
  //   openCancelDeployModal(true, { record });
  // }
  // function handleRelease(record: Recordable) {
  //   maHttp
  //     .get(
  //       {
  //         url: 'modelGeneration/askPublishModelGeneration',
  //         params: { modelGenerationId: record.id },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then(() => {
  //       createMessage.success('申请发布成功！');
  //       reload();
  //     });
  // }

  //刷新状态
  const loading = ref(true);
  let dataTimer;

  onMounted(async () => {
    loading.value = false;
    // dataTimer = setInterval(reload, 10000);
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
    clearInterval(dataTimer);
  });

  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.latestJobId}_1`);
  }

  function goTestJobDetails(testJob: Recordable, anotherTestJob: Recordable) {
    if (anotherTestJob === null) {
      go(`/maTrainingCenter/testJobDetails/${testJob.id}_${nullJobId}`);
    } else {
      go(`/maTrainingCenter/testJobDetails/${testJob.id}_${anotherTestJob.id}`);
    }
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
        createMessage.success('删除成功！');
        reload();
      });
  }

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
  //         console.log(v[i]);
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
