<template>
  <div class="px-2 py-1">
    <div class="flex flex-row" style="margin-bottom: 8px; margin-top: 8px"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >绑定设备状态</span
      >
    </div>
    <Card :loading="loading">
      <div ref="chartRef" style="width: 100%; height: 220px"></div>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { onUnmounted, Ref, ref, watch } from 'vue';
  import { Card } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const loading = ref(true);
  let data = ref();
  let timer;
  maHttp
    .get(
      {
        url: 'monitorDataset/getMonitorState',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    )
    .then((v) => {
      data.value = v;
      loading.value = false;
      timer = setInterval(() => {
        maHttp
          .get(
            {
              url: 'monitorDataset/getMonitorState',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then((v) => {
            for (const item in v) {
              if (v[item].toString() !== data.value[item].toString()) {
                data.value[item] = v[item];
              }
            }
          });
      }, 10000);
    });

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
        color: ['#51bafc', '#6bf958', '#77cda8'],
        series: [
          {
            name: '设备类型占比',
            type: 'pie',
            radius: '50%',
            center: ['50%', '40%'],
            data: [
              { value: data.value.on, name: '启用' },
              { value: data.value.off, name: '停用' },
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

  onUnmounted(() => clearInterval(timer));
</script>
