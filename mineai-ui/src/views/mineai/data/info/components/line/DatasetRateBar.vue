<template>
  <div class="px-2 py-1">
    <div class="flex flex-row" style="margin-bottom: 8px; margin-top: 8px"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >数据集发布情况</span
      >
    </div>
    <Card :loading="loading">
      <div ref="chartRef" style="width: 100%; height: 220px"></div>
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
          url: 'dataset/getSeriesNum',
          params: { num: 5 },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
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
        url: 'dataset/getSeriesNum',
        params: { num: 5 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
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
            data: data.value['nameList'],
          },
        ],
        yAxis: [
          {
            type: 'value',
            name: '数量',
            minInterval: 1,
          },
        ],
        series: [
          {
            type: 'bar',
            stack: 'Ad',
            emphasis: {
              focus: 'series',
            },
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
