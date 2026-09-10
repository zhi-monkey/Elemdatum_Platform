<template>
  <div style="height: 100%">
    <a-row :gutter="[8, 16]" style="height: 100%">
      <!-- 左边部分：紧凑 -->
      <a-col :span="8" style="height: 87vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card
            :title="() => (changeTabTitle ? '作业详情' : '')"
            style="height: 40vh"
            :loading="loading"
            class="myCard"
          >
            <Description :column="1" :data="data" :schema="schema" layout="horizontal" />
          </a-card>

          <div style="height: 45vh" class="flex flex-row justify-center w-full">
            <a-card title="超参" class="myCard w-full h-full">
              <!-- 调整超参的大小 -->
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
          </div>
        </div>
      </a-col>

      <!-- 右边部分：扩大日志区域 -->
      <a-col :span="16" style="height: 87vh">
        <div
          style="position: absolute; right: 50%; margin-top: 5px; margin-bottom: 5px"
          v-if="showBar"
        >
        </div>
        <div class="bar-wrapper h-full pt-8" v-if="!trans && showBar">
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
  import { defineProps, onMounted, onUnmounted, ref, Ref } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    notification,
    Row as ARow,
    Spin as ASpin,
    Table as ATable,
  } from 'ant-design-vue';
  import { Description } from '/@/components/Description';
  import { columns, jobName, MaxAccList, newAccSeries, newLossSeries, schema } from './detailsData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  import { useTabs } from '/@/hooks/web/useTabs';
  import { useECharts } from '/@/hooks/web/useECharts';

  const props = defineProps<{ jobId: number; showBar: boolean; changeTabTitle: boolean }>();

  const trans = ref(true);
  const selected = ref(false);
  // 作业信息
  const data: Ref<any> = ref({});
  // 作业参数
  const paramData: Ref<any[]> = ref([]);
  // 控制台输出
  const consoleLogs: Ref<string[]> = ref([]);
  const loading: Ref<boolean> = ref(true);
  const terminalLoading: Ref<boolean> = ref(true);
  let timer1: NodeJS.Timer;
  let timer2: NodeJS.Timer;
  const { setTitle } = useTabs();

  const cardLoading = ref(true);
  const trainAcc = ref(0);
  const recall = ref(0);
  const trainAccPie = ref<HTMLDivElement | null>(null);
  const recallPie = ref<HTMLDivElement | null>(null);
  const { setOptions: setTAP } = useECharts(trainAccPie as Ref<HTMLDivElement>);
  const { setOptions: setRP } = useECharts(recallPie as Ref<HTMLDivElement>);

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
      if (key !== 'MODE_WORKING_MODE') {
        paramData.value.push({ key: index, paramName: key, paramValue: jobParams[key] });
      }
    });
    if (jobLogs != null) {
      jobLogs.forEach((value) => {
        consoleLogs.value.push(value[Object.keys(value)[0]].replace(/[\b]/g, '█'));
      });
    }

    loading.value = false;
    terminalLoading.value = false;

    timer1 = setInterval(refreshContent, 5000);
  });

  onUnmounted(() => {
    clearInterval(timer1);
    clearInterval(timer2);
  });

  // 获取生产任务信息
  async function getGeneration(jobId) {
    await maHttp
      .get(
        {
          url: 'modelGeneration/findGeneraByJob',
          params: { modelJobId: jobId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        if (props.changeTabTitle) setTitle('转换作业详情');
      });
  }

  // 获取作业信息
  async function getData(jobId) {
    return await maHttp
      .get(
        {
          url: 'modelJob/getJobByJobId',
          params: { id: jobId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((job) => {
        return job;
      });
  }

  // 获取作业参数
  async function getParams(jobId) {
    return await maHttp.get(
      {
        url: 'modelJob/getParamByJobId',
        params: { id: jobId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  }

  let notificationKey = 'queueNotification'; // 定义唯一的通知 key

  // 获取控制台输出
  async function getLog(jobId) {
    return await maHttp
      .get(
        {
          url: 'modelJob/getJobByJobId',
          params: { id: jobId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(async (job) => {
        jobName.value = job.name;
        const logContent = await maHttp.get(
          {
            url: 'log/getJobLog',
            params: {
              jobName: job.name,
              direction: 'BACKWARD',
              namespace: 'ai-platform',
              limit: 99999,
              // start: 1,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );

        // 检查 logContent 是否为空
        if (!logContent || logContent.length === 0) {
          // 获取当前队列中的任务数量
          let jobCount = await maHttp.get(
            {
              url: 'modelJob/getQueuePositionByModelJobName',
              params: {
                modelJobName: job.name,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );

          // 打开或更新通知
          if (jobCount != -1) {
            notification.open({
              key: notificationKey,
              message: '集群外转换正在排队',
              description: `你的位置是第 ${jobCount} 个`,
              duration: 0, // 持续显示
            });
          }
        } else {
          // 如果成功获取到日志，关闭通知
          notification.close(notificationKey); // 关闭通知
        }

        return logContent;
      });
  }

  function toggle() {
    trans.value = !trans.value;
    selected.value = false;
  }

  // 刷新控制台内容
  async function refreshContent() {
    // 如果已经加载完成或任务已结束，不再执行
    if (
      terminalLoading.value ||
      data.value.status === 6 ||
      data.value.status === -5 ||
      data.value.status === -10
    ) {
      clearInterval(timer1);
      clearInterval(timer2);
      return;
    }
    terminalLoading.value = false;
    data.value = await getData(props.jobId);
    const jobLogs = await getLog(props.jobId);
    consoleLogs.value = [];
    jobLogs.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]].replace(/[\b]/g, '█'));
    });
    if (!(data.value.status === 6 || data.value.status === -5)) {
      const terminal = document.querySelector('#terminal');
      terminal.scrollTop = terminal?.scrollHeight;
    }
    terminalLoading.value = false;
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
