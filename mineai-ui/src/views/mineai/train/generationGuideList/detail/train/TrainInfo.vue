<template>
  <div style="height: 100%">
    <div v-if="shouldShowContent">
      <a-row :gutter="[8, 16]" style="height: 100%">
        <a-col :span="12" style="height: 90vh">
          <div style="height: 100%; overflow: hidden" class="flex flex-col">
            <a-card
              :title="() => (changeTabTitle ? '作业详情' : '')"
              style="height: 34vh"
              :loading="loading"
              class="myCard"
            >
              <Description
                :column="1"
                :data="{ ...data, defaultPercent }"
                :schema="schema"
                layout="horizontal"
              />
            </a-card>
            <!-- 算法包标签卡片 -->
            <a-card v-if="hasLabels" title="训练选择的标签" style="height: 14vh" class="myCard">
              <div class="labels-section">
                <div class="labels-container" :class="{ expanded: showAllLabels }">
                  <ATag
                    v-for="(label, index) in visibleLabels"
                    :key="`label-${label.index}-${index}`"
                    :color="getLabelColor(index)"
                    class="label-tag"
                  >
                    {{ label.index }}.{{ label.name }}
                  </ATag>

                  <!-- 溢出指示器 -->
                  <!--                <ATag-->
                  <!--                  v-if="!showAllLabels && labelsArray.length > maxVisibleLabels"-->
                  <!--                  class="overflow-indicator"-->
                  <!--                  @click="showAllLabels = true"-->
                  <!--                >-->
                  <!--                  +{{ labelsArray.length - maxVisibleLabels }}-->
                  <!--                </ATag>-->
                  <Button
                    v-if="labelsArray.length > maxVisibleLabels"
                    type="link"
                    size="small"
                    @click="showAllLabels = !showAllLabels"
                    style="padding: 0; height: auto; margin-left: 8px"
                  >
                    {{ showAllLabels ? '收起' : `展开全部(${labelsArray.length})` }}
                  </Button>
                </div>
              </div>
            </a-card>

            <a-card title="训练进度" style="height: 15vh; padding-bottom: 4px" class="myCard">
              <i class="flex justify-center">{{ realPercent }}%</i>
              <a-progress :percent="realPercent" :show-info="false" />
            </a-card>

            <div style="height: 30vh" class="flex flex-row justify-center w-full">
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
              <a-card title="训练结果" class="myCard w-2/3 h-full">
                <!-- 当主 loading 为 true 且未完成时，显示加载中 -->
                <template v-if="loading && realPercent < 100">
                  <div class="w-full h-full flex items-center justify-center text-gray-500">
                    数据加载中，请稍候...
                  </div>
                </template>
                <!-- 当训练完成且图表数据准备好后，显示图表 -->
                <template v-else-if="realPercent === 100 && chartsReady">
                  <div class="w-full h-full flex flex-row justify-around">
                    <div ref="trainAccPie" class="w-1/2 h-full"></div>
                    <div ref="recallPie" class="w-1/2 h-full"></div>
                  </div>
                </template>
                <!-- 其他情况（例如训练未完成） -->
                <template v-else>
                  <div class="w-full h-full flex items-center justify-center text-gray-500">
                    训练未完成
                  </div>
                </template>
              </a-card>
            </div>
          </div>
        </a-col>
        <a-col :span="12" style="height: 94vh">
          <div
            style="position: absolute; right: 50%; margin-top: 5px; margin-bottom: 5px"
            v-if="showBar"
          >
            <Button shape="circle" type="primary" @click="openLogModal">
              <template #icon>
                <FileTextOutlined />
              </template>
            </Button>
          </div>
          <div class="bar-wrapper h-full pt-8" v-if="showBar">
            <LossBar class="h-7/15" :data="epochData" />
            <AccBar class="h-7/15" :data="epochData" />
          </div>
        </a-col>
      </a-row>
    </div>
    <div v-else>
      <a-alert
        message="训练部分暂未开始，请先完成数据标注部分"
        description="The training part has not started yet. Please complete the data labeling part first."
        type="info"
        show-icon
        style="width: 100%"
      />
    </div>

    <!-- LogModal弹窗 -->
    <LogModal v-model:visible="logModalVisible" :jobId="props.jobId" />
  </div>
</template>

