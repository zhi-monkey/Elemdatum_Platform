<template>
  <div class="info-overview-container">
    <a-row :gutter="[8, 8]" style="height: 100%">
      <a-col :span="12" style="height: 60vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card :loading="loading" title="数据集信息">
            <Description
              :column="1"
              :data="datasetInfo"
              :schema="datasetSchema"
              layout="horizontal"
            />
          </a-card>
        </div>
      </a-col>
      <a-col :span="12" style="height: 60vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card :loading="loading" title="文件信息">
            <Description
              :column="1"
              :data="fileInfo"
              :schema="fileSchema"
              layout="horizontal"
            />
          </a-card>
        </div>
      </a-col>
    </a-row>
    <a-row :gutter="[8, 8]" style="height: 100%">
      <a-col :span="24" style="height: auto">
        <div style="height: 95%" class="flex flex-col">
          <a-table :columns="labelColumn" :data-source="labelData" :loading="loading" />
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { h, onMounted, reactive, ref } from 'vue';
  import { Description } from '/@/components/Description';
  import { Card as ACard, Col as ACol, Row as ARow, Table as ATable, Tag } from 'ant-design-vue';
  import {
    getPcDatasetDetail,
    getPcDatasetLabels,
  } from '../api/index';

  const props = defineProps({
    id: Number,
    name: String,
  });

  // 点云数据集状态码 -> 中文描述映射（与数据库 pc_dataset.status 注释保持一致）
  // 1001:未采样 1002:导入中 1003:未标注 1004:自动标注中 1005:自动标注停止
  // 1006:自动标注失败 1007:标注中 1008:自动标注完成 1009:难例发布中 1010:难例发布失败 1011:已发布
  const pcDatasetStatusMap: Record<number, { name: string; color: string }> = {
    1001: { name: '未采样', color: 'default' },
    1002: { name: '导入中', color: 'processing' },
    1003: { name: '未标注', color: 'warning' },
    1004: { name: '自动标注中', color: 'processing' },
    1005: { name: '自动标注停止', color: 'default' },
    1006: { name: '自动标注失败', color: 'error' },
    1007: { name: '标注中', color: 'warning' },
    1008: { name: '自动标注完成', color: 'success' },
    1009: { name: '难例发布中', color: 'processing' },
    1010: { name: '难例发布失败', color: 'error' },
    1011: { name: '已发布', color: 'success' },
  };

  const loading = ref(false);
  const datasetInfo: any = reactive({});
  const fileInfo: any = reactive({});
  const labelData = ref([]);

  // 数据集信息展示字段
  const datasetSchema = [
    { field: 'id', label: '数据集ID' },
    { field: 'name', label: '数据集名称' },
    {
      field: 'status',
      label: '数据集状态',
      // 将点云状态码(1001~1011)渲染为中文标签，未识别码值兜底显示原始数字
      render: (status) => {
        const item = pcDatasetStatusMap[status];
        return h(Tag, { color: item?.color || 'default' }, () => item?.name ?? String(status ?? '-'));
      },
    },
    { field: 'fileCount', label: '文件数量' },
    { field: 'remark', label: '描述' },
    { field: 'createTime', label: '创建时间' },
  ];

  // 文件信息展示字段
  const fileSchema = [
    { field: 'fileCount', label: 'PCD 文件总数' },
    { field: 'totalPointCount', label: '总点数' },
    { field: 'totalFileSize', label: '总大小(字节)' },
  ];

  // 标签列表列
  const labelColumn = [
    { title: '标签ID', dataIndex: 'labelId' },
    { title: '标签名称', dataIndex: 'labelName' },
    { title: '标注数量', dataIndex: 'annotationCount' },
  ];

  onMounted(async () => {
    loading.value = true;
    try {
      const detail = await getPcDatasetDetail(props.id);
      datasetInfo.id = detail.id;
      datasetInfo.name = detail.name;
      datasetInfo.status = detail.status;
      datasetInfo.fileCount = detail.fileCount;
      datasetInfo.remark = detail.remark || '暂无描述';
      datasetInfo.createTime = detail.createTime;

      fileInfo.fileCount = detail.fileCount;
      fileInfo.totalPointCount = detail.totalPointCount || 0;
      fileInfo.totalFileSize = detail.totalFileSize || 0;

      const labels = await getPcDatasetLabels(props.id);
      labelData.value = (labels || []).map((item, index) => ({
        key: index.toString(),
        labelId: item.labelId,
        labelName: item.labelName,
        annotationCount: item.annotationCount,
      }));
    } catch (e) {
      console.error('加载点云数据集详情失败:', e);
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped lang="less">
  .info-overview-container {
    box-sizing: border-box;
    background-color: #1a202c !important;
  }
</style>
