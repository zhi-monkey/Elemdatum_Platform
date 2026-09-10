<template>
  <div class="convert-page">
    <!-- 有转换任务且已开始 -->
    <div v-if="hasConvert && jobId != 0" class="convert-container">
      <a-card hoverable class="convert-card" :loading="loading">
        <!-- 卡片头部 -->
        <template #title>
          <div class="card-header">
            <div class="card-title-section">
              <div class="status-icon" :class="statusClass">
                <LoadingOutlined v-if="isConverting" spin />
                <CheckCircleOutlined v-else-if="isSuccess" />
                <CloseCircleOutlined v-else-if="isFailed" />
                <MinusCircleOutlined v-else />
              </div>
              <div class="title-info">
                <span class="title-text">模型转换任务</span>
                <span class="status-tag" :class="statusClass">{{ statusText }}</span>
              </div>
            </div>
            <a-button type="primary" @click="openLogModal">
              <template #icon><FileTextOutlined /></template>
              查看日志
            </a-button>
          </div>
        </template>

        <!-- 卡片内容 -->
        <div class="card-content">
          <!-- 上部：统计信息 -->
          <div class="stats-section">
            <div class="stats-row">
              <a-statistic
                title="作业类型"
                :value="jobTypeText"
                class="stat-item"
                :value-style="{ color: '#1890ff', fontSize: '22px', fontWeight: 'bold' }"
              />
              <a-statistic
                title="CPU"
                :value="cpuDisplay"
                class="stat-item"
                :value-style="{ color: '#52c41a', fontSize: '22px', fontWeight: 'bold' }"
              />
              <a-statistic
                title="GPU显存"
                :value="gpuDisplay"
                class="stat-item"
                :value-style="{ color: '#722ed1', fontSize: '22px', fontWeight: 'bold' }"
              />
              <a-statistic
                title="内存上限"
                :value="memoryDisplay"
                class="stat-item"
                :value-style="{ color: '#f8a349', fontSize: '22px', fontWeight: 'bold' }"
              />
            </div>
          </div>

          <!-- 中部：详情信息 -->
          <div class="detail-section">
            <div class="detail-block info-block">
              <div class="info-header">
                <div class="header-item">
                  <ClockCircleOutlined class="header-icon" />
                  <span class="header-label">开始时间</span>
                  <span class="header-value">{{ data.createTime || '--' }}</span>
                </div>
              </div>

              <div class="params-area">
                <div class="params-title">
                  <SettingOutlined class="title-icon" />
                  <span>超参配置</span>
                </div>
                <div class="params-grid">
                  <template v-if="paramData.length > 0">
                    <div v-for="item in paramData" :key="item.key" class="param-item">
                      <span class="param-name" :title="item.paramName">{{ item.paramName }}</span>
                      <span class="param-value" :title="item.paramValue">{{
                        item.paramValue
                      }}</span>
                    </div>
                  </template>
                  <a-empty
                    v-else
                    description="暂无超参配置"
                    :image="AEmpty.PRESENTED_IMAGE_SIMPLE"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 下部：转换状态动画 -->
          <div class="animation-section">
            <template v-if="isConverting">
              <div class="converting-animation">
                <div class="pulse-container">
                  <div class="pulse-ring"></div>
                  <div class="pulse-ring delay-1"></div>
                  <div class="pulse-ring delay-2"></div>
                  <SyncOutlined spin class="center-icon" />
                </div>
                <div class="animation-text">
                  <div class="main-text">模型量化转换中...</div>
                  <div class="sub-text">正在将模型转换为目标格式，请耐心等待</div>
                </div>
              </div>
            </template>
            <template v-else-if="isSuccess">
              <div class="result-display success">
                <CheckCircleOutlined class="result-icon" />
                <div class="result-text">转换完成</div>
              </div>
            </template>
            <template v-else-if="isFailed">
              <div class="result-display failed">
                <CloseCircleOutlined class="result-icon" />
                <div class="result-text">转换失败，请查看日志</div>
              </div>
            </template>
            <template v-else>
              <div class="result-display default">
                <MinusCircleOutlined class="result-icon" />
                <div class="result-text">等待转换</div>
              </div>
            </template>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 没有转换任务 -->
    <a-alert
      v-if="!hasConvert"
      message="这个任务不包含转换任务或转换任务还未开始"
      description="The task does not include a conversion task, or the conversion task has not started yet."
      type="info"
      show-icon
      class="alert-center"
    />

    <!-- 转换任务未开始 -->
    <a-alert
      v-if="hasConvert && jobId === 0"
      message="转换任务还未开始"
      description="The conversion job is not started yet."
      type="info"
      show-icon
      class="alert-center"
    />

    <!-- 日志弹窗 -->
    <ConvertLogModal
      v-model:visible="logModalVisible"
      :logs="consoleLogs"
      @refresh="refreshContent"
    />
  </div>
