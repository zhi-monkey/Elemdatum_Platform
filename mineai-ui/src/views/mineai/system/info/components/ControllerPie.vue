<template>
  <div class="flex flex-row" style="margin-bottom: 8px; margin-top: 8px; margin-bottom: 8px"
    ><img src="../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8; font-size: medium"
      >算力控制器概览</span
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

  const getControllerNum = async () => {
    data.value = await maHttp.get(
      {
        url: 'controller/getControllerNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
  };

  onMounted(async () => {
    await getControllerNum();
    loading.value = false;
    dataTimer = setInterval(getControllerNum, 10000);
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
            name: '算力控制器预览',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.workingNum, name: '运行' },

              { value: data.value.errorNum, name: '故障' },
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
