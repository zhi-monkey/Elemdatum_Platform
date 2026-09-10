<template>
  <BorderBox3 style="height: 300px; padding: 10px">
    <a-row :gutter="6">
      <a-col class="charts" :span="4">
        <div class="chart-name">
          监控设备总数
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div class="number-chart">
          {{
            monitorData.notBindNum + monitorData.bindNum
              ? monitorData.notBindNum + monitorData.bindNum
              : ''
          }}
        </div>
      </a-col>

      <a-col class="charts" :span="4">
        <div class="chart-name">
          今日报警总数
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div class="number-chart" style="color: #094bd9"> {{ alertData.sumNum }}</div>
      </a-col>

      <a-col class="charts" :span="4">
        <div class="chart-name">
          今日未解决报警数
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div class="number-chart" style="color: #dd4a68"> {{ alertData.unHandledNum }}</div>
      </a-col>

      <a-col class="charts" :span="6">
        <div class="chart-name">
          算法绑定统计
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="monitorChart" class="chart-body"></div>
      </a-col>

      <a-col class="charts" :span="6">
        <div class="chart-name">
          数据集数量占比
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="quantityChart" class="chart-body"></div>
      </a-col>
    </a-row>
  </BorderBox3>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { BorderBox3, Decoration3 } from '@kjgl77/datav-vue3';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';

  // const userData = ref({});
  const monitorData = ref({});
  const alertData = ref({});
  // const userChart = ref<HTMLDivElement | null>(null);
  const monitorChart = ref<HTMLDivElement | null>(null);
  const alertChart = ref<HTMLDivElement | null>(null);
  const quantityChart = ref<HTMLDivElement | null>(null);

  async function getData2() {
    //数据集类型数量占比
    maHttp
      .get(
        {
          url: 'datasets/getDatasetStatByType',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        const { setOptions } = useECharts(quantityChart as Ref<HTMLDivElement>);
        setOptions({
          tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
          color: ['#4d99fc', '#40faee', '#91cd77', '#f4c427'],
          series: [
            {
              type: 'pie',
              radius: '55%',
              center: ['50%', '50%'],
              data: [
                { value: v.detection, name: '目标检测' },
                { value: v.segmentation, name: '目标分割' },
              ],
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)',
                },
              },
              labelLine: {
                show: true,
              },
              label: {
                show: true,
                position: 'outer',
                alignTo: 'edge',
                margin: '12%',
                formatter: '{b}\n({d}%)',
                color: 'inherit',
              },
            },
          ],
        });
      });
  }

  const getAlertNum = async () => {
    alertData.value = await maHttp.get(
      {
        url: 'modelAlert/getModelAlertStatusNumByDays',
        params: { day: 0 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );

    const { setOptions } = useECharts(alertChart as Ref<HTMLDivElement>);
    setOptions({
      tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
      color: ['#ffc530', '#00baff', '#3de7c9', '#469f4b'],
      series: [
        {
          type: 'pie',
          radius: '55%',
          center: ['50%', '60%'],
          data: [
            { name: '未解决', value: alertData.value.unHandledNum },
            { name: '已解决', value: alertData.value.handledNum },
            { name: '忽略', value: alertData.value.ignoredNum },
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          labelLine: {
            show: true,
            // 方案1
            // length: 30,
            // length2: 50,
          },
          label: {
            show: true,
            // 方案2
            position: 'outer',
            alignTo: 'edge',
            margin: '1%',
            formatter: '{b}\n({d}%)',
            color: 'inherit',
          },
        },
      ],
    });
  };

  const getMonitorNum = async () => {
    monitorData.value = await maHttp.get(
      {
        url: 'monitor/getMonitorNumWhetherBindModel',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );

    const { setOptions } = useECharts(monitorChart as Ref<HTMLDivElement>);
    setOptions({
      tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
      color: ['#00baff', '#3de7c9', '#fff', '#ffc530', '#469f4b'],
      series: [
        {
          type: 'pie',
          radius: '55%',
          center: ['50%', '50%'],
          data: [
            { name: '已绑定算法', value: monitorData.value.bindNum },
            { name: '未绑定算法', value: monitorData.value.notBindNum },
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
          labelLine: {
            show: true,
          },
          label: {
            show: true,
            position: 'outer',
            alignTo: 'edge',
            margin: '8%',
            formatter: '{b}\n({d}%)',
            color: 'inherit',
            overflow: 'break',
          },
        },
      ],
    });
  };

  let timer: NodeJS.Timer;

  onMounted(async () => {
    await Promise.all([getData2(), getAlertNum(), getMonitorNum()]);
    timer = setInterval(async () => {
      await Promise.all([getData2(), getAlertNum(), getMonitorNum()]);
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style lang="less">
  .charts {
    top: 20px;
    display: flex;
    flex-direction: column;
  }

  .chart-name {
    position: absolute;
    margin-left: 20px;
    text-align: left;
    font-size: 20px;
    top: 10px;
    font-weight: bold;
  }

  .chart-body {
    position: absolute;
    margin-top: 45px;
    height: 85%;
    width: 95%;
  }

  .number-chart {
    font-size: 50px;
    color: #c490e6;
    font-weight: bold;
    margin-bottom: 30px;
    padding-top: 20%;
    width: 80%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
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
