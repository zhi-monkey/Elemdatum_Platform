<template>
  <div>
    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <template #title>测试集图片信息</template>
      <template #extra>
        <a-input-number v-model="confidence" placeholder="置信度" style="margin-right: 8px" />
        <a-button type="primary" :loading="loading" @click="startTest">开始测试</a-button>
      </template>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-statistic title="图片数量" :value="fileList.length" />
        </a-col>
        <a-col :span="12">
          <a-statistic title="测试进度" :value="`${currentIndex}/${fileList.length}`" />
        </a-col>
      </a-row>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="8">
          <a-statistic title="数据集" :value="'train-test V0001'" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="测试准确率" :value="accuracy" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="测试损失" :value="loss" />
        </a-col>
      </a-row>
    </a-card>

    <ImageViewCard v-if="showImageViewCard" />
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
  import ImageViewCard from '/@/views/mineai/refPage/generationGuide/modelTest/ImageViewCard.vue';

  const previewVisible = ref<boolean>(false);
  const previewImage = ref<string | undefined>('');
  const currentIndex = ref<number>(0);
  const confidence = ref<number | null>(null); // 置信度值
  const loading = ref<boolean>(false);
  const showImageViewCard = ref<boolean>(false);
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
    let index = 0;
    const interval = setInterval(() => {
      currentIndex.value = ++index;
      if (index === fileList.value.length) {
        clearInterval(interval);
        loading.value = false;
        showImageViewCard.value = true;

        // 模拟填充测试准确率和损失
        accuracy.value = '95%'; // 示例值
        loss.value = '5%'; // 示例值
      }
    }, 300);
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
