<template>
  <PageWrapper title="作业管理" @back="goBack" fixedHeight contentFullHeight class="myPage">
    <div style="height: 100%">
      <a-row :gutter="[8, 16]" style="height: 100%">
        <a-col :span="12" style="height: 87vh">
          <div style="height: 95%" class="flex flex-col">
            <a-card title="作业详情" style="height: 60vh" :loading="loading">
              <div>
                <a-button type="primary" @click="createDeploy">部署</a-button>
                <a-button type="primary" @click="createInspectJob" v-show="jobType == 1"
                  >质检
                </a-button>
              </div>
              <Description :column="1" :data="data" :schema="schema" layout="horizontal" />
            </a-card>
            <a-card title="超参" style="height: 40vh" class="myCard">
              <a-table
                class="myTable"
                :columns="columns"
                size="small"
                :pagination="false"
                :data-source="paramData"
                :canResize="false"
                :loading="loading"
                :rowClassName="(record, index) => (index % 2 === 1 ? 'table-striped' : null)"
                bordered
              />
            </a-card>
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
          <div
            style="
              position: absolute;
              right: 5px;
              margin-top: 32px;
              display: flex;
              flex-direction: row;
              width: 75%;
            "
            v-if="!trans"
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
            />
          </div>
          <div class="bar-wrapper h-full pt-8" v-if="!trans">
            <LossBar class="h-7/15" />
            <AccBar class="h-7/15" />
          </div>
          <div class="terminal-wrapper" v-if="trans">
            <div class="terminal-content">
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
    <CreateInspectJob @register="registerModal" />
    <CreateDeploy @register="registerModal1" />
  </PageWrapper>
</template>

<script lang="ts">
  import { defineComponent, h, onMounted, onUnmounted, ref, Ref } from 'vue';
  import {
    Button,
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Select,
    Spin as ASpin,
    Table as ATable,
    Tag as ATag,
  } from 'ant-design-vue';
  import { SwapOutlined } from '@ant-design/icons-vue';
  import { Description } from '/@/components/Description';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import {
    columns,
    currentJobMaxAcc,
    jobList,
    jobName,
    MaxAccList,
    newAccSeries,
    newLossSeries,
    schema,
  } from './detailsData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import LossBar from './LossBar.vue';
  import AccBar from './AccBar.vue';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useModal } from '/@/components/Modal';
  import CreateInspectJob from '/@/views/mineai/model/jobDetails/createJob/CreateInspectJob.vue';
  import CreateDeploy from '/@/views/mineai/model/jobDetails/createDeploy/CreateDeploy.vue';
  import AButton from '/@/components/Button/src/BasicButton.vue';

  export default defineComponent({
    components: {
      AButton,
      CreateDeploy,
      CreateInspectJob,
      Description,
      PageWrapper,
      ATag,
      ARow,
      ACol,
      ACard,
      AccBar,
      LossBar,
      Button,
      Select,
      ASpin,
      ATable,
      SwapOutlined,
    },
    setup() {
      // const route = useRoute();
      // const jobId = ref(route.params?.jobId);
      const go = useGo();
      const jobInfo = location.href.substring(
        location.href.lastIndexOf('/') + 1,
        location.href.length,
      );
      console.log(location.href);
      const jobId = jobInfo.substring(0, jobInfo.lastIndexOf('_'));
      const jobType = jobInfo.substring(jobInfo.lastIndexOf('_') + 1, jobInfo.length);

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
      let selectedJobs = [];
      let betterJob = ref('');
      const { closeCurrent } = useTabs();

      const [registerModal, { openModal }] = useModal();
      const [registerModal1, { openModal: openModal1 }] = useModal();

      function createInspectJob() {
        openModal(true, data.value);
      }

      function createDeploy() {
        openModal1(true, data.value);
      }

      onMounted(async () => {
        newAccSeries.value = [];
        newLossSeries.value = [];
        MaxAccList.value = [];
        const [jobData, jobParams, jobLogs] = await Promise.all([
          getData(jobId),
          getParams(jobId),
          getLog(jobId),
        ]);

        data.value = jobData;
        Object.keys(jobParams as any).forEach((key) => {
          paramData.value.push({ paramName: key, paramValue: jobParams[key] });
        });
        jobLogs.forEach((value) => {
          consoleLogs.value.push(value[Object.keys(value)[0]]);
        });

        loading.value = false;
        terminalLoading.value = false;
        timer = setInterval(refreshContent, 30000);
      });

      onUnmounted(() => {
        clearInterval(timer);
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
            maHttp
              .get(
                {
                  url: 'modelJob/getJobCompare',
                  params: {
                    // modelVersionId: job.modelVersion.id,
                    modelId: job.modelVersion.model.id,
                    jobId: job.id,
                    jobType: job.jobType,
                  },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then((v) => {
                jobList.value = [];
                v.forEach((item) => {
                  jobList.value.push({ value: item, label: item });
                });
              });
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
                  limit: 50,
                  //start: 1,
                  namespace: 'ai-platform-s2',
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTAINER_MANAGER },
            );
          });
      }

      function toggle() {
        trans.value = !trans.value;
        selected.value = false;
      }

      //刷新控制台内容
      async function refreshContent() {
        terminalLoading.value = true;
        const jobLogs = await getLog(jobId);
        consoleLogs.value = [];
        jobLogs.forEach((value) => {
          consoleLogs.value.push(value[Object.keys(value)[0]]);
        });
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
                namespace: 'ai-platform-s2',
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTAINER_MANAGER },
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

      const goBack = () => {
        go('/maTrainingCenter/jobList');
        closeCurrent();
      };
      return {
        goBack,
        closeCurrent,
        ACard,
        ACol,
        ARow,
        ASpin,
        ATable,
        Button,
        Select,
        SwapOutlined,
        refreshContent,
        Description,
        PageWrapper,
        columns,
        schema,
        MaxAccList,
        LossBar,
        AccBar,
        selectedJobs,
        handleSelect,
        handleDeselect,
        toggle,
        ATag,
        h,
        loading,
        data,
        paramData,
        terminalLoading,
        currentJobMaxAcc,
        trans,
        change,
        jobName,
        jobList,
        selected,
        betterJob,
        consoleLogs,
        registerModal,
        registerModal1,
        createDeploy,
        createInspectJob,
        jobType,
      };
    },
  });
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
