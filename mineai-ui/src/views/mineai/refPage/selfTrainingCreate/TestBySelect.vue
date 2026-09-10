<template>
  <div>
    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <a-row :gutter="16">
        <a-col :span="8" style="font-size: 15px"> 回流路径：摄像头1 </a-col>
        <a-col :span="8" style="font-size: 15px"> 回流任务：任务1 </a-col>
        <a-col :span="8">
          <a-button type="primary" :loading="loading" @click="startTest">开始回流</a-button>
        </a-col>
      </a-row>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="8">
          <a-statistic title="图片数量" :value="fileList.length" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="回流进度" :value="`${currentIndex}/${fileList.length}`" />
        </a-col>
        <a-col :span="8">
          <a-button type="primary" :loading="loading1" @click="stopTest">中止回流</a-button>
        </a-col>
      </a-row>
    </a-card>

    <ViewCard v-if="showImageViewCard" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    Card as ACard,
    Button as AButton,
    Statistic as AStatistic,
    Row as ARow,
    Col as ACol,
    InputNumber as AInputNumber,
  } from 'ant-design-vue';
  import ViewCard from '/@/views/mineai/refPage/selfTrainingCreate/ViewCard.vue';

  const previewVisible = ref<boolean>(false);
  const previewImage = ref<string | undefined>('');
  const currentIndex = ref<number>(0);
  const confidence = ref<number | null>(null); // 置信度值
  const loading = ref<boolean>(false);
  const loading1 = ref<boolean>(false);
  const showImageViewCard = ref<boolean>(false);
  const stopReturn = ref<boolean>(false);
  const accuracy = ref<string>('N/A'); // 测试准确率
  const loss = ref<string>('N/A'); // 测试损失

  interface FileItem {
    uid: string;
    name?: string;
    status?: string;
    response?: string;
    percent?: number;
    url?: string;
    preview?: string;
    originFileObj?: any;
  }

  // 将图片数量减少到21张
  const fileList = ref<FileItem[]>(
    Array.from({ length: 21 }, (_, index) => ({
      uid: `-${index + 1}`,
      name: `image-${index + 1}.png`,
      status: 'done',
      url: 'https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png',
    })),
  );

  const startTest = () => {
    loading.value = true;
    stopReturn.value = false;
    let index = 0;
    const interval = setInterval(() => {
      currentIndex.value = ++index;
      if (index >= fileList.value.length || stopReturn.value) {
        clearInterval(interval);
        loading.value = false;
        loading1.value = false;
        showImageViewCard.value = true;

        // 模拟填充测试准确率和损失
        accuracy.value = '95%'; // 示例值
        loss.value = '5%'; // 示例值
      }
    }, 300);
  };

  const stopTest = () => {
    loading1.value = true;
    stopReturn.value = true;
  };
</script>

<style>
  .ant-upload-select-picture-card i {
    font-size: 32px;
    color: #999;
  }

  .ant-upload-select-picture-card .ant-upload-text {
    margin-top: 8px;
    color: #666;
  }
</style>
