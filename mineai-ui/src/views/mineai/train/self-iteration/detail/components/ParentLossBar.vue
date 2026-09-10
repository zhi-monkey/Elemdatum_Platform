<template>
  <a-card size="small" class="chart-card">
    <template #title>召回率曲线</template>
    <div ref="chartRef" class="chart-body"></div>
  </a-card>
</template>

<script lang="ts" setup>
  import { ref, watchEffect } from 'vue';
  import { Card as ACard } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';

  const props = defineProps<{
    data: {
      epoch: Array<string | number>;
      recall: number[];
    };
  }>();

  const chartRef = ref<HTMLDivElement>();
  const { setOptions } = useECharts(chartRef);

  watchEffect(() => {
    setOptions({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '4%', containLabel: true },
      xAxis: {
        type: 'category',
        name: '轮次',
        data: props.data.epoch,
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        name: '召回率',
        splitNumber: 4,
      },
      series: [
        {
          name: '召回率',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          data: props.data.recall,
          itemStyle: { color: '#faad14' },
          areaStyle: { color: 'rgba(250, 173, 20, 0.15)' },
        },
      ],
    });
  });
</script>

<style scoped>
  .chart-card :deep(.ant-card-head),
  .chart-card :deep(.ant-card-body) {
    background: #181d31;
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .chart-body {
    width: 100%;
    height: 260px;
  }
</style>
