<template>
  <div class="resource-cards-container">
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="preset" tab="默认配置">
        <div class="tab-content-wrapper">
          <!-- 左侧导航按钮 -->
          <button
            v-if="totalPresetResources > 0"
            class="nav-button nav-button-left"
            :disabled="presetCurrentPage <= 1"
            @click="handlePresetPageChange(presetCurrentPage - 1)"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M15 18L9 12L15 6"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>

          <div class="resource-cards-wrapper">
            <div class="resource-cards">
              <a-card
                v-for="resource in currentPresetConfigs"
                :key="resource.id"
                class="resource-card"
                :class="{ selected: selectedResourceId === resource.id }"
                @click="selectResource(resource.id)"
              >
                <div class="card-header">
                  <h4 class="card-title">{{ resource.title || '暂无标题' }}</h4>
                </div>
                <div class="resource-details">
                  <div class="detail-item">
                    <span class="detail-label">描述:</span>
                    <span class="detail-value">{{ resource.description || '暂无描述' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">名称:</span>
                    <span class="detail-value">{{ resource.name }}</span>
                  </div>
                </div>
              </a-card>
            </div>
            <div v-if="presetConfigs.length === 0" class="no-resources">
              <div class="no-resources-icon">📋</div>
              <div class="no-resources-text">暂无默认配置</div>
            </div>
          </div>

          <!-- 右侧导航按钮 -->
          <button
            v-if="totalPresetResources > 0"
            class="nav-button nav-button-right"
            :disabled="presetCurrentPage >= Math.ceil(totalPresetResources / pageSize)"
            @click="handlePresetPageChange(presetCurrentPage + 1)"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M9 18L15 12L9 6"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>

        <!-- GPU模式选择区域 -->
        <div class="gpu-mode-section no-border">
          <div class="gpu-mode-header no-icon">
            <span class="gpu-mode-title">GPU使用模式</span>
          </div>
          <div class="gpu-mode-options">
            <div class="gpu-radio-group">
              <label
                v-for="option in gpuModeOptions"
                :key="option.value"
                class="gpu-option"
                :class="{ selected: presetGpuMode === option.value }"
              >
                <input
                  type="radio"
                  :value="option.value"
                  v-model="presetGpuMode"
                  @change="handleGpuModeChange('preset', $event)"
                  class="gpu-radio-input"
                />
                <div class="option-content">
                  <div class="option-text">{{ option.label }}</div>
                  <div class="option-desc">{{ option.description }}</div>
                </div>
              </label>
            </div>
          </div>

          <!-- 显卡数量手动输入器 -->
          <div v-if="isMultiCardMode(presetGpuMode)" class="gpu-count-selector no-border">
            <div class="gpu-count-wrapper">
              <span class="gpu-count-label">显卡数量:</span>
              <div class="gpu-count-input-wrapper">
                <a-input-number
                  v-model:value="presetGpuCount"
                  @change="handleGpuCountChange('preset', $event)"
                  class="gpu-count-input"
                  placeholder="输入显卡数量"
                  :min="2"
                  :max="9999"
                  :step="1"
                  :precision="0"
                />
                <span class="gpu-count-unit">张</span>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="custom" tab="自定义配置">
        <div class="tab-content-wrapper">
          <!-- 左侧导航按钮 -->
          <button
            v-if="totalCustomResources > 0"
            class="nav-button nav-button-left"
            :disabled="customCurrentPage <= 1"
            @click="handleCustomPageChange(customCurrentPage - 1)"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M15 18L9 12L15 6"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>

          <div class="resource-cards-wrapper">
            <div class="resource-cards">
              <a-card
                v-for="resource in currentUserDefinedResources"
                :key="resource.id"
                class="resource-card"
                :class="{ selected: selectedResourceId === resource.id }"
                @click="selectResource(resource.id)"
              >
                <div class="card-header">
                  <h4 class="card-title">{{ resource.title || '暂无标题' }}</h4>
                </div>
                <div class="resource-details">
                  <div class="detail-item">
                    <span class="detail-label">描述:</span>
                    <span class="detail-value">{{ resource.description || '暂无描述' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">名称:</span>
                    <span class="detail-value">{{ resource.name }}</span>
                  </div>
                </div>
              </a-card>
            </div>

            <!-- 无数据和loading状态都在这个固定高度容器中 -->
            <div v-if="userDefinedResources.length === 0" class="empty-state-container">
              <div v-if="loading" class="loading-state">
                <a-spin size="large" />
                <div class="loading-text">加载中...</div>
              </div>
              <div v-else class="no-resources">
                <div class="no-resources-icon">⚙️</div>
                <div class="no-resources-text">暂无自定义配置</div>
              </div>
            </div>
          </div>

          <!-- 右侧导航按钮 -->
          <button
            v-if="totalCustomResources > 0"
            class="nav-button nav-button-right"
            :disabled="customCurrentPage >= Math.ceil(totalCustomResources / pageSize)"
            @click="handleCustomPageChange(customCurrentPage + 1)"
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M9 18L15 12L9 6"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>

        <!-- GPU模式选择区域 -->
        <div class="gpu-mode-section no-border">
          <div class="gpu-mode-header no-icon">
            <span class="gpu-mode-title">GPU使用模式</span>
          </div>
          <div class="gpu-mode-options">
            <div class="gpu-radio-group">
              <label
                v-for="option in gpuModeOptions"
                :key="option.value"
                class="gpu-option"
                :class="{ selected: customGpuMode === option.value }"
              >
                <input
                  type="radio"
                  :value="option.value"
                  v-model="customGpuMode"
                  @change="handleGpuModeChange('custom', $event)"
                  class="gpu-radio-input"
                />
                <div class="option-content">
                  <div class="option-text">{{ option.label }}</div>
                  <div class="option-desc">{{ option.description }}</div>
                </div>
              </label>
            </div>
          </div>

          <!-- 显卡数量手动输入器 -->
          <div v-if="isMultiCardMode(customGpuMode)" class="gpu-count-selector no-border">
            <div class="gpu-count-wrapper">
              <span class="gpu-count-label">显卡数量:</span>
              <div class="gpu-count-input-wrapper">
                <a-input-number
                  v-model:value="customGpuCount"
                  @change="handleGpuCountChange('custom', $event)"
                  class="gpu-count-input"
                  placeholder="输入显卡数量"
                  :min="2"
                  :max="9999"
                  :step="1"
                  :precision="0"
                />
                <span class="gpu-count-unit">张</span>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 资源占用提示和集群状态 -->
    <div class="resource-status-section">
      <div class="resource-tips">
        <div class="tips-content">
          <div class="tips-icon">💡</div>
          <div class="tips-text">
            <span v-if="selectedResourceId">
              当前选择将占用
              <span class="highlight">{{ getSelectedResourceInfo.gpuCount || 1 }}</span>
              张显卡的
              <span class="highlight">{{ getGpuComputeUsageText() }}</span>
              算力，
              <span class="highlight">{{ getSelectedResourceInfo.cpu || 0 }}</span>
              核CPU，
              <span class="highlight">{{ getSelectedResourceInfo.memory || '0G' }}</span>
              内存，
              <span class="highlight">{{ getSelectedResourceInfo.gpuMemory || '0G' }}</span>
              显存，
              <span v-if="isResourceRequirementTooHigh" class="error-text"
                >当前资源需求过高，集群无法满足要求</span
              >
              <span v-else-if="needsWaiting" class="waiting-text"
                >预计需要等待其余任务释放资源</span
              >
              <span v-else class="available-text">资源充足可立即启动</span>
            </span>
            <div v-if="clusterResourceStatus.lastUpdateTime" class="cluster-resource-info">
              <div class="resource-summary">
                集群资源:
                <span class="resource-item"
                  >CPU {{ clusterResourceStatus.availableCpu }}/{{
                    clusterResourceStatus.totalCpu
                  }}核</span
                >
                <span class="resource-item"
                  >内存 {{ clusterResourceStatus.availableMemory }}/{{
                    clusterResourceStatus.totalMemory
                  }}GB</span
                >
                <span class="resource-item"
                  >GPU {{ clusterResourceStatus.availableGpu }}/{{
                    clusterResourceStatus.totalGpu
                  }}张</span
                >
              </div>
            </div>
            <span v-else class="no-selection">请先选择资源配置</span>
          </div>
        </div>
        <a-button
          type="primary"
          size="small"
          @click="openClusterStatus"
          :disabled="!selectedResourceId"
          class="detail-button"
        >
          查看集群详情
        </a-button>
      </div>
    </div>

    <!-- 集群状态模态框 -->
    <ClusterStatusModal
      v-model:visible="clusterStatusVisible"
      :selected-resource="getSelectedResourceInfo"
    />
  </div>
</template>

<script setup lang="ts">
  import {
    Card as ACard,
    Tabs as ATabs,
    TabPane as ATabPane,
    Spin as ASpin,
    InputNumber as AInputNumber,
    Button as AButton,
  } from 'ant-design-vue';
  import { defineProps, defineEmits, computed, ref, onMounted, watch } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import ClusterStatusModal from '/@/views/mineai/train/generationCreate/ClusterStatusModal.vue';

  const props = defineProps({
    selectedResourceId: {
      type: Number,
      default: null,
    },
    // 添加初始GPU模式和数量的props
    initialGpuMode: {
      type: String,
      default: 'single-exclusive',
    },
    initialGpuCount: {
      type: Number,
      default: 1,
    },
  });

  const emit = defineEmits(['select-resource', 'gpu-mode-change', 'gpu-count-change']);

  // Tab相关状态
  const activeTab = ref<string>('preset');
  const pageSize = ref<number>(3); // 每页显示3个

  // 默认配置相关状态 (is_default = 1)
  const presetConfigs = ref<any[]>([]);
  const presetCurrentPage = ref(1);
  const totalPresetResources = ref(0);

  // 自定义配置相关状态 (is_default = 0)
  const userDefinedResources = ref<any[]>([]);
  const customCurrentPage = ref(1);
  const totalCustomResources = ref(0);
  const loading = ref(false);

  // GPU模式相关状态
  const presetGpuMode = ref(props.initialGpuMode);
  const customGpuMode = ref(props.initialGpuMode);
  const presetGpuCount = ref(props.initialGpuCount);
  const customGpuCount = ref(props.initialGpuCount);

  // 集群状态相关状态
  const clusterStatusVisible = ref(false);

  // GPU模式选项配置
  const gpuModeOptions = [
    {
      value: 'single-exclusive',
      label: '单卡独占',
      description: '独占一张显卡',
    },
    {
      value: 'single-shared',
      label: '单卡共享',
      description: '与其他任务共享一张显卡',
    },
    {
      value: 'multi-exclusive',
      label: '多卡独占',
      description: '独占多张显卡',
    },
    {
      value: 'multi-shared',
      label: '多卡共享',
      description: '与其他任务共享多张显卡',
    },
  ];

  // 当前页显示的默认配置
  const currentPresetConfigs = computed(() => {
    return presetConfigs.value;
  });

  // 当前页显示的自定义配置
  const currentUserDefinedResources = computed(() => {
    return userDefinedResources.value;
  });

  // 判断是否为多卡模式
  const isMultiCardMode = (mode: string) => {
    return mode === 'multi-exclusive' || mode === 'multi-shared';
  };

  // 获取当前GPU配置（改为computed）
  const getCurrentGpuConfig = computed(() => {
    const currentTab = activeTab.value;
    const mode = currentTab === 'preset' ? presetGpuMode.value : customGpuMode.value;
    const gpuCount = currentTab === 'preset' ? presetGpuCount.value : customGpuCount.value;

    return {
      tabType: currentTab,
      mode,
      gpuCount: isMultiCardMode(mode) ? gpuCount : 1,
    };
  });

  // 获取选中的资源信息
  const getSelectedResourceInfo = computed(() => {
    if (!props.selectedResourceId) {
      return {};
    }

    // 从当前显示的配置中查找选中的资源
    const allConfigs = [...presetConfigs.value, ...userDefinedResources.value];
    const selectedConfig = allConfigs.find((config) => config.id === props.selectedResourceId);

    if (!selectedConfig) {
      return {};
    }

    // 获取当前GPU配置
    const currentGpuConfig = getCurrentGpuConfig.value;

    return {
      id: selectedConfig.id,
      name: selectedConfig.name,
      title: selectedConfig.title,
      description: selectedConfig.description,
      cpu: selectedConfig.cpus || 0,
      memory: selectedConfig.memory || '0G',
      gpuMemory: selectedConfig.gpuMemory || '0G',
      gpuCount: currentGpuConfig.gpuCount,
      gpuMode: currentGpuConfig.mode,
    };
  });

  // 集群资源状态
  const clusterResourceStatus = ref({
    totalCpu: 0,
    availableCpu: 0,
    totalMemory: 0,
    availableMemory: 0,
    totalGpu: 0,
    availableGpu: 0,
    lastUpdateTime: null as Date | null,
  });

  // 集群节点数据
  const clusterNodes = ref<any[]>([]);

  // 获取集群资源状态
  const fetchClusterResourceStatus = async () => {
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

      if (response && response.nodes) {
        // 保存所有节点数据
        clusterNodes.value = response.nodes;

        let totalCpu = 0;
        let availableCpu = 0;
        let totalMemory = 0;
        let availableMemory = 0;
        let totalGpu = 0;
        let availableGpu = 0;

        // 只统计GPU节点的资源
        const gpuNodes = response.nodes.filter((node) => node.gpus && node.gpus.length > 0);

        gpuNodes.forEach((node) => {
          // CPU统计
          totalCpu += node.cpuTotal || 0;
          availableCpu += (node.cpuTotal || 0) - (node.cpuUsed || 0);

          // 内存统计
          totalMemory += node.memoryTotal || 0;
          availableMemory += (node.memoryTotal || 0) - (node.memoryUsed || 0);

          // GPU统计
          totalGpu += node.gpus.length;
          // 统计可用GPU：状态为Running且算力使用率小于50%的显卡
          availableGpu += node.gpus.filter(
            (gpu) => gpu.status === 'Running' && gpu.computeUsage < 50,
          ).length;
        });

        clusterResourceStatus.value = {
          totalCpu,
          availableCpu,
          totalMemory,
          availableMemory,
          totalGpu,
          availableGpu,
          lastUpdateTime: new Date(),
        };
      }
    } catch (error) {
      console.error('获取集群资源状态失败:', error);
      // 如果获取失败，使用默认值
      clusterResourceStatus.value = {
        totalCpu: 0,
        availableCpu: 0,
        totalMemory: 0,
        availableMemory: 0,
        totalGpu: 0,
        availableGpu: 0,
        lastUpdateTime: null,
      };
    }
  };

  // 检查资源需求是否超过GPU节点总量
  const isResourceRequirementTooHigh = computed(() => {
    const resourceInfo = getSelectedResourceInfo.value;
    if (!resourceInfo.cpu || !resourceInfo.memory) {
      return false;
    }

    const cpuNeeded = parseInt(resourceInfo.cpu) || 0;
    const memoryNeeded = parseInt(resourceInfo.memory.replace('G', '')) || 0;
    const gpuNeeded = resourceInfo.gpuCount || 1;

    // 检查是否超过GPU节点资源总量
    const { totalCpu, totalMemory, totalGpu } = clusterResourceStatus.value;

    return cpuNeeded > totalCpu || memoryNeeded > totalMemory || gpuNeeded > totalGpu;
  });

  // 检查是否有单个节点能满足资源需求
  const canSingleNodeSatisfy = computed(() => {
    const resourceInfo = getSelectedResourceInfo.value;
    if (!resourceInfo.cpu || !resourceInfo.memory) {
      return false;
    }

    const cpuNeeded = parseInt(resourceInfo.cpu) || 0;
    const memoryNeeded = parseInt(resourceInfo.memory.replace('G', '')) || 0;
    const gpuMemoryNeeded = parseInt(resourceInfo.gpuMemory.replace('G', '')) || 0;
    const gpuNeeded = resourceInfo.gpuCount || 1;
    const gpuMode = resourceInfo.gpuMode;

    // 检查是否有单个GPU节点能满足所有资源需求
    return clusterNodes.value.some((node) => {
      if (!node.gpus || node.gpus.length === 0) {
        return false; // 非GPU节点不考虑
      }

      const availableCpu = (node.cpuTotal || 0) - (node.cpuUsed || 0);
      const availableMemory = (node.memoryTotal || 0) - (node.memoryUsed || 0);

      // 根据GPU模式检查显卡可用性
      let availableGpus = [];
      if (gpuMode === 'single-exclusive' || gpuMode === 'multi-exclusive') {
        // 独占模式：需要显卡完全空闲（computeUsage = 0）
        availableGpus = node.gpus.filter(
          (gpu) => gpu.status === 'Running' && gpu.computeUsage === 0,
        );
      } else if (gpuMode === 'single-shared' || gpuMode === 'multi-shared') {
        // 共享模式：需要显卡算力使用率小于50%
        availableGpus = node.gpus.filter(
          (gpu) => gpu.status === 'Running' && gpu.computeUsage < 50,
        );
      } else {
        // 默认按独占模式处理
        availableGpus = node.gpus.filter(
          (gpu) => gpu.status === 'Running' && gpu.computeUsage === 0,
        );
      }

      // 检查可用GPU数量是否足够
      if (availableGpus.length < gpuNeeded) {
        return false;
      }

      // 对于多卡模式，需要检查是否有满足要求的GPU组合
      if (gpuNeeded > 1) {
        // 检查是否有足够的GPU数量
        if (availableGpus.length < gpuNeeded) {
          return false;
        }

        // 检查显存要求 - 找出所有满足显存要求的GPU
        let validGpus = availableGpus;
        if (gpuMemoryNeeded > 0) {
          validGpus = availableGpus.filter((gpu: any) => {
            const gpuTotalMemory = gpu.memoryTotal || 0;
            const gpuUsedMemory = gpu.memoryUsed || 0;
            const gpuAvailableMemory = gpuTotalMemory - gpuUsedMemory;
            return gpuAvailableMemory >= gpuMemoryNeeded;
          });
        }

        // 检查算力要求 - 在满足显存要求的GPU中，进一步筛选满足算力要求的
        if (gpuMode === 'single-exclusive' || gpuMode === 'multi-exclusive') {
          // 独占模式：需要显卡完全空闲
          validGpus = validGpus.filter((gpu: any) => gpu.computeUsage === 0);
        } else if (gpuMode === 'single-shared' || gpuMode === 'multi-shared') {
          // 共享模式：需要显卡算力使用率小于50%
          validGpus = validGpus.filter((gpu: any) => gpu.computeUsage < 50);
        }

        // 检查是否有足够的满足所有要求的GPU
        if (validGpus.length < gpuNeeded) {
          return false;
        }
      } else {
        // 单卡模式，检查显存要求
        if (gpuMemoryNeeded > 0) {
          // 检查是否有任何一张显卡满足显存要求
          const hasValidGpu = availableGpus.some((gpu: any) => {
            const gpuTotalMemory = gpu.memoryTotal || 0;
            const gpuUsedMemory = gpu.memoryUsed || 0;
            const gpuAvailableMemory = gpuTotalMemory - gpuUsedMemory;
            return gpuAvailableMemory >= gpuMemoryNeeded;
          });

          if (!hasValidGpu) {
            return false;
          }
        }
      }

      return availableCpu >= cpuNeeded && availableMemory >= memoryNeeded;
    });
  });

  // 判断是否需要等待（基于真实集群数据）
  const needsWaiting = computed(() => {
    // 如果资源需求过高，直接返回true
    if (isResourceRequirementTooHigh.value) {
      return true;
    }

    // 检查是否有单个节点能满足需求
    return !canSingleNodeSatisfy.value;
  });

  // 获取GPU算力使用文本
  const getGpuComputeUsageText = () => {
    const resourceInfo = getSelectedResourceInfo.value;
    const gpuMode = resourceInfo.gpuMode;

    if (gpuMode === 'single-exclusive' || gpuMode === 'multi-exclusive') {
      return '全部';
    } else if (gpuMode === 'single-shared' || gpuMode === 'multi-shared') {
      return '一半';
    }
    return '全部';
  };

  // 打开集群状态模态框
  const openClusterStatus = () => {
    clusterStatusVisible.value = true;
  };

  // GPU模式变化处理
  const handleGpuModeChange = (tabType: string, event: any) => {
    const mode = event.target.value;

    // 如果是多卡模式且当前GPU数量为1，则设置为2
    if (isMultiCardMode(mode)) {
      if (tabType === 'preset' && presetGpuCount.value === 1) {
        presetGpuCount.value = 2;
      } else if (tabType === 'custom' && customGpuCount.value === 1) {
        customGpuCount.value = 2;
      }
    }

    // 发送事件给父组件
    emit('gpu-mode-change', {
      tabType,
      mode,
      gpuCount: tabType === 'preset' ? presetGpuCount.value : customGpuCount.value,
    });
  };

  // GPU数量变化处理
  const handleGpuCountChange = (tabType: string, count: number) => {
    // 发送事件给父组件
    emit('gpu-count-change', {
      tabType,
      mode: tabType === 'preset' ? presetGpuMode.value : customGpuMode.value,
      gpuCount: count,
    });
  };

  // 获取默认配置列表
  const fetchPresetResources = async () => {
    if (activeTab.value !== 'preset') return;

    loading.value = true;
    try {
      const response = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: presetCurrentPage.value - 1,
            size: pageSize.value,
            isDefault: 1, // 获取默认配置
          },
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (response && response.content) {
        presetConfigs.value = response.content || [];
        totalPresetResources.value = response.totalElements || 0;
      } else {
        // 兼容旧版API
        presetConfigs.value = response || [];
        totalPresetResources.value = response.length || 0;
      }

      // 如果当前没有选中的资源，且默认配置列表有数据，且在第一页，自动选中第一个
      if (
        !props.selectedResourceId &&
        presetConfigs.value.length > 0 &&
        presetCurrentPage.value === 1
      ) {
        const firstResource = presetConfigs.value[0];
        if (firstResource && firstResource.id) {
          selectResource(firstResource.id);
        }
      }
    } catch (error) {
      console.error('获取默认配置失败:', error);
      presetConfigs.value = [];
      totalPresetResources.value = 0;
    } finally {
      loading.value = false;
    }
  };

  // 获取自定义配置列表
  const fetchCustomResources = async () => {
    if (activeTab.value !== 'custom') return;

    loading.value = true;
    try {
      const response = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: customCurrentPage.value - 1,
            size: pageSize.value,
            isDefault: 0, // 获取自定义配置
          },
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (response && response.content) {
        userDefinedResources.value = response.content || [];
        totalCustomResources.value = response.totalElements || 0;
      } else {
        // 兼容旧版API
        userDefinedResources.value = response || [];
        totalCustomResources.value = response.length || 0;
      }
    } catch (error) {
      console.error('获取自定义配置失败:', error);
      userDefinedResources.value = [];
      totalCustomResources.value = 0;
    } finally {
      loading.value = false;
    }
  };

  // 选择资源配置
  const selectResource = (id: number) => {
    emit('select-resource', id);
  };

  // Tab切换处理
  const handleTabChange = (key: string) => {
    activeTab.value = key;
    if (key === 'preset') {
      fetchPresetResources();
    } else if (key === 'custom') {
      fetchCustomResources();
    }
  };

  // 默认配置分页处理
  const handlePresetPageChange = (page: number) => {
    if (page < 1 || page > Math.ceil(totalPresetResources.value / pageSize.value)) return;
    presetCurrentPage.value = page;
    fetchPresetResources();
  };

  // 自定义配置分页处理
  const handleCustomPageChange = (page: number) => {
    if (page < 1 || page > Math.ceil(totalCustomResources.value / pageSize.value)) return;
    customCurrentPage.value = page;
    fetchCustomResources();
  };

  // 查找并跳转到包含所选资源的页面
  const findAndNavigateToSelectedResource = async () => {
    if (!props.selectedResourceId) return;

    // 先确定资源在哪个tab中
    let foundInPreset = false;
    let foundInCustom = false;

    // 检查预设配置
    try {
      const presetResponse = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: 0,
            size: totalPresetResources.value || 100, // 获取所有预设配置
            isDefault: 1,
          },
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      const allPresetConfigs = presetResponse.content || presetResponse || [];
      foundInPreset = allPresetConfigs.some(
        (config: any) => config.id === props.selectedResourceId,
      );

      if (foundInPreset) {
        // 计算所选资源在预设配置中的页码
        const selectedIndex = allPresetConfigs.findIndex(
          (config: any) => config.id === props.selectedResourceId,
        );
        if (selectedIndex !== -1) {
          const targetPage = Math.floor(selectedIndex / pageSize.value) + 1;
          presetCurrentPage.value = targetPage;
          activeTab.value = 'preset';
          await fetchPresetResources();
          return;
        }
      }
    } catch (error) {
      console.error('查找预设配置中的所选资源失败:', error);
    }

    // 检查自定义配置
    try {
      const customResponse = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: 0,
            size: totalCustomResources.value || 100, // 获取所有自定义配置
            isDefault: 0,
          },
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      const allCustomConfigs = customResponse.content || customResponse || [];
      foundInCustom = allCustomConfigs.some(
        (config: any) => config.id === props.selectedResourceId,
      );

      if (foundInCustom) {
        // 计算所选资源在自定义配置中的页码
        const selectedIndex = allCustomConfigs.findIndex(
          (config: any) => config.id === props.selectedResourceId,
        );
        if (selectedIndex !== -1) {
          const targetPage = Math.floor(selectedIndex / pageSize.value) + 1;
          customCurrentPage.value = targetPage;
          activeTab.value = 'custom';
          await fetchCustomResources();
          return;
        }
      }
    } catch (error) {
      console.error('查找自定义配置中的所选资源失败:', error);
    }
  };

  // 监听activeTab变化，确保切换到相应配置时加载数据
  watch(activeTab, (newTab) => {
    if (newTab === 'preset' && presetConfigs.value.length === 0) {
      fetchPresetResources();
    } else if (newTab === 'custom' && userDefinedResources.value.length === 0) {
      fetchCustomResources();
    }
  });

  // 监听初始GPU模式和数量的变化
  watch(
    () => props.initialGpuMode,
    (newMode) => {
      presetGpuMode.value = newMode;
      customGpuMode.value = newMode;
    },
  );

  watch(
    () => props.initialGpuCount,
    (newCount) => {
      presetGpuCount.value = newCount;
      customGpuCount.value = newCount;
    },
  );

  // 监听selectedResourceId变化，自动跳转到对应页面
  watch(
    () => props.selectedResourceId,
    async (newId) => {
      if (newId) {
        await findAndNavigateToSelectedResource();
      }
    },
  );

  // 暴露刷新方法供父组件调用
  const refreshConfigurations = async () => {
    if (activeTab.value === 'preset') {
      presetCurrentPage.value = 1;
      await fetchPresetResources();
    } else if (activeTab.value === 'custom') {
      customCurrentPage.value = 1;
      await fetchCustomResources();
    }
  };

  defineExpose({
    refreshConfigurations,
    getCurrentGpuConfig: () => getCurrentGpuConfig.value,
  });

  onMounted(async () => {
    // 获取集群资源状态
    await fetchClusterResourceStatus();

    // 如果有选中的资源ID，先查找并跳转到对应页面
    if (props.selectedResourceId) {
      await findAndNavigateToSelectedResource();
    } else {
      // 如果没有选中的资源ID，按默认逻辑加载数据
      if (activeTab.value === 'preset') {
        fetchPresetResources();
      } else if (activeTab.value === 'custom') {
        fetchCustomResources();
      }
    }
  });
