<template>
  <div class="container">
    <div class="form-items-container">
      <a-form-item label="选择标注任务" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-select v-model:value="task">
          <a-select-option v-for="task in tasks" :key="task.value" :value="task.value">
            {{ task.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="数据回流路径" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-select v-model:value="route">
          <a-select-option v-for="route in routes" :key="route.value" :value="route.value">
            {{ route.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {
  FormItem as AFormItem,
  Select as ASelect,
  SelectOption as ASelectOption,
} from 'ant-design-vue';

const task = ref(null);
const route = ref(null);

const tasks = ref([
  { value: 'safetyHelmetDetection', label: '任务1' },
  { value: 'faceRecognition', label: '任务2' },
]);
const routes = ref([
  { value: 'safetyHelmetDetection', label: '摄像头1' },
  { value: 'faceRecognition', label: '摄像头2' },
]);
</script>

<style scoped lang="less">
.container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
}

.form-items-container {
  width: 30%;
  padding: 20px;
}

</style>

