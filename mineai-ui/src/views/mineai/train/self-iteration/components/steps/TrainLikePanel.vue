<template>
  <a-row :gutter="12" class="train-like-root">
    <a-col :span="10">
      <a-card class="train-card" :bordered="false" title="作业详情">
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[detail.modelIteration.train.status]">{{
              statusTextMap[detail.modelIteration.train.status]
            }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{
            detail.modelIteration.train.startTime
          }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{
            detail.modelIteration.train.endTime
          }}</a-descriptions-item>
          <a-descriptions-item label="Epoch">{{
            detail.modelIteration.train.epoch
          }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card class="train-card" :bordered="false" title="训练进度">
        <i class="progress-text">{{ progressPercent }}%</i>
        <a-progress :percent="progressPercent" :show-info="false" />
      </a-card>

      <a-card class="train-card" :bordered="false" title="超参">
        <a-table
          :columns="columns"
          :data-source="detail.modelIteration.train.params"
          :pagination="false"
          size="small"
          row-key="field"
        />
      </a-card>
    </a-col>

    <a-col :span="14">
      <div class="bar-wrapper">
        <LossBar class="bar-item" :data="trainCurveData" />
        <AccBar class="bar-item" :data="trainCurveData" />
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Progress as AProgress,
    Row as ARow,
    Table as ATable,
    Tag as ATag,
  } from 'ant-design-vue';
  import LossBar from '/@/views/mineai/train/generationGuideList/detail/train/LossBar.vue';
  import AccBar from '/@/views/mineai/train/generationGuideList/detail/train/AccBar.vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
  }>();

  const statusTextMap: Record<string, string> = {
    pending: '待开始',
    collecting: '采集中',
    annotating: '标注中',
    training: '训练中',
    converting: '转换中',
    packaging: '打包中',
    dispatching: '下发中',
    completed: '已完成',
    failed: '失败',
    waiting_review: '待审核',
  };

  const statusColorMap: Record<string, string> = {
    pending: 'default',
    collecting: 'processing',
    annotating: 'processing',
    training: 'processing',
    converting: 'gold',
    packaging: 'cyan',
    dispatching: 'purple',
    completed: 'success',
    failed: 'error',
    waiting_review: 'orange',
  };

  const columns = [
    { title: '参数名', dataIndex: 'label', width: 130 },
    { title: '参数值', dataIndex: 'value' },
  ];

  const progressPercent = computed(() => {
    const v = Number(props.detail.modelIteration.train.map50 || 0);
    return Math.max(0, Math.min(100, Math.round(v * 100)));
  });

  const trainCurveData = ref({
    epoch: ['1', '2', '3', '4', '5', '6', '7', '8'],
    'train loss': [1.21, 0.92, 0.73, 0.61, 0.52, 0.46, 0.42, 0.39],
    'test accuracy': [0.41, 0.55, 0.62, 0.7, 0.77, 0.82, 0.86, 0.89],
  });
</script>

<style scoped>
  .train-like-root {
    min-height: 70vh;
  }

  .train-card {
    margin-bottom: 10px;
    background: #181d31;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .train-card :deep(.ant-card-body) {
    background: #181d31;
  }

  .progress-text {
    display: flex;
    justify-content: center;
    margin-bottom: 6px;
  }

  .bar-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .bar-item {
    height: calc(50% - 5px);
  }
</style>
