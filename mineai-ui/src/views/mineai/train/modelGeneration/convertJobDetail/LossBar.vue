<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../assets/icons/titles.svg" alt="" />
      <span style="font-size: medium; min-width: 72px; white-space: nowrap">LOSS曲线</span>
    </div>
  </div>
  <Card>
    <div ref="chartRef" style="width: 100%; height: 31vh"></div>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, Ref, ref, watchEffect } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { newLossSeries } from './detailsData';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  const props = defineProps<{
    epochData: any;
  }>();

  onMounted(async () => {
    newLossSeries.value = [];
    loading.value = false;
  });

  watchEffect(() => {
    setOptions({
      legend: {
        orient: 'vertical',
        top: 24,
        x: 'right',
        y: 'center',
        padding: [0, 10, 0, 0],
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          lineStyle: {
            width: 2,
            color: '#24ffb8',
          },
        },
      },
      xAxis: {
        type: 'category',
        name: 'Epoch',
        nameLocation: 'end',
        boundaryGap: true,
        data: props.epochData?.['epoch'] ?? ['1'],
        axisTick: {
          show: false,
        },
      },
      yAxis: [
        {
          type: 'value',
          name: 'Loss',
          nameLocation: 'end',
          splitNumber: 5,
          axisTick: {
            show: false,
          },
        },
      ],
      grid: { left: '1%', right: '10%', top: '10%', bottom: '5%', containLabel: true },
      series: [
        {
          name: '当前作业',
          smooth: true,
          symbolSize: 6,
          data: props.epochData?.['train loss'] ?? [0],
          type: 'line',
          itemStyle: {
            color: '#ff8049',
          },
        },
        ...newLossSeries.value,
      ],
    });
  });
</script>
<style scoped>
  .ant-card-body {
    padding: 12px 12px 0;
  }
</style>
