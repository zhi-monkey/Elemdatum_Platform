<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../assets/icons/titles.svg" />
      <span style="font-size: medium; min-width: 72px; white-space: nowrap">ACC曲线</span>
    </div>
  </div>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 31vh"></div>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref, watchEffect } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useRoute } from 'vue-router';
  import { currentJobMaxAcc, newAccSeries, jobName } from './detailsData';

  const route = useRoute();
  const jobId = ref(route.params?.jobId);
  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
  let data = ref({});
  let dataTimer;

  const getData = async () => {
    data.value = await maHttp.get(
      {
        url: 'log/getEpochDetail',
        params: {
          jobName: jobName.value,
          limit: 500,
          start: 1,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    console.log(data);
    let currentMaxAccNumber = 0;
    data.value['test accuracy'].forEach((value) => {
      if (Number(value) > currentMaxAccNumber) {
        currentMaxAccNumber = Number(value);
      }
    });
    currentJobMaxAcc.value = currentMaxAccNumber;
  };

  onMounted(async () => {
    newAccSeries.value = [];
    await getData();
    loading.value = false;
    dataTimer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
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
        name: 'Epoch',
        nameLocation: 'end',
        type: 'category',
        boundaryGap: true,
        data: data.value['epoch'],
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
          data: data.value['test accuracy'],
          type: 'line',
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
