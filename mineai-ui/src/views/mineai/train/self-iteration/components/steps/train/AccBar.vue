<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../../../assets/icons/titles.svg" alt="" />
      <span style="font-size: medium; min-width: 72px; white-space: nowrap">ACC曲线</span>
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
  import { newAccSeries } from './detailsData';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  const props = defineProps<{
    data: any;
  }>();

  onMounted(async () => {
    newAccSeries.value = [];
    loading.value = false;
  });

  watchEffect(() => {
    console.log('数据变化');
    // console.log(props.data);
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
        name: 'Epoch',
        nameLocation: 'end',
        type: 'category',
        boundaryGap: true,
        data: props.data?.['epoch'] ?? ['1'],
        axisTick: {
          show: false,
        },
      },
      yAxis: [
        {
          name: 'Accuracy',
          nameLocation: 'end',
          type: 'value',
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
          data: props.data && props.data['test accuracy'] ? props.data['test accuracy'] : [0],
          type: 'line',
          large: true,
          itemStyle: {
            color: '#ff8049',
          },
        },
        ...newAccSeries.value,
      ],
    });
  });
</script>
<style scoped>
  .ant-card-body {
    padding: 12px 12px 0;
  }
</style>
