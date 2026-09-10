<template>
  <div class="custom-card">
    <div class="flex flex-row">
      <div class="flex py-1">
        <img src="../../../../../../assets/icons/titles.svg" />
        <span style="font-size: medium">报警情况</span>
      </div>
    </div>
    <Card :loading="loading">
      <div ref="chartRef" style="width: 100%; height: 250px"></div>
    </Card>
  </div>
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
        url: 'modelAlert/getDataBar',
        params: { dateNum: 7 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
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
        grid: { left: '1%', right: '1%', top: '3%', bottom: 0, containLabel: true },
        series: [
          {
            name: '报警信息数量',
            smooth: true,
            data: data.value['alertNumList'],
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
<style scoped lang="less">
  .custom-card :deep(.ant-card) {
    background: transparent;
  }
</style>
