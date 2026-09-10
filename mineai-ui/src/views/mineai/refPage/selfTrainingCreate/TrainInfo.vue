<template>
  <div style="height: 100%">
    <a-row :gutter="[8, 16]" style="height: 100%">
      <a-col :span="12" style="height: 87vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card
            :title="() => (changeTabTitle ? '作业详情' : '')"
            style="height: 30vh"
            :loading="loading"
            class="myCard"
          >
            <Description :column="1" :data="data" :schema="schema" layout="horizontal" />
          </a-card>
          <!--          <a-card title="训练进度" style="height: 15vh; padding-bottom: 4px" class="myCard">-->
          <!--            <i class="flex justify-center">{{ 100 }}%</i>-->
          <!--            <a-progress :percent="100" />-->
          <!--          </a-card>-->
          <div style="height: 50vh" class="flex flex-row justify-center w-full">
            <a-card title="超参" class="myCard w-1/3 h-full">
              <a-table
                class="myTable"
                :columns="columns"
                size="small"
                :pagination="false"
                :data-source="paramData"
                :canResize="false"
                :loading="loading"
                :rowClassName="(_, index) => (index % 2 === 1 ? 'table-striped' : null)"
                bordered
              />
            </a-card>
            <a-card title="训练结果" :loading="cardLoading" class="myCard w-2/3 h-full">
              <div class="w-full h-full flex flex-row justify-around">
                <div ref="trainAccPie" class="w-1/2 h-full"></div>
                <div ref="recallPie" class="w-1/2 h-full"></div>
              </div>
            </a-card>
          </div>
        </div>
      </a-col>
      <a-col :span="12" style="height: 87vh">
        <div style="position: absolute; right: 50%; margin-top: 5px; margin-bottom: 5px">
          <Button shape="circle" type="primary" @click="toggle">
            <template #icon>
              <SwapOutlined />
            </template>
          </Button>
        </div>
        <div class="bar-wrapper h-full pt-8" v-if="!trans">
          <LossBar class="h-7/15" />
          <AccBar class="h-7/15" />
        </div>
        <div class="terminal-wrapper" v-if="trans">
          <div class="terminal-content" id="terminal">
            <a-spin :spinning="terminalLoading">
              <p v-for="(log, index) in consoleLogs" :key="index">{{ log }}</p>
            </a-spin>
          </div>
          <div class="toolkit">
            <span class="icon icon-clickable icon-changeable" @click="refreshContent"
              ><i class="iconfont icon-shuaxin"></i>
            </span>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, Ref, ref, defineProps } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Spin as ASpin,
    Table as ATable,
    Button,
  } from 'ant-design-vue';
  import { SwapOutlined } from '@ant-design/icons-vue';
  import { Description } from '/@/components/Description';
  import { columns, schema, newLossSeries, newAccSeries, MaxAccList } from './detailsData';
  import LossBar from './LossBar.vue';
  import AccBar from './AccBar.vue';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useECharts } from '/@/hooks/web/useECharts';
  import {
    fetchGenerationDataMock,
    fetchJobDataMock,
    fetchJobLogMock,
    fetchJobParamsMock,
    getTrainJobProgress,
  } from '/@/views/mineai/refPage/generationGuide/api/modelApi';

  const props = defineProps<{ jobId: number; showBar: boolean; changeTabTitle: boolean }>();

  const trans = ref(true);
  const selected = ref(false);
  // 作业信息
  const data: Ref<any> = ref({});
  // 作业参数
  const paramData: Ref<any[]> = ref([]);
  // 控制台输出
  const consoleLogs: Ref<string[]> = ref([]);
  const loading: Ref<boolean> = ref(false);
  const terminalLoading: Ref<boolean> = ref(true);
  let timer1: NodeJS.Timer;
  let timer2: NodeJS.Timer;
  const { setTitle } = useTabs();

  const cardLoading = ref(false);
  const trainAcc = ref(0);
  const recall = ref(0);
  const trainAccPie = ref<HTMLDivElement | null>(null);
  const recallPie = ref<HTMLDivElement | null>(null);
  const { setOptions: setTAP } = useECharts(trainAccPie as Ref<HTMLDivElement>);
  const { setOptions: setRP } = useECharts(recallPie as Ref<HTMLDivElement>);

  const defaultPercent = ref<number>(0);

  function getProgress() {
    getPieData(props.jobId);
    getTrainJobProgress(props.jobId)
      .then(async (v) => {
        defaultPercent.value = Math.round(v);

        if (defaultPercent.value === 100) {
          const jobData = await getData(props.jobId);
          data.value = jobData;
          await getPieData(props.jobId);
        }
      })
      .catch((error) => {
        console.error('获取进度失败:', error);
      });
  }

  onMounted(async () => {
    newAccSeries.value = [];
    newLossSeries.value = [];
    MaxAccList.value = [];
    const [jobData, jobParams, jobLogs] = await Promise.all([
      getData(props.jobId),
      getParams(props.jobId),
      getLog(props.jobId),
    ]);
    await getGeneration(props.jobId);
    data.value = jobData;
    Object.keys(jobParams as any).forEach((key, index) => {
      if (key !== 'MODEL_WORKING_MODE') {
        paramData.value.push({ key: index, paramName: key, paramValue: jobParams[key] });
      }
    });
    jobLogs.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });

    loading.value = false;
    terminalLoading.value = false;

    const terminal = document.querySelector('#terminal');
    terminal.scrollTop = terminal?.scrollHeight;

    timer1 = setInterval(refreshContent, 5000);
    getProgress();
    timer2 = setInterval(getProgress, 5000);
  });

  onUnmounted(() => {
    clearInterval(timer1);
    clearInterval(timer2);
  });

  // 获取生产任务信息
  async function getGeneration(jobId) {
    await fetchGenerationDataMock();
    if (props.changeTabTitle) setTitle('训练作业详情');
  }

  // 获取作业信息
  async function getData(jobId) {
    return await fetchJobDataMock();
  }

  // 获取作业参数
  async function getParams() {
    return await fetchJobParamsMock();
  }

  // 获取控制台输出
  async function getLog(jobId) {
    return await fetchJobLogMock();
  }

  function toggle() {
    trans.value = !trans.value;
    selected.value = false;
  }

  // 刷新控制台内容
  async function refreshContent() {
    terminalLoading.value = false;
    const jobLogs = await getLog(props.jobId);
    consoleLogs.value = [];
    jobLogs.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });
    const terminal = document.querySelector('#terminal');
    terminal.scrollTop = terminal?.scrollHeight;
    terminalLoading.value = false;
  }

  // 获取饼图数据
  async function getPieData(jobId) {
    // cardLoading.value = true; // 开始加载
    try {
      // const pieData = await fetchPieData(jobId);
      //
      // // 更新准确率和召回率
      // trainAcc.value = pieData.newNowAcc ? pieData.newNowAcc : 0;
      // recall.value = pieData.newNowRecall ? pieData.newNowRecall : 0;
      //
      // // 关闭加载状态
      // cardLoading.value = false;

      // 设置 TAP 图表
      setTAP({
        series: {
          type: 'gauge',
          startAngle: 90,
          endAngle: -270,
          pointer: {
            show: false,
          },
          progress: {
            show: true,
            overlap: false,
            roundCap: true,
            clip: false,
            itemStyle: {
              borderWidth: 1,
              borderColor: '#464646',
            },
          },
          axisLine: {
            lineStyle: {
              width: 10,
            },
          },
          splitLine: {
            show: false,
            distance: 0,
            length: 10,
          },
          axisTick: {
            show: false,
          },
          axisLabel: {
            show: false,
            distance: 50,
          },
          data: [
            {
              value: Number(88.23),
              name: '准确率',
              title: {
                offsetCenter: ['0%', '-15%'],
              },
              detail: {
                valueAnimation: true,
                offsetCenter: ['0%', '20%'],
              },
            },
          ],
          detail: {
            width: 40,
            height: 12,
            fontSize: 12,
            color: 'inherit',
            borderColor: 'inherit',
            borderRadius: 60,
            borderWidth: 1,
            formatter: '{value}%',
          },
        },
      });

      // 设置 RP 图表
      setRP({
        series: {
          type: 'gauge',
          startAngle: 90,
          endAngle: -270,
          pointer: {
            show: false,
          },
          progress: {
            show: true,
            overlap: false,
            roundCap: true,
            clip: false,
            itemStyle: {
              borderWidth: 1,
              borderColor: '#464646',
            },
          },
          axisLine: {
            lineStyle: {
              width: 10,
            },
          },
          splitLine: {
            show: false,
            distance: 0,
            length: 10,
          },
          axisTick: {
            show: false,
          },
          axisLabel: {
            show: false,
            distance: 50,
          },
          data: [
            {
              value: Number(13.11),
              name: '召回率',
              title: {
                offsetCenter: ['0%', '-15%'],
              },
              detail: {
                valueAnimation: true,
                offsetCenter: ['0%', '20%'],
              },
            },
          ],
          detail: {
            width: 40,
            height: 12,
            fontSize: 12,
            color: 'inherit',
            borderColor: 'inherit',
            borderRadius: 60,
            borderWidth: 1,
            formatter: '{value}%',
          },
        },
      });
    } catch (error) {
      console.error('获取饼图数据失败:', error);
      cardLoading.value = false; // 关闭加载状态
    }
  }
