<template>
  <div>
    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <template #title>图片上传</template>
      <div class="clearfix">
        <a-upload
          action="https://www.mocky.io/v2/5cc8019d300000980a055e76"
          list-type="picture-card"
          v-model:file-list="fileList"
          @preview="handlePreview"
          :max-count="200"
        >
          <div v-if="fileList.length < 200">
            <plus-outlined />
            <div class="ant-upload-text">Upload</div>
          </div>
        </a-upload>
        <a-modal :visible="previewVisible" :footer="null" @cancel="handleCancel">
          <img alt="example" style="width: 100%" :src="previewImage" />
        </a-modal>
      </div>
    </a-card>

    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <template #title>图片信息</template>
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
    </a-card>

    <ImageViewCard v-if="showImageViewCard" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { PlusOutlined } from '@ant-design/icons-vue';
  import {
    Upload as AUpload,
    Modal as AModal,
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
  const testResult = ref<string | null>(null);
  const loading = ref<boolean>(false);
  const showImageViewCard = ref<boolean>(false);

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

  const handleCancel = () => {
    previewVisible.value = false;
  };

  const handlePreview = async (file: FileItem) => {
    if (!file.url && !file.preview) {
      file.preview = (await getBase64(file.originFileObj)) as string;
    }
    previewImage.value = file.url || file.preview;
    previewVisible.value = true;
  };

  const handleChange = ({ fileList: newFileList }: { fileList: FileItem[] }) => {
    fileList.value = newFileList;
  };

  function getBase64(file: File) {
    return new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
    });
  }

  const startTest = () => {
    loading.value = true;
    let index = 0;
    const interval = setInterval(() => {
      currentIndex.value = ++index;
      if (index === fileList.value.length) {
        clearInterval(interval);
        loading.value = false;
        showImageViewCard.value = true;
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
