<template>
  <div>
    <PageWrapper @back="goBack" contentClass="page-content">
      <template #title> 引导式训练任务详情</template>

      <template #footer>
        <a-steps
          v-model:current="currentStepPhase"
          type="navigation"
          :style="{ marginBottom: 60 }"
          size="small"
        >
          <a-step :status="pipelineState.dataStepStatus" title="数据处理" />
          <a-step :status="pipelineState.trainStepStatus" title="模型训练" />
          <a-step :status="pipelineState.convertStepStatus" title="模型转换" />
          <a-step :status="pipelineState.resultStepStatus" title="模型部署" />
        </a-steps>
      </template>
    </PageWrapper>
    <!-- 需要切换到当前步骤且完成初始化了才会显示数据处理组件-->
    <a-spin :indicator="spinIndicator" v-if="!isPipelineStateInited" />
    <DatasetProcess
      v-if="currentStepPhase === StepPhase.DATA && isPipelineStateInited == true"
      :createdDatasetId="createdDatasetId"
      :record="currentRowRecord"
      :currentPipelinePhase="currentPipelinePhase"
      @next="handleDataProcessFinished"
      :bound-versions="boundVersions"
      @bind-success="handleBindSuccess"
    />
    <TrainInfo
      v-if="currentStepPhase === StepPhase.TRAINING"
      :currentPipelinePhase="currentPipelinePhase"
      :currentStepPhase="currentStepPhase"
      :job-id="latestJobId"
      :show-bar="true"
      :change-tab-title="true"
    />
    <ConvertInfo
      v-if="currentStepPhase === StepPhase.CONVERTING"
      :job-id="convertJobID"
      :show-bar="true"
      :change-tab-title="true"
      :key="convertInfoKey"
      :has-convert="hasConvert"
    />
    <TrainConvertDetails
      v-if="currentStepPhase === StepPhase.FINISHED"
      :job-id="latestJobId"
      :dataset-id="datasetId"
      :dataset-version-name="datasetVersionName"
      :image="image"
      :gpu-num="gpus"
      :weight-path="weightPath"
      :key="trainConvertDetailsKey"
      :has-convert="hasConvert"
      :train-job-status="trainJobStatus"
      :convert-job-status="convertJobStatus"
      :model-application="modelApplication"
      :model-generation-id="modelGenerationId"
    />
  </div>
</template>

