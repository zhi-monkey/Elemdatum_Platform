<template>
  <!-- 下发模型的卡片 -->
  <a-card title="下发模型">
    <a-form layout="vertical">
      <!-- 转换模型名称选择 -->
      <a-form-item label="选择转换模型名称">
        <a-select
          v-model:value="formState.selectedModel"
          :options="modelNames"
          placeholder="请选择转换模型名称"
        />
      </a-form-item>

      <!-- 下发设备型号选择 -->
      <a-form-item label="选择下发设备型号">
        <a-select
          v-model:value="formState.selectedDevice"
          :options="deviceModels"
          placeholder="请选择下发设备型号"
        />
      </a-form-item>

      <!-- 下发按钮 -->
      <a-form-item>
        <a-button
          type="primary"
          @click="dispatchModel"
          :disabled="!formState.selectedModel || !formState.selectedDevice"
        >
          下发
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Form as AForm,
    FormItem as AFormItem,
    Select as ASelect,
  } from 'ant-design-vue';

  // 模拟转换模型名称的数据
  const modelNames = ref([
    { label: 'convert-2024-7-1-14-58-59.rknn', value: 'convert-2024-7-1-14-58-59.rknn' },
    { label: 'convert-2024-7-1-15-00-00.rknn', value: 'convert-2024-7-1-15-00-00.rknn' },
    { label: 'convert-2024-7-1-15-05-30.rknn', value: 'convert-2024-7-1-15-05-30.rknn' },
  ]);

  // 模拟设备型号的数据
  const deviceModels = ref([
    { label: '设备型号A', value: 'deviceA' },
    { label: '设备型号B', value: 'deviceB' },
    { label: '设备型号C', value: 'deviceC' },
  ]);

  // 表单数据
  const formState = ref({
    selectedModel: null,
    selectedDevice: null,
  });

  // 下发模型的函数
  const dispatchModel = () => {
    console.log(
      `Model: ${formState.value.selectedModel}, Device: ${formState.value.selectedDevice}`,
    );
    // 在这里添加下发逻辑
  };
</script>

<style scoped lang="less">
  /* 您可以在这里添加样式 */
</style>
