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

  const getModelAlertNum = async () => {
    data.value = await maHttp.get(
      {
        url: 'modelAlert/getModelAlertDataType',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
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
        color: ['#7CFFB2', '#FF6E76', '#517cfc'],
        series: [
          {
            name: '报警信息类型占比',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.image, name: '图片' },
              { value: data.value.audio, name: '音频' },
              { value: data.value.video, name: '视频' },
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
    await getModelAlertNum();
    loading.value = false;
    timer = setInterval(getModelAlertNum, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
