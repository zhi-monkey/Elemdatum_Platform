<template>
  <!-- 保存训练模型的卡片 -->
  <a-card title="保存训练模型" style="margin-bottom: 20px">
    <a-table :dataSource="trainingModelData" :pagination="false">
      <a-table-column title="文件名" dataIndex="fileName" key="fileName" />
      <a-table-column title="文件大小" dataIndex="fileSize" key="fileSize" />
      <a-table-column title="操作" key="action">
        <template #default="{ record }">
          <a-button type="link" @click="downloadFile(record.fileName)">下载</a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 保存转换模型的卡片 -->
  <a-card title="保存转换模型">
    <a-table :dataSource="convertedModelData" :pagination="false">
      <a-table-column title="文件名" dataIndex="fileName" key="fileName" />
      <a-table-column title="文件大小" dataIndex="fileSize" key="fileSize" />
      <a-table-column title="操作" key="action">
        <template #default="{ record }">
          <a-button type="link" @click="downloadFile(record.fileName)">下载</a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Table as ATable,
    TableColumn as ATableColumn,
  } from 'ant-design-vue';

  // 定义保存训练模型的数据
  const trainingModelData = ref([
    { key: '1', fileName: 'best.pt', fileSize: '2.5MB' },
    { key: '2', fileName: 'best.onnx', fileSize: '3.1MB' },
  ]);

  // 定义保存转换模型的数据
  const convertedModelData = ref([
    { key: '1', fileName: 'convert-2024-7-1-14-58-59.rknn', fileSize: '1.2MB' },
    { key: '2', fileName: 'convert-2024-7-1-15-00-00.rknn', fileSize: '1.3MB' },
    { key: '3', fileName: 'convert-2024-7-1-15-05-30.rknn', fileSize: '1.4MB' },
  ]);

  // 下载文件的函数
  const downloadFile = (fileName: string) => {
    console.log(`Downloading ${fileName}`);
    // 在这里添加下载逻辑
  };
</script>

<style scoped lang="less">
  /* 您可以在这里添加样式 */
</style>
