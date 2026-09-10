<template>
  <div class="resource-cards-container">
    <!-- 配置卡片区域 -->
    <div class="content-wrapper">
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
    <div class="gpu-mode-section">
      <div class="gpu-mode-header">
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
              @change="handleGpuModeChange($event)"
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
      <div v-if="isMultiCardMode(presetGpuMode)" class="gpu-count-selector">
        <div class="gpu-count-wrapper">
          <span class="gpu-count-label">显卡数量:</span>
          <div class="gpu-count-input-wrapper">
            <a-input-number
              v-model:value="presetGpuCount"
              @change="handleGpuCountChange($event)"
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
  import { Card as ACard, InputNumber as AInputNumber, Button as AButton } from 'ant-design-vue';
  import { defineProps, defineEmits, computed, ref, onMounted } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import ClusterStatusModal from '/@/views/mineai/train/generationCreate/ClusterStatusModal.vue';

  const props = defineProps({
    selectedResourceId: {
      type: Number,
      default: null,
    },
  });

  const emit = defineEmits(['select-resource', 'gpu-mode-change', 'gpu-count-change']);

  const pageSize = 3; // 每页显示3个

  // 默认配置相关状态
  const presetConfigs = ref<any[]>([]);
  const presetCurrentPage = ref(1);
  const totalPresetResources = ref(0);
  const loading = ref(false);

  // GPU模式相关状态
  const presetGpuMode = ref('single-exclusive');
  const presetGpuCount = ref(2);

  // 集群状态相关状态
  const clusterStatusVisible = ref(false);

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

  // GPU模式选项配置
  const gpuModeOptions = [
    {
      value: 'single-exclusive',
      label: '单卡独占',
      description: '独占使用一张显卡',
    },
    {
      value: 'single-shared',
      label: '单卡共享',
      description: '共享使用一张显卡',
    },
    {
      value: 'multi-exclusive',
      label: '多卡独占',
      description: '独占使用多张显卡',
    },
    {
      value: 'multi-shared',
      label: '多卡共享',
      description: '共享使用多张显卡',
    },
  ];

  // 当前页显示的默认配置
  const currentPresetConfigs = computed(() => {
    return presetConfigs.value;
  });

  // 判断是否为多卡模式
  const isMultiCardMode = (mode: string) => {
    return mode === 'multi-exclusive' || mode === 'multi-shared';
  };

  // 获取选中的资源信息
  const getSelectedResourceInfo = computed(() => {
    if (!props.selectedResourceId) {
      return {};
    }

    // 从当前显示的配置中查找选中的资源
    const selectedConfig = presetConfigs.value.find(
      (config) => config.id === props.selectedResourceId,
    );

    if (!selectedConfig) {
      return {};
    }

    return {
      id: selectedConfig.id,
      name: selectedConfig.name,
      title: selectedConfig.title,
      description: selectedConfig.description,
      cpu: selectedConfig.cpus || 0,
      memory: selectedConfig.memory || '0G',
      gpuMemory: selectedConfig.gpuMemory || '0G',
      gpuCount: isMultiCardMode(presetGpuMode.value) ? presetGpuCount.value : 1,
      gpuMode: presetGpuMode.value,
    };
  });

  // GPU模式变化处理
  const handleGpuModeChange = (event: any) => {
    const mode = event.target.value;

    // 发送事件给父组件
    emit('gpu-mode-change', {
      tabType: 'preset',
      mode,
      gpuCount: presetGpuCount.value,
    });
  };

  // GPU数量变化处理
  const handleGpuCountChange = (count: number) => {
    // 发送事件给父组件
    emit('gpu-count-change', {
      tabType: 'preset',
      mode: presetGpuMode.value,
      gpuCount: count,
    });
  };

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

  // 获取默认配置列表
  const fetchPresetResources = async () => {
    loading.value = true;
    try {
      const response = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: presetCurrentPage.value - 1,
            size: pageSize,
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

      // 如果有配置且没有已选择的资源，自动选择第一个
      if (presetConfigs.value.length > 0 && !props.selectedResourceId) {
        const firstConfig = presetConfigs.value[0];
        if (firstConfig && firstConfig.id) {
          emit('select-resource', firstConfig.id);
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

  // 选择资源配置
  const selectResource = (id: number) => {
    emit('select-resource', id);
  };

  // 默认配置分页处理
  const handlePresetPageChange = (page: number) => {
    if (page < 1 || page > Math.ceil(totalPresetResources.value / pageSize)) return;
    presetCurrentPage.value = page;
    fetchPresetResources();
  };

  // 暴露刷新方法供父组件调用
  const refreshConfigurations = async () => {
    presetCurrentPage.value = 1;
    await fetchPresetResources();
  };

  // 暴露获取当前GPU配置的方法
  const getCurrentGpuConfig = () => {
    const mode = presetGpuMode.value;
    const gpuCount = presetGpuCount.value;

    return {
      tabType: 'preset',
      mode,
      gpuCount: isMultiCardMode(mode) ? gpuCount : 1,
    };
  };

  defineExpose({
    refreshConfigurations,
    getCurrentGpuConfig,
  });

  onMounted(async () => {
    // 获取集群资源状态
    await fetchClusterResourceStatus();

    // 获取默认配置
    fetchPresetResources();
  });
</script>

<style scoped>
  /* 主容器 */
  .resource-cards-container {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 28px 20px 0 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  /* 内容包装器 - 横向布局，导航按钮相对于卡片内容对齐 */
  .content-wrapper {
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
    width: 100%;
    max-width: 1200px;
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
    grid-column: 2;
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

  /* 左右导航按钮样式 */
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
    padding: 14px !important;
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

  /* GPU模式选择区域样式 */
  .gpu-mode-section {
    width: 100%;
    max-width: 1200px;
    padding: 14px 14px 0 14px;
    margin-bottom: 10px;
    background: transparent;
    border: none;
    border-radius: 0;
    backdrop-filter: none;
    box-shadow: none;
  }

  .gpu-mode-header {
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
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
    margin-bottom: 12px;
  }

  /* GPU选项一行布局 */
  .gpu-radio-group {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
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
    padding: 16px 12px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
    height: 75px;
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
    font-size: 14px;
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
    font-size: 12px;
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

  /* GPU数量选择器 */
  .gpu-count-selector {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-top: 10px;
    padding: 10px 14px;
    background: transparent;
    border: none;
    border-radius: 0;
    animation: slideInFromTop 0.4s ease-out;
    box-shadow: none;
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

  .gpu-count-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .gpu-count-label {
    color: #cbd5e1;
    font-size: 14px;
    font-weight: 600;
    white-space: nowrap;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .gpu-count-input-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .gpu-count-input {
    min-width: 120px;
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
    font-size: 14px;
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
    font-size: 14px;
    font-weight: 500;
    white-space: nowrap;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  /* 响应式布局优化 */
  @media (max-width: 1200px) {
    .resource-cards {
      gap: 14px;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }

    .gpu-radio-group {
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 10px;
    }

    .gpu-option {
      height: 70px;
      padding: 12px 8px;
    }

    .nav-button-left,
    .nav-button-right {
      width: 48px !important;
      height: 48px !important;
    }
  }

  @media (max-width: 768px) {
    .resource-cards-container {
      padding: 16px 0;
      gap: 18px;
    }

    .resource-cards {
      grid-template-columns: 1fr;
      gap: 12px;
      place-items: center;
    }

    .resource-card {
      max-width: 100%;
      width: 100%;
    }

    .content-wrapper {
      min-height: 180px;
      max-height: 200px;
      gap: 16px;
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

    .gpu-mode-section {
      padding: 16px;
    }

    .gpu-mode-title {
      font-size: 13px;
    }

    .gpu-radio-group {
      grid-template-columns: 1fr;
      gap: 8px;
    }

    .gpu-option {
      height: 60px;
      width: 100%;
    }

    .option-content {
      flex-direction: column;
      gap: 4px;
    }

    .option-text {
      font-size: 13px;
    }

    .option-desc {
      font-size: 11px;
    }

    .gpu-count-selector {
      flex-direction: column;
      gap: 12px;
      padding: 14px;
    }

    .gpu-count-wrapper {
      justify-content: center;
    }

    .gpu-count-input-wrapper {
      width: 100%;
      justify-content: center;
    }

    .gpu-count-input {
      width: 100px;
      min-width: unset;
    }
  }

  @media (max-width: 480px) {
    .resource-cards-container {
      padding: 12px 0;
      gap: 14px;
    }

    .resource-card {
      height: 140px;
    }

    .resource-card :deep(.ant-card-body) {
      padding-bottom: 16px;
    }

    .resource-cards {
      gap: 10px;
    }

    .content-wrapper {
      min-height: 160px;
      max-height: 180px;
      gap: 12px;
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
      padding: 12px;
    }

    .gpu-mode-title {
      font-size: 14px;
    }

    .gpu-option {
      height: 55px;
      padding: 8px;
    }

    .option-content {
      gap: 4px;
    }

    .option-text {
      font-size: 11px;
    }

    .option-desc {
      font-size: 9px;
    }

    .gpu-count-selector {
      padding: 12px;
    }

    .gpu-count-label {
      font-size: 12px;
    }
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
