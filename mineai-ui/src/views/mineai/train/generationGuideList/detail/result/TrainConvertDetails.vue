<template>
  <div>
    <PageWrapper title=" " style="margin: 0 40px 40px" v-if="shouldShowTabs">
      <template #extra>
        <a-button type="primary" @click="handleModelSaveNext">完成</a-button>
        <a-button type="primary" @click="openForkModal">重新训练</a-button>
      </template>

      <template #footer>
        <a-tabs
          tab-position="left"
          v-model:activeKey="activeKey"
          size="large"
          :tabBarGutter="70"
          :tabBarStyle="{ fontSize: '132px' }"
        >
          <a-tab-pane key="1" tab="模型验证">
            <Validate
              :job-id="modelInfo.id"
              :image="modelInfo.image"
              :gpu-num="modelInfo.gpuNum"
              :weight-path="modelInfo.weightPath"
            />
          </a-tab-pane>
          <a-tab-pane key="2" tab="离线下载">
            <ModelSave
              :app-zip-path="modelApplication.appZipPath"
              :job-id="modelInfo.id"
              :dataset-id="modelInfo.datasetId"
              :dataset-version-name="modelInfo.datasetVersionName"
            />
          </a-tab-pane>
          <!--          <a-tab-pane key="3" tab="模型在线下发">-->
          <!--            <ModelSchedule />-->
          <!--          </a-tab-pane>-->
        </a-tabs>
      </template>
    </PageWrapper>
    <!-- 条件显示 a-alert -->
    <div v-else>
      <a-alert
        v-if="hasConvert && convertJobStatus != 0 ? convertJobStatus !== 6 : false"
        message="转换任务进行中"
        description="Conversion job going"
        type="info"
        show-icon
      />
      <a-alert
        v-else-if="trainJobStatus !== 2"
        message="训练未完成"
        type="warning"
        description="Train job going"
        show-icon
      />
    </div>
    <a-modal
      title="基于当前任务的数据创建新的训练任务"
      v-model:visible="showForkModal"
      width="50%"
      :footer="null"
      @close="onModalClose"
    >
      <TaskForm
        style="margin: 30px 0 30px 0"
        @can-be-commit="canBeCommit"
        :model-application="props.modelApplication"
        :dataset-id="props.datasetId"
        :model-generation-id="props.modelGenerationId"
        ref="taskFormRef"
      />
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
        <!--        <a-button @click="handleForkCancel" style="margin-right: 8px" :loading="isSubmitting">-->
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
      <!-- 这里可以放弹窗内容，比如表单或者提示 -->
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { defineEmits, defineProps, ref, toRaw, unref } from 'vue';
  import {
    Alert as AAlert,
    Button as AButton,
    TabPane as ATabPane,
    Tabs as ATabs,
    Modal as AModal,
  } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import ModelSave from '/@/views/mineai/train/generationGuideList/detail/saveOrSchedule/ModelSave.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import Validate from '/@/views/mineai/train/generationGuideList/detail/validate/Validate.vue';
  import TaskForm from '/@/views/mineai/train/generationGuideList/detail/result/TaskForm.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    getModelByApplicationNameAndDevice,
    getModelExploreByModelId,
    getUniqueModelApplication,
    startForkGuidedGeneration,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { router } from '/@/router';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const emit = defineEmits(['next']);
  const activeKey = ref('1');

  const go = useGo();

  const props = defineProps({
    jobId: Number,
    image: String,
    weightPath: String,
    gpuNum: Number,
    modelApplication: Object,
    datasetId: Number,
    datasetVersionName: String,
    hasConvert: Boolean,
    trainJobStatus: Number,
    convertJobStatus: Number,
    modelGenerationId: Number,
  });

  const isStartDisabled = ref(true);
  const { createMessage } = useMessage();
  const taskFormRef = ref();
  const showForkModal = ref(false);

  const openForkModal = () => {
    showForkModal.value = true;
    // 延迟设置isStartDisabled为false，确保TaskForm组件已加载
    setTimeout(() => {
      isStartDisabled.value = false;
    }, 100);
  };
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
    const params = {
      modelGeneration: {
        ...toRaw(unref(taskFormRef.value.formState)),
        isGuided: true,
        dataSource: 0,
        isReuse: true,
        modelExplore: toRaw(unref(modelExplore.value)),
        model: model,
      },
      modelApplication: modelApplication,
      forkDatasetId: props.datasetId,
      datasetName: taskFormRef.value.formState.datasetName,
      modelGenerationId: props.modelGenerationId,
      datasetGroupName: taskFormRef.value.formState.datasetGroup,
      // HardwareParamsID: selectedResource,
    };
    let newTaskId;
    try {
      newTaskId = await startForkGuidedGeneration(params);
    } catch (e) {
      console.log(e);
      isSubmitting.value = false;
      return;
    }
    isStartDisabled.value = true;
    showForkModal.value = false;
    taskFormRef.value.resetFields();
    // 获取新创建任务的完整信息并跳转到详情页
    if (newTaskId && typeof newTaskId === 'number') {
      try {
        // 使用列表API获取完整的任务信息（包含完整的modelApplication嵌套结构）
        const response = await maHttp.get(
          {
            url: 'modelGeneration/dynamicFindGuidedModelGenerationPage',
            params: {
              page: 0,
              size: 1,
              id: newTaskId,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
        if (response && response.content && response.content.length > 0) {
          const newTaskDetail = response.content[0];
          const recordJson = JSON.stringify(newTaskDetail);
          router.push({
            path: `/maTrainingCenter/generationGuideDetail/${newTaskId}`,
            query: {
              record: encodeURIComponent(recordJson),
            },
          });
        } else {
          createMessage.error('获取新任务详情失败');
          go('/maTrainingCenter/generationGuideList');
        }
      } catch (e) {
        console.error('获取新任务详情失败:', e);
        createMessage.error('跳转失败，请手动进入任务详情页');
        go('/maTrainingCenter/generationGuideList');
      }
    } else {
      go('/maTrainingCenter/generationGuideList');
    }
  };

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

  const isSubmitting = ref(false);

  function handleForkCancel() {
    taskFormRef.value.resetFields();
    isStartDisabled.value = true;
    showForkModal.value = false;
  }

  // 条件判断：是否应该显示 Tabs
  const shouldShowTabs = ref(
    (props.hasConvert && props.convertJobStatus === 6) ||
      (!props.hasConvert && props.trainJobStatus === 2),
  );

  const modelInfo = ref({
    id: props.jobId,
    image: props.image,
    weightPath: props.weightPath,
    gpuNum: props.gpuNum,
    params: {
      MODE_WORKING_MODE: 4,
    },
    modelApplication: props.modelApplication,
    datasetId: props.datasetId,
    datasetVersionName: props.datasetVersionName,
  });

  function handleModelSaveNext() {
    go('/maTrainingCenter/generationGuideList');
  }
</script>
