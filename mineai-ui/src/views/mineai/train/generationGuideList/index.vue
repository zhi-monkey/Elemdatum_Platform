<template>
  <div>
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <Button type="primary" @click="openCreateModal">新增任务</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '详情',
              onClick: gotoDetail.bind(null, record),
              disabled: !record.trainDatasetStartAll,
            },
            {
              icon: 'ant-design:stop-outlined',
              tooltip: '停止任务',
              popConfirm: { title: '是否停止任务', confirm: handleStop.bind(null, record) },
              ifShow: record.statusForGuided === 1 || record.statusForGuided === 3,
            },
            // {
            //   icon: 'ant-design:node-expand-outlined',
            //   tooltip: '新建fork任务',
            //   onClick: handleCreateFork.bind(null, record),
            //   ifShow: () => {
            //     return record.statusForGuided === 2 || record.statusForGuided === 4;
            //   },
            // },
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
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除任务',
              onClick: handleDelete.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
    <!--    <DeployMentModel @register="registerDeployModal" @success="handleSuccess" />-->
    <!--    <CancelDeployModel @register="registerCancelDeployModal" @success="handleSuccess" />-->
    <!--  新建引导任务的drawer  -->
    <a-modal
      title="新建任务"
      :centered="true"
      v-model:visible="createModelVisible"
      width="70%"
      :footer="null"
      @close="onModalClose"
    >
      <TaskForm
        style="margin: 30px 0 30px 0"
        @can-be-commit="canBeCommit"
        @show-hyper-parameters="showHyperParameters"
        @train-dataset-select-change="handleTrainDatasetSelectChange"
        ref="taskFormRef"
      />
      <a-divider />
      <HyperParameterForm ref="hyperFormRef" v-show="isHyperParamVisible" />
      <div
        :style="{
          position: 'sticky',
          right: 0,
          bottom: 0,
          width: '100%',
          padding: '10px 16px',
          textAlign: 'right',
          zIndex: 1,
        }"
      >
        <!--        <a-button @click="resetFormData" style="margin-right: 8px" :loading="isSubmitting">-->
        <!--          清空数据-->
        <!--        </a-button>-->
        <a-button
          @click="startTrainConvertOrValidate"
          type="primary"
          :disabled="isStartDisabled"
          :loading="isSubmitting"
          >确认
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import BasicTable from '/@/components/Table/src/BasicTable.vue';
  import { TableAction, useTable } from '/@/components/Table';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Button, Divider as ADivider, Modal, Modal as AModal } from 'ant-design-vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import {
    getModelExploreByModelId,
    columns,
    createFork,
    getModelByApplicationNameAndDevice,
    getUniqueModelApplication,
    searchFormSchema,
    startGuidedGeneration,
    deleteGuidedModelGeneration,
    deleteDatasetBatch,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { router } from '/@/router';
  import { createVNode, onMounted, onUnmounted, Ref, ref, toRaw, unref } from 'vue';
  // import { useModal } from '/@/components/Modal';
  // import { downloadByUrl } from '/@/utils/file/download';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import TaskForm from '/@/views/mineai/train/generationGuideList/TaskForm.vue';
  import HyperParameterForm from '/@/views/mineai/train/generationGuideList/HyperParameterForm.vue';
  import { Recordable } from 'vite-plugin-mock';
  import { getHardwareParamsById } from '/@/views/mineai/model/generationGuide/api/modelApi';

  const taskFormRef = ref();
  const hyperFormRef = ref();
  const isStartDisabled = ref(true);

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
  const createModelVisible = ref(false);
  const { createMessage } = useMessage();
  const isHyperParamVisible = ref(false);

  const [registerTable, { reload, updateTableDataRecord }] = useTable({
    title: '引导式训练任务列表 ',
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
    showTableSetting: false,
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
  const resetFormData = () => {
    AModal.confirm({
      title: () => '你正在清除已填写的所有信息!',
      icon: () => createVNode(ExclamationCircleOutlined),
      content: () => '清除后的数据无法恢复, 确认清空数据吗',
      async onOk() {
        taskFormRef.value.resetFields();
        isHyperParamVisible.value = false;
        isStartDisabled.value = true;
        await taskFormRef.value.getApplicationName();
        await taskFormRef.value.getDeviceFirmware();
      },
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      onCancel() {},
    });
  };
  const openCreateModal = () => {
    createModelVisible.value = true;
  };

  const onModalClose = () => {
    createModelVisible.value = false;
  };

  // 校验用户资源所剩还够不够开这一次任务
  const checkResource = async (hardwareParams) => {
    // 如果没传hardwareParams，则从hyperFormRef中获取
    if (!hardwareParams) {
      hardwareParams = await getHardwareParamsById(unref(hyperFormRef.value.selectedResource));
    }
    console.log('checkResource', hardwareParams);
    const cpuNeed = Number.parseInt(hardwareParams.cpus);
    const memoryNeed = Number.parseInt(hardwareParams.memory.split('G')[0]);
    const gpuMemoryNeed = Number.parseInt(hardwareParams.gpuMemory.split('G')[0]);
    const userCpuMemoryUsed = await getUserResourceUsed(userData.id);
    if (userCpuMemoryUsed.cpuUsed + cpuNeed > userCpuMemoryUsed.cpuTotal) {
      createMessage.error(
        `您所在部门剩余的CPU资源不足(已用${userCpuMemoryUsed.cpuUsed}/共${
          userCpuMemoryUsed.cpuTotal
        }/可用${userCpuMemoryUsed.cpuTotal - userCpuMemoryUsed.cpuUsed})，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    if (userCpuMemoryUsed.memoryUsed + memoryNeed > userCpuMemoryUsed.memoryTotal) {
      createMessage.error(
        `您所在部门剩余的MEMORY资源不足(已用${userCpuMemoryUsed.memoryUsed}/共${
          userCpuMemoryUsed.memoryTotal
        }/可用${
          userCpuMemoryUsed.memoryTotal - userCpuMemoryUsed.memoryUsed
        })，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    if (userCpuMemoryUsed.gpuMemoryUsed + gpuMemoryNeed > userCpuMemoryUsed.gpuMemoryTotal) {
      createMessage.error(
        `您所在部门剩余的显存资源不足(已用${userCpuMemoryUsed.gpuMemoryUsed}/共${
          userCpuMemoryUsed.gpuMemoryTotal
        }/可用${
          userCpuMemoryUsed.gpuMemoryTotal - userCpuMemoryUsed.gpuMemoryUsed
        })，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    return true;
  };

  /**
   * 获取用户当前所在部门一共用了的CPU和内存
   * @param userId 用户ID
   */
  async function getUserResourceUsed(userId: any) {
    return maHttp.get(
      {
        url: 'users/getUserResourceUsed',
        params: {
          userId: userId,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
    );
  }

  const isSubmitting = ref(false);
  const startTrainConvertOrValidate = async () => {
    isSubmitting.value = true;
    try {
      await taskFormRef.value.validateForm();
      // await hyperFormRef.value.validateForm();
    } catch (e) {
      console.error(e);
      createMessage.error('请完善表单');
      isSubmitting.value = false;
      return;
    }
    // 获取model
    const model = await getModelByApplicationNameAndDevice(
      taskFormRef.value.formState.applicationName,
      taskFormRef.value.formState.deviceFirmware.substring(
        0,
        taskFormRef.value.formState.deviceFirmware.lastIndexOf('-'),
      ),
    );

    const modelApplication = await getUniqueModelApplication(
      taskFormRef.value.formState.applicationName,
      taskFormRef.value.formState.deviceFirmware,
    );
    // 处理dataSplit问题
    taskFormRef.value.formState.splitSize =
      taskFormRef.value.formState.datasetSplit === 'yes'
        ? taskFormRef.value.formState.splitSize
        : '';

    const params = {
      modelGeneration: {
        ...toRaw(unref(taskFormRef.value.formState)),
        isGuided: true,
        dataSource: 0,
        isReuse: true,
        userId: userData.id,
        modelExplore: toRaw(unref(modelExplore.value)),
        model: model,
      },
      modelApplication: modelApplication,
      datasetName: taskFormRef.value.formState.datasetName,
      datasetGroupName: taskFormRef.value.formState.datasetGroup,
    };
    let response;
    try {
      response = await startGuidedGeneration(params);
    } catch (e) {
      isSubmitting.value = false;
      return;
    }
    createModelVisible.value = false;
    taskFormRef.value.resetFields();
    isHyperParamVisible.value = false;
    isStartDisabled.value = true;
    await taskFormRef.value.getApplicationName();
    await taskFormRef.value.getDeviceFirmware();
    isSubmitting.value = false;
    await reload();

    // 创建成功后自动跳转到详情页
    if (response && typeof response === 'number') {
      try {
        // 使用列表API获取完整的任务信息（包含完整的modelApplication嵌套结构）
        const taskResponse = await maHttp.get(
          {
            url: 'modelGeneration/dynamicFindGuidedModelGenerationPage',
            params: {
              page: 0,
              size: 1,
              id: response,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );

        if (taskResponse && taskResponse.content && taskResponse.content.length > 0) {
          const record = taskResponse.content[0];
          gotoDetail(record);
        } else {
          createMessage.warning('任务创建成功，但获取详情失败，请从列表中查看');
        }
      } catch (e) {
        console.error('获取新任务详情失败:', e);
        createMessage.warning('任务创建成功，但获取详情失败，请从列表中查看');
      }
    }
  };

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
      const newStatus = result[e];
      const currentRow = mgTrainUpdateList.value.find((m) => m.id === Number(e));

      if (!currentRow) {
        // 如果找不到当前行，直接跳过
        return;
      }
      const currentStatus = currentRow.statusForGuided;
      //判断训练中
      if (newStatus !== currentStatus) {
        updateList.push({
          id: Number(e),
          statusForGuided: Number(newStatus),
        });
        if (newStatus < 0 || newStatus === 4) {
          // 状态异常的时候或者训练成功才取消轮询
          mgTrainUpdateList.value.splice(
            mgTrainUpdateList.value.findIndex((m) => m.id === e),
            1,
          );
        }
      }
    });
    if (updateList.length > 0) {
      // 更新该行 api
      updateList.forEach((e) => {
        updateTableDataRecord(e.id, e);
      });
    }
  }

  const modelExplore = ref({});
  const canBeCommit = async () => {
    const modelApplication = await getUniqueModelApplication(
      taskFormRef.value.formState.applicationName,
      taskFormRef.value.formState.deviceFirmware,
    );
    const modelId = modelApplication.model.id;
    modelExplore.value = await getModelExploreByModelId(modelId);
    isStartDisabled.value = false;
    // isHyperParamVisible.value = true;
  };
  const showHyperParameters = async () => {
    modelExplore.value = await hyperFormRef.value.getHyperParams(
      taskFormRef.value.formState.applicationName,
      taskFormRef.value.formState.deviceFirmware,
    );
    isStartDisabled.value = false;
    isHyperParamVisible.value = true;
  };

  const gotoDetail = (record: Recordable) => {
    // console.log(record);
    const recordJson = JSON.stringify(record);
    router.push({
      path: `/maTrainingCenter/generationGuideDetail/${record.id}`,
      query: {
        record: encodeURIComponent(recordJson), // 编码字符串
      },
    });
  };

  const handleCreateFork = async (record: Recordable) => {
    Modal.confirm({
      title: '确认操作',
      content: '确定要创建引导分叉吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          if (!(await checkResource(toRaw(unref(record.hardwareParams))))) {
            return;
          }
          const response = await createFork(record.id);
          if (response === 'SUCCEED') {
            createMessage.success(response.message || '创建引导分叉成功！');
            await reload(); // 刷新数据
          } else {
            createMessage.error(response.message || '创建引导分叉失败！');
          }
        } catch (error) {
          console.error('Error creating fork:', error);
          createMessage.error('创建引导分叉失败');
        }
      },
      onCancel: () => {
        createMessage.info('操作已取消');
      },
    });
  };

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

  function handleTrainDatasetSelectChange(datasetId) {
    hyperFormRef.value.getQuantizationImageNum(datasetId);
  }

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
    clearInterval(timerTrain.value);
    clearInterval(timerTest.value);
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

  const handleDelete = async (record: Recordable) => {
    const content = record.trainDatasetStartAll?.isPublic
      ? '该引导式任务绑定的数据集已发布过, 此次操作将只删除任务, 确认继续删除吗?'
      : '该引导式任务绑定的数据集未发布过, 此次操作将同时删除任务和对应数据集, 确认继续删除吗?';

    Modal.confirm({
      title: '删除确认',
      content,
      okText: '确认删除',
      cancelText: '取消',
      async onOk() {
        try {
          // 执行删除操作
          await deleteGuidedModelGeneration(record.id);

          // 未发布过的数据集需要额外删除数据集
          if (record.trainDatasetStartAll && !record.trainDatasetStartAll.isPublic) {
            await deleteDatasetBatch([record.trainDatasetStartAll.id]);
          }

          createMessage.success('删除成功');
          // 不需要等待，不然里外同时刷新有点难看
          reload();
        } catch (error) {
          createMessage.error('删除失败: ' + (error as Error).message);
        }
      },
      onCancel() {
        createMessage.info('用户取消删除');
      },
    });
  };
</script>
<style lang="less" scoped>
  .ant-modal-body {
    min-height: 1000px;
  }
</style>
