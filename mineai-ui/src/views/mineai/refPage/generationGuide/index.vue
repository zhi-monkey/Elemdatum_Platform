<template>
  <PageWrapper :contentStyle="{ 'background-color': '#181d31', 'border-radius': '4px' }">
    <div class="step-form-form">
      <a-steps :current="currentStep">
        <a-step title="新建引导任务" />
        <a-step title="模型训练" />
        <a-step title="模型测试" />
        <a-step title="模型转换" />
        <a-step title="结果" />
      </a-steps>
    </div>
    <div class="mt-5 justify-center">
      <TestNewModel @next="handleModelNext" v-if="current === 0" />
      <div v-if="current === 1">
        <div class="flex flex-col justify-center w-full">
          <a-card title="训练进度" style="height: 15vh">
            <template #extra>
              <a-button type="primary" style="margin-right: 50px" @click="handleTrainProcessNext">
                下一步
              </a-button>
            </template>
            <i class="flex justify-center">{{ trainProgress }}%</i>
            <div class="flex justify-center">
              <a-progress :percent="trainProgress" :show-info="false" style="width: 60%" />
            </div>
          </a-card>
          <TrainInfo :job-id="trainJobId" :show-bar="false" :change-tab-title="false" />
        </div>
      </div>
      <div v-if="current === 2">
        <div class="flex flex-col justify-center w-full">
          <!--          <a-card title="测试进度" style="height: 15vh">-->
          <!--            <i class="flex justify-center">{{ testProgress }}%</i>-->
          <!--            <div class="flex justify-center">-->
          <!--              <a-progress :percent="testProgress" :show-info="false" style="width: 60%" />-->
          <!--            </div>-->
          <!--          </a-card>-->
          <ModelTest @next="handleModelTestNext" />
        </div>
      </div>
      <ConvertInfo v-if="current === 3" @next="handleConverInfoNext" />
      <!--      <Convert2-->
      <!--        :generation-id="generationId"-->
      <!--        :generation-name="generationName"-->
      <!--        v-if="current === 3"-->
      <!--      />-->

      <!--      <Release-->
      <!--        :generation-id="generationId"-->
      <!--        :generation-name="generationName"-->
      <!--        v-if="current === 4"-->
      <!--      />-->

      <ModelSaveOrSchedule v-if="current === 4" />
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import ModelTest from '/src/views/mineai/refPage/generationGuide/modelTest/ModelTest.vue';
  import { PageWrapper } from '/@/components/Page';
  import {
    Steps as ASteps,
    Step as AStep,
    Progress as AProgress,
    Card as ACard,
  } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import Release from '/@/views/mineai/refPage/generationGuide/Release.vue';
  import TrainInfo from '/@/views/mineai/refPage/modelGeneration/trainJobDetail/TrainInfo.vue';
  import Convert2 from '/@/views/mineai/refPage/generationGuide/convertPage/Convert2.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRoute } from 'vue-router';
  import {
    createJob,
    createTestJob,
    createTrainJob,
    fetchTrainJobProgressMock,
    findModelGenerationById,
    findModelGenerationByIdWithTest,
    getTestJobProgress,
  } from '/@/views/mineai/refPage/generationGuide/api/modelApi';
  import TestNewModel from '/@/views/mineai/refPage/generationGuide/TestNewModel.vue';
  import ConvertInfo from '/@/views/mineai/refPage/generationGuide/modelConvertDetail/ConvertInfo.vue';
  import ModelSaveOrSchedule from '/@/views/mineai/refPage/generationGuide/modelSaveOrSchedule/index.vue';

  const route = useRoute();
  const currentStep = ref(0);
  const current = ref(0);

  const trainJobId = ref<number>(0);
  const ifTrainShow = ref<boolean>(false);
  const testJobId = ref<number>(0);
  const anotherTestJobId = ref<number>(0);
  const ifTestShow = ref<boolean>(false);
  const hasConvertMV = ref<boolean>(false);

  const { createMessage } = useMessage();
  const loading = ref(false);
  let testTimer;
  let trainTimer;
  let timer;
  let defaultPercent = ref();
  let trainProgress = ref(0);
  let testProgress = ref(50);
  const generationStatus = ref(0);
  // const deploymentStatus = ref(null);

  const modelGeneration = ref();

  //Model组件传出的数据
  const generationName = ref('');
  const generationId = ref(0);
  // const imageType = ref('');
  // const datasetId = ref(0);

  //Upload组件传出的数据
  // const modelVersion = ref(null);
  // const jobType = ref(0);
  // const monitor = ref(null);
  //存储质检作业完成之后的准确率
  const newJobAcc = ref(0);
  const originJobAcc = ref(0);

  // 处理训练作业的逻辑
  async function handleTrainJob() {
    current.value = 1; // 设置当前作业类型为训练
    currentStep.value = 1; // 设置当前步骤为1

    try {
      // await createTrainJob(generationId.value); // 创建训练作业
      setTimeout(setTrainInfo, 10000); // 进入训练页面，延迟10秒后设置训练信息

      // 获取模型生成信息
      modelGeneration.value = await findModelGenerationById(generationId.value);
      trainJobId.value = modelGeneration.value.latestJobId; // 设置训练作业的ID
      generationStatus.value = 1; // 状态设置为“正在训练”

      // 定时器每5秒更新训练状态
      timer = setInterval(async () => {
        console.log('onmounted train timer');
        generationStatus.value = await findModelGenerationById(generationId.value).then((value) => {
          if (value.modelStore.convertModelVersion != null) {
            hasConvertMV.value = true; // 检查是否有转换模型版本
          }
          return value.status; // 返回当前状态
        });
      }, 5000);

      // 定时器每5秒获取训练进度
      trainTimer = setInterval(getTrainProgress, 5000);
    } catch {
      createMessage.error('训练作业创建失败！'); // 创建失败提示
    }
  }

  // 处理质检作业的逻辑
  async function handleTestJob() {
    current.value = 2; // 设置当前作业类型为质检
    currentStep.value = 1; // 设置当前步骤为1

    try {
      await createTestJob(generationId.value); // 创建质检作业

      // 获取模型生成信息
      modelGeneration.value = await findModelGenerationById(generationId.value);
      testJobId.value = modelGeneration.value.testJob.id; // 设置质检作业的ID
      anotherTestJobId.value = modelGeneration.value.anotherTestJob.id;

      generationStatus.value = 2; // 状态设置为“正在质检”
      loading.value = true; // 显示加载状态

      // 定时器每5秒更新质检状态
      timer = setInterval(async () => {
        console.log('onmounted test timer running');
        generationStatus.value = await findModelGenerationById(generationId.value).then((value) => {
          if (value.modelStore.convertModelVersion != null) {
            hasConvertMV.value = true; // 检查是否有转换模型版本
          }
          return value.status; // 返回当前状态
        });
      }, 5000);

      // 隐藏训练显示，显示测试显示
      ifTrainShow.value = false;
      ifTestShow.value = true;

      // 定时器每5秒获取测试进度
      testTimer = setInterval(getTestProgress, 5000);
    } catch {
      createMessage.error('质检作业创建失败！'); // 创建失败提示
    }
  }

  // 处理结果展示和发布页面的逻辑
  function handleResultDisplay() {
    if (Number(route.query.jobType) === 1 && Number(route.query.currentStep) === 3) {
      current.value = 4; // 设置当前作业类型为结果展示
      currentStep.value = 3; // 设置当前步骤为3
    } else if (Number(route.query.jobType) === 3 && Number(route.query.currentStep) === 2) {
      current.value = 3; // 设置当前作业类型为其他类型
      currentStep.value = 2; // 设置当前步骤为2
    }
  }

  // onMounted 钩子
  onMounted(() => {
    newJobAcc.value = 0; // 初始化新作业的准确率
    originJobAcc.value = 0; // 初始化原始作业的准确率

    getTrainProgress();

    // 判断传入的 GenerationId 是否为空
    if (route.query.modelGenerationId != null) {
      generationId.value = Number(route.query.modelGenerationId); // 转换为数字
      generationName.value = String(route.query.modelGenerationName); // 转换为字符串

      // 根据作业类型和步骤调用相应的处理函数
      if (Number(route.query.jobType) === 1 && Number(route.query.currentStep) === 1) {
        handleTrainJob(); // 处理训练作业
      } else if (Number(route.query.jobType) === 2 && Number(route.query.currentStep) === 1) {
        handleTestJob(); // 处理质检作业
      } else {
        handleResultDisplay(); // 处理结果展示和发布页面
      }
    }
  });

  function getTestProgress() {
    getTestJobProgress(testJobId.value, anotherTestJobId.value)
      .then((v) => {
        defaultPercent.value = v;
        testProgress.value = Math.round(defaultPercent.value.progress);
        newJobAcc.value = defaultPercent.value.newNowAcc.toFixed(3);
        originJobAcc.value = defaultPercent.value.newOriginAcc.toFixed(3);
        judgeConvert();
      })
      .catch((error) => {
        // 处理错误
        console.error('获取测试进度失败:', error);
      });
  }

  function setTrainInfo() {
    ifTrainShow.value = true;
  }

  async function handleModelNext(modelInfo: { generationName: string; generationId: number }) {
    // 将mg状态设置为1——正在训练
    generationStatus.value = 1;
    loading.value = true;
    currentStep.value = 1;
    current.value = 1;
    trainJobId.value = modelGeneration.value.latestJobId;
    ifTrainShow.value = true;
  }

  async function handleTrainProcessNext() {
    // 将mg状态设置为1——正在训练
    loading.value = true;
    currentStep.value = 2;
    current.value = 2;
    ifTestShow.value = true;
  }

  watch(generationStatus, async (newValue) => {
    if (newValue === 6) {
      clearInterval(trainTimer);

      try {
        await createTestJob(generationId.value);
        console.log('test Job Created');

        const modelGenerationData = await findModelGenerationByIdWithTest(generationId.value);
        modelGeneration.value = modelGenerationData;

        //设置质检作业的id
        testJobId.value = modelGeneration.value.testJob.id;
        anotherTestJobId.value = modelGeneration.value.anotherTestJob.id;

        //将mg状态设置为2——正在质检
        generationStatus.value = 2;
        loading.value = true;
        currentStep.value = 1;
        current.value = 2;

        ifTrainShow.value = false;
        ifTestShow.value = true;

        testTimer = setInterval(async () => {
          await getTestProgress();
        }, 5000);
      } catch (error) {
        // 处理错误
      }
    }

    if (newValue === 15) {
      createMessage.success('算法转换完成');
      current.value = 4;
      currentStep.value = 3;
    }
  });

  function judgeConvert() {
    if (newJobAcc.value != 0 && originJobAcc.value != 0) {
      if (newJobAcc.value <= originJobAcc.value || !hasConvertMV.value) {
        clearInterval(testTimer);
        current.value = 4;
        currentStep.value = 3;
      } else if (newJobAcc.value > originJobAcc.value && hasConvertMV.value) {
        clearInterval(testTimer);
        current.value = 3;
        currentStep.value = 2;
      }
    }
  }

  function getTrainProgress() {
    const v = fetchTrainJobProgressMock(100);
    trainProgress.value = v;
  }

  function handleModelTestNext() {
    currentStep.value = 3;
    current.value = 3;
  }

  function handleConverInfoNext() {
    currentStep.value = 4;
    current.value = 4;
  }

  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(trainTimer);
    clearInterval(testTimer);
  });
</script>
<style lang="less" scoped>
  .step-form-content {
    padding: 24px;
  }

  .step-form-form {
    width: 80%;
    margin: 40px auto 0;
  }
</style>
