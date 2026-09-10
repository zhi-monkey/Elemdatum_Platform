<template>
  <div class="px-0 py-1">
    <a-card style="padding: 10px">
      <div ref="chartRef" style="width: 100%; height: 280px"></div>
    </a-card>
  </div>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref, watch } from 'vue';
  import { Card as ACard } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  let data = ref({});
  let timer;

  const getData = async () => {
    data.value = await maHttp.get(
      {
        url: 'controllerAlert/getDataBar',
        params: { dateNum: 7 },
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
    timer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });

  watch(
    () => {
      setOptions({
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
          boundaryGap: true,
          data: data.value['dateList'],
          axisTick: {
            show: false,
          },
        },
        yAxis: [
          {
            type: 'value',
            splitNumber: 5,
            axisTick: {
              show: false,
            },
          },
        ],
        grid: { left: '1%', right: '1%', top: '2  %', bottom: 0, containLabel: true },
        series: [
          {
            name: '报警信息数量',
            smooth: true,
            data: data.value['controllerAlertNumList'],
            type: 'line',
            areaStyle: {},
            itemStyle: {
              color: '#218cfc',
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
<style scoped>
  ant-card-body {
    padding: 12px;
  }
</style>
