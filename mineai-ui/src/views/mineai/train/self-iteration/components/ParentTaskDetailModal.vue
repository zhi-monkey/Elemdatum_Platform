<template>
  <a-modal
    :visible="visible"
    title="自迭代训练任务详情"
    :width="960"
    wrap-class-name="self-iteration-detail-modal"
    :body-style="{ padding: '20px 24px', background: '#1f1f1f' }"
    destroy-on-close
    :footer="null"
    @cancel="emit('close')"
  >
    <template v-if="task">
      <a-descriptions :column="2" bordered size="small" class="detail-descriptions">
        <a-descriptions-item label="任务ID">{{ task.id }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ task.taskName }}</a-descriptions-item>
        <a-descriptions-item label="当前迭代轮次">{{ task.currentIteration }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ task.createTime }}</a-descriptions-item>
        <a-descriptions-item label="标签选择">{{
          task.form.labelSelection.join(', ') || '-'
        }}</a-descriptions-item>
        <a-descriptions-item label="人工审核">{{
          task.form.needManualReview ? '是' : '否'
        }}</a-descriptions-item>
      </a-descriptions>

      <a-row :gutter="14" style="margin-top: 14px">
        <a-col :span="24" style="margin-bottom: 14px">
          <a-card title="数据集切分配置" size="small" class="section-card">
            <a-descriptions :column="3" size="small">
              <a-descriptions-item label="切分比例">{{ task.form.splitSize }}</a-descriptions-item>
              <a-descriptions-item label="训练集">{{ splitParts(task.form.splitSize)[0] }}%</a-descriptions-item>
              <a-descriptions-item label="验证/测试">
                {{ splitParts(task.form.splitSize)[1] }}% / {{ splitParts(task.form.splitSize)[2] }}%
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="训练参数" size="small" class="section-card">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item
                v-for="item in kvRows(task.form.trainParams)"
                :key="`train-${item.key}`"
                :label="item.key"
              >
                {{ item.value }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="转换参数" size="small" class="section-card">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item
                v-for="item in kvRows(task.form.convertParams)"
                :key="`convert-${item.key}`"
                :label="item.key"
              >
                {{ item.value }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
      </a-row>
    </template>
    <a-empty v-else description="暂无详情" />
  </a-modal>
</template>

<script setup lang="ts">
  import {
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Empty as AEmpty,
    Modal as AModal,
    Row as ARow,
  } from 'ant-design-vue';
  import { SelfIterationParentTask } from '../types/selfIteration';

  defineProps<{
    visible: boolean;
    task: SelfIterationParentTask | null;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
  }>();

  function kvRows(record: Record<string, number | string | boolean>) {
    return Object.entries(record).map(([key, value]) => ({ key, value: String(value) }));
  }

  function splitParts(value: string) {
    const [train = 8, test = 1, val = 1] = String(value || '8-1-1')
      .split('-')
      .map((item) => Number(item));
    return [train * 10, test * 10, val * 10];
  }
</script>

<style scoped>
  .section-card {
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .section-card:hover {
    transform: translateY(-2px);
    border-color: rgba(24, 144, 255, 0.35);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.24);
  }

  .section-card :deep(.ant-card-head) {
    background: #1f1f1f;
    border-bottom-color: rgba(255, 255, 255, 0.1);
    padding: 0 16px;
  }

  .section-card :deep(.ant-card-body) {
    background: #1f1f1f;
    padding: 14px 16px;
  }

  :deep(.self-iteration-detail-modal .ant-modal-content) {
    background: #1f1f1f;
  }

  :deep(.self-iteration-detail-modal .ant-modal-header) {
    background: #1f1f1f;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding: 14px 24px;
  }

  :deep(.self-iteration-detail-modal .ant-modal-title) {
    font-size: 15px;
    font-weight: 600;
  }

  :deep(.self-iteration-detail-modal .ant-modal-close-x) {
    color: rgba(255, 255, 255, 0.7);
  }

  :deep(.self-iteration-detail-modal .ant-modal-close-x:hover) {
    color: #ffffff;
  }

  .detail-descriptions :deep(.ant-descriptions-view) {
    background: #1f1f1f;
  }

  :deep(.self-iteration-detail-modal .ant-descriptions-bordered) {
    border-color: rgba(255, 255, 255, 0.1);
  }

  :deep(.self-iteration-detail-modal .ant-descriptions-bordered .ant-descriptions-item-label) {
    background: rgba(255, 255, 255, 0.04);
  }

  :deep(.self-iteration-detail-modal .ant-descriptions-bordered .ant-descriptions-item-content) {
    background: rgba(255, 255, 255, 0.02);
  }
</style>
