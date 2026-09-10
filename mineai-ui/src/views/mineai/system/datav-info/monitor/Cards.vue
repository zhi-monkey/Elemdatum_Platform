<template>
  <BorderBox3 style="height: 300px">
    <div style="display: flex; flex-direction: row; justify-content: space-around; height: 100%">
      <div id="cards">
        <div class="card-item">
          <div class="card-header">
            <div class="card-header-left">报警分布</div>
          </div>
          <div style="padding-left: 20px"><decoration3 style="width: 110px; height: 20px" /></div>
          <div ref="numberOfAlarmMessages" class="ring-charts"></div>
        </div>
      </div>
      <div class="capsule-chart">
        <span style="font-size: 18px; font-weight: bold">监控设备报警数</span>
        <decoration3 style="width: 125px; height: 20px" />
        <div ref="storageBar" class="w-full h-4/5" style="margin-top: -25px"></div>
      </div>

      <div id="cards">
        <div class="card-item">
          <div class="info">不同数据类型监控设备台数</div>
          <div style="padding-left: 20px"><decoration3 style="width: 222px; height: 20px" /></div>
          <div ref="diffDataTypesMoniterNumbers" class="ring-charts"></div>
        </div>
      </div>
    </div>
  </BorderBox3>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  import { BorderBox3, Decoration3 } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useECharts } from '/@/hooks/web/useECharts';

  const storageBar = ref<HTMLDivElement | null>(null);
  const diffDataTypesMoniterNumbers = ref<HTMLDivElement | null>(null);
  const numberOfAlarmMessages = ref<HTMLDivElement | null>(null);

  async function getData1() {
    maHttp
      .get(
        {
          url: 'modelAlert/getModelAlertDataType',
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((madt) => {
        const { setOptions } = useECharts(numberOfAlarmMessages as Ref<HTMLDivElement>);
        setOptions({
          tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
          color: ['#00baff', '#3de7c9', '#ffc530'],
          series: [
            {
              type: 'pie',
              radius: '65%',
              data: [
                { name: '视频报警', value: madt.video },
                { name: '音频报警', value: madt.audio },
                { name: '图片报警', value: madt.image },
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
                position: 'outer',
                alignTo: 'edge',
                margin: '4%',
                formatter: '{b}\n({d}%)',
                color: 'inherit',
                overflow: 'break',
              },
            },
          ],
        });
      });
  }

  async function getData2() {
    maHttp
      .get(
        {
          url: 'monitor/getDataType',
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then((mdt) => {
        // 总数目
        const { setOptions } = useECharts(diffDataTypesMoniterNumbers as Ref<HTMLDivElement>);
        setOptions({
          tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
          color: ['#00baff', '#3de7c9', '#fff', '#ffc530'],
          series: [
            {
              type: 'pie',
              radius: '65%',
              data: [
                { name: '视频', value: mdt.video },
                { name: '图片', value: mdt.image },
                { name: '音频', value: mdt.audio },
                { name: '点云', value: mdt.pointCloud },
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
                position: 'outer',
                alignTo: 'edge',
                margin: '20%',
                formatter: '{b}\n({d}%)',
                color: 'inherit',
                overflow: 'break',
              },
            },
          ],
        });
      });
  }

  async function getData3() {
    maHttp
      .get(
        {
          url: 'modelAlert/getMostModelAlertMonitorListByDateNum',
          params: { dateNum: 7 },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((ml) => {
        // 转换成charts对应格式
        let temp = ml['nameList'].map(function (e, i) {
          return { name: e, value: ml['numList'][i] };
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
        //监控设备报警数
        const { setOptions } = useECharts(storageBar as Ref<HTMLDivElement>);
        setOptions({
          tooltip: {},
          grid: {
            left: 95,
            bottom: 20,
          },
          xAxis: {
            name: '条',
            type: 'value',
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
              label: { show: true, offset: [5, 0], color: '#ffffff' },
              itemStyle: {
                color: function (params) {
                  let colorList = [
                    '#e062ae',
                    '#fb7293',
                    '#df9fce',
                    '#2aa5c3',
                    '#5dc8ca',
                    '#91d1a8',
                    '#d8ba4f',
                  ];
                  return colorList[params.dataIndex];
                },
              },
            },
          ],
        });
      });
  }

  let timer: NodeJS.Timer;
  onMounted(async () => {
    await Promise.all([getData1(), getData2(), getData3()]);
    timer = setInterval(async () => {
      await Promise.all([getData1(), getData2(), getData3()]);
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style lang="less" scoped>
  #cards {
    display: flex;
    width: 17%;
    height: 83%;
    margin-left: 25px;
    margin-top: 26px;

    .card-item {
      width: 100%;
      display: flex;
      flex-direction: column;
    }
    .card-right {
      width: ;
      height: ;
    }
    .info {
      font-size: 18px;
      font-weight: bold;
      padding-left: 20px;
    }

    .card-header {
      display: flex;
      //height: 20%;
      justify-content: flex-start;
      flex-direction: row;

      .card-header-left {
        font-size: 18px;
        font-weight: bold;
        padding-left: 20px;
      }

      .card-header-right {
        padding-right: 20px;
        font-size: 40px;
        color: #03d3ec;
      }
    }

    .ring-charts {
      height: 75%;
    }

    .card-footer {
      height: 25%;
      display: flex;
      justify-content: space-around;
    }

    .card-footer-item {
      padding: 5px 10px 0 10px;
      box-sizing: border-box;
      width: 55%;
      background-color: rgba(6, 30, 93, 0.7);
      border-radius: 3px;

      .footer-title {
        font-size: 15px;
        margin-bottom: 5px;
      }

      .footer-detail {
        color: #1294fb;
        display: flex;
        font-size: 18px;
        justify-content: center;
        align-items: center;

        .dv-digital-flop {
          margin-right: 3px;
        }
      }
    }
  }

  .capsule-chart {
    height: 85%;
    width: 50%;
    margin-top: 28px;
    margin-left: 25px;
  }
</style>
