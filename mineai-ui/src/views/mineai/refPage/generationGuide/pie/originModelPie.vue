<template>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 220px"></div>
    <span class="flex justify-center align-center">
      <a style="color: white; align-content: center; padding-bottom: 10px">原始模型准确率</a>
    </span>
    <a style="color: #517cfc" class="text-5xl ; flex justify-center">{{ props.originJobAcc }}%</a>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';

  const props = defineProps<{
    originJobAcc: number;
  }>();
  const loading = ref(true);
  let data = ref();

  // const getDataType = async () => {
  //   data.value = await maHttp.get(
  //     {
  //       url: 'monitor/getDataType',
  //       headers: {
  //         // @ts-ignore
  //         ignoreCancelToken: true,
  //       },
  //     },
  //     { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
  //   );
  // };
  let stys = ref();

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
        color: ['#517cfc', '#ff8049'],
        series: [
          {
            name: '准确率',
            type: 'pie',
            radius: '75%',
            center: ['50%', '40%'],
            data: [{ value: props.originJobAcc }, { value: 100 - props.originJobAcc }],
            labelLine: { show: false },
            label: {
              show: false,
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
    // await getDataType();
    loading.value = false;
  });
</script>
