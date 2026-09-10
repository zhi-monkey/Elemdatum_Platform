<template>
  <a-modal
    title="集群节点详情"
    v-model:visible="visible"
    width="90%"
    :footer="null"
    @cancel="handleCancel"
    :maskClosable="false"
  >
    <div class="cluster-status-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <a-spin size="large" />
        <div class="loading-text">正在获取集群信息...</div>
      </div>

      <!-- 集群基本信息 -->
      <div v-if="!loading && clusterData.cluster" class="cluster-info">
        <div class="cluster-basic">
          <h3 class="cluster-title">{{ clusterData.cluster.clusterName || '未知集群' }}</h3>
          <div class="cluster-stats">
            <div class="stat-item">
              <span class="stat-label">GPU节点:</span>
              <span class="stat-value">{{ gpuNodeCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">非GPU节点:</span>
              <span class="stat-value">{{ nonGpuNodeCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">就绪节点:</span>
              <span class="stat-value">{{ clusterData.cluster.readyNodeCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">集群IP:</span>
              <span class="stat-value">{{ clusterData.cluster.clusterIp || '未知' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 节点详情 -->
      <div v-if="!loading" class="node-details">
        <div v-if="clusterData.nodes && clusterData.nodes.length > 0" class="nodes-list">
          <!-- GPU节点 -->
          <div v-if="gpuNodes.length > 0" class="node-group">
            <div class="group-header">
              <h3 class="group-title">GPU节点 ({{ gpuNodes.length }})</h3>
            </div>
            <div v-for="node in gpuNodes" :key="node.key" class="node-card gpu-node">
              <div class="node-header">
                <h3 class="node-name">{{ node.name }}</h3>
                <a-tag :color="getNodeStatusColor(node.status)">
                  {{ node.status }}
                </a-tag>
              </div>

              <div class="resource-grid">
                <!-- CPU 信息 -->
                <div class="resource-card cpu-card">
                  <div class="resource-title">CPU</div>
                  <div class="resource-details">
                    <div class="resource-usage">
                      <span class="used">{{ node.cpuUsed }}</span>
                      <span class="separator">/</span>
                      <span class="total">{{ node.cpuTotal }}</span>
                      <span class="unit">核</span>
                    </div>
                    <div class="resource-progress">
                      <a-progress
                        :percent="node.cpuUsage"
                        :stroke-color="getProgressColor(node.cpuUsage)"
                        :show-info="false"
                        size="small"
                      />
                      <span class="percentage-text">{{ node.cpuUsage }}%</span>
                    </div>
                  </div>
                </div>

                <!-- 内存信息 -->
                <div class="resource-card memory-card">
                  <div class="resource-title">内存</div>
                  <div class="resource-details">
                    <div class="resource-usage">
                      <span class="used">{{ node.memoryUsed }}</span>
                      <span class="separator">/</span>
                      <span class="total">{{ node.memoryTotal }}</span>
                      <span class="unit">GB</span>
                    </div>
                    <div class="resource-progress">
                      <a-progress
                        :percent="node.memoryUsage"
                        :stroke-color="getProgressColor(node.memoryUsage)"
                        :show-info="false"
                        size="small"
                      />
                      <span class="percentage-text">{{ node.memoryUsage }}%</span>
                    </div>
                  </div>
                </div>

                <!-- GPU 信息 -->
                <div class="resource-card gpu-card">
                  <div class="resource-title">
                    <span>GPU ({{ node.gpus.length }})</span>
                    <a-button
                      v-if="node.gpus.length > 2"
                      type="text"
                      size="small"
                      @click="toggleGpuExpanded(node.key)"
                      class="expand-btn"
                    >
                      {{ expandedGpus[node.key] ? '收起' : '展开' }}
                    </a-button>
                  </div>
                  <div class="gpu-list">
                    <div v-for="(gpu, index) in getVisibleGpus(node)" :key="index" class="gpu-item">
                      <div class="gpu-header">
                        <span class="gpu-name">{{ gpu.name }}</span>
                        <a-tag :color="getGpuStatusColor(gpu.status)" size="small">
                          {{ gpu.status }}
                        </a-tag>
                      </div>

                      <!-- 显存信息 -->
                      <div class="gpu-resource">
                        <div class="gpu-resource-label">显存</div>
                        <div class="gpu-resource-usage">
                          <span class="used">{{ gpu.memoryUsed }}</span>
                          <span class="separator">/</span>
                          <span class="total">{{ gpu.memoryTotal }}</span>
                          <span class="unit">GB</span>
                        </div>
                        <div class="gpu-resource-progress">
                          <a-progress
                            :percent="gpu.memoryUsage"
                            :stroke-color="getProgressColor(gpu.memoryUsage)"
                            :show-info="false"
                            size="small"
                          />
                          <span class="percentage-text">{{ gpu.memoryUsage }}%</span>
                        </div>
                      </div>

                      <!-- 算力信息 -->
                      <div class="gpu-resource">
                        <div class="gpu-resource-label">算力</div>
                        <div class="gpu-resource-usage">
                          <span class="used">{{ gpu.computeUsed }}</span>
                          <span class="separator">/</span>
                          <span class="total">{{ gpu.computeTotal }}</span>
                          <span class="unit">%</span>
                        </div>
                        <div class="gpu-resource-progress">
                          <a-progress
                            :percent="gpu.computeUsage"
                            :stroke-color="getProgressColor(gpu.computeUsage)"
                            :show-info="false"
                            size="small"
                          />
                          <span class="percentage-text">{{ gpu.computeUsage }}%</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 非GPU节点 -->
          <div v-if="nonGpuNodes.length > 0" class="node-group">
            <div class="group-header">
              <h3 class="group-title">非GPU节点 ({{ nonGpuNodes.length }})</h3>
              <a-button type="text" size="small" @click="toggleNonGpuExpanded" class="expand-btn">
                {{ nonGpuExpanded ? '收起' : '展开' }}
              </a-button>
            </div>
            <div v-if="nonGpuExpanded" class="non-gpu-nodes">
              <div v-for="node in nonGpuNodes" :key="node.key" class="node-card non-gpu-node">
                <div class="node-header">
                  <h3 class="node-name">{{ node.name }}</h3>
                  <a-tag :color="getNodeStatusColor(node.status)">
                    {{ node.status }}
                  </a-tag>
                </div>

                <div class="resource-grid">
                  <!-- CPU 信息 -->
                  <div class="resource-card cpu-card">
                    <div class="resource-title">CPU</div>
                    <div class="resource-details">
                      <div class="resource-usage">
                        <span class="used">{{ node.cpuUsed }}</span>
                        <span class="separator">/</span>
                        <span class="total">{{ node.cpuTotal }}</span>
                        <span class="unit">核</span>
                      </div>
                      <div class="resource-progress">
                        <a-progress
                          :percent="node.cpuUsage"
                          :stroke-color="getProgressColor(node.cpuUsage)"
                          :show-info="false"
                          size="small"
                        />
                        <span class="percentage-text">{{ node.cpuUsage }}%</span>
                      </div>
                    </div>
                  </div>

                  <!-- 内存信息 -->
                  <div class="resource-card memory-card">
                    <div class="resource-title">内存</div>
                    <div class="resource-details">
                      <div class="resource-usage">
                        <span class="used">{{ node.memoryUsed }}</span>
                        <span class="separator">/</span>
                        <span class="total">{{ node.memoryTotal }}</span>
                        <span class="unit">GB</span>
                      </div>
                      <div class="resource-progress">
                        <a-progress
                          :percent="node.memoryUsage"
                          :stroke-color="getProgressColor(node.memoryUsage)"
                          :show-info="false"
                          size="small"
                        />
                        <span class="percentage-text">{{ node.memoryUsage }}%</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 无节点数据状态 -->
        <div v-else class="no-nodes">
          <div class="no-nodes-icon">📊</div>
          <div class="no-nodes-text">暂无节点数据</div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import {
    Modal as AModal,
    Progress as AProgress,
    Tag as ATag,
    Spin as ASpin,
    Button as AButton,
  } from 'ant-design-vue';
  import { ref, defineProps, defineEmits, watch, computed } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  // 类型定义
  interface GpuInfo {
    name: string;
    status: string;
    memoryUsed: number;
    memoryTotal: number;
    memoryUsage: number;
    computeUsed: number;
    computeTotal: number;
    computeUsage: number;
  }

  interface NodeInfo {
    key: string;
    name: string;
    status: string;
    cpuUsed: number;
    cpuTotal: number;
    cpuUsage: number;
    memoryUsed: number;
    memoryTotal: number;
    memoryUsage: number;
    gpus: GpuInfo[];
  }

  interface ClusterInfo {
    clusterName: string;
    clusterIp: string;
    clusterPort: number;
    nodeCount: number;
    readyNodeCount: number;
    lastUpdateTime: string | null;
  }

  interface ClusterData {
    cluster: ClusterInfo;
    nodes: NodeInfo[];
  }

  const props = defineProps({
    visible: {
      type: Boolean,
      default: false,
    },
    selectedResource: {
      type: Object,
      default: () => ({}),
    },
  });

  const emit = defineEmits(['update:visible']);

  // 集群数据
  const clusterData = ref<ClusterData>({
    cluster: {
      clusterName: '',
      clusterIp: '',
      clusterPort: 0,
      nodeCount: 0,
      readyNodeCount: 0,
      lastUpdateTime: null,
    },
    nodes: [],
  });

  // 加载状态
  const loading = ref(false);

  // 展开状态管理
  const expandedGpus = ref<Record<string, boolean>>({});
  const nonGpuExpanded = ref(false);

  // 计算属性：分离GPU节点和非GPU节点
  const gpuNodes = computed(() => {
    return clusterData.value.nodes?.filter((node) => node.gpus && node.gpus.length > 0) || [];
  });

  const nonGpuNodes = computed(() => {
    return clusterData.value.nodes?.filter((node) => !node.gpus || node.gpus.length === 0) || [];
  });

  // 计算属性：节点统计
  const gpuNodeCount = computed(() => gpuNodes.value.length);
  const nonGpuNodeCount = computed(() => nonGpuNodes.value.length);

  // 获取集群详情数据
  const fetchClusterDetails = async () => {
    loading.value = true;
    try {
      const response = await maHttp.get(
        {
          url: 'api/k8s/cluster/details',
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (response) {
        clusterData.value = response;
      }
    } catch (error) {
      console.error('获取集群详情失败:', error);
      // 如果获取失败，使用默认数据
      clusterData.value = {
        cluster: {
          clusterName: '集群连接失败',
          clusterIp: '',
          clusterPort: 0,
          nodeCount: 0,
          readyNodeCount: 0,
          lastUpdateTime: null,
        },
        nodes: [],
      };
    } finally {
      loading.value = false;
    }
  };

  // 监听模态框显示状态，当显示时获取数据
  watch(
    () => props.visible,
    (newVisible) => {
      if (newVisible) {
        fetchClusterDetails();
      }
    },
    { immediate: true },
  );

  // 获取进度条颜色
  const getProgressColor = (percent: number) => {
    if (percent < 50) return '#52c41a';
    if (percent < 80) return '#faad14';
    return '#ff4d4f';
  };

  // 获取节点状态颜色
  const getNodeStatusColor = (status: string) => {
    switch (status) {
      case 'Running':
        return 'green';
      case 'Idle':
        return 'blue';
      case 'Error':
        return 'red';
      default:
        return 'default';
    }
  };

  // 获取GPU状态颜色
  const getGpuStatusColor = (status: string) => {
    switch (status) {
      case 'Running':
        return 'green';
      case 'Idle':
        return 'blue';
      case 'Error':
        return 'red';
      default:
        return 'default';
    }
  };

  // 切换GPU展开状态
  const toggleGpuExpanded = (nodeKey: string) => {
    expandedGpus.value[nodeKey] = !expandedGpus.value[nodeKey];
  };

  // 切换非GPU节点展开状态
  const toggleNonGpuExpanded = () => {
    nonGpuExpanded.value = !nonGpuExpanded.value;
  };

  // 获取可见的GPU列表（用于折叠功能）
  const getVisibleGpus = (node: NodeInfo) => {
    if (node.gpus.length <= 2) {
      return node.gpus;
    }
    return expandedGpus.value[node.key] ? node.gpus : node.gpus.slice(0, 2);
  };

  // 处理取消
  const handleCancel = () => {
    emit('update:visible', false);
  };
</script>

<style scoped>
  .cluster-status-container {
    padding: 24px;
  }

  /* 加载状态样式 */
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    gap: 16px;
  }

  .loading-text {
    color: #cbd5e1;
    font-size: 16px;
    font-weight: 500;
  }

  /* 集群基本信息样式 */
  .cluster-info {
    margin-bottom: 24px;
    padding: 20px;
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.6) 0%, rgba(15, 23, 42, 0.8) 100%);
    border: 1px solid rgba(71, 85, 105, 0.3);
    border-radius: 12px;
    backdrop-filter: blur(10px);
  }

  .cluster-basic {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .cluster-title {
    color: #60a5fa;
    font-size: 20px;
    font-weight: 700;
    margin: 0;
    text-align: center;
    background: linear-gradient(90deg, #60a5fa, #3b82f6, #60a5fa);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .cluster-stats {
    display: flex;
    justify-content: space-around;
    gap: 20px;
  }

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }

  .stat-label {
    color: #94a3b8;
    font-size: 12px;
    font-weight: 500;
  }

  .stat-value {
    color: #cbd5e1;
    font-size: 16px;
    font-weight: 600;
  }

  /* 无节点数据状态样式 */
  .no-nodes {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    gap: 16px;
  }

  .no-nodes-icon {
    font-size: 48px;
    opacity: 0.6;
  }

  .no-nodes-text {
    color: #94a3b8;
    font-size: 16px;
    font-style: italic;
  }

  .node-details {
    margin-bottom: 24px;
  }

  .nodes-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .node-group {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .group-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: rgba(30, 41, 59, 0.4);
    border: 1px solid rgba(71, 85, 105, 0.3);
    border-radius: 8px;
  }

  .group-title {
    color: #60a5fa;
    font-size: 16px;
    font-weight: 600;
    margin: 0;
  }

  .expand-btn {
    color: #60a5fa;
    font-size: 12px;
    padding: 4px 8px;
    height: auto;
  }

  .expand-btn:hover {
    color: #93c5fd;
    background: rgba(96, 165, 250, 0.1);
  }

  .non-gpu-nodes {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .node-card {
    border: 1px solid rgba(71, 85, 105, 0.3);
    background: linear-gradient(
      145deg,
      rgba(30, 41, 59, 0.8) 0%,
      rgba(15, 23, 42, 0.95) 50%,
      rgba(30, 41, 59, 0.8) 100%
    );
    backdrop-filter: blur(10px);
    border-radius: 12px;
    padding: 20px;
  }

  .node-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(71, 85, 105, 0.3);
  }

  .node-name {
    color: #cbd5e1;
    font-size: 18px;
    font-weight: 600;
    margin: 0;
  }

  .resource-section {
    margin-bottom: 20px;
  }

  .resource-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #cbd5e1;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .resource-icon {
    font-size: 16px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
  }

  .cpu-icon {
    background: rgba(34, 197, 94, 0.1);
  }

  .memory-icon {
    background: rgba(59, 130, 246, 0.1);
  }

  .gpu-icon {
    background: rgba(168, 85, 247, 0.1);
  }

  .node-card {
    border: 1px solid rgba(71, 85, 105, 0.3);
    background: rgba(15, 23, 42, 0.6);
    border-radius: 12px;
    padding: 20px;
    backdrop-filter: blur(10px);
  }

  .gpu-node {
    border-left: 4px solid #60a5fa;
  }

  .non-gpu-node {
    border-left: 4px solid #64748b;
  }

  .node-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(71, 85, 105, 0.3);
  }

  .node-name {
    color: #cbd5e1;
    font-size: 18px;
    font-weight: 600;
    margin: 0;
  }

  .resource-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
  }

  .resource-card {
    background: rgba(30, 41, 59, 0.4);
    border: 1px solid rgba(71, 85, 105, 0.2);
    border-radius: 8px;
    padding: 16px;
  }

  .cpu-card {
    border-left: 3px solid #22c55e;
  }

  .memory-card {
    border-left: 3px solid #3b82f6;
  }

  .gpu-card {
    border-left: 3px solid #a855f7;
    grid-column: 1 / -1;
  }

  .resource-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #cbd5e1;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .resource-usage {
    color: #cbd5e1;
    font-size: 14px;
    margin-bottom: 8px;
  }

  .used {
    color: #60a5fa;
    font-weight: 600;
  }

  .separator {
    color: #64748b;
    margin: 0 4px;
  }

  .total {
    color: #94a3b8;
  }

  .unit {
    color: #64748b;
    font-size: 12px;
    margin-left: 4px;
  }

  .resource-progress {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .percentage-text {
    color: #cbd5e1;
    font-size: 12px;
    min-width: 35px;
  }

  .gpu-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 12px;
    margin-top: 8px;
  }

  .gpu-item {
    border: 1px solid rgba(71, 85, 105, 0.2);
    border-radius: 8px;
    padding: 12px;
    background: rgba(15, 23, 42, 0.4);
    margin-bottom: 0;
  }

  .gpu-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .gpu-name {
    color: #cbd5e1;
    font-size: 13px;
    font-weight: 600;
  }

  .gpu-resource {
    margin-bottom: 8px;
  }

  .gpu-resource:last-child {
    margin-bottom: 0;
  }

  .gpu-resource-label {
    color: #94a3b8;
    font-size: 12px;
    margin-bottom: 4px;
  }

  .gpu-resource-usage {
    color: #cbd5e1;
    font-size: 12px;
    margin-bottom: 4px;
  }

  .gpu-resource-progress {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* 进度条样式 */
  :deep(.ant-progress-bg) {
    background: rgba(71, 85, 105, 0.3);
  }

  /* 响应式设计 */
  @media (max-width: 1200px) {
    .resource-grid {
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 12px;
    }

    .cluster-stats {
      gap: 16px;
    }
  }

  @media (max-width: 768px) {
    .resource-grid {
      grid-template-columns: 1fr;
      gap: 12px;
    }

    .gpu-list {
      grid-template-columns: 1fr;
    }

    .node-card {
      padding: 16px;
    }

    .node-name {
      font-size: 16px;
    }

    .cluster-stats {
      flex-direction: column;
      gap: 12px;
    }

    .stat-item {
      flex-direction: row;
      justify-content: space-between;
    }

    .cluster-info {
      padding: 16px;
    }

    .cluster-title {
      font-size: 18px;
    }

    .cluster-status-container {
      padding: 16px;
    }
  }

  @media (max-width: 480px) {
    .node-card {
      padding: 12px;
    }

    .node-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }

    .gpu-item {
      padding: 8px;
    }

    .resource-card {
      padding: 12px;
    }

    .cluster-status-container {
      padding: 12px;
    }
  }

  /* 模态框样式 */
  :deep(.ant-modal-header) {
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.9) 0%, rgba(15, 23, 42, 0.95) 100%);
    border-bottom: 1px solid rgba(71, 85, 105, 0.3);
  }

  :deep(.ant-modal-title) {
    color: #cbd5e1;
    font-weight: 600;
  }

  :deep(.ant-modal-body) {
    background: linear-gradient(135deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.9) 100%);
    padding: 0;
  }

  :deep(.ant-modal-close) {
    color: #94a3b8;
  }

  :deep(.ant-modal-close:hover) {
    color: #cbd5e1;
  }
</style>
