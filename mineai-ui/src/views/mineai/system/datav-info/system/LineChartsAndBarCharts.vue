<template>
  <border-box13>
    <a-row :gutter="6">
      <a-col class="charts" :span="8">
        <div class="chart-name"> CPU负载情况 </div>
        <!--        <charts :option="alertData" />-->
        <charts :option="loadData" />
      </a-col>
      <a-col class="charts" :span="8">
        <div class="header-name">算法报警数</div>
        <div ref="modelBar" class="w-full h-full"></div>
      </a-col>
      <a-col class="charts" :span="8">
        <div class="header-name">监控设备报警数</div>
        <div ref="monitorBar" class="w-full h-full"></div>
      </a-col>
    </a-row>
  </border-box13>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
  import { BorderBox13, Charts } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useECharts } from '/@/hooks/web/useECharts';

  const modelBar = ref<HTMLDivElement | null>(null);
  const monitorBar = ref<HTMLDivElement | null>(null);
  const loadData = ref({});

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
        bottom: '10%',
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
          // label: {
          //   show: true,
          //   formatter: '{value} %',
          // },
        },
      ],
    };
  };

  const getAlgorithmAlertData = async () => {
    const result = await maHttp.get(
      {
        url: 'modelAlert/getMostModelAlertModelListByDateNum',
        params: { dateNum: 7, modelNum: 5 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );

    // 转换成charts对应格式
    let temp = result['nameList'].map(function (e, i) {
      return { name: e, value: result['numList'][i] };
    });
    temp.sort((a, b) => {
      return a.value - b.value;
    });
    let x: number[] = [];
    let y: string[] = [];
    temp.forEach((t) => {
      x.push(t.value);
      y.push(t.name);
    });
    const { setOptions } = useECharts(modelBar as Ref<HTMLDivElement>);
    setOptions({
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      grid: {
        left: 95,
        bottom: 75,
      },
      xAxis: {
        name: '条',
        type: 'value',
        minInterval: 1,
      },
      yAxis: {
        name: '算法',
        type: 'category',
        data: y,
        axisLabel: {
          interval: 0,
          formatter: (value) => {
            let texts = value;
            let length = 0;
            let endIndex = 0;
            for (let i = 0; i < texts.length; i++) {
              if (texts.charAt(i).match(/[\u4e00-\u9fa5]/g) != null) length += 2;
              else if (texts.charAt(i).match(/[A-Z]/g)) length += 1.5;
              else length += 1;
              if (length > 14) {
                endIndex = i;
                break;
              }
            }
            if (length > 14) {
              texts = texts.substring(0, endIndex) + '...';
            }
            return texts;
          },
        },
      },
      series: [
        {
          data: x,
          type: 'bar',
          label: { show: true, offset: [7, 0], color: '#ffffff' },
          itemStyle: {
            color: function (params) {
              let colorList = ['#6abc6b', '#d8ba4f', '#91d1a8', '#75bfde', '#5dc8ca', '#bca16a'];
              return colorList[params.dataIndex];
            },
          },
        },
      ],
    });
  };

  const getMonitorAlertData = async () => {
    const result = await maHttp.get(
      {
        url: 'modelAlert/getMostModelAlertMonitorListByDateNum',
        params: { dateNum: 7 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );

    // 转换成charts对应格式
    let temp = result['nameList'].map(function (e, i) {
      return { name: e, value: result['numList'][i] };
    });
    temp.sort((a, b) => {
      return a.value - b.value;
    });
    let x: number[] = [];
    let y: string[] = [];
    temp.forEach((t) => {
      x.push(t.value);
      y.push(t.name);
    });
    const { setOptions } = useECharts(monitorBar as Ref<HTMLDivElement>);
    setOptions({
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      grid: {
        left: 95,
        bottom: 75,
      },
      xAxis: {
        name: '条',
        type: 'value',
        minInterval: 1,
      },
      yAxis: {
        name: '监控设备',
        type: 'category',
        data: y,
        axisLabel: {
          interval: 0,
          formatter: (value) => {
            let texts = value;
            let length = 0;
            let endIndex = 0;
            for (let i = 0; i < texts.length; i++) {
              if (texts.charAt(i).match(/[\u4e00-\u9fa5]/g) != null) length += 2;
              else if (texts.charAt(i).match(/[A-Z]/g)) length += 1.5;
              else length += 1;
              if (length > 13) {
                endIndex = i;
                break;
              }
            }
            if (length > 13) {
              texts = texts.substring(0, endIndex) + '...';
            }
            return texts;
          },
        },
      },
      series: [
        {
          data: x,
          type: 'bar',
          label: { show: true, offset: [7, 0], color: '#ffffff' },
          itemStyle: {
            color: function (params) {
              let colorList = ['#2aa5c3', '#aaabd3', '#df9fce', '#fb7293', '#e062ae'];
              return colorList[params.dataIndex];
            },
          },
        },
      ],
    });
  };

  let timer: NodeJS.Timer;

  onMounted(async () => {
    await Promise.all([getLoadData(), getAlgorithmAlertData(), getMonitorAlertData()]);
    timer = setInterval(async () => {
      await Promise.all([getLoadData(), getAlgorithmAlertData(), getMonitorAlertData()]);
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
<style lang="less">
  .charts {
    height: 250px;
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
    top: 10px;
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
    top: 15px;
    font-size: 18px;
    height: 20px;
    line-height: 20px;
    font-weight: bold;
  }

  .dv-capsule-chart {
    top: 10px;
  }

  .dv-border-box-13 {
    height: 250px !important;
  }
</style>