<script lang="ts" setup>
  import { computed, defineProps, nextTick, onMounted, onUnmounted, ref, Ref } from 'vue';
  import {
    Alert as AAlert,
    Button,
    Card as ACard,
    Col as ACol,
    notification,
    Progress as AProgress,
    Row as ARow,
    Spin as ASpin,
    Table as ATable,
    Tag as ATag,
  } from 'ant-design-vue';
  import { FileTextOutlined } from '@ant-design/icons-vue';
  import { Description } from '/@/components/Description';
  import { columns, newAccSeries, newLossSeries, schema } from './detailsData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import LossBar from './LossBar.vue';
  import AccBar from './AccBar.vue';
  import LogModal from './LogModal.vue';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useECharts } from '/@/hooks/web/useECharts';
  import AButton from '/@/components/Button/src/BasicButton.vue';

  const props = defineProps<{
    jobId: number;
    showBar: boolean;
    changeTabTitle: boolean;
    currentPipelinePhase: number;
    currentStepPhase: number;
  }>();

  const loading = ref(true);
  const terminalLoading = ref(true);
  const trainingComplete = ref(false);
  const chartsReady = ref(false);
  const logModalVisible = ref(false);

  const data: Ref<any> = ref({});
  const paramData: Ref<any[]> = ref([]);
  const consoleLogs: Ref<string[]> = ref([]);
  const epochData: Ref<any | null> = ref(null);

  const realPercent = ref(0);
  const trainAcc = ref(0);
  const recall = ref(0);

  let pollingTimer: NodeJS.Timer;
  const { setTitle } = useTabs();
  const notificationKey = `queueNotification-${props.jobId}`;

  const trainAccPie = ref<HTMLDivElement | null>(null);
  const recallPie = ref<HTMLDivElement | null>(null);
  const { setOptions: setTAP } = useECharts(trainAccPie as Ref<HTMLDivElement>);
  const { setOptions: setRP } = useECharts(recallPie as Ref<HTMLDivElement>);

  const defaultPercent = computed(() => realPercent.value);

  // 标签相关计算属性
  const hasLabels = computed(() => {
    return data.value?.selectedLabels && Object.keys(data.value.selectedLabels).length > 0;
  });

  const labelsArray = computed(() => {
    if (!hasLabels.value) return [];

    let labelsMap = data.value.selectedLabels;
    return Object.entries(labelsMap)
      .map(([index, name]) => ({
        index: Number(index),
        name: String(name),
      }))
      .sort((a, b) => a.index - b.index); // 按索引排序
  });

  const visibleLabels = computed(() => {
    if (showAllLabels.value) {
      return labelsArray.value;
    }
    return labelsArray.value.slice(0, maxVisibleLabels.value);
  });

  // 标签颜色配置
  const labelColors = [
    'blue',
    'green',
    'orange',
    'red',
    'purple',
    'cyan',
    'magenta',
    'lime',
    'pink',
    'volcano',
    'gold',
    'geekblue',
  ];
  const getLabelColor = (index: number) => {
    return labelColors[index % labelColors.length];
  };

  const TERMINAL_STATUSES = [0, 1, 11, 12];
  const showAllLabels = ref(false);
  const maxVisibleLabels = ref(10);
  function openLogModal() {
    logModalVisible.value = true;
  }

  async function pollDashboardData() {
    if (trainingComplete.value || !props.jobId) {
      clearInterval(pollingTimer);
      return;
    }

    try {
      const dashboardData = await maHttp.get(
        {
          url: 'modelJob/getTrainingDashboard',
          params: { jobId: props.jobId, maxLines: 2000 },
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (!dashboardData) return;

      realPercent.value = Math.round(dashboardData.progress || 0);
      epochData.value = dashboardData.epochDetail;

      if (dashboardData.rawLogs && dashboardData.rawLogs.length > 0) {
        consoleLogs.value = dashboardData.rawLogs.map((logEntry) => Object.values(logEntry)[0]);
        notification.close(notificationKey);
      } else {
        await checkQueueStatus();
      }

      // 检查任务是否已完成
      const jobData = await getData(props.jobId);
      data.value = jobData;
      
      // 如果任务状态不再是排队中（11），关闭排队通知
      if (jobData.status !== 11) {
        notification.close(notificationKey);
      }

      const isJobTerminated = !TERMINAL_STATUSES.includes(jobData.status);
      if (isJobTerminated || realPercent.value >= 100) {
        if (!trainingComplete.value) {
          trainingComplete.value = true;
          realPercent.value = 100;

          if (dashboardData.finalMetrics) {
            trainAcc.value = dashboardData.finalMetrics.newNowAcc || 0;
            recall.value = dashboardData.finalMetrics.newNowRecall || 0;
            await renderPieCharts();
            chartsReady.value = true;
          } else {
            await getPieData(props.jobId); // 使用旧逻辑作为回退
          }
        }
        clearInterval(pollingTimer);
      }
    } catch (error) {
      console.error('Failed to poll dashboard data:', error);
      clearInterval(pollingTimer);
    } finally {
      loading.value = false;
      terminalLoading.value = false;
    }
  }

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
  async function getParams(jobId) {
    if (jobId != 0 && jobId != null) {
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
  }

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
        if (props.changeTabTitle) setTitle('训练作业详情');
      });
  }

  onMounted(async () => {
    if (!shouldShowContent.value || !props.jobId) {
      loading.value = false;
      return;
    }

    newAccSeries.value = [];
    newLossSeries.value = [];

    loading.value = true;
    try {
      // 加载静态数据
      data.value = await getData(props.jobId);

      const [jobParams, _] = await Promise.all([
        getParams(props.jobId),
        getGeneration(props.jobId),
      ]);

      Object.keys(jobParams as any).forEach((key, index) => {
        if (key !== 'MODE_WORKING_MODE') {
          paramData.value.push({ key: index, paramName: key, paramValue: jobParams[key] });
        }
      });
    } catch (e) {
      console.error('Error during initial data loading:', e);
    }

    await pollDashboardData();
    if (!trainingComplete.value) {
      pollingTimer = setInterval(pollDashboardData, 5000);
    }
  });

  onUnmounted(() => {
    clearInterval(pollingTimer); // 清理单个计时器
  });

  async function getPieData(jobId) {
    try {
      const v = await maHttp.get(
        {
          url: 'modelJob/getTrainAccAndRecall',
          params: { trainJobId: jobId },
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (v) {
        trainAcc.value = v.newNowAcc || 0;
        recall.value = v.newNowRecall || 0;
        await renderPieCharts();
        chartsReady.value = true;
      }
    } catch (error) {
      console.error('Fallback getPieData failed:', error);
    }
  }

  async function checkQueueStatus() {
    try {
      // 先检查当前任务的状态，只有状态为排队中（11）时才显示排队提示
      if (data.value?.status !== 11) {
        // 如果任务不在排队中，关闭可能已显示的排队通知
        notification.close(notificationKey);
        return;
      }

      const jobCount = await maHttp.get(
        {
          url: 'job/getJobCount',
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      // 只有当任务状态为排队中且队列中有任务时才显示提示
      if (jobCount > 0 && data.value?.status === 11) {
        notification.open({
          key: notificationKey,
          message: '正在排队',
          description: `队列中共有 ${jobCount} 个任务`,
          duration: 0, // 持续显示直到有日志或状态改变
        });
      }
    } catch (error) {
      console.error('Failed to get job queue count:', error);
    }
  }

  async function renderPieCharts() {
    await nextTick();
    const accValue = Number((trainAcc.value || 0).toFixed(2));
    const recallValue = Number((recall.value || 0).toFixed(2));
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
            value: Number(accValue.toFixed(2)),
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
            value: Number(recallValue.toFixed(2)),
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
  }

  const shouldShowContent = computed(() => {
    const PipelinePhase = { TRAINING: 2 }; // 根据您的实际枚举值调整
    return props.currentPipelinePhase >= PipelinePhase.TRAINING;
  });
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
  /* 标签相关样式 */
  .labels-section {
    padding-top: 6px;
  }

  .labels-section .labels-header {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
  }

  .labels-section .labels-header .labels-title {
    font-weight: 500;
    color: #a9a9a9;
    font-size: 14px;
  }

  .labels-section .labels-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 60px;
    overflow: hidden;
    transition: all 0.3s ease;
  }

  .labels-section .labels-container.expanded {
    max-height: none;
  }

  .labels-section .labels-container .label-tag {
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    margin: 0;
    font-weight: 500;
    cursor: default;
    border: 1px solid transparent;
    transition: all 0.2s ease;
  }

  .labels-section .labels-container .label-tag:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .labels-section .labels-container .overflow-indicator {
    background: #36435c;
    color: #1890ff;
    border: 1px dashed #d9d9d9;
    cursor: pointer;
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    margin: 0;
    transition: all 0.2s ease;
  }

  .labels-section .labels-container .overflow-indicator:hover {
    background: #e6f7ff;
    color: #1890ff;
    border-color: #1890ff;
    transform: translateY(-1px);
  }

  .no-labels {
    border-top: 1px solid #f0f0f0;
    padding-top: 16px;
    text-align: center;
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
