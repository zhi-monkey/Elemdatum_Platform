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
    timer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });

  watch(
    () => {
      if (loading.value) {
        return;
      }
      setOptions({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            lineStyle: {
              width: 1,
              color: 'orangeRed',
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
        grid: { left: '1%', right: '1%', top: '2%', bottom: 0, containLabel: true },
        series: [
          {
            smooth: 0.6,
            symbol: 'none',
            data: data.value['cpuLoad'],
            type: 'line',
            zlevel: 1,
            z: 1,
            areaStyle: {
              color: 'cyan',
              opacity: 0.5,
            },
            itemStyle: {
              color: 'DarkBlue',
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

  onUnmounted(() => clearInterval(timer));
</script>
<style scoped></style>