</template>

<script lang="ts" setup>
  import { computed, defineProps, onMounted, onUnmounted, ref, Ref } from 'vue';
  import {
    Alert as AAlert,
    Button as AButton,
    Card as ACard,
    Empty as AEmpty,
    notification,
    Statistic as AStatistic,
  } from 'ant-design-vue';
  import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    FileTextOutlined,
    LoadingOutlined,
    MinusCircleOutlined,
    SettingOutlined,
    SyncOutlined,
  } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { intervalBus } from '../train/intervalBus';
  import ConvertLogModal from './ConvertLogModal.vue';
  import { jobName } from './detailsData';

  const props = defineProps<{
    jobId: number;
    showBar: boolean;
    changeTabTitle: boolean;
    hasConvert: boolean;
  }>();

  const data: Ref<any> = ref({});
  const paramData: Ref<any[]> = ref([]);
  const consoleLogs: Ref<string[]> = ref([]);
  const loading: Ref<boolean> = ref(true);
  const logModalVisible = ref(false);

  const { setTitle } = useTabs();
  let notificationKey = 'queueNotification';

  // 计算属性
  const isConverting = computed(() => [10, 11, 1].includes(data.value.status));
  const isSuccess = computed(() => [6, 2].includes(data.value.status));
  const isFailed = computed(() => data.value.status === -5);

  const statusClass = computed(() => {
    if (isConverting.value) return 'status-converting';
    if (isSuccess.value) return 'status-success';
    if (isFailed.value) return 'status-failed';
    return 'status-default';
  });

  const statusText = computed(() => {
    const status = data.value.status;
    if (status === 10) return '转换任务创建中';
    if (status === 11) return '排队中';
    if (status === 1) return '转换中';
    if (status === 6 || status === 2) return '转换成功';
    if (status === -5) return '转换失败';
    if (status === -10) return '转换取消';
    return '未知';
  });

  const jobTypeText = computed(() => {
    const type = data.value.jobType;
    if (type === 1) return '模型训练';
    if (type === 2) return '模型质检';
    if (type === 3) return '模型转换';
    return '--';
  });

  const cpuDisplay = computed(() => {
    const cpus = data.value.cpus;
    if (!cpus) return '未设置';
    if (cpus === '无限制') return cpus;
    return cpus + '核';
  });

  const gpuDisplay = computed(() => {
    const gpu = data.value.gpuMemory;
    if (!gpu) return '未设置';
    return gpu;
  });

  const memoryDisplay = computed(() => {
    const mem = data.value.memory;
    if (!mem) return '未设置';
    if (mem === '无限制') return mem;
    return mem + 'B';
  });

  const openLogModal = () => {
    logModalVisible.value = true;
  };

  const TERMINAL_STATUSES = [6, 2, -5, -10];

  const stopConvertPolling = () => {
    if (intervalBus.refreshInterval_C.value !== null) {
      clearInterval(intervalBus.refreshInterval_C.value);
      intervalBus.refreshInterval_C.value = null;
    }
  };

  onMounted(async () => {
    const [jobData, jobParams, jobLogs] = await Promise.all([
      getData(props.jobId),
      getParams(props.jobId),
      getLog(props.jobId),
    ]);
    await getGeneration(props.jobId);

    data.value = jobData || {};

    if (jobParams && Object.keys(jobParams).length > 0) {
      Object.keys(jobParams as any).forEach((key, index) => {
        if (key !== 'MODE_WORKING_MODE') {
          paramData.value.push({ key: index, paramName: key, paramValue: jobParams[key] });
        }
      });
    }

    if (jobLogs) {
      jobLogs.forEach((value) => {
        consoleLogs.value.push(value[Object.keys(value)[0]].replace(/[\b]/g, '█'));
      });
    }

    loading.value = false;

    if (intervalBus.canSet.value && !intervalBus.isRefreshIntervalCExists() && !TERMINAL_STATUSES.includes(data.value.status)) {
      intervalBus.setRefreshInterval_C(setInterval(refreshContent, 10000));
    }
  });

  onUnmounted(() => {
    stopConvertPolling();
  });

  async function getGeneration(jobId) {
    if (jobId != 0) {
      await maHttp
        .get(
          {
            url: 'modelGeneration/findGeneraByJob',
            params: { modelJobId: jobId },
            headers: { ignoreCancelToken: true },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(() => {
          if (props.changeTabTitle) setTitle('训练作业详情');
        });
    }
  }

  async function getData(jobId) {
    if (jobId != 0) {
      return await maHttp.get(
        {
          url: 'modelJob/getJobByJobId',
          params: { id: jobId },
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    }
  }

  async function getParams(jobId) {
    if (jobId != 0) {
      return await maHttp.get(
        {
          url: 'modelJob/getParamByJobId',
          params: { id: jobId },
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    }
  }

  async function getLog(jobId) {
    if (jobId != 0) {
      return await maHttp
        .get(
          {
            url: 'modelJob/getJobByJobId',
            params: { id: jobId },
            headers: { ignoreCancelToken: true },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(async (job) => {
          if (TERMINAL_STATUSES.includes(job.status)) {
            stopConvertPolling();
            notification.close(notificationKey);
          }
          jobName.value = job.name;
          data.value = job;

          // 非运行态下不再持续拉整量日志，避免页面长时间刷 log/getJobLog
          if (TERMINAL_STATUSES.includes(job.status) && !logModalVisible.value) {
            return [];
          }

          let logContent = await maHttp.get(
            {
              url: 'log/getJobLog',
              params: {
                jobName: job.name,
                direction: 'BACKWARD',
                namespace: 'ai-platform',
                limit: 99999,
              },
              headers: { ignoreCancelToken: true },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );

          if (!logContent || logContent.length === 0) {
            let jobCount = await maHttp.get(
              {
                url: 'modelJob/getQueuePositionByModelJobName',
                params: { modelJobName: job.name },
                headers: { ignoreCancelToken: true },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );

            if (jobCount != -1) {
              notification.open({
                key: notificationKey,
                message: '集群外转换正在排队',
                description: `你的位置是第 ${jobCount} 个`,
                duration: 0,
              });
            }
          } else {
            notification.close(notificationKey);
          }

          return logContent;
        });
    }
  }

  async function refreshContent() {
    const jobLogs = await getLog(props.jobId);
    consoleLogs.value = [];
    if (jobLogs) {
      jobLogs.forEach((value) => {
        consoleLogs.value.push(value[Object.keys(value)[0]].replace(/[\b]/g, '█'));
      });
    }
  }
</script>

<style scoped lang="less">
  .convert-page {
    height: 100%;
    padding: 16px;
    background: #181d31;
  }

  .convert-container {
    height: 100%;
  }

  .convert-card {
    height: 85vh;
    border: 1px solid rgba(24, 144, 255, 0.15);
    border-radius: 8px;
    background: #1e2339 !important;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 24px rgba(24, 144, 255, 0.25);
      border-color: rgba(24, 144, 255, 0.5);
    }

    :deep(.ant-card-head) {
      border-bottom: 1px solid rgba(24, 144, 255, 0.2);
      padding: 0 24px;
      min-height: 64px;
    }

    :deep(.ant-card-head-title) {
      padding: 16px 0;
    }

    :deep(.ant-card-body) {
      background: #181d31;
      padding: 24px;
      height: calc(85vh - 64px);
      display: flex;
      flex-direction: column;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .card-title-section {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .status-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;

    &.status-converting {
      background: rgba(24, 144, 255, 0.15);
      color: #1890ff;
    }
    &.status-success {
      background: rgba(82, 196, 26, 0.15);
      color: #52c41a;
    }
    &.status-failed {
      background: rgba(255, 77, 79, 0.15);
      color: #ff4d4f;
    }
    &.status-default {
      background: rgba(140, 140, 140, 0.15);
      color: #8c8c8c;
    }
  }

  .title-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .title-text {
      font-size: 18px;
      font-weight: 600;
    }

    .status-tag {
      padding: 4px 12px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;

      &.status-converting {
        background: rgba(24, 144, 255, 0.15);
        color: #1890ff;
      }
      &.status-success {
        background: rgba(82, 196, 26, 0.15);
        color: #52c41a;
      }
      &.status-failed {
        background: rgba(255, 77, 79, 0.15);
        color: #ff4d4f;
      }
      &.status-default {
        background: rgba(140, 140, 140, 0.15);
        color: #8c8c8c;
      }
    }
  }

  .card-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  // 统计区域
  .stats-section {
    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;

      .stat-item {
        text-align: center;
        padding: 20px 16px;
        border-radius: 8px;
        background: rgba(24, 144, 255, 0.03);
        border: 1px solid rgba(24, 144, 255, 0.2);
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
          border-color: rgba(24, 144, 255, 0.4);
          background: rgba(24, 144, 255, 0.05);
        }

        :deep(.ant-statistic-title) {
          font-size: 13px;
          color: rgba(255, 255, 255, 0.65);
          margin-bottom: 8px;
        }
      }
    }
  }

  // 详情区域
  .detail-section {
    .detail-block {
      padding: 20px 24px;
      border-radius: 8px;
      border: 1px solid rgba(24, 144, 255, 0.15);
      background: rgba(24, 144, 255, 0.03);
      transition: all 0.3s ease;

      &:hover {
        border-color: rgba(24, 144, 255, 0.3);
        background: rgba(24, 144, 255, 0.05);
      }

      &.info-block {
        display: flex;
        flex-direction: column;
        gap: 20px;
      }
    }

    // 时间信息头部（横向布局）
    .info-header {
      display: flex;
      gap: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid rgba(24, 144, 255, 0.1);

      .header-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 20px;
        background: rgba(24, 144, 255, 0.05);
        border-radius: 6px;
        border: 1px solid rgba(24, 144, 255, 0.1);
        flex: 1;
        transition: all 0.2s ease;

        &:hover {
          border-color: rgba(24, 144, 255, 0.3);
          background: rgba(24, 144, 255, 0.08);
        }

        .header-icon {
          font-size: 18px;
          color: #1890ff;
        }

        .header-label {
          color: rgba(255, 255, 255, 0.45);
          font-size: 13px;
        }

        .header-value {
          color: rgba(255, 255, 255, 0.85);
          font-size: 14px;
          font-weight: 500;
          margin-left: auto;
        }
      }
    }

    // 超参配置区域
    .params-area {
      .params-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: rgba(255, 255, 255, 0.85);
        font-weight: 500;
        margin-bottom: 12px;

        .title-icon {
          color: #1890ff;
          font-size: 16px;
        }
      }

      .params-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 12px;
        max-height: 200px;
        overflow-y: auto;
        padding: 4px;

        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-track {
          background: rgba(24, 144, 255, 0.05);
          border-radius: 3px;
        }

        &::-webkit-scrollbar-thumb {
          background: rgba(24, 144, 255, 0.3);
          border-radius: 3px;

          &:hover {
            background: rgba(24, 144, 255, 0.5);
          }
        }

        .param-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 14px;
          background: rgba(24, 144, 255, 0.05);
          border-radius: 4px;
          border: 1px solid rgba(24, 144, 255, 0.1);
          transition: all 0.2s ease;

          &:hover {
            border-color: rgba(24, 144, 255, 0.3);
            background: rgba(24, 144, 255, 0.08);
            transform: translateX(2px);
          }

          .param-name {
            color: rgba(255, 255, 255, 0.65);
            font-size: 12px;
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-right: 12px;
          }

          .param-value {
            color: #fff;
            font-size: 12px;
            font-weight: 500;
          }
        }
      }
    }
  }

  // 动画区域
  .animation-section {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: rgba(24, 144, 255, 0.03);
    border: 1px solid rgba(24, 144, 255, 0.15);
    min-height: 200px;
  }

  .converting-animation {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  .pulse-container {
    position: relative;
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .pulse-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border: 2px solid #1890ff;
    border-radius: 50%;
    animation: pulse-ring 2s ease-out infinite;
    opacity: 0;
  }

  .pulse-ring.delay-1 {
    animation-delay: 0.6s;
  }

  .pulse-ring.delay-2 {
    animation-delay: 1.2s;
  }

  .center-icon {
    font-size: 40px;
    color: #1890ff;
  }

  .animation-text {
    text-align: center;

    .main-text {
      font-size: 18px;
      color: #fff;
      font-weight: 500;
      margin-bottom: 8px;
    }

    .sub-text {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.45);
    }
  }

  .result-display {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;

    .result-icon {
      font-size: 64px;
    }

    .result-text {
      font-size: 18px;
      font-weight: 500;
    }

    &.success {
      .result-icon,
      .result-text {
        color: #52c41a;
      }
    }

    &.failed {
      .result-icon,
      .result-text {
        color: #ff4d4f;
      }
    }

    &.default {
      .result-icon,
      .result-text {
        color: #8c8c8c;
      }
    }
  }

  .alert-center {
    width: 100%;
  }

  @keyframes pulse-ring {
    0% {
      transform: scale(0.5);
      opacity: 1;
    }
    100% {
      transform: scale(1.5);
      opacity: 0;
    }
  }
</style>
