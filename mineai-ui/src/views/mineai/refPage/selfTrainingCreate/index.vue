<template>
  <PageWrapper
    :contentStyle="{ 'background-color': '#181d31', 'border-radius': '4px', height: '88vh' }"
  >
    <div class="step-form-form">
      <a-steps :current="currentStep">
        <a-step title="选择标注任务" />
        <a-step title="数据回流" />
        <a-step title="数据审核" />
        <a-step title="模型训练" />
        <a-step title="模型测试" />
        <a-step title="模型转换" />
        <a-step title="模型下发" />
      </a-steps>
    </div>
    <div class="mt-5 justify-center component-container">
      <!-- 选择标注任务 -->
      <SelectAnnotationTask @next="handleSelectAnnotationTaskNext" v-if="current === 0" />
      <!-- 数据回流 -->
      <DataBackFlow @next="handleDataBackFlowNext" v-if="current === 1" />
      <!-- 数据审核 -->
      <DataReview @next="handleDataReviewNext" v-if="current === 2" />
      <!-- 模型训练 -->
      <ModelTraining @next="handleModelTrainingNext" v-if="current === 3" />
      <!-- 模型测试 -->
      <ModelTesting @next="handleModelTestingNext" v-if="current === 4" />
      <!-- 模型转换 -->
      <ModelConversion @next="handleModelConversionNext" v-if="current === 5" />
      <!-- 模型下发 -->
      <ModelDeployment @next="handleModelDeploymentNext" v-if="current === 6" />
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { Steps as ASteps, Step as AStep } from 'ant-design-vue';
  import { useRouter } from 'vue-router';
  import SelectAnnotationTask from '/@/views/mineai/refPage/selfTrainingCreate/SelectAnnotationTask.vue';
  import DataBackFlow from '/@/views/mineai/refPage/selfTrainingCreate/DataBackflow.vue';
  import DataReview from '/@/views/mineai/refPage/selfTrainingCreate/DataReview.vue';
  import ModelTraining from '/@/views/mineai/refPage/selfTrainingCreate/ModelTraining.vue';
  import ModelTesting from '/@/views/mineai/refPage/selfTrainingCreate/ModelTesting.vue';
  import ModelConversion from '/@/views/mineai/refPage/selfTrainingCreate/ModelConversion.vue';
  import ModelDeployment from '/@/views/mineai/refPage/selfTrainingCreate/ModelDeployment.vue';

  const currentStep = ref(0);
  const current = ref(0);
  const router = useRouter(); // 获取 router 实例
  //1、选择标注任务：handleSelectAnnotationTaskNext
  function handleSelectAnnotationTaskNext() {
    currentStep.value = 1;
    current.value = 1;
  }

  //2、数据回流：handleDataBackFlowNext
  function handleDataBackFlowNext() {
    currentStep.value = 2;
    current.value = 2;
  }

  //3、数据审核：handleDataReviewNext
  function handleDataReviewNext() {
    currentStep.value = 3;
    current.value = 3;
  }

  //4、模型训练：handleModelTrainingNext
  function handleModelTrainingNext() {
    currentStep.value = 4;
    current.value = 4;
  }

  //5、模型测试：handleModelTestingNext
  function handleModelTestingNext() {
    currentStep.value = 5;
    current.value = 5;
  }

  //6、模型转换：handleModelConversionNext
  function handleModelConversionNext() {
    currentStep.value = 6;
    current.value = 6;
  }

  //7、模型下发：handleModelDeploymentNext
  function handleModelDeploymentNext() {
    //路由跳转到自迭代训练列表页面
    router.push({
      path: '/maTrainingCenter/selfTraining', // 页面路径
    });
  }
</script>
<style lang="less" scoped>
  .step-form-content {
    padding: 24px;
  }

  .step-form-form {
    width: 80%;
    margin: 40px auto 0;
  }

  .component-container {
    height: 83vh;
    display: flex;
    justify-content: center;
    //align-items: center;
  }

  :deep(.ant-card-body) {
    padding: 8px;
  }
</style>
