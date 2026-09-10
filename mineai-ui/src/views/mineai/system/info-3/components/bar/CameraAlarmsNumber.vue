<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../../assets/icons/titles.svg" />
      <span style="font-size: medium">算法报警数</span>
    </div>
  </div>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 250px"></div>
  </Card>
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
  let dataSize = 7;
  let slidingTimer;
  let options = {
    dataZoom: {
      type: 'inside',
      startValue: 0,
      endValue: 3,
    },
  };
  function sliding() {
    options.dataZoom.startValue = (options.dataZoom.startValue + 1) % dataSize;
    options.dataZoom.endValue = (options.dataZoom.endValue + 1) % dataSize;
    setOptions(options, false);
  }
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
      timer = setInterval(getData, 60000);
      slidingTimer = setInterval(sliding, 5100);
    });
  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(slidingTimer);
  });

  watch(
    () => {
      options.dataZoom.startValue = 0;
      options.dataZoom.endValue = 3;
      setOptions({
        dataZoom: {
          type: 'inside',
          startValue: 0,
          endValue: 2,
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
          top: '2%',
          bottom: '10%',
          left: '2%',
          right: '1%',
          containLabel: true,
        },
        xAxis: [
          {
            type: 'category',
            data: data.value['nameList'],
            axisLabel: {
              interval: 0,
            },
          },
        ],
        yAxis: [
          {
            type: 'value',
            name: '数量',
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
            barWidth: '50%',
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