</script>

<style scoped>
  .myCard >>> .ant-card-body {
    height: 80%;
    overflow: auto;
    margin: 2px;
    padding: 15px !important;
  }

  .myTable >>> .ant-table-thead > tr > th {
    background-color: #212639;
  }

  .ant-btn-primary {
    color: #fff;
    border-color: #2e3a4dff;
    background-color: #2e3a4dff;
    box-shadow: 0 2px 0 rgba(0, 0, 0, 0.045);
  }

  .ant-btn-primary:hover,
  .ant-btn-primary:focus {
    color: #fff;
    background: #424f70ff;
    border-color: #424f70ff;
  }

  .terminal-wrapper {
    height: 95%;
    padding: 20px;
    background-color: #212639;
  }

  .bar-wrapper {
    height: 95%;
    padding-left: 20px;
    padding-right: 20px;
    background-color: #212639;
  }

  .terminal-content {
    height: 100%;
    background-color: #242e42;
    font-family: PT Mono, Monaco, Menlo, Consolas, Courier New, monospace;
    padding: 20px;
    border-radius: 4px;
    overflow: auto;
  }

  .terminal-content p {
    color: #b7c4d1;
    font-weight: 600;
    line-height: 20px;
    white-space: pre-wrap;
  }

  .toolkit {
    position: absolute;
    display: -ms-flexbox;
    display: flex;
    -ms-flex-align: center;
    align-items: center;
    top: 28px;
    right: 40px;
    z-index: 2;
    height: 40px;
    padding: 8px 8px;
    color: #fff;
    border-radius: 12px;
    background-color: #36435c;
  }

  @font-face {
    font-family: 'iconfont'; /* Project id 4098445 */
    src: url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.woff2?t=1685585739405') format('woff2'),
      url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.woff?t=1685585739405') format('woff'),
      url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.ttf?t=1685585739405') format('truetype');
  }

  .iconfont {
    font-family: 'iconfont' !important;
    font-size: 25px;
    font-style: normal;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  .icon-shuaxin:before {
    content: '\ec08';
  }

  .icon {
    display: -ms-inline-flexbox;
    display: inline-flex;
    -ms-flex-item-align: center;
    align-self: center;
    background-position: 50%;
    background-size: contain;
    background-repeat: no-repeat;
    vertical-align: middle;
  }

  .icon-clickable {
    cursor: pointer;
    border-radius: 4px;
    pointer-events: auto;
  }
</style>
