<template>
  <PageWrapper title="算法部署状态" @back="goBack">
    <div>
      <div>
        <a-row :gutter="[16, 16]">
          <a-col :span="12" style="height: 87vh">
            <div style="height: 95%" class="flex flex-col">
              <a-card class="des" style="height: 100vh" :loading="loading">
                <Description
                  title="算法版本详情信息"
                  :column="1"
                  :data="data"
                  :schema="schema"
                  size:big
                  layout="horizontal"
                />
              </a-card>
            </div>
          </a-col>

          <a-col :span="12" style="height: 87vh">
            <div class="terminal-wrapper">
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
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, onMounted, Ref, onUnmounted } from 'vue';
  import { Col as ACol, Row as ARow, Card as ACard } from 'ant-design-vue';
  import {
    schema,
    deployList,
  } from '/@/views/mineai/monitor/model-list/modelDeployment/table.data';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { Description } from '/@/components/Description';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import { useRoute } from 'vue-router';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let consoleLogs: Ref<string[]> = ref([]);
  const go = useGo();
  let loading: Ref<boolean> = ref(true);
  let terminalLoading: Ref<boolean> = ref(true);
  let timer: NodeJS.Timer;
  //作业信息
  let data: Ref<any> = ref({});
  const route = useRoute();
  const modelVersionId = route.query.modelVersionId;
  const { closeCurrent } = useTabs();

  onMounted(async () => {
    const [deployData, deployLog] = await Promise.all([
      getData(modelVersionId),
      getDeployLog(modelVersionId),
    ]);
    data.value = deployData;
    deployLog.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });

    loading.value = false;
    terminalLoading.value = false;
    timer = setInterval(refreshContent, 30000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });

  //获取部署信息
  async function getData(modelVersionId) {
    return await maHttp
      .get(
        {
          url: 'modelDeployment/findModelDeploymentByModelVersionId',
          params: { modelVersionId: modelVersionId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        maHttp
          .get(
            {
              url: 'modelDeployment/findModelDeploymentById',
              params: { id: v[0].id },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            deployList.value = [];
            v.forEach((item) => {
              deployList.value.push({ value: item, label: item });
            });
          });
        return v[0];
      });
  }

  //获取日志信息
  async function getDeployLog(modelVersionId) {
    const deployment = await maHttp.get(
      {
        url: 'modelDeployment/findModelDeploymentByModelVersionId',
        params: { modelVersionId: modelVersionId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return await maHttp.get(
      {
        url: 'log/getDeployLog',
        params: {
          namespace: 'ai-platform-s2',
          deployName: deployment[0].deploymentName,
          direction: 'BACKWARD',
          limit: 50,
          start: 1,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  }

  async function refreshContent() {
    terminalLoading.value = true;
    const deployLog = await getDeployLog(modelVersionId);
    consoleLogs.value = [];
    deployLog.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });
    terminalLoading.value = false;
  }

  const goBack = () => {
    go('/maTrainingCenter/modelList');
    closeCurrent();
  };
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
