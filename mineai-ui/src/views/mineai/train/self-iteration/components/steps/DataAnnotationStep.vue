<template>
  <a-row :gutter="14" class="annotation-layout">
    <a-col :xs="24" :lg="10">
      <a-card size="small" class="section-gap panel-card">
        <template #title>
          <div class="card-title-wrap">
            <span>自动标注配置</span>
            <span class="card-subtitle">策略信息</span>
          </div>
        </template>
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="基线模型">
            {{ detail.dataAnnotation.baselineModelName }}
          </a-descriptions-item>
          <a-descriptions-item label="标签">
            <a-space wrap>
              <a-tag v-for="tag in detail.dataAnnotation.labels" :key="tag" color="blue">{{
                tag
              }}</a-tag>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="自动标注置信度">
            {{ detail.dataAnnotation.confidenceThreshold }}
          </a-descriptions-item>
          <a-descriptions-item label="人工审核">
            {{ detail.needManualReview ? '需要' : '不需要' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card size="small" class="panel-card">
        <template #title>
          <div class="card-title-wrap">
            <span>阶段操作</span>
            <span class="card-subtitle">审核流转</span>
          </div>
        </template>
        <a-alert
          :type="detail.needManualReview ? 'warning' : 'info'"
          show-icon
          :message="operationMessage"
          style="margin-bottom: 12px"
        />
        <a-space class="action-buttons">
          <a-button type="primary" :disabled="!canReviewNow" @click="emit('goReview')">
            前往标注
          </a-button>
          <a-button
            type="primary"
            :disabled="!canReviewNow"
            :loading="confirmLoading"
            @click="emit('confirmReview')"
          >
            完成审核
          </a-button>
        </a-space>
      </a-card>
    </a-col>

    <a-col :xs="24" :lg="14">
      <a-row :gutter="12" class="section-gap">
        <a-col :span="8">
          <a-card size="small" class="metric-card">
            <a-statistic title="数据集数量" :value="datasetStatuses.length" suffix="个" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" class="metric-card">
            <a-statistic title="已标注图片" :value="summary.annotatedImages" suffix="张" />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small" class="metric-card">
            <a-statistic title="待处理图片" :value="summary.pendingImages" suffix="张" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="12">
        <a-col
          v-for="dataset in datasetStatuses"
          :key="dataset.datasetId || dataset.datasetName"
          :xs="24"
          :md="12"
        >
          <a-card :title="dataset.datasetName" size="small" class="dataset-card panel-card">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="已标注图片/总数">
                {{ dataset.annotatedImages }}/{{ dataset.totalImages }}
              </a-descriptions-item>
              <a-descriptions-item label="标注类型">
                {{ annotationTypeMap[dataset.annotationType] }}
              </a-descriptions-item>
              <a-descriptions-item label="数据集状态">
                <a-tag :color="statusColorMap[dataset.status]">{{
                  statusTextMap[dataset.status]
                }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
            <a-button
              type="link"
              :disabled="!canReviewNow"
              style="padding-left: 0"
              @click="emit('goReview', dataset)"
            >
              前往标注
            </a-button>
          </a-card>
        </a-col>
      </a-row>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import {
    Alert as AAlert,
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Row as ARow,
    Space as ASpace,
    Statistic as AStatistic,
    Tag as ATag,
  } from 'ant-design-vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
    confirmLoading?: boolean;
  }>();

  const emit = defineEmits<{
    confirmReview: [];
    goReview: [dataset?: SelfIterationChildTaskDetail['dataAnnotation']['datasetStatuses'][number]];
  }>();

  const confirmLoading = computed(() => Boolean(props.confirmLoading));
  const datasetStatuses = computed(() => props.detail.dataAnnotation.datasetStatuses || []);
  const canReviewNow = computed(
    () =>
      props.detail.status === 'waiting_review' && props.detail.dataAnnotation.canOperate,
  );

  const operationMessage = computed(() => {
    if (props.detail.status !== 'waiting_review') {
      return '自动标注尚未结束，当前还未进入人工审核停驻阶段。';
    }
    if (!props.detail.dataAnnotation.canOperate) {
      return '当前阶段不允许审核操作，请等待任务状态刷新。';
    }
    return '当前轮次已停留在人工审核阶段，完成人工审核后即可自动推进到训练流程。';
  });

  const summary = computed(() => {
    const annotatedImages = datasetStatuses.value.reduce(
      (total, item) => total + item.annotatedImages,
      0,
    );
    const totalImages = datasetStatuses.value.reduce((total, item) => total + item.totalImages, 0);
    return {
      annotatedImages,
      pendingImages: Math.max(totalImages - annotatedImages, 0),
    };
  });

  const annotationTypeMap = {
    auto: '自动标注',
    manual: '人工标注',
    hybrid: '自动+人工',
  };

  const statusTextMap = {
    pending: '待处理',
    processing: '处理中',
    waiting_review: '待审核',
    completed: '已完成',
    failed: '失败',
  };

  const statusColorMap = {
    pending: 'default',
    processing: 'processing',
    waiting_review: 'orange',
    completed: 'success',
    failed: 'error',
  };
</script>

<style scoped>
  .annotation-layout {
    --si-surface: #181d31;
    --si-surface-elevated: #181d31;
    --si-border: rgba(255, 255, 255, 0.1);
    --si-hover-border: rgba(24, 144, 255, 0.26);
    --si-shadow: 0 8px 20px rgba(8, 14, 26, 0.24);
  }

  .panel-card {
    border: 1px solid var(--si-border);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .panel-card:hover {
    transform: translateY(-2px);
    border-color: var(--si-hover-border);
    box-shadow: var(--si-shadow);
  }

  .panel-card :deep(.ant-card-head) {
    background: var(--si-surface-elevated);
    border-bottom-color: var(--si-border);
    padding: 0 14px;
  }

  .panel-card :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    padding: 14px;
  }

  .card-title-wrap {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
  }

  .card-subtitle {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.58);
    font-weight: 400;
  }

  .section-gap {
    margin-bottom: 14px;
  }

  .action-buttons :deep(.ant-btn) {
    min-width: 94px;
  }

  .metric-card {
    border: 1px solid var(--si-border);
    transition: border-color 0.2s ease;
  }

  .metric-card:hover {
    border-color: var(--si-hover-border);
  }

  .metric-card :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    padding: 12px;
  }

  .metric-card :deep(.ant-statistic-title) {
    color: rgba(255, 255, 255, 0.62);
    font-size: 12px;
  }

  .metric-card :deep(.ant-statistic-content) {
    color: rgba(255, 255, 255, 0.9);
    font-size: 20px;
  }

  .dataset-card {
    margin-bottom: 12px;
  }

  .dataset-card :deep(.ant-card-head-title) {
    font-size: 13px;
    font-weight: 600;
  }
</style>
