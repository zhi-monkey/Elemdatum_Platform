<template>
  <div class="container">
    <!-- 文本内容居中显示 -->

    <div class="flex flex-col justify-center w-full card-container">
      <!-- 训练进度展示 -->
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
      <!-- 训练详情 -->
      <TrainInfo :job-id="trainJobId" :show-bar="false" :change-tab-title="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import {
    createTestJob,
    fetchTrainJobProgressMock,
    findModelGenerationById,
  } from '/@/views/mineai/refPage/generationGuide/api/modelApi';
  import TrainInfo from './TrainInfo.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton, Card as ACard, Progress as AProgress } from 'ant-design-vue';
  import { useRoute } from 'vue-router';

  const emit = defineEmits(['next']);
  const trainJobId = ref(0); // 训练作业ID
  const trainProgress = ref(0); // 训练进度
  let timer; // 定时器
  const { createMessage } = useMessage();
  const newJobAcc = ref(0);
  const testJobId = ref<number>(0);
  const anotherTestJobId = ref<number>(0);
  const originJobAcc = ref(0);
  const route = useRoute();
  const generationName = ref('');
  const generationId = ref(0);
  const modelGeneration = ref();
  const generationStatus = ref(0);
  const loading = ref(false);
  const hasConvertMV = ref<boolean>(false);
  const ifTrainShow = ref<boolean>(false);
  const ifTestShow = ref<boolean>(false);

  // 获取训练进度
  function getTrainProgress() {
    trainProgress.value = fetchTrainJobProgressMock(100); // 模拟获取训练进度
  }

  // 下一步操作
  function handleTrainProcessNext() {
    // 停止定时器并跳转到下一步
    clearInterval(timer);
    emit('next');
  }

  // 初始化时获取模型生成信息并开始定时获取训练进度
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
      }
    }
  });

  async function handleTestJob() {
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
    } catch {
      createMessage.error('质检作业创建失败！'); // 创建失败提示
    }
  }

  function setTrainInfo() {
    ifTrainShow.value = true;
  }

  // 处理训练作业的逻辑
  async function handleTrainJob() {
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

      // 定时器每5秒获取训练进
    } catch {
      createMessage.error('训练作业创建失败！'); // 创建失败提示
    }
  }

  // 组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style scoped>
  .container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  .card-container {
    height: 70vh;
  }
</style>
