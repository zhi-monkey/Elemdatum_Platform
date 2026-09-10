<template>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 220px"></div>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  let data = ref();
  let timer;

  const getPushOrForwardNum = async () => {
    data.value = await maHttp.get(
      {
        url: 'monitor/getPushNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
  };

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
  watch(
    () => {
      if (loading.value) {
        return;
      }
      setOptions({
        grid: [{ left: '50%', top: '7%', width: '45%', height: '90%' }],
        tooltip: {},
        legend: {
          bottom: '1%',
          left: 'center',
        },
        color: ['#7CFFB2', '#FFF369'],
        series: [
          {
            name: '推流占比',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.pushStream, name: '直接连接' },
              { value: data.value.notPushStream, name: '未直接连接' },
            ],
            labelLine: { show: true },
            label: {
              show: true,
              formatter: '{b} \n ({d}%)',
              color: '#B1B9D3',
            },
          },
        ],
      });
    },
    () => {
      console.log(data.value);
    },
  );

  onMounted(async () => {
    await getPushOrForwardNum();
    loading.value = false;
    timer = setInterval(getPushOrForwardNum, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
