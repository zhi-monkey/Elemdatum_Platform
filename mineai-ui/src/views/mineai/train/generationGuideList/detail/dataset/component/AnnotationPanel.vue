<!-- AnnotationPanel.vue -->
<template>
  <ACard
    style="padding: 20px; margin-bottom: 40px; height: 100%; background: transparent !important"
    title="标注方式"
    :tab-list="tabList"
    :active-tab-key="activeTabKey"
    @tabChange="handleTabChange"
  >
    <template #extra>
      <AButton
        v-if="activeTabKey === 'manual'"
        @click="goLabel"
        type="primary"
        style="margin-right: 20px"
        :disabled="isManualLabelDisabled"
      >
        前往手动标注
      </AButton>
      <AButton
        v-if="activeTabKey === 'auto'"
        type="primary"
        @click="handleAutoLabel"
        style="margin-right: 20px"
        :disabled="isAutoLabelDisabled"
      >
        前往自动标注
      </AButton>
    </template>

    <!-- 弹窗组件 -->
    <AutoLabelModal @register="registerAutoLabelModel" @success="handleAutoLabelSuccess" />

    <!-- 手动标注内容 -->
    <div v-if="activeTabKey === 'manual'">
      <ARow :gutter="24">
        <ACol :span="16">
          <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 18px">手动标注指南</h3>
          <p style="line-height: 1.6; font-size: 16px">
            1. 目标检测支持矩形框标注<br />
            2. 请在右侧标签处选择对应的的标签<br />
            3. 在图像上对应目标处画出矩形框进行标注<br />
            4. 同一图像可以标注不同标签的多个目标。例如多个人或者多个车辆<br />
          </p>
        </ACol>
      </ARow>
    </div>

    <!-- 自动标注内容 -->
    <div v-if="activeTabKey === 'auto'">
      <ARow :gutter="24">
        <ACol :span="12">
          <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 18px">自动标注说明</h3>
          <p style="line-height: 1.6; font-size: 16px; margin-bottom: 24px">
            1. 自动标注可以简化标注流程，借助已有模型，标注图像中的内容<br />
            2. 已有模型可以来源于之前的引导式训练，或训练镜像中自带的预训练模型<br />
            3. 自动标注后的结果需要由人工二次确认<br />
            4. 对自动标注的结果进行修改或增加，用于本轮训练<br />
          </p>
        </ACol>
      </ARow>
    </div>
  </ACard>
</template>

<script lang="ts" setup>
  import { computed, defineProps, onMounted, onUnmounted, ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import AutoLabelModal from '/@/views/mineai/train/generationGuideList/detail/dataset/component/autoLabel/auto-label-modal.vue';
  import { Button as AButton, Card as ACard, Col as ACol, Row as ARow } from 'ant-design-vue';
  import { queryFirstImg } from '/@/views/mineai/data/dataset-details2/api';
  import { router } from '/@/router';
  import { getDatasetCardInfo } from '/@/views/mineai/train/generationGuideList/detail/dataset/component/api';
  import { guidedTrainEventEmitter } from '/@/views/mineai/train/generationGuideList/detail/train/intervalBus';

  const [registerAutoLabelModel, { openModal }] = useModal();
  const props = defineProps<{
    createdDatasetId: number;
    applicationName?: string;
    deviceFirmware?: string;
  }>();

  const activeTabKey = ref<'manual' | 'auto'>('manual');
  const datasetName = ref('');
  const annotateType = ref(0);
  const currentStatus = ref<number | null>(null);
  const tabList = [
    { key: 'manual', tab: '手动标注' },
    { key: 'auto', tab: '自动标注' },
  ];

  // 监听状态变化
  const setupStatusListener = () => {
    const handler = ({ status }) => {
      currentStatus.value = status;
    };
    guidedTrainEventEmitter.on('datasetStatusChanged', handler);

    return () => {
      guidedTrainEventEmitter.off('datasetStatusChanged', handler);
    };
  };

  // 手动标注禁用条件
  const isManualLabelDisabled = computed(() => {
    return (
      currentStatus.value === 1 || // 上传中
      currentStatus.value === 403 || // 导入中
      currentStatus.value === 103 || // 自动标注中
      currentStatus.value === 302 // 抽帧中
    );
  });

  // 自动标注禁用条件
  const isAutoLabelDisabled = computed(() => {
    return (
      currentStatus.value === 1 || // 上传中
      currentStatus.value === 103 || // 自动标注中
      currentStatus.value === 403 || // 导入中
      currentStatus.value === 302 // 抽帧中
    );
  });

  // 处理自动标注成功
  const handleAutoLabelSuccess = () => {
    // 触发轮询状态检查
    guidedTrainEventEmitter.emit('datasetCardStatusPoll');
    // 更新状态为自动标注中(103)
    currentStatus.value = 103;
    guidedTrainEventEmitter.emit('datasetStatusChanged', { status: 103 });
    console.log('自动标注完成，内部处理逻辑');
  };

  const handleTabChange = (key: string) => {
    activeTabKey.value = key as 'manual' | 'auto';
  };

  const handleAutoLabel = () => {
    openModal(true, {
      id: props.createdDatasetId,
      annotateType: annotateType.value,
      applicationName: props.applicationName,
      deviceFirmware: props.deviceFirmware,
    });
  };

  onMounted(async () => {
    const cleanup = setupStatusListener();

    onUnmounted(() => {
      cleanup();
    });

    if (props.createdDatasetId !== 0) {
      try {
        const response = await getDatasetCardInfo(props.createdDatasetId);
        datasetName.value = response.name;
        annotateType.value = response.annotateType;
        currentStatus.value = response.status; // 初始化状态
      } catch (error) {
        console.error('数据加载失败:', error);
      }
    }
  });

  async function goLabel(imgId: number | null = null) {
    const prefix = annotateType.value === 103 ? 'segmentation' : 'annotate';
    const goImgId = imgId === null ? await queryFirstImg(props.createdDatasetId) : imgId;
    await router.push({
      path: `/maData/${prefix}/${props.createdDatasetId}/${datasetName.value}`,
      state: { imgId: goImgId },
    });
  }
</script>
