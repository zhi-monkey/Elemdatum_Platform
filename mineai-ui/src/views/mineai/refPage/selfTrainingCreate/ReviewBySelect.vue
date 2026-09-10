<template>
  <div>
    <ReviewCard />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import ReviewCard from '/@/views/mineai/refPage/selfTrainingCreate/ReviewCard.vue';

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
