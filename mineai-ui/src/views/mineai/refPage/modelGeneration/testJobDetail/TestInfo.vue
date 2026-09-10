<template>
  <div style="height: 100%">
    <a-row :gutter="[8, 16]" style="height: 100%">
      <a-col :span="12" style="height: 87vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card
            :title="() => (changeTabTitle ? '作业详情' : '')"
            style="height: 80vh"
            :loading="loading"
          >
            <Description :column="1" :data="data" :schema="schema" layout="horizontal" />
          </a-card>
          <a-card title="质检进度" style="height: 11vh">
            <i class="flex justify-center">{{ progress }}%</i>
            <a-progress :percent="progress" :show-info="false" />
          </a-card>

          <!--            <a-card title="超参" style="height: 40vh" class="myCard">-->
          <!--              <a-table-->
          <!--                class="myTable"-->
          <!--                :columns="columns"-->
          <!--                size="small"-->
          <!--                :pagination="false"-->
          <!--                :data-source="paramData"-->
          <!--                :canResize="false"-->
          <!--                :loading="loading"-->
          <!--                :rowClassName="(record, index) => (index % 2 === 1 ? 'table-striped' : null)"-->
          <!--                bordered-->
          <!--              />-->
          <!--            </a-card>-->
        </div>
      </a-col>
      <a-col :span="12" style="height: 87vh">
        <div
          style="position: absolute; right: 50%; margin-top: 5px; margin-bottom: 5px"
          v-if="showBar"
        >
          <Button shape="circle" type="primary" @click="toggle">
            <template #icon>
              <SwapOutlined />
            </template>
          </Button>
        </div>
        <div
          style="
            position: absolute;
            right: 5px;
            margin-top: 32px;
            display: flex;
            flex-direction: row;
            width: 75%;
          "
          v-if="!trans && showBar"
        >
          <div style="margin-left: 10px; width: 40%">
            <a-tag v-if="selected" color="cyan">{{ betterJob }}</a-tag>
          </div>
          <Select
            :dropdownMatchSelectWidth="false"
            placeholder="选择要对比的权重文件:"
            style="width: 40%"
            :options="jobList"
            :onSelect="handleSelect"
            v-model:value="selectedJobs"
            @deselect="handleDeselect"
            @change="change"
            mode="multiple"
            :bordered="true"
            v-if="false"
          />
        </div>
        <div class="bar-wrapper h-full pt-8" v-if="!trans && showBar">
          <i class="h-1/15" v-if="!accBarShow && progress === 100 && newJobAcc > originJobAcc"
            >当前模型准确率为{{ newJobAcc }}%,原始模型准确率为{{ originJobAcc }}%,更推荐当前模型</i
          >
          <i class="h-1/15" v-if="!accBarShow && progress === 100 && newJobAcc <= originJobAcc"
            >当前模型准确率为{{ newJobAcc }}%,原始模型准确率为{{ originJobAcc }}%,更推荐原始模型</i
          >
          <i class="h-1/15" v-if="accBarShow && progress === 100"
            >当前模型准确率为{{ newJobAcc }}%</i
          >
          <new-model-pie class="h-7/15" :new-job-acc="newJobAcc" />
          <origin-model-pie class="h-7/15" :origin-job-acc="originJobAcc" v-if="!accBarShow" />
          <acc-bar class="h-7/15" v-if="accBarShow" />
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
  import { onMounted, onUnmounted, Ref, ref, defineProps, watchEffect } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Spin as ASpin,
    Tag as ATag,
    Button,
    Select,
    Progress as AProgress,
  } from 'ant-design-vue';
  import { SwapOutlined } from '@ant-design/icons-vue';
  import { Description } from '/@/components/Description';
  import {
    jobName,
    schema,
    jobList,
    newLossSeries,
    newAccSeries,
    currentJobMaxAcc,
    MaxAccList,
  } from './detailsData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useTabs } from '/@/hooks/web/useTabs';
  import NewModelPie from '/@/views/mineai/model/modelGeneration/testJobDetail/pie/newModelPie.vue';
  import OriginModelPie from '/@/views/mineai/model/modelGeneration/testJobDetail/pie/originModelPie.vue';
  import AccBar from '/@/views/mineai/model/modelGeneration/testJobDetail/AccBar.vue';

  const props = defineProps({
    jobId: Number,
    anotherJobId: Number,
    showBar: Boolean,
    changeTabTitle: Boolean,
  });

  let defaultPercent = ref();

  const { setTitle } = useTabs();
  let accBarShow = ref(false);
  let accPieShow = ref(true);
  let progress = ref(0);
  let newJobAcc = ref();
  let originJobAcc = ref();
  const trans = ref(true);
  const selected = ref(false);
  // 作业信息
  let data: Ref<any> = ref({});
  // 作业参数
  let paramData: Ref<any[]> = ref([]);

  // 控制台输出
  let consoleLogs: Ref<string[]> = ref([]);
  let loading: Ref<boolean> = ref(true);
  let terminalLoading: Ref<boolean> = ref(true);
  let timer: NodeJS.Timer;
  let timer1: NodeJS.Timer;
  let selectedJobs = [];
  let betterJob = ref('');

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
    Object.keys(jobParams as any).forEach((key) => {
      paramData.value.push({ paramName: key, paramValue: jobParams[key] });
    });
    jobLogs.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });
    if (props.anotherJobId === -1) {
      accBarShow.value = true;
      accPieShow.value = false;
    }
    loading.value = false;
    terminalLoading.value = false;

    const terminal = document.querySelector('#terminal');
    terminal.scrollTop = terminal.scrollHeight;
    timer = setInterval(refreshContent, 5000);
    getProgress();
    timer1 = setInterval(getProgress, 5000);
  });

  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(timer1);
  });

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
      .then((job) => {
        jobName.value = job.name;
        return maHttp.get(
          {
            url: 'log/getJobLog',
            params: {
              jobName: job.name,
              direction: 'BACKWARD',
              limit: 1000,
              start: 1,
              namespace: 'ai-platform-s2',
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      });
  }

  function toggle() {
    trans.value = !trans.value;
    selected.value = false;
  }

  //获取生产任务名称
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
      .then((v) => {
        if (props.changeTabTitle) setTitle(v + '质检作业详情');
      });
  }

  function getProgress() {
    maHttp
      .get(
        {
          url: 'modelJob/getTestJobProgressAndAcc',
          params: { testJobId: props.jobId, anotherTestJobId: props.anotherJobId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        defaultPercent.value = v;
        progress.value = Math.round(defaultPercent.value.progress);
        newJobAcc.value = defaultPercent.value.newNowAcc;
        originJobAcc.value = defaultPercent.value.newOriginAcc;

        // 当进度达到100%时，获取新的数据
        if (progress.value === 100) {
          getData(props.jobId).then((jobData) => {
            data.value = jobData;
          });
        }
      });
  }

  //刷新控制台内容
  async function refreshContent() {
    terminalLoading.value = false;
    const jobLogs = await getLog(props.jobId);
    consoleLogs.value = [];
    jobLogs.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });
    const terminal = document.querySelector('#terminal');
    terminal.scrollTop = terminal.scrollHeight;
    terminalLoading.value = false;
  }

  function change() {}

  function handleDeselect(e: string) {
    newAccSeries.value = newAccSeries.value.filter((item) => item.name !== e);
    newLossSeries.value = newLossSeries.value.filter((item) => item.name !== e);
    MaxAccList.value = MaxAccList.value.filter((item) => item.jobName !== e);
    selected.value = false;
    compareAcc();
  }

  function handleSelect(e: string) {
    selected.value = false;
    maHttp
      .get(
        {
          url: 'log/getEpochDetail',
          params: {
            jobName: e,
            limit: 500,
            start: 1,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        let data = ref({});
        data.value = v;
        let anotherMaxAccNumber = 0;
        if (v != null) {
          data.value['test accuracy'].forEach((value) => {
            if (Number(value) > anotherMaxAccNumber) {
              anotherMaxAccNumber = Number(value);
            }
          });
          MaxAccList.value.push({ jobName: e, data: anotherMaxAccNumber });
        }
        compareAcc();
        if (v === null) {
          alert('所选算法数据不存在');
        } else {
          newLossSeries.value.push({
            name: e,
            data: v['train loss'],
            smooth: true,
            symbolSize: 6,
            type: 'line',
          });
          newAccSeries.value.push({
            name: e,
            data: v['test accuracy'],
            smooth: true,
            symbolSize: 6,
            type: 'line',
          });
        }
      });
  }

  function compareAcc() {
    if (MaxAccList.value.length !== 0) {
      let selectedJobMaxAccNumber = 0;
      let selectedJobMaxAccName = ref();
      MaxAccList.value.forEach((item) => {
        if (Number(item.data) >= selectedJobMaxAccNumber) {
          selectedJobMaxAccNumber = Number(item.data);
          selectedJobMaxAccName.value = item.jobName;
        }
      });
      if (Number(currentJobMaxAcc.value) >= selectedJobMaxAccNumber) {
        betterJob.value = '更推荐当前作业';
      } else {
        betterJob.value = '更推荐' + selectedJobMaxAccName.value;
      }
      selected.value = true;
    } else {
      selected.value = false;
    }
  }
</script>

<style scoped>
  span {
    font-size: 18px;
  }

  .myCard >>> .ant-card-body {
    height: 60%;
    overflow: auto;
    margin: 5px;
  }

  .myPage {
    margin: 0 16px 0 16px;
  }

  .myPage >>> .ant-card-head {
    min-height: 35px;
    display: flex;
  }

  .myPage >>> .ant-card-head-title {
    padding: 0 0;
  }

  .myPage >>> .vben-page-wrapper-content {
    margin: 16px 0 16px 0;
  }

  .myTable >>> .ant-table-thead > tr > th {
    background-color: #212639;
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
</style>