<script setup lang="ts">
  import { Step as AStep, Steps as ASteps, Spin as ASpin } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import { computed, onMounted, onUnmounted, reactive, ref, watch, h } from 'vue';
  import TrainConvertDetails from '/@/views/mineai/train/generationGuideList/detail/result/TrainConvertDetails.vue';
  import TrainInfo from '/@/views/mineai/train/generationGuideList/detail/train/TrainInfo.vue';
  import ConvertInfo from '/@/views/mineai/train/generationGuideList/detail/convert/ConvertInfo.vue';
  import DatasetProcess from '/@/views/mineai/train/generationGuideList/detail/dataset/dataset-process.vue';
  import { useRoute, useRouter } from 'vue-router';
  import _default from 'ant-design-vue/es/vc-table/src/TableCell';
  import {
    getConvertJobsByTrainJobID,
    getJobByID,
    getLatestJobId,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { intervalBus } from '/@/views/mineai/train/generationGuideList/detail/train/intervalBus';
  import record = _default.props.record;
  import {
    getGenerationDetailById,
    getIsPublishing,
    getModelGenerationBoundVersions,
    PipelinePhase,
    StepPhase,
  } from '/@/views/mineai/train/generationGuideList/detail/generationGuideDetail.data';
  import { LoadingOutlined } from '@ant-design/icons-vue';

  const router = useRouter();
  const route = useRoute();
  const currentStepPhase = ref(0);
  const currentPipelinePhase = ref(0);
  const latestJobId = ref(0);
  const trainJob = ref();
  const convertJobs = ref<any[]>([]);
  const convertJobID = ref<number>(0);
  const image = ref<string>('');
  const gpus = ref<number>(0);
  const weightPath = ref<string>('');
  const trainJobStatus = ref<number>(0);
  const createdDatasetId = ref<number>(0);
  const currentRowRecord = ref();
  const boundVersions = ref([]);

  // 定义 key 用于强制刷新组件
  const convertInfoKey = ref(0);
  const trainConvertDetailsKey = ref(0);
  const convertJobStatus = ref<number>(0);
  const modelApplication = ref(null);
  const modelGenerationId = ref(null);
  const modelId = ref(null);
  const datasetId = ref(null);
  const datasetVersionName = ref(null);
  //是否有转换
  const hasConvert = ref(false);

  const isPipelineStateInited = ref(false);
  //用于记录是否被跳转过一次的变量。防止每次轮询时都会强制切换到某个页面，影响用户操作。
  const hasSettedToTraining = ref(false);
  const hasSettedToConverting = ref(false);
  const spinIndicator = h(LoadingOutlined, {
    style: {
      fontSize: '100px',
      position: 'fixed',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      margin: 0, // 清除默认边距
      pointerEvents: 'none', // 防止点击穿透
    },
    spin: true,
  });

  // 响应式状态对象
  const pipelineState = reactive({
    currentStep: StepPhase.DATA,
    dataStepStatus: 'wait',
    trainStepStatus: 'wait',
    convertStepStatus: 'wait',
    resultStepStatus: 'wait',
  });

  const changeState = async (stepPhase: number, pipelinePhase: number) => {
    currentStepPhase.value = stepPhase;
    currentPipelinePhase.value = pipelinePhase;
  };

  const changeStepStatus = async (
    dataStepStatus: string,
    trainStepStatus: string,
    convertStepStatus: string,
    resultStepStatus: string,
  ) => {
    pipelineState.dataStepStatus = dataStepStatus;
    pipelineState.trainStepStatus = trainStepStatus;
    pipelineState.convertStepStatus = convertStepStatus;
    pipelineState.resultStepStatus = resultStepStatus;
  };

  const setComponentParams = async (latestJobId: number) => {
    modelApplication.value = record.value.modelApplication;
    trainJob.value = await getJobByID(latestJobId);
    image.value = record.value.model
      ? record.value.model.trainModelVersion.url
      : record.value.modelStore
      ? record.value.modelStore.trainModelVersion.url
      : '';
    hasConvert.value = record.value.model
      ? record.value.model.convertModelVersion
        ? !!record.value.model.convertModelVersion.url
        : false
      : false;
    gpus.value = trainJob.value.gpus;
    trainJobStatus.value = trainJob.value.status;
    weightPath.value = trainJob.value.weightPath;
  };

  // // 定期获取最新数据
  // // 引导式状态流程
  // /* 标注 =》 发布 =》 切分 =》  训练 =》转换 =》完成
  //  * |======latestJobId = 0===|（说明在训练之前）
  //  * isPublishing可以确定是否在发布中
  //  * 通过trainDatasetId是否等于引导式训练的dataSetId来判断处于标注还是切分（发布前还是发布后）*/
  // 状态更新函数
  const initPipelineState = async () => {
    const generationDetail = await getGenerationDetailById(record.value.id);
    latestJobId.value = generationDetail.latestJobId;
    createdDatasetId.value = generationDetail.trainDatasetStart;

    //判断latestJobId是否为0，如果为0说明在训练之前
    if (generationDetail.latestJobId === 0) {
      //训练之前
      const isPublishing = await getIsPublishing(generationDetail.trainDatasetStart);
      if (isPublishing) {
        //发布中显示数据，且标记状态为发布中
        await changeState(StepPhase.DATA, PipelinePhase.PUBLISHING);
        await changeStepStatus('process', 'wait', 'wait', 'wait');
        isPipelineStateInited.value = true;
        //如果没有设置定时查询，就设置一下。
        if (!intervalBus.isInitStateIntervalExists() && intervalBus.canSet.value) {
          intervalBus.setInitStateInterval(setInterval(initPipelineState, 10000));
        }
      } else {
        //并非发布中
        if (
          generationDetail.trainDatasetStart === generationDetail.trainDataset?.id ||
          generationDetail.trainDataset == null
        ) {
          //数据处理中（标注中）
          await changeState(StepPhase.DATA, PipelinePhase.ANNOTATING);
          await changeStepStatus('process', 'wait', 'wait', 'wait');
          isPipelineStateInited.value = true;
          //如果处于数据标注中，就不用一直刷新了
        } else {
          //数据集切分中（显示训练界面，但是有切分中的加载）
          await changeState(StepPhase.DATA, PipelinePhase.SPLITTING);
          await changeStepStatus('process', 'wait', 'wait', 'wait');
          isPipelineStateInited.value = true;
          //如果没有设置定时查询，就设置一下。
          if (!intervalBus.isInitStateIntervalExists() && intervalBus.canSet.value) {
            intervalBus.setInitStateInterval(setInterval(initPipelineState, 10000));
          }
        }
      }
    } else {
      //训练之后
      //获取转换的任务
      try {
        convertJobs.value = await getConvertJobsByTrainJobID(generationDetail.latestJobId);
      } catch (error) {
        console.error('获取转换作业失败:', error);
      }
      if (convertJobs.value.length == 0) {
        //找不到对应的转换任务，说明转换没有开始，正在训练，也有可能是训练失败了。
        await setComponentParams(generationDetail.latestJobId);
        //如果还没有跳转过页面
        if (!hasSettedToTraining.value) {
          await changeState(StepPhase.TRAINING, PipelinePhase.TRAINING);
          hasSettedToTraining.value = true;
        }
        await changeStepStatus('finish', 'process', 'wait', 'wait');
        isPipelineStateInited.value = true;
        //如果没有设置定时查询，就设置一下。
        if (!intervalBus.isInitStateIntervalExists() && intervalBus.canSet.value) {
          intervalBus.setInitStateInterval(setInterval(initPipelineState, 10000));
        }
      } else {
        //有转换任务，说明正在转换？或者是已完成
        await setComponentParams(generationDetail.latestJobId);
        convertJobStatus.value = convertJobs.value[0].status;
        convertJobID.value = convertJobs.value[0].id;
        if (convertJobStatus.value == 1) {
          //正在转换
          //如果还没有跳转过页面。就跳转一次
          if (!hasSettedToConverting.value) {
            await changeState(StepPhase.CONVERTING, PipelinePhase.CONVERTING);
            hasSettedToConverting.value = true;
          }

          await changeStepStatus('finish', 'finish', 'process', 'wait');
          isPipelineStateInited.value = true;
          //如果没有设置定时查询，就设置一下。
          if (!intervalBus.isInitStateIntervalExists() && intervalBus.canSet.value) {
            intervalBus.setInitStateInterval(setInterval(initPipelineState, 10000));
          }
        } else {
          //结果页
          await changeState(StepPhase.FINISHED, PipelinePhase.FINISHED);
          await changeStepStatus('finish', 'finish', 'finish', 'process');
          isPipelineStateInited.value = true;
          intervalBus.clearIntervals();
          if (intervalBus.isInitStateIntervalExists()) {
            intervalBus.clearInitStateInterval();
          }
        }
      }
    }
  };

  const handleBindSuccess = async () => {
    boundVersions.value = await getModelGenerationBoundVersions(record.value.id);
  };

  onMounted(async () => {
    intervalBus.setCanSet(true);
    const recordString = route.query.record as string;
    if (recordString) {
      record.value = JSON.parse(decodeURIComponent(recordString));
    }
    boundVersions.value = await getModelGenerationBoundVersions(record.value.id);
    // record的赋值在await之前, 要不然会导致值传入的时候组件已经加载完毕
    datasetId.value = record.value.trainDataset?.datasetId;
    if (datasetId.value == null) {
      datasetId.value = record.value.trainDatasetStartAll?.id;
    }
    currentRowRecord.value = record;
    datasetVersionName.value = record.value.trainDataset?.versionName;
    modelGenerationId.value = record.value.id;

    await initPipelineState();
  });

  // 清除定时器
  onUnmounted(() => {
    intervalBus.setCanSet(false);
    intervalBus.clearInitStateInterval();
    intervalBus.clearIntervals();
  });

  // watch(
  //   () => convertJobID.value,
  //   (newConvertJobID) => {
  //     if (newConvertJobID != 0) {
  //       convertInfoKey.value++; // 通过改变 key 强制刷新 ConvertInfo 组件
  //       if (currentStepPhase.value < 1) currentStepPhase.value = 1;
  //     }
  //   },
  // );
  //
  // // 监听 convertJobStatus，当其等于6时重新加载 TrainConvertDetails
  // watch(
  //   () => convertJobStatus.value,
  //   (newStatus) => {
  //     if (newStatus == 6) {
  //       trainConvertDetailsKey.value++; // 通过改变 key 强制刷新 TrainConvertDetails 组件
  //       if (currentStepPhase.value < 2) currentStepPhase.value = 2;
  //     }
  //   },
  // );

  function goBack() {
    router.go(-1);
  }

  const handleDataProcessFinished = async () => {
    await changeState(StepPhase.DATA, PipelinePhase.PUBLISHING);
    //await changeStepStatus('process', 'wait', 'wait', 'wait');
    latestJobId.value = await getLatestJobId(record.value.id);
    if (!intervalBus.isInitStateIntervalExists() && intervalBus.canSet.value) {
      intervalBus.setInitStateInterval(setInterval(initPipelineState, 5000));
    }
    //initPipelineState();
  };
</script>

<style lang="scss">
  // 使用deep修改
  ::v-deep(.vben-page-wrapper-content) {
    overflow: scroll;
    margin: 15px 0 30px;
  }

  .page-content {
    overflow: hidden;
    margin: 0;
  }
</style>
