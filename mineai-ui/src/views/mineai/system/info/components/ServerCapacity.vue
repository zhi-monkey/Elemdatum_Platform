<template>
  <div class="flex flex-row"
    ><img src="../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8; font-size: medium"
      >算力控制器CPU总负载</span
    >
  </div>
  <Card :loading="loading">
    <div ref="chartRef" class="pt-5" style="width: 100%; height: 270px"></div>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
  let data = ref({});
  let dataTimer;

  const getData = async () => {
    data.value = await maHttp.get(
      {
        url: 'controllerLoad/getControllerCpuLoad',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
  };

  onMounted(async () => {
    await getData();
    loading.value = false;
    dataTimer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
  });

  watch(
    () => {
      setOptions({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            lineStyle: {
              width: 1,
              color: 'red',
            },
          },
        },
        xAxis: {
          type: 'category',
          boundaryGap: true,
          data: data.value['time'],
          axisTick: {
            show: false,
          },
        },
        yAxis: [
          {
            type: 'value',
            max: 100,
            splitNumber: 10,
            axisTick: {
              show: false,
            },
          },
        ],
        grid: { left: '1%', top: '2  %', bottom: 0, containLabel: true },
        series: [
          {
            smooth: 0.6,
            symbol: 'none',
            data: data.value['cpuLoad'],
            type: 'line',
            areaStyle: {
              color: '#108ee9',
              opacity: 0.5,
            },
            itemStyle: {
              color: '#108ee9',
            },
            lineStyle: {
              width: 2.5, // 0.1的线条是非常细的了
            },
          },
        ],
      });
    },
    () => {
      console.log(data.value);
    },
  );
</script>
