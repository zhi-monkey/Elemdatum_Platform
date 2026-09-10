<template>
  <div class="convert-page">
    <div class="convert-container">
      <a-card hoverable class="convert-card" :bordered="false">
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
            <a-button type="primary" @click="logVisible = true">
              <template #icon><FileTextOutlined /></template>
              查看日志
            </a-button>
          </div>
        </template>

        <div class="card-content">
          <div class="stats-section">
            <div class="stats-row">
              <a-statistic title="作业类型" value="模型转换" class="stat-item" />
              <a-statistic
                title="框架"
                :value="detail.modelIteration.convert.framework"
                class="stat-item"
              />
              <a-statistic
                title="输入尺寸"
                :value="detail.modelIteration.convert.inputSize"
                class="stat-item"
              />
              <a-statistic
                title="输出类型"
                :value="detail.modelIteration.convert.outputType"
                class="stat-item"
              />
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-block info-block">
              <div class="info-header">
                <div class="header-item">
                  <ClockCircleOutlined class="header-icon" />
                  <span class="header-label">开始时间</span>
                  <span class="header-value">{{
                    detail.modelIteration.convert.startTime || '--'
                  }}</span>
                </div>
                <div class="header-item">
                  <ClockCircleOutlined class="header-icon" />
                  <span class="header-label">结束时间</span>
                  <span class="header-value">{{
                    detail.modelIteration.convert.endTime || '--'
                  }}</span>
                </div>
              </div>

              <div class="params-area">
                <div class="params-title">
                  <SettingOutlined class="title-icon" />
                  <span>超参配置</span>
                </div>
                <div class="params-grid">
                  <div
                    v-for="item in detail.modelIteration.convert.params"
                    :key="item.field"
                    class="param-item"
                  >
                    <span class="param-name">{{ item.label || item.field }}</span>
                    <span class="param-value">{{ item.value }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

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

    <ConvertLogModal v-model:visible="logVisible" :logs="logs" @refresh="refreshLogs" />
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { Button as AButton, Card as ACard, Statistic as AStatistic } from 'ant-design-vue';
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
  import ConvertLogModal from '/@/views/mineai/train/generationGuideList/detail/convert/ConvertLogModal.vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
  }>();

  const logVisible = ref(false);
  const logs = ref([
    '2026-04-06 11:10:01 [INFO] 初始化转换环境',
    '2026-04-06 11:10:05 [INFO] 加载模型权重完成',
    '2026-04-06 11:10:09 [INFO] 执行量化策略 INT8',
    '2026-04-06 11:10:12 [INFO] 写入转换产物成功',
  ]);

  const convertStatus = computed(() => props.detail.modelIteration.convert.status);
  const isConverting = computed(() => convertStatus.value === 'converting');
  const isSuccess = computed(() => convertStatus.value === 'completed');
  const isFailed = computed(() => convertStatus.value === 'failed');

  const statusClass = computed(() => {
    if (isConverting.value) return 'status-converting';
    if (isSuccess.value) return 'status-success';
    if (isFailed.value) return 'status-failed';
    return 'status-default';
  });

  const statusText = computed(() => {
    if (isConverting.value) return '转换中';
    if (isSuccess.value) return '转换成功';
    if (isFailed.value) return '转换失败';
    return '待转换';
  });

  function refreshLogs() {
    logs.value = [...logs.value, `${new Date().toLocaleString('zh-CN')} [INFO] 手动刷新日志`];
  }
</script>

<style scoped lang="less">
  .convert-page {
    padding: 6px;
    background: #181d31;
  }

  .convert-card {
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    background: #181d31 !important;

    :deep(.ant-card-head) {
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      padding: 0 24px;
      min-height: 64px;
    }

    :deep(.ant-card-body) {
      background: #181d31;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 18px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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
    }
  }

  .stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;

    .stat-item {
      text-align: center;
      padding: 12px;
      border-radius: 8px;
      background: rgba(24, 144, 255, 0.03);
      border: 1px solid rgba(24, 144, 255, 0.2);
    }
  }

  .detail-block {
    padding: 16px;
    border-radius: 8px;
    border: 1px solid rgba(24, 144, 255, 0.15);
    background: rgba(24, 144, 255, 0.03);
  }

  .info-header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;

    .header-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: rgba(24, 144, 255, 0.05);
      border-radius: 6px;
      border: 1px solid rgba(24, 144, 255, 0.1);
      flex: 1;
    }

    .header-icon {
      color: #1890ff;
    }

    .header-label {
      color: rgba(255, 255, 255, 0.45);
      font-size: 12px;
    }

    .header-value {
      color: rgba(255, 255, 255, 0.85);
      margin-left: auto;
      font-size: 12px;
    }
  }

  .params-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;

    .title-icon {
      color: #1890ff;
    }
  }

  .params-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 10px;
  }

  .param-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 10px;
    background: rgba(24, 144, 255, 0.05);
    border-radius: 4px;
    border: 1px solid rgba(24, 144, 255, 0.1);
  }

  .param-name {
    color: rgba(255, 255, 255, 0.65);
    font-size: 12px;
  }

  .param-value {
    color: #fff;
    font-size: 12px;
    font-weight: 500;
  }

  .animation-section {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: rgba(24, 144, 255, 0.03);
    border: 1px solid rgba(24, 144, 255, 0.15);
    min-height: 180px;
  }

  .converting-animation {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
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
