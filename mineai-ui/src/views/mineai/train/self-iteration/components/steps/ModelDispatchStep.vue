<template>
  <a-row :gutter="14" class="dispatch-layout">
    <a-col :xs="24" :lg="10">
      <a-row :gutter="12" class="section-gap">
        <a-col :span="12">
          <a-card size="small" class="metric-card">
            <a-statistic title="成功数量" :value="detail.modelDispatch.summary.successCount" />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" class="metric-card">
            <a-statistic title="失败数量" :value="detail.modelDispatch.summary.failedCount" />
          </a-card>
        </a-col>
      </a-row>

      <a-card
        v-if="isManualDispatch"
        title="下发任务发布"
        size="small"
        class="section-gap panel-card"
      >
        <a-button type="primary" :loading="dispatching" @click="handleManualDispatch">
          立即下发
        </a-button>
      </a-card>

      <a-card title="下发结果摘要" size="small" class="section-gap panel-card">
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="下发时间">{{
            detail.modelDispatch.summary.dispatchTime
          }}</a-descriptions-item>
          <a-descriptions-item label="下发结果">
            <a-tag :color="dispatchResultColor">{{ dispatchResultText }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card title="下发模型信息" size="small" class="section-gap panel-card">
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="模型名">{{
            detail.modelDispatch.summary.modelName
          }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{
            detail.modelDispatch.summary.modelVersion
          }}</a-descriptions-item>
          <a-descriptions-item label="来源迭代"
            >第 {{ detail.modelDispatch.summary.sourceIteration }} 轮</a-descriptions-item
          >
        </a-descriptions>
      </a-card>

      <!-- <a-card title="问题与建议" size="small" class="panel-card">
        <a-list
          :data-source="detail.modelDispatch.suggestions"
          size="small"
          bordered
          class="suggest-list"
        >
          <template #renderItem="{ item }">
            <a-list-item>{{ item }}</a-list-item>
          </template>
        </a-list>
      </a-card> -->
    </a-col>

    <a-col :xs="24" :lg="14">
      <a-card title="下发目标地址" size="small" class="section-gap panel-card">
        <a-table
          :columns="targetColumns"
          :data-source="detail.modelDispatch.targets"
          :pagination="false"
          row-key="targetName"
          size="small"
          class="dispatch-table"
        />
      </a-card>

      <!-- <a-card title="本轮 vs 上轮效果对比" size="small" class="panel-card">
        <a-table
          :columns="metricColumns"
          :data-source="detail.modelDispatch.metrics"
          :pagination="false"
          row-key="metricName"
          size="small"
          class="dispatch-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'trend'">
              <a-tag :color="trendColorMap[record.trend]">{{ trendTextMap[record.trend] }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card> -->
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
  import { computed, h, ref } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Row as ARow,
    Statistic as AStatistic,
    Table as ATable,
    Tag as ATag,
    message,
  } from 'ant-design-vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';
  import { manualDispatchSelfIterationJob } from '../../api/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const dispatching = ref(false);

  const isManualDispatch = computed(() => !props.detail.modelDispatch.autoDeployEnabled);

  async function handleManualDispatch() {
    dispatching.value = true;
    try {
      await manualDispatchSelfIterationJob(props.detail.modelDispatch.jobId);
      message.success('下发成功');
      emit('refresh');
    } catch (error) {
      message.error('下发失败，请检查平台地址和推理分析装置地址配置');
    } finally {
      dispatching.value = false;
    }
  }

  const dispatchResultText = computed(() => {
    const { successCount, failedCount } = props.detail.modelDispatch.summary;
    if (failedCount > 0 && successCount > 0) return '部分成功';
    if (failedCount > 0) return '失败';
    if (successCount > 0) return '成功';
    return '待下发';
  });

  const dispatchResultColor = computed(() => {
    const { successCount, failedCount } = props.detail.modelDispatch.summary;
    if (failedCount > 0 && successCount > 0) return 'warning';
    if (failedCount > 0) return 'error';
    if (successCount > 0) return 'success';
    return 'default';
  });

  const targetColumns = [
    { title: '目标名称', dataIndex: 'targetName' },
    {
      title: '类型',
      dataIndex: 'targetType',
      width: 100,
      customRender: ({ record }) =>
        targetTypeTextMap[record.targetType] || record.targetType || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      customRender: ({ record }) =>
        h(
          ATag,
          { color: targetStatusColorMap[record.status] || 'default' },
          () => targetStatusTextMap[record.status] || record.status || '未知',
        ),
    },
  ];

  const targetStatusTextMap: Record<string, string> = {
    success: '成功',
    failed: '失败',
    pending: '待下发',
    processing: '下发中',
    dispatching: '下发中',
    completed: '成功',
  };

  const targetStatusColorMap: Record<string, string> = {
    success: 'success',
    failed: 'error',
    pending: 'processing',
    processing: 'processing',
    dispatching: 'processing',
    completed: 'success',
  };

  const targetTypeTextMap: Record<string, string> = {
    device: '模型接收地址',
    gpuUrl: '推理分析装置地址',
  };
</script>

<style scoped>
  .dispatch-layout {
    --si-surface: #181d31;
    --si-surface-elevated: #181d31;
    --si-border: rgba(255, 255, 255, 0.1);
    --si-hover-border: rgba(24, 144, 255, 0.26);
    --si-shadow: 0 8px 20px rgba(8, 14, 26, 0.24);
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

  .metric-card {
    border: 1px solid var(--si-border);
    transition: border-color 0.2s ease, transform 0.2s ease;
  }

  .metric-card:hover {
    border-color: var(--si-hover-border);
    transform: translateY(-2px);
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

  .section-gap {
    margin-bottom: 14px;
  }

  .suggest-list :deep(.ant-list-bordered) {
    border-color: var(--si-border);
  }

  .suggest-list :deep(.ant-list-item) {
    border-color: rgba(255, 255, 255, 0.08);
    transition: background-color 0.2s ease;
  }

  .suggest-list :deep(.ant-list-item:hover) {
    background: rgba(24, 144, 255, 0.12);
  }

  .dispatch-table :deep(.ant-table) {
    background: transparent;
  }

  .dispatch-table :deep(.ant-table-thead > tr > th) {
    background: rgba(255, 255, 255, 0.04);
    border-bottom-color: var(--si-border);
  }

  .dispatch-table :deep(.ant-table-tbody > tr > td) {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .dispatch-table :deep(.ant-table-tbody > tr:hover > td) {
    background: rgba(24, 144, 255, 0.12);
  }
</style>
