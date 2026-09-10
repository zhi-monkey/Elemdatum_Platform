<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="确认启动自迭代任务">
    <div class="resource-status-section">
      <div class="resource-tips">
        <div class="tips-content">
          <div class="tips-icon">i</div>
          <div class="tips-text">
            <span v-if="selectedResourceInfo">
              当前选择将占用
              <span class="highlight">{{ selectedResourceInfo.gpuCount || 1 }}</span>
              张显卡的
              <span class="highlight">{{ getGpuComputeUsageText() }}</span>
              算力，
              <span class="highlight">{{ selectedResourceInfo.cpu || 0 }}</span>
              核CPU，
              <span class="highlight">{{ selectedResourceInfo.memory || '0G' }}</span>
              内存，
              <span class="highlight">{{ selectedResourceInfo.gpuMemory || '0G' }}</span>
              显存，
              <span v-if="isResourceRequirementTooHigh" class="error-text"
                >当前资源需求过高，集群无法满足要求</span
              >
              <span v-else-if="needsWaiting" class="waiting-text"
                >预计需要等待其余任务释放资源</span
              >
              <span v-else class="available-text">资源充足可立即启动</span>
            </span>
            <span v-else class="no-selection">未选择资源配置</span>

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
          </div>
        </div>
        <AButton
          type="primary"
          size="small"
          class="detail-button"
          :disabled="!selectedResourceInfo"
          @click="openClusterStatus"
        >
          查看集群详情
        </AButton>
      </div>
    </div>

    <template #footer>
      <AButton @click="closeModal">取消</AButton>
      <AButton type="primary" :loading="isSubmitting" @click="handleSubmit">确认启动</AButton>
    </template>
  </BasicModal>

  <ClusterStatusModal
    v-model:visible="clusterStatusVisible"
    :selected-resource="selectedResourceInfo"
  />
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ClusterStatusModal from '/@/views/mineai/train/generationCreate/ClusterStatusModal.vue';
  import { getHardwareParamsById } from '/@/views/mineai/train/generationGuide/api/modelApi';
  import { startSelfIterationParentTask } from '../api/selfIteration';
  import { SelfIterationParentTask } from '../types/selfIteration';

  const emit = defineEmits<{
    (e: 'success', taskId: number): void;
  }>();

  const { createMessage } = useMessage();
  const taskId = ref(0);
  const isSubmitting = ref(false);
  const clusterStatusVisible = ref(false);
  const selectedResourceInfo = ref<any>(null);
  const clusterNodes = ref<any[]>([]);
  const clusterResourceStatus = ref({
    totalCpu: 0,
    availableCpu: 0,
    totalMemory: 0,
    availableMemory: 0,
    totalGpu: 0,
    availableGpu: 0,
    lastUpdateTime: null as Date | null,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    isSubmitting.value = false;
    taskId.value = Number(data?.record?.id || 0);
    selectedResourceInfo.value = null;
    resetClusterResourceStatus();
    setModalProps({ confirmLoading: true, width: '1000px' });

    try {
      await fetchClusterResourceStatus();
      selectedResourceInfo.value = await buildSelectedResourceInfo(data?.record);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  });

  async function buildSelectedResourceInfo(record?: SelfIterationParentTask) {
    if (!record) return null;
    const form = record.form;
    const hardwareParamsId = record.hardwareParamsId ?? form?.hardwareParamsId ?? null;
    let hardwareParams = (record as any).hardwareParams ?? null;

    if (!hardwareParams && hardwareParamsId) {
      hardwareParams = await getHardwareParamsById(hardwareParamsId);
    }
    if (!hardwareParams) return null;

    return {
      id: hardwareParams.id ?? hardwareParamsId,
      name: hardwareParams.name,
      title: hardwareParams.title ?? record.hardwareParamsTitle,
      description: hardwareParams.description,
      cpu: hardwareParams.cpus || 0,
      memory: hardwareParams.memory || '0G',
      gpuMemory: hardwareParams.gpuMemory || '0G',
      gpuCount: record.gpuCount ?? form?.gpuCount ?? 1,
      gpuMode: record.gpuMode ?? form?.gpuMode ?? 'single-exclusive',
    };
  }

  async function fetchClusterResourceStatus() {
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

      if (!response?.nodes) return;

      clusterNodes.value = response.nodes;
      let totalCpu = 0;
      let availableCpu = 0;
      let totalMemory = 0;
      let availableMemory = 0;
      let totalGpu = 0;
      let availableGpu = 0;

      const gpuNodes = response.nodes.filter((node) => node.gpus && node.gpus.length > 0);
      gpuNodes.forEach((node) => {
        totalCpu += node.cpuTotal || 0;
        availableCpu += (node.cpuTotal || 0) - (node.cpuUsed || 0);
        totalMemory += node.memoryTotal || 0;
        availableMemory += (node.memoryTotal || 0) - (node.memoryUsed || 0);
        totalGpu += node.gpus.length;
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
    } catch (error) {
      console.error('获取集群资源状态失败:', error);
      resetClusterResourceStatus();
    }
  }

  function resetClusterResourceStatus() {
    clusterNodes.value = [];
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

  function getGpuComputeUsageText() {
    const gpuMode = selectedResourceInfo.value?.gpuMode;
    if (gpuMode === 'single-shared' || gpuMode === 'multi-shared') return '一半';
    return '全部';
  }

  const isResourceRequirementTooHigh = computed(() => {
    const resourceInfo = selectedResourceInfo.value;
    if (!resourceInfo) return false;

    const cpuNeeded = Number(resourceInfo.cpu) || 0;
    const memoryNeeded = parseResourceNumber(resourceInfo.memory);
    const gpuNeeded = resourceInfo.gpuCount || 1;
    const { totalCpu, totalMemory, totalGpu } = clusterResourceStatus.value;

    return cpuNeeded > totalCpu || memoryNeeded > totalMemory || gpuNeeded > totalGpu;
  });

  const canSingleNodeSatisfy = computed(() => {
    const resourceInfo = selectedResourceInfo.value;
    if (!resourceInfo) return false;

    const cpuNeeded = Number(resourceInfo.cpu) || 0;
    const memoryNeeded = parseResourceNumber(resourceInfo.memory);
    const gpuMemoryNeeded = parseResourceNumber(resourceInfo.gpuMemory);
    const gpuNeeded = resourceInfo.gpuCount || 1;
    const gpuMode = resourceInfo.gpuMode;

    return clusterNodes.value.some((node) => {
      if (!node.gpus || node.gpus.length === 0) return false;

      const availableCpu = (node.cpuTotal || 0) - (node.cpuUsed || 0);
      const availableMemory = (node.memoryTotal || 0) - (node.memoryUsed || 0);
      let availableGpus = node.gpus.filter((gpu) => gpu.status === 'Running');

      if (gpuMode === 'single-exclusive' || gpuMode === 'multi-exclusive') {
        availableGpus = availableGpus.filter((gpu) => gpu.computeUsage === 0);
      } else {
        availableGpus = availableGpus.filter((gpu) => gpu.computeUsage < 50);
      }

      const validGpus = availableGpus.filter((gpu) => {
        if (!gpuMemoryNeeded) return true;
        return (gpu.memoryTotal || 0) - (gpu.memoryUsed || 0) >= gpuMemoryNeeded;
      });

      return (
        availableCpu >= cpuNeeded &&
        availableMemory >= memoryNeeded &&
        validGpus.length >= gpuNeeded
      );
    });
  });

  const needsWaiting = computed(() => {
    if (isResourceRequirementTooHigh.value) return true;
    return !canSingleNodeSatisfy.value;
  });

  function parseResourceNumber(value: string | number | undefined) {
    if (typeof value === 'number') return value;
    return Number(String(value || '0').replace(/[^\d.]/g, '')) || 0;
  }

  function openClusterStatus() {
    clusterStatusVisible.value = true;
  }

  async function handleSubmit() {
    if (!taskId.value) {
      createMessage.warning('未找到任务ID');
      return;
    }

    isSubmitting.value = true;
    try {
      const ok = await startSelfIterationParentTask(taskId.value);
      if (ok) {
        createMessage.success('任务已启动');
        emit('success', taskId.value);
        closeModal();
      } else {
        createMessage.error('启动失败');
      }
    } finally {
      isSubmitting.value = false;
    }
  }
</script>

<style scoped>
  .resource-status-section {
    margin: 20px auto 0 auto;
    padding: 16px;
    max-width: 90%;
    border: 1px solid #4a5568;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .resource-tips {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
  }

  .tips-content {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex: 1;
  }

  .tips-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 18px;
    width: 18px;
    height: 18px;
    margin-top: 1px;
    border: 1px solid #a0aec0;
    border-radius: 50%;
    color: #a0aec0;
    font-size: 12px;
    font-weight: 600;
    line-height: 1;
  }

  .tips-text {
    color: #e2e8f0;
    font-size: 13px;
    line-height: 1.5;
    flex: 1;
  }

  .highlight {
    color: #60a5fa;
    font-weight: 600;
    background: rgba(96, 165, 250, 0.15);
    padding: 2px 6px;
    border-radius: 4px;
    margin: 0 2px;
    border: 1px solid rgba(96, 165, 250, 0.3);
  }

  .waiting-text {
    color: #f6ad55;
    font-weight: 500;
  }

  .available-text {
    color: #68d391;
    font-weight: 500;
  }

  .error-text {
    color: #fc8181;
    font-weight: 500;
  }

  .no-selection {
    color: #a0aec0;
    font-style: italic;
  }

  .cluster-resource-info {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid #4a5568;
  }

  .resource-summary {
    color: #a0aec0;
    font-size: 12px;
    line-height: 1.4;
  }

  .resource-item {
    color: #e2e8f0;
    font-weight: 500;
    margin: 0 3px;
    padding: 2px 6px;
    background: rgba(74, 85, 104, 0.3);
    border-radius: 3px;
    border: 1px solid #4a5568;
  }

  .detail-button {
    white-space: nowrap;
    flex-shrink: 0;
  }

  @media (max-width: 768px) {
    .resource-status-section {
      max-width: 100%;
      margin: 16px auto 0 auto;
    }

    .resource-tips {
      flex-direction: column;
      gap: 12px;
    }

    .detail-button {
      width: 100%;
    }
  }
</style>