</script>

<style scoped>
  /* 主容器 */
  .resource-cards-container {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
  }

  /* Tab内容包装器 - 横向布局，导航按钮相对于卡片内容对齐 */
  .tab-content-wrapper {
    min-height: 180px;
    max-height: 200px;
    position: relative;
    padding: 0;
    margin: 0;
    display: grid;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    justify-content: center;
    gap: 20px;
  }

  /* 当只有中间内容时，grid会自动适应 */
  .tab-content-wrapper > .resource-cards-wrapper {
    grid-column: 2;
  }

  /* 资源卡片包装器 */
  .resource-cards-wrapper {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    min-height: 180px;
    max-height: 200px;
    position: relative;
  }

  /* 卡片网格容器 - 垂直居中显示 */
  .resource-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 10px;
    margin: 0;
    padding: 0;
    width: 100%;
    place-items: start center;
  }

  /* 左右导航按钮样式 - 相对于卡片内容居中 */
  .nav-button-left,
  .nav-button-right {
    width: 54px !important;
    height: 54px !important;
    border: 2px solid rgba(71, 85, 105, 0.5);
    background: linear-gradient(135deg, rgba(51, 65, 85, 0.7) 0%, rgba(30, 41, 59, 0.9) 100%);
    border-radius: 50%;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #cbd5e1;
    position: relative;
    overflow: hidden;
    flex-shrink: 0;
    box-shadow: 0 6px 15px rgba(0, 0, 0, 0.3);
  }

  .nav-button-left::before,
  .nav-button-right::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s ease;
  }

  .nav-button-left:hover::before,
  .nav-button-right:hover::before {
    transform: translateX(100%);
  }

  .nav-button-left:hover:not(:disabled),
  .nav-button-right:hover:not(:disabled) {
    border-color: rgba(96, 165, 250, 0.8);
    background: linear-gradient(135deg, rgba(71, 85, 105, 0.8) 0%, rgba(51, 65, 85, 0.95) 100%);
    color: #60a5fa;
    transform: scale(1.1);
    box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3), 0 4px 8px rgba(0, 0, 0, 0.4);
  }

  .nav-button-left:active:not(:disabled),
  .nav-button-right:active:not(:disabled) {
    transform: scale(1.05);
    transition: transform 0.1s ease;
  }

  .nav-button-left:disabled,
  .nav-button-right:disabled {
    opacity: 0.3;
    cursor: not-allowed;
    border-color: rgba(71, 85, 105, 0.3);
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.6) 100%);
    color: rgba(148, 163, 184, 0.4);
    transform: none;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .nav-button-left svg,
  .nav-button-right svg {
    width: 22px;
    height: 22px;
    transition: transform 0.3s ease;
  }

  .nav-button-left:hover:not(:disabled) svg,
  .nav-button-right:hover:not(:disabled) svg {
    transform: scale(1.2);
  }

  /* 卡片基础样式 */
  .resource-card {
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 2px solid rgba(71, 85, 105, 0.3);
    background: linear-gradient(
      145deg,
      rgba(30, 41, 59, 0.8) 0%,
      rgba(15, 23, 42, 0.95) 50%,
      rgba(30, 41, 59, 0.8) 100%
    );
    backdrop-filter: blur(10px);
    min-height: 110px;
    height: auto;
    width: 100%;
    max-width: 280px;
    border-radius: 8px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 0 2px 4px -1px rgba(0, 0, 0, 0.2),
      inset 0 1px 0 0 rgba(255, 255, 255, 0.05);
    position: relative;
    overflow: visible;
    justify-self: center;
  }

  .resource-card :deep(.ant-card-body) {
    padding: 14px;
    height: 100%;
  }

  .card-header {
    margin-bottom: 10px;
    position: relative;
  }

  .card-title {
    color: #f1f5f9;
    font-size: 14px;
    font-weight: 600;
    margin: 0;
    padding-bottom: 6px;
    border-bottom: 2px solid rgba(59, 130, 246, 0.3);
    background: linear-gradient(90deg, #60a5fa, #3b82f6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .resource-details {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .detail-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
  }

  .detail-label {
    font-weight: 500;
    color: #94a3b8;
    min-width: 40px;
    font-size: 12px;
    flex-shrink: 0;
    opacity: 0.9;
  }

  .detail-value {
    color: #cbd5e1;
    font-size: 12px;
    word-break: break-word;
    line-height: 1.3;
    flex: 1;
  }

  /* 悬停效果 */
  .resource-card:hover {
    border-color: rgba(96, 165, 250, 0.6);
    background: linear-gradient(
      145deg,
      rgba(51, 65, 85, 0.9) 0%,
      rgba(30, 41, 59, 0.95) 50%,
      rgba(51, 65, 85, 0.9) 100%
    );
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -2px rgba(0, 0, 0, 0.3),
      inset 0 1px 0 0 rgba(255, 255, 255, 0.1);
    transform: translateY(-2px) scale(1.01);
  }

  /* 选中状态样式 */
  .resource-card.selected {
    border-color: #60a5fa;
    background: linear-gradient(
      145deg,
      rgba(59, 130, 246, 0.15) 0%,
      rgba(30, 41, 59, 0.95) 30%,
      rgba(15, 23, 42, 0.95) 70%,
      rgba(59, 130, 246, 0.15) 100%
    );
    box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.3), 0 12px 20px -5px rgba(59, 130, 246, 0.4),
      0 8px 16px -8px rgba(0, 0, 0, 0.4), inset 0 1px 0 0 rgba(255, 255, 255, 0.15);
    transform: translateY(-3px) scale(1.02);
  }

  .resource-card.selected::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    border-radius: 10px;
    background: linear-gradient(
      45deg,
      rgba(96, 165, 250, 0.6),
      rgba(59, 130, 246, 0.8),
      rgba(96, 165, 250, 0.6)
    );
    z-index: -1;
    animation: selectedPulse 3s ease-in-out infinite;
  }

  @keyframes selectedPulse {
    0%,
    100% {
      opacity: 0.6;
      transform: scale(1);
    }
    50% {
      opacity: 0.8;
      transform: scale(1.01);
    }
  }

  /* 空状态容器 - 也要相对于导航按钮居中 */
  .empty-state-container {
    min-height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    grid-column: 1 / -1;
    width: 100%;
  }

  /* 加载状态 */
  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #94a3b8;
  }

  .loading-text {
    font-size: 13px;
    opacity: 0.8;
  }

  /* 无数据状态 */
  .no-resources {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 20px 16px;
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.6) 100%);
    border: 2px dashed rgba(71, 85, 105, 0.3);
    border-radius: 8px;
    max-width: 280px;
    margin: 0 auto;
    width: 100%;
  }

  .no-resources-icon {
    font-size: 28px;
    opacity: 0.6;
  }

  .no-resources-text {
    color: #94a3b8;
    font-size: 13px;
    font-style: italic;
    opacity: 0.8;
  }

  /* GPU模式选择区域样式 - 无边框版本 */
  .gpu-mode-section {
    margin-top: 14px;
    padding: 14px;
    background: transparent;
    border: none;
    border-radius: 0;
    backdrop-filter: none;
    box-shadow: none;
  }

  .gpu-mode-section.no-border {
    border: none;
    background: transparent;
  }

  .gpu-mode-header {
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .gpu-mode-header.no-icon {
    gap: 0;
  }

  .gpu-mode-icon {
    display: none;
  }

  .gpu-mode-title {
    color: #60a5fa;
    font-size: 15px;
    font-weight: 700;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    background: linear-gradient(90deg, #60a5fa, #3b82f6, #60a5fa);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .gpu-mode-options {
    margin-bottom: 10px;
  }

  /* GPU选项一行布局 */
  .gpu-radio-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .gpu-option {
    position: relative;
    cursor: pointer;
    border: 2px solid rgba(71, 85, 105, 0.3);
    background: linear-gradient(
      145deg,
      rgba(30, 41, 59, 0.8) 0%,
      rgba(15, 23, 42, 0.95) 50%,
      rgba(30, 41, 59, 0.8) 100%
    );
    backdrop-filter: blur(10px);
    border-radius: 8px;
    padding: 12px 8px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
    height: 70px;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 0 2px 4px -1px rgba(0, 0, 0, 0.2),
      inset 0 1px 0 0 rgba(255, 255, 255, 0.05);
  }

  .gpu-option:hover {
    border-color: rgba(96, 165, 250, 0.6);
    background: linear-gradient(
      145deg,
      rgba(51, 65, 85, 0.9) 0%,
      rgba(30, 41, 59, 0.95) 50%,
      rgba(51, 65, 85, 0.9) 100%
    );
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -2px rgba(0, 0, 0, 0.3),
      inset 0 1px 0 0 rgba(255, 255, 255, 0.1);
    transform: translateY(-2px) scale(1.01);
  }

  .gpu-option.selected {
    border-color: #60a5fa;
    background: linear-gradient(
      145deg,
      rgba(59, 130, 246, 0.15) 0%,
      rgba(30, 41, 59, 0.95) 30%,
      rgba(15, 23, 42, 0.95) 70%,
      rgba(59, 130, 246, 0.15) 100%
    );
    box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.3), 0 12px 20px -5px rgba(59, 130, 246, 0.4),
      0 8px 16px -8px rgba(0, 0, 0, 0.4), inset 0 1px 0 0 rgba(255, 255, 255, 0.15);
    transform: translateY(-3px) scale(1.02);
  }

  .gpu-option.selected::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    border-radius: 10px;
    background: linear-gradient(
      45deg,
      rgba(96, 165, 250, 0.6),
      rgba(59, 130, 246, 0.8),
      rgba(96, 165, 250, 0.6)
    );
    z-index: -1;
    animation: selectedPulse 3s ease-in-out infinite;
  }

  @keyframes selectedPulse {
    0%,
    100% {
      opacity: 0.6;
      transform: scale(1);
    }
    50% {
      opacity: 0.8;
      transform: scale(1.01);
    }
  }

  .gpu-radio-input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  .option-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    position: relative;
    z-index: 1;
    width: 100%;
    height: 100%;
  }

  .option-text {
    font-size: 13px;
    font-weight: 600;
    color: #cbd5e1;
    text-align: center;
    transition: all 0.3s ease;
    letter-spacing: 0.3px;
    line-height: 1.2;
  }

  .gpu-option:hover .option-text {
    color: #e2e8f0;
  }

  .gpu-option.selected .option-text {
    color: #60a5fa;
    text-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
  }

  .option-desc {
    font-size: 11px;
    color: #94a3b8;
    text-align: center;
    opacity: 0.8;
    transition: all 0.3s ease;
    line-height: 1.2;
  }

  .gpu-option:hover .option-desc {
    opacity: 1;
    color: #cbd5e1;
  }

  .gpu-option.selected .option-desc {
    color: #a5b4fc;
    opacity: 1;
  }

  /* GPU数量选择器 - 无边框版本 */
  .gpu-count-selector {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 10px;
    padding: 10px 14px;
    background: transparent;
    border: none;
    border-radius: 0;
    animation: slideInFromTop 0.4s ease-out;
    box-shadow: none;
  }

  .gpu-count-selector.no-border {
    border: none;
    background: transparent;
  }

  @keyframes slideInFromTop {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .gpu-count-icon {
    display: none;
  }

  .gpu-count-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }

  .gpu-count-label {
    color: #cbd5e1;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .gpu-count-input-wrapper {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .gpu-count-input {
    min-width: 100px;
  }

  .gpu-count-input :deep(.ant-input-number) {
    width: 100%;
  }

  .gpu-count-input :deep(.ant-input-number-input) {
    background: linear-gradient(
      135deg,
      rgba(30, 41, 59, 0.9) 0%,
      rgba(15, 23, 42, 0.95) 100%
    ) !important;
    border: 2px solid rgba(71, 85, 105, 0.6) !important;
    border-radius: 6px !important;
    color: #cbd5e1 !important;
    font-size: 13px;
    font-weight: 500;
    text-align: center;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
    transition: all 0.3s ease;
  }

  .gpu-count-input :deep(.ant-input-number-handler-wrap) {
    background: linear-gradient(
      135deg,
      rgba(30, 41, 59, 0.9) 0%,
      rgba(15, 23, 42, 0.95) 100%
    ) !important;
    border-left: 2px solid rgba(71, 85, 105, 0.6) !important;
    border-radius: 0 6px 6px 0 !important;
  }

  .gpu-count-input :deep(.ant-input-number-handler) {
    border: none !important;
    color: #94a3b8 !important;
    transition: all 0.3s ease;
    background: rgba(51, 65, 85, 0.3);
  }

  .gpu-count-input :deep(.ant-input-number-handler:hover) {
    color: #60a5fa !important;
    background: rgba(96, 165, 250, 0.15) !important;
    transform: scale(1.05);
  }

  .gpu-count-input :deep(.ant-input-number-handler-up-inner),
  .gpu-count-input :deep(.ant-input-number-handler-down-inner) {
    color: inherit !important;
  }

  .gpu-count-input :deep(.ant-input-number:hover) {
    border-color: rgba(96, 165, 250, 0.7) !important;
    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2), inset 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .gpu-count-input :deep(.ant-input-number:focus),
  .gpu-count-input :deep(.ant-input-number-focused) {
    border-color: #60a5fa !important;
    box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.3), inset 0 2px 4px rgba(0, 0, 0, 0.2) !important;
  }

  .gpu-count-unit {
    color: #94a3b8;
    font-size: 13px;
    font-weight: 500;
    white-space: nowrap;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  /* Tab样式优化 */
  :deep(.ant-tabs-nav) {
    margin-bottom: 12px;
  }

  :deep(.ant-tabs-content-holder) {
    overflow: visible;
  }

  :deep(.ant-tabs-tabpane) {
    overflow: visible;
  }

  :deep(.ant-tabs-tab) {
    color: #94a3b8;
    font-weight: 500;
    transition: all 0.3s ease;
    padding: 8px 12px;
  }

  :deep(.ant-tabs-tab:hover) {
    color: #cbd5e1;
  }

  :deep(.ant-tabs-tab-active) {
    color: #60a5fa !important;
    font-weight: 600;
  }

  :deep(.ant-tabs-ink-bar) {
    background: linear-gradient(90deg, #60a5fa, #3b82f6);
    height: 3px;
    border-radius: 2px;
  }

  /* 响应式布局优化 */
  @media (max-width: 1200px) {
    .resource-cards {
      gap: 14px;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }

    .gpu-radio-group {
      flex-wrap: wrap;
      gap: 6px;
    }

    .gpu-option {
      height: 60px;
      padding: 8px 4px;
      min-width: calc(25% - 6px);
    }

    .nav-button-left,
    .nav-button-right {
      width: 48px !important;
      height: 48px !important;
    }
  }

  @media (max-width: 768px) {
    .resource-cards {
      grid-template-columns: 1fr;
      gap: 12px;
      place-items: center;
    }

    .resource-card {
      max-width: 100%;
      width: 100%;
    }

    .tab-content-wrapper {
      min-height: 180px;
      max-height: 200px;
      gap: 16px;
      /* 在移动端使用flex布局，更简单 */
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .resource-cards-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }

    .nav-button-left,
    .nav-button-right {
      width: 44px !important;
      height: 44px !important;
      margin: 0;
      align-self: center;
    }

    .nav-button-left svg,
    .nav-button-right svg {
      width: 18px;
      height: 18px;
    }

    .gpu-radio-group {
      flex-direction: column;
      gap: 6px;
    }

    .gpu-option {
      height: 55px;
      width: 100%;
      min-width: unset;
    }

    .option-content {
      flex-direction: column;
      gap: 4px;
    }

    .option-text {
      font-size: 12px;
    }

    .option-desc {
      font-size: 10px;
    }

    .gpu-count-selector {
      flex-direction: column;
      gap: 8px;
      padding: 10px 12px;
    }

    .gpu-count-wrapper {
      justify-content: center;
    }

    .gpu-count-input-wrapper {
      width: 100%;
      justify-content: center;
    }

    .gpu-count-input {
      width: 80px;
      min-width: unset;
    }
  }

  @media (max-width: 480px) {
    .resource-card {
      height: 140px;
    }

    .resource-card :deep(.ant-card-body) {
      padding: 16px;
    }

    .resource-cards {
      gap: 10px;
    }

    .tab-content-wrapper {
      min-height: 160px;
      max-height: 180px;
      gap: 12px;
      /* 确保小屏幕使用flex布局 */
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .resource-cards-wrapper {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }

    .nav-button-left,
    .nav-button-right {
      width: 40px !important;
      height: 40px !important;
      margin: 0;
      align-self: center;
    }

    .nav-button-left svg,
    .nav-button-right svg {
      width: 16px;
      height: 16px;
    }

    .gpu-mode-section {
      padding: 10px;
      margin-top: 10px;
    }

    .gpu-mode-header {
      margin-bottom: 8px;
    }

    .gpu-mode-title {
      font-size: 13px;
    }

    .gpu-mode-icon {
      font-size: 16px;
    }

    .gpu-option {
      height: 50px;
      padding: 6px;
    }

    .option-content {
      gap: 4px;
    }

    .option-text {
      font-size: 10px;
    }

    .option-desc {
      font-size: 8px;
    }

    .gpu-count-selector {
      padding: 8px;
    }

    .gpu-count-icon {
      font-size: 14px;
    }

    .gpu-count-label {
      font-size: 11px;
    }
  }

  /* 滚动条样式 */
  .ant-modal-body::-webkit-scrollbar {
    width: 6px;
  }

  .ant-modal-body::-webkit-scrollbar-track {
    background: rgba(15, 23, 42, 0.5);
    border-radius: 3px;
  }

  .ant-modal-body::-webkit-scrollbar-thumb {
    background: rgba(71, 85, 105, 0.5);
    border-radius: 3px;
  }

  .ant-modal-body::-webkit-scrollbar-thumb:hover {
    background: rgba(71, 85, 105, 0.7);
  }

  /* 资源状态提示区域样式 */
  .resource-status-section {
    margin-top: 20px;
    padding: 16px;
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.6) 0%, rgba(15, 23, 42, 0.8) 100%);
    border: 1px solid rgba(71, 85, 105, 0.3);
    border-radius: 8px;
    backdrop-filter: blur(10px);
  }

  .resource-tips {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .tips-content {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
  }

  .tips-icon {
    font-size: 20px;
    opacity: 0.8;
  }

  .tips-text {
    color: #cbd5e1;
    font-size: 14px;
    line-height: 1.5;
  }

  .highlight {
    color: #60a5fa;
    font-weight: 600;
    background: rgba(96, 165, 250, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
    margin: 0 2px;
  }

  .waiting-text {
    color: #f59e0b;
    font-weight: 500;
  }

  .available-text {
    color: #10b981;
    font-weight: 500;
  }

  .error-text {
    color: #ef4444;
    font-weight: 500;
  }

  .no-selection {
    color: #94a3b8;
    font-style: italic;
  }

  /* 集群资源信息样式 */
  .cluster-resource-info {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid rgba(71, 85, 105, 0.2);
  }

  .resource-summary {
    color: #94a3b8;
    font-size: 12px;
    line-height: 1.4;
  }

  .resource-item {
    color: #cbd5e1;
    font-weight: 500;
    margin: 0 4px;
    padding: 1px 4px;
    background: rgba(71, 85, 105, 0.2);
    border-radius: 3px;
  }

  .detail-button {
    background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
    border: none;
    border-radius: 6px;
    color: #ffffff;
    font-weight: 500;
    transition: all 0.3s ease;
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
  }

  .detail-button:hover:not(:disabled) {
    background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(59, 130, 246, 0.4);
  }

  .detail-button:disabled {
    background: rgba(71, 85, 105, 0.3);
    color: rgba(148, 163, 184, 0.5);
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .resource-tips {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .tips-content {
      width: 100%;
    }

    .detail-button {
      width: 100%;
    }

    .tips-text {
      font-size: 13px;
    }
  }

  @media (max-width: 480px) {
    .resource-status-section {
      padding: 12px;
      margin-top: 16px;
    }

    .tips-text {
      font-size: 12px;
    }

    .highlight {
      padding: 1px 4px;
      font-size: 11px;
    }
  }
</style>
