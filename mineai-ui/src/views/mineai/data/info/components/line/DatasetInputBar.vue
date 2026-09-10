<template>
  <div class="px-2 py-1">
    <div class="flex flex-row" style="margin-bottom: 8px"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >数据集类别</span
      >
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
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  let data = ref({});
  let timer;

  function getData() {
    maHttp
      .get(
        {
          url: 'datasets/getDatasetStatByTypeAndDate',
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
  maHttp
    .get(
      {
        url: 'datasets/getDatasetStatByTypeAndDate',
        params: { dateNum: 7 },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
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
            data: data.value['dateList'],
          },
        ],
        yAxis: [
          {
            type: 'value',
            minInterval: 1,
          },
        ],
        series: [
          {
            name: '目标检测',
            type: 'bar',
            stack: 'Ad',
            emphasis: {
              focus: 'series',
            },
            data: data.value['detectionNumList'],
            itemStyle: {
              color: '#218cfc',
            },
          },
          {
            name: '目标分割',
            type: 'bar',
            stack: 'Ad',
            emphasis: {
              focus: 'series',
            },
            data: data.value['segmentationNumList'],
            itemStyle: {
              color: '#87d068',
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
