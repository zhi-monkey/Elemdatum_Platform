<template>
  <div class="flex flex-row">
    <img src="../../../../../assets/icons/titles.svg" />
    <span style="opacity: 0.8; font-size: medium"> 算力控制器服务占比 </span>
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
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  let data = ref({});
  let dataTimer;

  const getData = async () => {
    data.value = await maHttp.get(
      {
        url: 'controller/getControllerMineServiceNum',
        params: { num: 3 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
  };

  onMounted(async () => {
    await getData();
    loading.value = false;
    dataTimer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
  });

  watch(
    () => {
      let sceneList: any[] = [];
      for (let key in data.value) {
        sceneList.push({ value: data.value[key], name: key });
      }
      const colorList = ['#517cfc', '#FFF369', '#77cda8'];
      setOptions({
        grid: [{ left: '50%', top: '7%', width: '45%', height: '90%' }],
        tooltip: {},
        legend: {
          bottom: '1%',
          left: 'center',
        },
        color: sceneList.map((_, idx) => colorList[idx % 3]),
        series: [
          {
            name: '运行状况占比',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: sceneList,
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
</script>
