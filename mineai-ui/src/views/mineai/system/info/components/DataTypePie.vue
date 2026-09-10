<template>
  <div class="flex flex-row">
    <img src="../../../../../assets/icons/titles.svg" />
    <span style="opacity: 0.8; font-size: medium"> 监控设备数据集版本占比 </span>
  </div>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 270px"></div>
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

  const getDataType = async () => {
    data.value = await maHttp.get(
      {
        url: 'monitor/getDataType',
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
        color: ['#517cfc', '#58D9F9', '#77cda8'],
        series: [
          {
            name: '数据集版本占比',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.image, name: '图像' },
              { value: data.value.video, name: '视频' },
              { value: data.value.audio, name: '音频' },
              { value: data.value.pointCloud, name: '点云' },
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
    await getDataType();
    loading.value = false;
    timer = setInterval(getDataType, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
