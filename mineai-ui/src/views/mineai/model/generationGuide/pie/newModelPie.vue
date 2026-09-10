<template>
  <Card :loading="loading">
    <div ref="chartRef" style="width: 100%; height: 220px"></div>
    <span class="flex justify-center align-center">
      <a style="color: white; align-content: center; padding-bottom: 10px">最新模型准确率</a>
    </span>
    <a style="color: #f40" class="text-5xl ; flex justify-center">{{ props.newJobAcc }}%</a>
  </Card>
</template>
<script lang="ts" setup>
  import { onMounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';

  const loading = ref(true);
  let data = ref();
  const props = defineProps<{
    newJobAcc: number;
  }>();

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
            data: [{ value: props.newJobAcc }, { value: 100 - props.newJobAcc }],
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
