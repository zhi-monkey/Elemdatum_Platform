<template>
  <div class="container">
    <!-- 文本内容居中显示 -->
    <div class="text-container">
      <TaskSelect />
    </div>

    <!-- 下一步按钮居中显示 -->
    <div class="button-container">
      <a-button type="primary" @click="goToNextStep">下一步</a-button>
    </div>
  </div>
</template>

<script setup>
  import TaskSelect from '/@/views/mineai/refPage/selfTrainingCreate/TaskSelect.vue';

  const emit = defineEmits(['next']); // 定义发射事件

  // 定义下一步的处理方法
  const goToNextStep = () => {
    emit('next'); // 触发父组件的下一步事件
  };
</script>

<style scoped>
  .container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 180px; /* 可以根据需要调整高度 */
    width: 100%; /* 宽度为 100% */
  }

  .text-container {
    margin-bottom: 10px; /* 调整文本与按钮的距离 */
    margin-top: 20px;
    text-align: center;
    width: 100%;
    height: 400px;
  }

  .button-container {
    text-align: center;
    margin-bottom: 20px;
  }
</style>
