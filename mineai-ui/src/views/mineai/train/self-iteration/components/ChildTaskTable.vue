<template>
  <a-table :columns="columns" :data-source="tasks" :pagination="false" row-key="id" size="small">
    <template #status="{ record }">
      <a-tag :color="statusColorMap[record.status] || 'default'">{{
        statusTextMap[record.status] || '未知'
      }}</a-tag>
    </template>
    <template #currentStep="{ record }">
      {{ stepTextMap[record.currentStep] || '未知' }}
    </template>
    <template #createTime="{ text }">
      {{ formatToDateTime(text) }}
    </template>
    <template #updateTime="{ text }">
      {{ formatToDateTime(text) }}
    </template>
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'ant-design:info-circle-outlined',
            tooltip: '详情',
            onClick: () => emit('detail', record),
          },
        ]"
      />
    </template>
  </a-table>
</template>

<script setup lang="ts">
  import { Table as ATable, Tag as ATag } from 'ant-design-vue';
  import { TableAction } from '/@/components/Table';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import { SelfIterationChildTask } from '../types/selfIteration';

  defineProps<{
    tasks: SelfIterationChildTask[];
  }>();

  const emit = defineEmits<{
    (e: 'detail', record: SelfIterationChildTask): void;
  }>();

  const columns = [
    { title: '自迭代任务ID', dataIndex: 'id', width: 120 },
    { title: '迭代轮次', dataIndex: 'round', width: 100 },
    {
      title: '当前阶段',
      dataIndex: 'currentStep',
      width: 120,
      slots: { customRender: 'currentStep' },
    },
    { title: '状态', dataIndex: 'status', width: 120, slots: { customRender: 'status' } },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      slots: { customRender: 'createTime' },
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 180,
      slots: { customRender: 'updateTime' },
    },
    { title: '操作', dataIndex: 'action', width: 80, slots: { customRender: 'action' } },
  ];

  const stepTextMap: Record<string, string> = {
    dataCollection: '数据采集',
    dataAnnotation: '数据标注',
    modelIteration: '模型迭代',
    modelDispatch: '模型下发',
  };

  const statusTextMap: Record<string, string> = {
    pending: '待开始',
    collecting: '采集中',
    annotating: '标注中',
    training: '训练中',
    converting: '转换中',
    packaging: '打包中',
    dispatching: '下发中',
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
    waiting_review: '待审核',
  };

  const statusColorMap: Record<string, string> = {
    pending: 'default',
    collecting: 'blue',
    annotating: 'geekblue',
    training: 'processing',
    converting: 'gold',
    packaging: 'cyan',
    dispatching: 'purple',
    processing: 'processing',
    completed: 'success',
    failed: 'error',
    waiting_review: 'orange',
  };
</script>
