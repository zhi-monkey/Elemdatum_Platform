<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="请选择训练选项">
    <BasicForm @register="registerForm" />

    <!-- 资源状态提示区域 -->
    <div class="resource-status-section">
      <div class="resource-tips">
        <div class="tips-content">
          <div class="tips-icon">💡</div>
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
          :disabled="!selectedResourceInfo"
          class="detail-button"
        >
          查看集群详情
        </a-button>
      </div>
    </div>

    <template #footer>
      <AButton @click="closeModal">取消</AButton>
      <AButton type="primary" @click="handleSubmit" :loading="isSubmitting">确认</AButton>
    </template>
  </BasicModal>

  <!-- 集群状态模态框 -->
  <ClusterStatusModal
    v-model:visible="clusterStatusVisible"
    :selected-resource="selectedResourceInfo"
  />
</template>

<script setup lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref, computed } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { getModelJobWeights } from '/@/views/mineai/train/generationGuide/api/modelApi';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import ClusterStatusModal from '/@/views/mineai/train/generationCreate/ClusterStatusModal.vue';
  // 接收 Props
  defineProps<{
    modelGenerationId: number;
  }>();
  const { createMessage } = useMessage();
  const emit = defineEmits(['success']);
  let modelGenerationId = ref(0);
  const modelJobWeights: any = ref([]);

  const isSubmitting = ref(false);
  const hasPretrainedWeights = ref(false);

  // 资源状态相关
  const clusterStatusVisible = ref(false);
  const selectedResourceInfo = ref<any>(null);
  const clusterResourceStatus = ref({
    totalCpu: 0,
    availableCpu: 0,
    totalMemory: 0,
    availableMemory: 0,
    totalGpu: 0,
    availableGpu: 0,
    lastUpdateTime: null as Date | null,
  });
  const clusterNodes = ref<any[]>([]);

  const initialOptions = [
    { label: '官方模型', value: 'OFFICIAL' },
    { label: '从零开始', value: 'SCRATCH' },
  ];
  const fullOptions = [
    { label: '官方模型', value: 'OFFICIAL' },
    { label: '预训练模型', value: 'PRETRAINED' },
    { label: '从零开始', value: 'SCRATCH' },
  ];

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    // 1. 重置所有状态和字段
    await resetFields();
    isSubmitting.value = false;
    modelJobWeights.value = [];
    hasPretrainedWeights.value = false;

    // 2. 设置当前任务ID
    modelGenerationId.value = data.record.id;

    // 3. 获取预训练权重
    setModalProps({ confirmLoading: true, width: '1000px' });
    const response = await getModelJobWeights(data.record.id);
    setModalProps({ confirmLoading: false });

    // 4. 根据权重是否存在，更新表单
    const weightsAvailable = Object.keys(response).length > 0;
    hasPretrainedWeights.value = weightsAvailable;

    if (weightsAvailable) {
      // 如果有权重，使用完整选项
      await updateSchema({
        field: 'modelType',
        componentProps: { options: fullOptions },
      });
      // 并且处理权重数据以供 ApiSelect 使用
      modelJobWeights.value = Object.entries(response).map(([key, value]) => ({
        label: value as string,
        value: key,
      }));
    } else {
      // 如果没有权重，使用初始选项
      await updateSchema({
        field: 'modelType',
        componentProps: { options: initialOptions },
      });
    }

    // 5. 获取集群资源状态和选中的资源信息
    await fetchClusterResourceStatus();

    // 从外部传入的资源配置信息构建资源信息对象
    const hardwareParams = data.record.hardwareParams;
    const gpuMode = data.record.gpuMode || 'single-exclusive';
    const gpuCount = data.record.gpuCount || 1;

    if (hardwareParams) {
      selectedResourceInfo.value = {
        id: hardwareParams.id,
        name: hardwareParams.name,
        title: hardwareParams.title,
        description: hardwareParams.description,
        cpu: hardwareParams.cpus || 0,
        memory: hardwareParams.memory || '0G',
        gpuMemory: hardwareParams.gpuMemory || '0G',
        gpuCount: gpuCount,
        gpuMode: gpuMode,
      };
    } else {
      selectedResourceInfo.value = null;
    }
  });

  const [registerForm, { validate, resetFields, updateSchema }] = useForm({
    labelWidth: 160,
    schemas: [
      {
        field: 'modelType',
        label: '请选择训练方式', // 标签可以更通用一些
        component: 'RadioGroup',
        defaultValue: 'OFFICIAL',
        componentProps: {
          options: initialOptions, // 默认为初始选项
        },
        required: true,
      },
      {
        field: 'modelJobId',
        label: '请选择预训练权重',
        component: 'ApiSelect',
        componentProps: {
          // api 函数现在直接使用 ref 的值
          api: () => Promise.resolve(modelJobWeights.value),
          labelField: 'label',
          valueField: 'value',
        },
        ifShow: ({ values }) => {
          return hasPretrainedWeights.value && values.modelType === 'PRETRAINED';
        },
        required: true,
      },
    ],
    showActionButtonGroup: false,
  });

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

  // 获取GPU算力使用文本
  const getGpuComputeUsageText = () => {
    const resourceInfo = selectedResourceInfo.value;
    if (!resourceInfo) return '全部';

    const gpuMode = resourceInfo.gpuMode;

    if (gpuMode === 'single-exclusive' || gpuMode === 'multi-exclusive') {
      return '全部';
    } else if (gpuMode === 'single-shared' || gpuMode === 'multi-shared') {
      return '一半';
    }
    return '全部';
  };

  // 检查资源需求是否过高
  const isResourceRequirementTooHigh = computed(() => {
    const resourceInfo = selectedResourceInfo.value;
    if (!resourceInfo || !resourceInfo.cpu || !resourceInfo.memory) {
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
    const resourceInfo = selectedResourceInfo.value;
    if (!resourceInfo || !resourceInfo.cpu || !resourceInfo.memory) {
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

  // 判断是否需要等待
  const needsWaiting = computed(() => {
    // 如果资源需求过高，直接返回true
    if (isResourceRequirementTooHigh.value) {
      return true;
    }

    // 检查是否有单个节点能满足需求
    return !canSingleNodeSatisfy.value;
  });

  // 打开集群状态模态框
  const openClusterStatus = () => {
    clusterStatusVisible.value = true;
  };

  async function handleSubmit() {
    isSubmitting.value = true;
    try {
      const { modelJobId, modelType } = await validate();
      await maHttp
        .get(
          {
            url: 'modelJob/createJob',
            params: {
              modelGenerationId: modelGenerationId.value,
              modelWorkingMode: 1,
              modelType,
              // 只有当 modelType 为 PRETRAINED 时，modelJobId 才会有值
              modelJobId: modelType === 'PRETRAINED' ? modelJobId : undefined,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(() => {
          createMessage.success('训练作业创建成功');
          emit('success', modelGenerationId.value);
          closeModal();
        })
        .catch(() => {
          // catch 中通常不需要再 then 一个空函数
          createMessage.error('训练作业创建失败！');
        });
    } catch (error) {
      console.error('Validation or API call failed', error);
      // 如果校验失败，也应该停止 loading
    } finally {
      isSubmitting.value = false;
    }
  }
</script>

<style scoped>
  /* 资源状态提示区域样式 - 深色主题适配 */
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
    font-size: 18px;
    opacity: 0.8;
    margin-top: 1px;
    color: #a0aec0;
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

  /* 集群资源信息样式 */
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
    background: #4299e1;
    border: 1px solid #4299e1;
    border-radius: 6px;
    color: #ffffff;
    font-weight: 500;
    font-size: 12px;
    padding: 8px 16px;
    transition: all 0.2s ease;
    white-space: nowrap;
    flex-shrink: 0;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 32px;
  }

  .detail-button:hover:not(:disabled) {
    background: #3182ce;
    border-color: #3182ce;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(66, 153, 225, 0.3);
  }

  .detail-button:active:not(:disabled) {
    transform: translateY(0);
  }

  .detail-button:disabled {
    background: #4a5568;
    border-color: #4a5568;
    color: #a0aec0;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
    opacity: 0.6;
  }

  /* 调整表单样式以对齐 */
  :deep(.ant-form-item-label) {
    padding-left: 0 !important;
  }

  :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  :deep(.ant-radio-group) {
    margin-left: 0;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .resource-status-section {
      width: 500px;
      margin: 16px auto 0 auto;
    }

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
      text-align: center;
    }

    .tips-text {
      font-size: 12px;
    }
  }

  @media (max-width: 480px) {
    .resource-status-section {
      width: 350px;
      margin: 16px auto 0 auto;
      padding: 12px;
    }

    .tips-text {
      font-size: 11px;
    }

    .highlight {
      padding: 1px 4px;
      font-size: 11px;
    }

    .resource-summary {
      font-size: 11px;
    }

    .detail-button {
      font-size: 11px;
      padding: 6px 12px;
      min-height: 28px;
    }
  }
</style>
