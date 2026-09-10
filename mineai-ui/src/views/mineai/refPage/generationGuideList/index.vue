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
              tooltip: '查看详情',
              color: 'warning',
              onClick: goTrainJobDetails.bind(null, record),
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
    <!--    <DeployMentModel @register="registerDeployModal" @success="handleSuccess" />-->
    <!--    <CancelDeployModel @register="registerCancelDeployModal" @success="handleSuccess" />-->
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
      return getModelGenerationPage();
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
      path: '/maRef/generationGuide', //页面路径
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

  function getModelGenerationPage() {
    return {
      items: [
        {
          id: 168,
          name: 'pro_test_0001',
          trainModelVersion: null,
          testModelVersion: null,
          deployModelVersion: null,
          trainJob: null,
          testJob: null,
          anotherTestJob: null,
          model: {
            id: 111,
            modelName: 'test_model',
            modelNickName: 'test_model_nick_name',
            mainConfig: null,
            monitorType: 0,
            description: 'test_model_desc',
            areaLabelingTip: null,
            isDelete: 0,
            modelStatus: 1,
            isBoundWithGeneration: 2,
            modelConfigList: [],
            subsystem: 'CENTRAL_PLATFORM',
            createTime: '2024-07-01 12:39:55',
            applyTime: '2024-08-28 18:15:19',
            creatorId: 1,
            applicantId: 1,
            releaseStatus: 1,
            dataType: null,
            trainModelVersion: {
              id: 69,
              name: 'trainimage_1.0',
              level: '1.0',
              versionLevel: 0,
              showName: 'trainimage',
              description: 'desc',
              size: 2048,
              modelConfigList: [
                {
                  id: 397,
                  field: 'HP_BATCH_SIZE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:[1,)',
                },
                {
                  id: 398,
                  field: 'HP_CONFIDENCE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(0,1)',
                },
                {
                  id: 399,
                  field: 'HP_WEIGHT_DECAY',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
                {
                  id: 400,
                  field: 'HP_EPOCHES',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:[1,)',
                },
                {
                  id: 401,
                  field: 'HP_MOMENTUN',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
                {
                  id: 402,
                  field: 'HP_LEARNING_RATE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
              ],
              architecture: 'arm64',
              weightPath: null,
              reuseId: 0,
              url: 'trainimage_image1719808736031',
              createTime: '2024-07-01 12:38:57',
              isDelete: 0,
              trainNum: null,
              inspectNum: null,
              areaLabelingTip: null,
              status: 1,
              conversionPlatform: null,
              roleId: 1,
              inspectable: false,
              autoLabel: false,
              reuse: false,
              useGpu: true,
              trainable: true,
              inferable: false,
            },
            deployModelVersion: null,
            convertModelVersion: {
              id: 66,
              name: 'convert0619_v1',
              level: 'v1',
              versionLevel: 0,
              showName: 'convert0619',
              description: 'convert',
              size: 2048,
              modelConfigList: [],
              architecture: 'arm64',
              weightPath: null,
              reuseId: 0,
              url: 'convert0619_image1718774009027',
              createTime: '2024-06-19 13:13:29',
              isDelete: 0,
              trainNum: null,
              inspectNum: null,
              areaLabelingTip: null,
              status: 1,
              conversionPlatform: null,
              roleId: 1,
              inspectable: true,
              autoLabel: false,
              reuse: false,
              useGpu: false,
              trainable: false,
              inferable: false,
            },
            bestWeightPath: 'job-cloud-merge-665',
            source: 1,
            rejectReason: null,
            autoLabelModelVersion: null,
            releaseJob: {
              id: 665,
              name: 'job-cloud-merge-665',
              modelVersion: null,
              jobType: 1,
              controller: null,
              status: 1,
              modelGenerationId: 168,
              description: null,
              params: {
                HP_BATCH_SIZE: '10',
                HP_CONFIDENCE: '0.85',
                HP_EPOCHES: '5',
                HP_WEIGHT_DECAY: '0.0005',
                HP_MOMENTUN: '0.937',
                HP_LEARNING_RATE: '0.01',
                MODEL_WORKING_MODE: '1',
              },
              weightPath: 'job-cloud-merge-665',
              memory: '10Gi',
              cpus: '4',
              gpus: '2',
              logFilePath: null,
              createTime: '2024-07-01 12:43:43',
              lastJobTime: '2024-07-01 12:45:00',
              accuracy: null,
            },
          },
          isRelease: null,
          isReuse: true,
          isGuided: false,
          trainDataset: {
            id: 227,
            datasetId: 179,
            dataType: 0,
            annotateType: 102,
            name: 'test0619sdajadj',
            versionName: 'V0001',
            versionSource: null,
            fullName: null,
            createTime: '2024-06-19T03:09:55.000+00:00',
            versionNote: '',
            isCurrent: true,
            status: 0,
            fileCount: null,
            progressVO: null,
            versionUrl: 'dataset/179/versionFile/V0001',
            versionOfRecordUrl: null,
            dataConversion: 1,
            imageCounts: null,
            presetFlag: null,
            isOfRecord: null,
            format: 'YOLO',
          },
          testDataset: null,
          valDataset: null,
          hasDeployments: null,
          datasetSource: 0,
          splitSize: null,
          createTime: '2024-07-01 12:43:09',
          status: 10,
          description: 'pro_test_0001_desc',
          hardwareParams: {
            id: 4,
            name: '4核10G',
            memory: '10Gi',
            cpus: '4',
            gpus: '2',
          },
          delayTrainTime: 0,
          maxTrainTime: 0,
          userName: '系统管理员',
          latestJobId: 665,
          modelStore: null,
          canPublish: null,
          hyperParams: {
            HP_BATCH_SIZE: '10',
            HP_CONFIDENCE: '0.85',
            HP_EPOCHES: '5',
            HP_WEIGHT_DECAY: '0.0005',
            HP_MOMENTUN: '0.937',
            HP_LEARNING_RATE: '0.01',
            MODEL_WORKING_MODE: '1',
          },
        },
        {
          id: 165,
          name: 'test111',
          trainModelVersion: null,
          testModelVersion: null,
          deployModelVersion: null,
          trainJob: null,
          testJob: null,
          anotherTestJob: null,
          model: {
            id: 110,
            modelName: '安全帽',
            modelNickName: 'safecap',
            mainConfig: null,
            monitorType: 0,
            description: '识别算法',
            areaLabelingTip: null,
            isDelete: 0,
            modelStatus: 1,
            isBoundWithGeneration: 2,
            modelConfigList: [],
            subsystem: 'CENTRAL_PLATFORM',
            createTime: '2024-06-22 20:26:23',
            applyTime: null,
            creatorId: 1,
            applicantId: 0,
            releaseStatus: 0,
            dataType: null,
            trainModelVersion: {
              id: 64,
              name: 'asda_v.10',
              level: 'v.10',
              versionLevel: 0,
              showName: 'asda',
              description: 'desc',
              size: 2048,
              modelConfigList: [
                {
                  id: 376,
                  field: 'HP_BATCH_SIZE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:[1,)',
                },
                {
                  id: 377,
                  field: 'HP_CONFIDENCE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(0,1)',
                },
                {
                  id: 378,
                  field: 'HP_WEIGHT_DECAY',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
                {
                  id: 379,
                  field: 'HP_EPOCHES',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:[1,)',
                },
                {
                  id: 380,
                  field: 'HP_MOMENTUN',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
                {
                  id: 381,
                  field: 'HP_LEARNING_RATE',
                  label: null,
                  defaultNum: null,
                  required: true,
                  min: null,
                  max: null,
                  msg: 'numeric:(,)',
                },
              ],
              architecture: 'arm64',
              weightPath: null,
              reuseId: 0,
              url: 'asda_image1718771427271',
              createTime: '2024-06-19 12:30:28',
              isDelete: 0,
              trainNum: null,
              inspectNum: null,
              areaLabelingTip: null,
              status: 1,
              conversionPlatform: null,
              roleId: 1,
              inspectable: false,
              autoLabel: false,
              reuse: false,
              useGpu: true,
              trainable: true,
              inferable: false,
            },
            deployModelVersion: null,
            convertModelVersion: null,
            bestWeightPath: null,
            source: 1,
            rejectReason: null,
            autoLabelModelVersion: null,
            releaseJob: null,
          },
          isRelease: null,
          isReuse: true,
          isGuided: false,
          trainDataset: {
            id: 227,
            datasetId: 179,
            dataType: 0,
            annotateType: 102,
            name: 'test0619sdajadj',
            versionName: 'V0001',
            versionSource: null,
            fullName: null,
            createTime: '2024-06-19T03:09:55.000+00:00',
            versionNote: '',
            isCurrent: true,
            status: 0,
            fileCount: null,
            progressVO: null,
            versionUrl: 'dataset/179/versionFile/V0001',
            versionOfRecordUrl: null,
            dataConversion: 1,
            imageCounts: null,
            presetFlag: null,
            isOfRecord: null,
            format: 'YOLO',
          },
          testDataset: null,
          valDataset: null,
          hasDeployments: null,
          datasetSource: 1,
          splitSize: '1-2-1',
          createTime: '2024-06-22 21:04:59',
          status: 9,
          description: null,
          hardwareParams: {
            id: 3,
            name: '2核2G',
            memory: '2Gi',
            cpus: '2',
            gpus: '1',
          },
          delayTrainTime: 0,
          maxTrainTime: 0,
          userName: '系统管理员',
          latestJobId: 629,
          modelStore: null,
          canPublish: null,
          hyperParams: {
            HP_BATCH_SIZE: '1',
            HP_CONFIDENCE: '1',
            HP_EPOCHES: '1',
            HP_WEIGHT_DECAY: '1',
            HP_MOMENTUN: '1',
            HP_LEARNING_RATE: '1',
            MODEL_WORKING_MODE: '1',
          },
        },
      ],
      pageable: {
        sort: {
          unsorted: false,
          sorted: true,
          empty: false,
        },
        pageNumber: 0,
        pageSize: 10,
        offset: 0,
        paged: true,
        unpaged: false,
      },
      last: false,
      totalPages: 2,
      total: 16,
      sort: {
        unsorted: false,
        sorted: true,
        empty: false,
      },
      first: true,
      numberOfElements: 10,
      size: 10,
      number: 0,
      empty: false,
    };
  }
</script>
