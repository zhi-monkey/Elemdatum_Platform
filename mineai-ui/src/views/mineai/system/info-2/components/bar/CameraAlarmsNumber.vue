<template>
  <div class="custom-card">
    <div class="flex flex-row">
      <div class="flex py-1">
        <img src="../../../../../../assets/icons/titles.svg" />
        <span style="font-size: medium">算法报警数</span>
      </div>
    </div>
    <Card :loading="loading">
      <div ref="chartRef" style="width: 100%; height: 250px"></div>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { onUnmounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  let data = ref({});
  let timer;
  function getData() {
    maHttp
      .get(
        {
          url: 'modelAlert/getMostModelAlertModelListByDateNum',
          params: { dateNum: 7 },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        for (const item in v) {
          if (v[item].toString() !== data.value[item].toString()) {
            data.value[item] = v[item];
          }
        }
      });
  }

  maHttp
    .get(
      {
        url: 'modelAlert/getMostModelAlertModelListByDateNum',
        params: { dateNum: 7 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      data.value = v;
      setTimeout(() => {
        loading.value = false;
      }, 100);
      timer = setInterval(getData, 10000);
    });
  onUnmounted(() => clearInterval(timer));

  watch(
    () => {
      setOptions({
        dataZoom: {
          type: 'inside',
          startValue: 0,
          endValue: 9,
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow',
          },
        },
        legend: {
          bottom: '1%',
          left: 'center',
        },
        grid: {
          top: '3%',
          bottom: '2%',
          left: '2%',
          right: '1%',
          containLabel: true,
        },
        xAxis: [
          {
            position: 'bottom',
            nameLocation: 'end',
            type: 'category',
            data: data.value['nameList'],
            axisLabel: {
              interval: 0,
              rotate: 45,
            },
          },
        ],
        yAxis: [
          {
            type: 'value',
            name: '',
            minInterval: 1,
            max: (value) => {
              return Math.ceil(value.max * 1.2);
            },
          },
        ],
        series: [
          {
            type: 'bar',
            stack: 'Ad',
            emphasis: {
              focus: 'series',
            },
            barWidth: '80%',
            data: data.value['numList'],
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
