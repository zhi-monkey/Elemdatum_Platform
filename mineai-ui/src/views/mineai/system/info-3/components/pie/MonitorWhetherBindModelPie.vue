<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../../assets/icons/titles.svg" />
      <span style="font-size: medium">设备绑定算法情况</span>
    </div>
  </div>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 250px"></div>
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

  const getMonitorNumWhetherBindModel = async () => {
    data.value = await maHttp.get(
      {
        url: 'monitor/getMonitorNumWhetherBindModel',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
  };

  onMounted(async () => {
    await getMonitorNumWhetherBindModel();
    loading.value = false;
    dataTimer = setInterval(getMonitorNumWhetherBindModel, 10000);
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
        color: ['#218cfc', '#fc1d7b'],
        series: [
          {
            name: '绑定算法与未绑定算法监控设备',
            type: 'pie',
            radius: '75%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.notBindNum, name: '未绑定算法' },

              { value: data.value.bindNum, name: '已绑定算法' },
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
