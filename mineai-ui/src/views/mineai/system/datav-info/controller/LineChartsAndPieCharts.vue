<template>
  <!--原来是border-box12-->
  <BorderBox3 style="height: 300px; padding: 10px">
    <a-row :gutter="6">
      <a-col class="charts" :span="9">
        <div class="chart-name">
          报警情况
          <decoration3 style="width: 80px; height: 20px" />
        </div>
        <charts :option="alertData" />
      </a-col>

      <a-col class="charts" :span="9">
        <div class="chart-name">
          算力控制器CPU总负载
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <charts :option="loadData" />
      </a-col>

      <a-col class="charts" :span="6">
        <div class="header-name">
          设备服务占比
          <decoration3 style="width: 110px; height: 20px" />
        </div>
        <div ref="deviceServiceChart" class="w-full h-9/10"></div>
      </a-col>
    </a-row>
  </BorderBox3>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
  import { BorderBox3, Charts, Decoration3 } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useECharts } from '/@/hooks/web/useECharts';

  const deviceServiceChart = ref<HTMLDivElement | null>(null);
  const alertData = ref({});
  const loadData = ref({});
  const getAlertData = async () => {
    const result = await maHttp.get(
      {
        url: 'modelAlert/getDataBar',
        params: { dateNum: 7 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    alertData.value = {
      legend: {
        data: ['报警情况'],
        textStyle: {
          fill: '#fff',
        },
      },
      xAxis: {
        data: result['dateList'],
        boundaryGap: false,
        axisLine: {
          style: {
            stroke: '#999',
          },
        },
        axisLabel: {
          style: {
            fill: '#999',
          },
        },
        axisTick: {
          show: false,
        },
      },
      yAxis: {
        data: 'value',
        splitLine: {
          show: false,
        },
        axisLine: {
          style: {
            stroke: '#999',
          },
        },
        axisLabel: {
          style: {
            fill: '#999',
          },
          formatter({ value }) {
            return value.toFixed(2);
          },
        },
        axisTick: {
          show: false,
        },
        min: 0,
        max: 100,
        interval: 20,
      },
      series: [
        {
          data: Array.from(result['alertNumList'], (k) => Number(k)),
          type: 'line',
          name: '报警情况',
          smooth: true,
          lineArea: {
            show: true,
            gradient: ['rgba(55, 162, 218, 0.6)', 'rgba(55, 162, 218, 0)'],
          },
          linePoint: {
            radius: 4,
            style: {
              fill: '#00db95',
            },
          },
        },
      ],
    };
  };
  const getLoadData = async () => {
    const result = await maHttp.get(
      {
        url: 'controllerLoad/getControllerCpuLoad',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    loadData.value = {
      legend: {
        data: ['算力控制器CPU总负载'],
        textStyle: {
          fill: '#fff',
        },
      },
      xAxis: {
        type: 'category',
        data: Array.from({ length: 8 }, (_, i) => result['time'][i * 3]),
        boundaryGap: false,
        axisLine: {
          style: {
            stroke: '#999',
          },
        },
        axisLabel: {
          style: {
            fill: '#999',
          },
        },
        axisTick: {
          show: false,
        },
      },
      yAxis: {
        data: 'value',
        splitLine: {
          show: false,
        },
        axisLine: {
          style: {
            stroke: '#999',
          },
        },
        axisLabel: {
          style: {
            fill: '#999',
          },
          formatter({ value }) {
            return value.toFixed(2);
          },
        },
        axisTick: {
          show: false,
        },
        min: 0,
        max: 100,
        interval: 20,
      },
      series: [
        {
          data: Array.from({ length: 8 }, (_, i) => Number(result['cpuLoad'][i * 3])),
          type: 'line',
          name: '算力控制器CPU总负载',
          smooth: true,
          lineArea: {
            show: true,
            gradient: ['rgba(55, 162, 218, 0.6)', 'rgba(55, 162, 218, 0)'],
          },
          linePoint: {
            radius: 4,
            style: {
              fill: '#00db95',
            },
          },
        },
      ],
    };
  };
  const getServiceData = async () => {
    const result = await maHttp.get(
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
    let serviceList: any[] = [];
    for (let key in result) {
      serviceList.push({ value: result[key], name: key });
    }
    const { setOptions } = useECharts(deviceServiceChart as Ref<HTMLDivElement>);
    setOptions({
      tooltip: {},
      grid: {
        left: 95,
        bottom: 20,
      },
      color: ['#4d99fc', '#40faee', '#91cd77', '#f4c427'],
      series: [
        {
          type: 'pie',
          radius: '68%',
          center: ['50%', '50%'],
          data: serviceList,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          labelLine: { show: true },
          label: {
            show: true,
            position: 'outer',
            alignTo: 'edge',
            margin: '18%',
            formatter: '{b}\n({d}%)',
            color: 'inherit',
          },
        },
      ],
    });
  };
  let timer: NodeJS.Timer;

  onMounted(async () => {
    await Promise.all([getAlertData(), getLoadData(), getServiceData()]);
    timer = setInterval(async () => {
      await Promise.all([getAlertData(), getLoadData(), getServiceData()]);
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
<style lang="less" scoped>
  .charts {
    top: 20px;
  }

  .bcci-header {
    height: 50px;
    text-align: center;
    line-height: 50px;
    font-size: 20px;
  }

  .dv-active-ring-chart {
    height: calc(~'100% - 80px');
  }

  .label-tag {
    height: 30px;
  }

  .active-ring-name {
    font-size: 18px !important;
  }

  .chart-name {
    position: absolute;
    left: 20px;
    text-align: left;
    font-size: 20px;
    top: 0;
    font-weight: bold;
  }

  .dv-charts-container {
    height: calc(~'100% - 20px');
    top: 10px;
  }

  .dv-capsule-chart {
    height: 80%;
  }

  .header-name {
    position: relative;
    top: 5px;
    font-size: 18px;
    height: 20px;
    line-height: 20px;
    font-weight: bold;
  }

  .dv-capsule-chart {
    top: 10px;
  }

  .dv-border-box-12 {
    height: 50% !important;
  }
</style>
