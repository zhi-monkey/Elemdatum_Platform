<template>
  <BorderBox3 style="height: 310px; padding-left: 10px; padding-top: -90px">
    <a-row :gutter="6">
      <a-col class="charts" :span="9">
        <div class="chart-name">
          数据集新增情况
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="addBar" class="chart-body"></div>
      </a-col>
      <a-col class="charts" :span="5">
        <div class="chart-name">
          数据集类型分布
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="quantityChart" class="chart-body"></div>
      </a-col>
      <a-col class="charts" :span="5">
        <div class="chart-name">
          数据集发布情况
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="publishChart" class="chart-body"></div>
      </a-col>
      <a-col class="charts" :span="5">
        <div class="chart-name">
          绑定设备状态
          <decoration3 style="width: 200px; height: 20px" />
        </div>
        <div ref="monitorBar" class="chart-body"></div>
      </a-col>
    </a-row>
  </BorderBox3>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, Ref } from 'vue';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
  import { BorderBox3, Decoration3 } from '@kjgl77/datav-vue3';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const addBar = ref<HTMLDivElement | null>(null);
  const quantityChart = ref<HTMLDivElement | null>(null);
  const publishChart = ref<HTMLDivElement | null>(null);
  const monitorBar = ref<HTMLDivElement | null>(null);

  async function getData1() {
    //数据集新增情况
    maHttp
      .get(
        {
          url: 'datasets/getPeriodDataByDate',
          params: { dateNum: 7 },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        const { setOptions } = useECharts(addBar as Ref<HTMLDivElement>);
        setOptions({
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow',
            },
          },
          grid: {
            left: '8%',
            right: '8%',
            bottom: '5%',
            containLabel: true,
          },
          xAxis: [
            {
              name: '日期',
              type: 'category',
              data: v['dateList'],
              axisTick: {
                alignWithLabel: true,
              },
            },
          ],
          yAxis: [
            {
              name: '近七天新增',
              type: 'value',
              minInterval: 1,
            },
          ],
          series: [
            {
              type: 'bar',
              data: v['numList'],
              label: { show: true, offset: [0, -7], color: '#ffffff' },
            },
          ],
        });
      });
  }

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
          color: ['#4d99fc', '#40faee'],
          series: [
            {
              type: 'pie',
              radius: '55%',
              center: ['50%', '50%'],
              data: [
                { value: v.detection, name: '目标检测' },
                { value: v.segmentation, name: '语义分割' },
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

  async function getData3() {
    //数据集发布情况
    maHttp
      .get(
        {
          url: 'datasets/versions/getDatasetVersionStat',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        const { setOptions } = useECharts(publishChart as Ref<HTMLDivElement>);
        setOptions({
          tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
          color: ['#8451fc', '#f958b1'],
          series: [
            {
              type: 'pie',
              radius: '55%',
              center: ['50%', '50%'],
              data: [
                { value: v.unpublishedDatasetNum, name: '未发布' },
                { value: v.publishedDatasetNum, name: '已发布' },
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

  async function getData4() {
    //获取数据集大小前num个数据集的信息
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
        const { setOptions } = useECharts(monitorBar as Ref<HTMLDivElement>);
        setOptions({
          tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
          color: ['#27f49f', '#f4c427'],
          series: [
            {
              type: 'pie',
              radius: '55%',
              center: ['50%', '50%'],
              data: [
                { value: v.on, name: '启用' },
                { value: v.off, name: '停用' },
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

  let timer: NodeJS.Timer;

  onMounted(async () => {
    await Promise.all([getData1(), getData2(), getData3(), getData4()]);
    timer = setInterval(async () => {
      await Promise.all([getData1(), getData2(), getData3(), getData4()]);
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style lang="less" scoped>
  .charts {
    top: 20px;
    display: flex;
    flex-direction: column;
  }

  .chart-name {
    position: absolute;
    left: 20px;
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
    top: 15px;
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
