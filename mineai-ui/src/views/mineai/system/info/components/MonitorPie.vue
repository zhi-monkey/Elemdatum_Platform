<template>
  <div class="flex flex-row" style="margin-bottom: 8px; margin-top: 8px"
    ><img src="../../../../../assets/icons/titles.svg" alt="图像显示失败" /><span
      style="opacity: 0.8; font-size: medium"
      >监控设备概览</span
    >
  </div>
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
  let dataTimer;

  const getMonitorNum = async () => {
    data.value = await maHttp.get(
      {
        url: 'monitor/getMonitorNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
  };

  onMounted(async () => {
    await getMonitorNum();
    loading.value = false;
    dataTimer = setInterval(getMonitorNum, 10000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
  });

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
  watch(
    () => loading.value,
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
        color: ['#218cfc', '#fc1d7b', '#24ffb8'],
        series: [
          {
            name: '监控设备预览',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.monitorOnNum, name: '启用' },
              { value: data.value.monitorOffNum, name: '停用' },
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
    { immediate: true },
  );
</script>
