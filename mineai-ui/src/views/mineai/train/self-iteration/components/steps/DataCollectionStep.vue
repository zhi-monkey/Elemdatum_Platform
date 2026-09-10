<template>
  <a-row :gutter="14" class="collection-layout">
    <a-col :xs="24" :lg="13">
      <a-card title="采样摄像头" size="small" class="panel-card section-gap">
        <a-row :gutter="12">
          <a-col
            v-for="item in detail.dataCollection.cameras"
            :key="item.executionId || item.cameraName"
            :xs="24"
            :md="12"
          >
            <a-card size="small" class="inner-card" :title="item.cameraName">
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="数据源">{{ item.rtspSourceName }}</a-descriptions-item>
                <a-descriptions-item label="需采集图片数量">{{
                  item.requiredImages
                }}</a-descriptions-item>
                <a-descriptions-item label="已采集图片数量">{{
                  item.collectedImages
                }}</a-descriptions-item>
                <a-descriptions-item label="采样间隔">{{
                  item.sampleInterval
                }}</a-descriptions-item>
                <a-descriptions-item label="采样状态">
                  <a-tag :color="sampleStatusColor[item.sampleStatus]">{{
                    sampleStatusText[item.sampleStatus]
                  }}</a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
        </a-row>
      </a-card>
    </a-col>

    <a-col :xs="24" :lg="11">
      <a-card size="small" class="section-gap panel-card cascade-card">
        <template #title>
          <div class="card-title-wrap">
            <span>数据集明细</span>
            <span class="card-subtitle">单组场景</span>
          </div>
        </template>

        <div class="dataset-group-summary">
          <div class="summary-left">
            <div class="summary-label">数据集组</div>
            <div class="summary-name">{{ primaryGroup?.datasetGroupName || '未绑定数据集组' }}</div>
          </div>
          <div class="summary-right">共 {{ flatDatasets.length }} 个数据集</div>
        </div>

        <div class="dataset-list">
          <div v-if="!flatDatasets.length" class="dataset-empty">暂无数据集信息</div>
          <div v-for="dataset in flatDatasets" :key="dataset.datasetName" class="dataset-item">
            <div class="item-name">{{ dataset.datasetName }}</div>
            <div class="item-desc"
              >图片 {{ dataset.imageCount }} / 视频 {{ dataset.videoCount }}</div
            >
          </div>
        </div>
      </a-card>

      <a-card size="small" class="section-gap panel-card overview-card">
        <template #title>
          <div class="card-title-wrap">
            <span>采集进度概览</span>
            <span class="card-subtitle">当前轮次</span>
          </div>
        </template>
        <a-alert
          v-if="detail.dataCollection.canRetry"
          type="error"
          show-icon
          message="采集失败，可触发重新采集"
          style="margin-bottom: 10px"
        >
          <template #action>
            <a-button size="small" danger @click="emit('retryCollect')">重新采集</a-button>
          </template>
        </a-alert>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-statistic
              title="绑定数据集数"
              :value="detail.dataCollection.overview.totalDatasetCount"
              :value-style="{ color: '#1890ff', fontSize: '18px' }"
              suffix="个"
            />
          </a-col>
          <a-col :span="12">
            <a-statistic
              title="图片总数"
              :value="detail.dataCollection.overview.totalImageCount"
              :value-style="{ color: '#52c41a', fontSize: '18px' }"
              suffix="张"
            />
          </a-col>
          <a-col :span="12">
            <a-statistic
              title="视频总数"
              :value="detail.dataCollection.overview.totalVideoCount"
              :value-style="{ color: '#faad14', fontSize: '18px' }"
              suffix="个"
            />
          </a-col>
          <a-col :span="12">
            <a-statistic
              title="已采集图片"
              :value="detail.dataCollection.overview.collectedImageCount"
              :value-style="{ color: '#13c2c2', fontSize: '18px' }"
              suffix="张"
            />
          </a-col>
        </a-row>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Row as ARow,
    Statistic as AStatistic,
    Alert as AAlert,
    Button as AButton,
    Tag as ATag,
  } from 'ant-design-vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
  }>();

  const emit = defineEmits<{
    (e: 'retryCollect'): void;
  }>();

  const flatDatasets = computed(() =>
    props.detail.dataCollection.datasetCascade.flatMap((group) => group.datasets),
  );

  const primaryGroup = computed(() => props.detail.dataCollection.datasetCascade[0]);

  const sampleStatusText = {
    success: '采样成功',
    running: '采样中',
    failed: '采样失败',
    pending: '待采样',
  };

  const sampleStatusColor = {
    success: 'success',
    running: 'processing',
    failed: 'error',
    pending: 'default',
  };
</script>

<style scoped>
  .collection-layout {
    --si-surface: #181d31;
    --si-surface-elevated: #181d31;
    --si-border: rgba(255, 255, 255, 0.1);
    --si-hover-border: rgba(24, 144, 255, 0.26);
    --si-shadow: 0 8px 20px rgba(8, 14, 26, 0.25);
  }

  .panel-card {
    border: 1px solid var(--si-border);
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
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
    gap: 8px;
  }

  .card-subtitle {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.58);
    font-weight: 400;
  }

  .inner-card {
    margin-bottom: 10px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: transform 0.2s ease, border-color 0.2s ease;
  }

  .inner-card:hover {
    transform: translateY(-2px);
    border-color: var(--si-hover-border);
  }

  .inner-card :deep(.ant-card-head),
  .inner-card :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .section-gap {
    margin-bottom: 14px;
  }

  .dataset-group-summary {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 8px;
    background: var(--si-surface-elevated);
    padding: 10px 12px;
    margin-bottom: 10px;
  }

  .summary-left {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .summary-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
  }

  .summary-name {
    margin-top: 2px;
    font-size: 14px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.9);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .summary-right {
    font-size: 12px;
    color: rgba(24, 144, 255, 0.95);
    background: rgba(24, 144, 255, 0.12);
    padding: 3px 10px;
    border-radius: 999px;
    border: 1px solid rgba(24, 144, 255, 0.35);
  }

  .dataset-list {
    max-height: 254px;
    overflow: auto;
    padding-right: 4px;
  }

  .dataset-item {
    padding: 10px;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 6px;
    margin-bottom: 8px;
    background: var(--si-surface-elevated);
    transition: transform 0.2s ease, border-color 0.2s ease;
  }

  .dataset-item:hover {
    transform: translateY(-1px);
    border-color: var(--si-hover-border);
  }

  .dataset-empty {
    border: 1px dashed rgba(255, 255, 255, 0.2);
    border-radius: 6px;
    padding: 18px 10px;
    text-align: center;
    color: rgba(255, 255, 255, 0.56);
    font-size: 12px;
    margin-bottom: 8px;
  }

  .item-name {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 4px;
  }

  .item-desc {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.62);
  }

  .overview-card :deep(.ant-card-body) {
    padding-top: 10px;
  }
</style>
