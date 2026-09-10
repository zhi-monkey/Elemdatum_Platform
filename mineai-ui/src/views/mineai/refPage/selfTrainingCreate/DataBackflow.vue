<template>
  <div class="container">
    <!-- 文本内容居中显示 -->
    <TaskStart />
    <div class="button-container">
      <a-button type="primary" @click="goToNextStep">下一步</a-button>
    </div>
  </div>
</template>

<script setup>
  import TaskStart from '/@/views/mineai/refPage/selfTrainingCreate/TaskStart.vue';

  const emit = defineEmits(['next']); // 定义发射事件

  // 定义下一步的处理方法
  const goToNextStep = () => {
    emit('next'); // 触发父组件的下一步事件
  };
</script>

<style scoped>
  .button-container {
    text-align: center;
    margin-bottom: 20px;
  }
</style>
