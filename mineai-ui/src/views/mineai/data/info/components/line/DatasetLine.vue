<template>
  <div class="px-2 py-1">
    <div class="flex flex-row" style="margin-bottom: 8px"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >数据集每日创建量</span
      >
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
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref<boolean>(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  let data = ref({});
  let timer;

  async function getData() {
    await maHttp
      .get(
        {
          url: 'datasets/getPeriodDataByDate',
          params: { dateNum: 7 },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        for (const item in v) {
          if (v[item] !== data.value[item]) {
            data.value[item] = v[item];
          }
        }
      });
  }

  onMounted(() => {
    getData().then(() => {
      loading.value = false;
      timer = setInterval(getData, 10000);
    });
  });

  onUnmounted(() => clearInterval(timer));

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
            // max: 20,
            minInterval: 1,
            axisTick: {
              show: false,
            },
          },
        ],
        grid: { left: '1%', right: '1%', top: '2  %', bottom: 0, containLabel: true },
        series: [
          {
            smooth: true,
            data: data.value['numList'],
            type: 'line',
            areaStyle: {},
            itemStyle: {
              color: '#218cfc',
            },
          },
        ],
      });
    },
    () => {},
  );
</script>
