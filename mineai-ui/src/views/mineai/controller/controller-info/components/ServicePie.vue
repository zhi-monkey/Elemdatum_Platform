<template>
  <div class="px-0 py-1">
    <Card :loading="loading">
      <div ref="chartRef" style="width: 100%; height: 300px"></div>
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

  const getControllerMineServiceNum = async () => {
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
    await getControllerMineServiceNum();
    loading.value = false;
    timer = setInterval(getControllerMineServiceNum, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });

  watch(
    () => {
      let sceneList: any[] = [];
      for (let key in data.value) {
        sceneList.push({ value: data.value[key], name: key });
      }
      const colorList = ['#517cfc', '#FFF369', '#77cda8', '#6666FF', '#66FFFF'];
      setOptions({
        grid: [{ left: '50%', top: '7%', width: '45%', height: '90%' }],
        tooltip: {},
        legend: {
          bottom: '1%',
          left: 'center',
        },
        color: sceneList.map((_, idx) => colorList[idx % 5]),
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
  onUnmounted(() => clearInterval(timer));
</script>
