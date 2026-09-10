<template>
  <div class="container">
    <TaskView />
    <!-- 下一步按钮居中显示 -->
    <div class="button-container">
      <a-button type="primary">审核完成</a-button>
    </div>
    <div class="button-container">
      <a-button type="primary" @click="goToNextStep">下一步</a-button>
    </div>
  </div>
</template>

<script setup>
  import TaskView from '/@/views/mineai/refPage/selfTrainingCreate/TaskView.vue';

  const emit = defineEmits(['next']); // 定义发射事件

  // 定义下一步的处理方法
  const goToNextStep = () => {
    emit('next'); // 触发父组件的下一步事件
  };
</script>

<style scoped>
  .container {
  }

  .button-container {
    text-align: center;
    margin-bottom: 20px;
  }
</style>
