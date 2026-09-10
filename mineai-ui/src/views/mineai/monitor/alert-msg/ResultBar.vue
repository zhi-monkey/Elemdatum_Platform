<template>
  <div class="px-1 py-1">
    <div class="flex py-1 px-1"
      ><img src="../../../../assets/icons/titles.svg" /><span style="font-size: medium">周报</span>
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
  let timer;

  const getDataBar = async () => {
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
    await getDataBar();
    loading.value = false;
    setInterval(getDataBar, 10000);
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
          },
        ],
        series: [
          {
            name: '警报',
            type: 'bar',
            stack: 'Ad',
            emphasis: {
              focus: 'series',
            },
            data: data.value['alertNumList'],
            itemStyle: {
              color: '#ff6e76',
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
