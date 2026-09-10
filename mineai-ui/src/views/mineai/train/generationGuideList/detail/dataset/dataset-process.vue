<template>
  <div v-if="shouldShowTabs">
    <component
      :is="currentStep === 0 ? DatasetImport : DatasetLabeling"
      @next="handleNext"
      @prev="handlePrev"
      @finish="handleFinish"
      @confirm-annotation="handleConfirmAnnotation"
      @bind-success="handleBindSuccess"
      :createdDatasetId="props.createdDatasetId"
      :modelGenerationId="props.record.value.id"
      :boundVersions="props.boundVersions"
      :annotate-type="annotateTypeNumber"
      :annotation-format="computedAnnotationFormat"
      :application-name="modelApplicationContext.applicationName"
      :device-firmware="modelApplicationContext.deviceFirmware"
    />
    <a-modal
      v-model:visible="isHyperParamVisible"
      @ok="handleOK"
      width="60%"
      title="模型参数设置"
      :maskClosable="false"
      :confirm-loading="confirmLoading"
    >
      <HyperParameterForm ref="hyperFormRef" />
    </a-modal>
  </div>
  <div v-else>
    <a-alert
      message="数据处理部分已完成，请选择后续步骤查看"
      description="The data processing part is complete. Please select the next steps to view."
      type="info"
      show-icon
      style="width: 100%"
      v-if="props.currentPipelinePhase >= PipelinePhase.TRAINING"
    />
    <a-card
      v-if="props.currentPipelinePhase >= PipelinePhase.PUBLISHING"
      style="height: 15vh; padding-bottom: 4px"
    >
      <template #title><span style="font-size: 24px">数据集处理</span></template>
      <a-row :gutter="16">
        <!-- 左侧保存状态 -->
        <a-col :span="12" class="status-column">
          <div class="status-item">
            <span class="status-title" v-if="props.currentPipelinePhase < PipelinePhase.PUBLISHING"
              >数据集切分: 等待开始</span
            >
            <span
              class="status-title"
              v-if="props.currentPipelinePhase === PipelinePhase.PUBLISHING"
              >数据集保存: 进行中</span
            >
            <span class="status-title" v-else>数据集保存:已完成</span>
            <a-spin
              v-if="props.currentPipelinePhase === PipelinePhase.PUBLISHING"
              indicator:spinIndicator
            />
            <CheckCircleOutlined v-else :style="{ color: '#52c41a', fontSize: '24px' }" />
          </div>
        </a-col>

        <!-- 右侧切分状态 -->
        <a-col :span="12" class="status-column">
          <div class="status-item">
            <span class="status-title" v-if="props.currentPipelinePhase < PipelinePhase.SPLITTING"
              >数据集切分: 等待开始</span
            >
            <span class="status-title" v-if="props.currentPipelinePhase === PipelinePhase.SPLITTING"
              >数据集切分: 进行中</span
            >
            <span class="status-title" v-if="props.currentPipelinePhase >= PipelinePhase.TRAINING"
              >数据集切分: 已完成</span
            >
            <a-spin
              v-if="props.currentPipelinePhase === PipelinePhase.SPLITTING"
              indicator:spinIndicator
            />
            <CheckCircleOutlined
              v-else-if="props.currentPipelinePhase >= PipelinePhase.TRAINING"
              :style="{ color: '#52c41a', fontSize: '24px' }"
            />
          </div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>
<script setup lang="ts">
  import DatasetImport from '/@/views/mineai/train/generationGuideList/detail/dataset/dataset-import.vue';
  import DatasetLabeling from '/src/views/mineai/train/generationGuideList/detail/dataset/dataset-labeling.vue';
  import { defineProps, nextTick, ref, toRaw, unref, defineEmits, computed, h } from 'vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    Modal as AModal,
    Alert as AAlert,
    Spin as ASpin,
    Card as ACard,
    Row as ARow,
    Col as ACol,
  } from 'ant-design-vue';
  import HyperParameterForm from '/@/views/mineai/train/generationGuideList/HyperParameterForm.vue';
  import { useUserStore } from '/@/store/modules/user';
  import { startGuidedGenerationParams } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { PipelinePhase } from '/@/views/mineai/train/generationGuideList/detail/generationGuideDetail.data';
  import { CheckCircleOutlined, LoadingOutlined } from '@ant-design/icons-vue';
  import { router } from '/@/router';
  import { queryFirstImg } from '/@/views/mineai/data/dataset-details2/api';

  const { createMessage } = useMessage();
  const isHyperParamVisible = ref(false);
  const hyperFormRef = ref(null);

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const confirmLoading = ref(false);

  const props = defineProps<{
    createdDatasetId: number;
    record: object;
    currentPipelinePhase: number;
    boundVersions: object[];
  }>();

  const emit = defineEmits(['next', 'bind-success']);

  const currentStep = ref(0);
  const totalSteps = 2; // 总步骤数

  // 将字符串类型的 annotationType 转换为数字类型
  // "Detection" -> 102 (目标检测)
  // "Segmentation" -> 103 (语义分割)
  const getAnnotateTypeNumber = (
    annotationType: string | number | undefined,
  ): number | undefined => {
    if (!annotationType) return undefined;
    if (typeof annotationType === 'number') return annotationType;
    if (annotationType === 'Segmentation') return 103;
    if (annotationType === 'Detection') return 102;
    return undefined;
  };

  // 计算属性：获取数字类型的 annotateType
  const annotateTypeNumber = computed(() => {
    const record = props.record as any;
    const annotationType = record?.value?.annotationType;
    // 如果 annotationType 不存在，尝试从 trainDatasetStartAll 获取
    if (!annotationType && record?.value?.trainDatasetStartAll?.annotateType) {
      return record.value.trainDatasetStartAll.annotateType;
    }
    return getAnnotateTypeNumber(annotationType);
  });

  // 计算属性：根据 annotationType 和 annotationFormat 计算最终的格式
  const computedAnnotationFormat = computed(() => {
    const record = props.record as any;
    const annotationTypeValue = record?.value?.annotationType;
    const formatValue = record?.value?.annotationFormat;

    if (annotationTypeValue === 'Detection' && formatValue === 'YOLO') {
      return 'YOLO';
    } else if (annotationTypeValue === 'Detection' && formatValue === 'COCO') {
      return 'COCO';
    } else if (annotationTypeValue === 'Segmentation' && formatValue === 'YOLO') {
      return 'Segment-YOLO';
    } else if (annotationTypeValue === 'Segmentation' && formatValue === 'COCO') {
      return 'COCO';
    }
    return undefined;
  });

  const modelApplicationContext = computed(() => {
    const record = props.record as any;
    const modelApplication = record?.value?.modelApplication;
    const applicationName = modelApplication?.applicationName?.applicationName;
    const device = modelApplication?.device;
    const deviceFirmware =
      device?.deviceName && device?.firmwareVersion
        ? `${device.deviceName}-${device.firmwareVersion}`
        : undefined;

    return { applicationName, deviceFirmware };
  });

  const handleNext = () => {
    if (currentStep.value < totalSteps - 1) {
      currentStep.value++;
    }
  };

  const handleBindSuccess = async () => {
    emit('bind-success');
  };

  const handlePrev = () => {
    if (currentStep.value > 0) {
      currentStep.value--;
    }
  };

  const modelExplore = ref({});
  const handleFinish = async (status: Number) => {
    // 完成处理逻辑
    // 1. 检查数据集状态
    const hasBoundVersions = Array.isArray(props.boundVersions) && props.boundVersions.length > 0;

    // 通过首张图片是否存在来判断主数据集是否有图片
    let hasImages = false;
    try {
      const firstImgId = await queryFirstImg(props.createdDatasetId as any);
      // queryFirstImg 在有图片时返回图片ID（number），无图片时返回 {}
      hasImages = typeof firstImgId === 'number';
    } catch (e) {
      hasImages = false;
    }

    // 情况1：主数据集中存在图片时，始终要求标注完成（status === 105）
    if (hasImages && status !== 105) {
      return createMessage.warning('数据集尚未标注完成');
    }

    // 情况2：主数据集没有图片时，只有在存在绑定数据集版本时才允许跳过 105 状态
    if (!hasImages && !hasBoundVersions && status !== 105) {
      return createMessage.warning('数据集尚未标注完成');
    }

    isHyperParamVisible.value = true;

    await nextTick(async () => {
      if (hyperFormRef.value) {
        modelExplore.value = await hyperFormRef.value.getHyperParams(
          props.record.value.modelApplication.applicationName.applicationName,
          props.record.value.modelApplication.device.deviceName +
            '-' +
            props.record.value.modelApplication.device.firmwareVersion,
        );
      } else {
        console.error('HyperParameterForm component not mounted');
      }
    });
  };

  const handleConfirmAnnotation = async () => {
    const prefix =
      props.record.value.trainDatasetStartAll.annotateType === 103 ? 'segmentation' : 'annotate';

    const goImgId = await queryFirstImg(props.createdDatasetId);

    await router.push({
      path: `/maData/${prefix}/${props.createdDatasetId}/${props.record.value.trainDatasetStartAll.name}`,
      state: { imgId: goImgId },
    });
  };

  const handleOK = async () => {
    try {
      await hyperFormRef.value.validateForm();
    } catch (e) {
      return createMessage.warning('请完善表单');
    }
    confirmLoading.value = true;

    const trainHyperParameters = toRaw(unref(hyperFormRef.value.trainHyperParameters)).map(
      (item) => {
        return {
          [item.paramName]: item.value,
        };
      },
    );

    // 将训练超参数数组合并为对象，避免后面的同名参数覆盖前面的值
    const trainHyperParamsMerged: Record<string, any> = {};
    trainHyperParameters.forEach((paramObj) => {
      const [key, value] = Object.entries(paramObj)[0] || [];
      if (key && !(key in trainHyperParamsMerged)) {
        trainHyperParamsMerged[key] = value;
      }
    });
    const convertHyperParameters =
      hyperFormRef.value.convertHyperParameters.length > 0
        ? toRaw(unref(hyperFormRef.value.convertHyperParameters)).map((item) => {
            return {
              [item.paramName]: item.value,
            };
          })
        : null;

    // 将转换超参数数组合并为对象，同样只保留第一次出现的参数值
    const convertHyperParamsMerged: Record<string, any> | null = convertHyperParameters
      ? (() => {
          const merged: Record<string, any> = {};
          convertHyperParameters.forEach((paramObj) => {
            const [key, value] = Object.entries(paramObj)[0] || [];
            if (key && !(key in merged)) {
              merged[key] = value;
            }
          });
          return merged;
        })()
      : null;

    // 从 hyperFormRef 中获取量化参数
    const quantizationOn = unref(hyperFormRef.value.quantizationOn);
    const quantizationImageNum = unref(hyperFormRef.value.quantizationImageNum);
    const selectedResource = unref(hyperFormRef.value.selectedResourceId);
    const gpuMode = unref(hyperFormRef.value.gpuMode);
    const gpuCount = unref(hyperFormRef.value.gpuCount);
    // 创建量化参数对象
    const quantizationParams = {};
    if (quantizationOn.value) {
      quantizationParams[quantizationOn.paramName] = true;

      // 确保数量为数字类型
      const imageNum = parseInt(quantizationImageNum.value, 10);
      if (!isNaN(imageNum)) {
        quantizationParams[quantizationImageNum.paramName] = imageNum;
      }
    } else {
      quantizationParams[quantizationOn.paramName] = false;
    }

    let actualSplitSize = [];
    let splitSize = '';
    if (hyperFormRef.value && hyperFormRef.value.getSplitSize) {
      actualSplitSize = hyperFormRef.value.getSplitSize();
    }

    // 处理 splitSize，将数组转换为符合要求的字符串格式
    if (Array.isArray(actualSplitSize) && actualSplitSize.length === 2) {
      const trainRatio = actualSplitSize[0] / 10;
      const testRatio = (actualSplitSize[1] - actualSplitSize[0]) / 10;
      const valRatio = (100 - actualSplitSize[1]) / 10;
      splitSize = `${trainRatio}-${testRatio}-${valRatio}`;
    }

    const params = {
      modelGeneration: {
        ...toRaw(unref(props.record.value)),
        isGuided: true,
        dataSource: 0,
        userId: userData.id,
        modelExplore: toRaw(unref(modelExplore.value)),
        splitSize: splitSize,
        testDataset: props.record.value.testDataset?.id,
        trainDataset: props.record.value.trainDataset?.id,
      },
      trainHyperParams: trainHyperParamsMerged,
      // 如果有转换参数，就传，没有就传null
      modelConvert: convertHyperParameters
        ? {
            params: convertHyperParamsMerged || {},
          }
        : null,
      // 添加量化参数
      quantizationParams: Object.keys(quantizationParams).length > 0 ? quantizationParams : null,
      modelApplication: props.record.value.modelApplication,
      datasetName: props.record.value.trainDatasetStartAll.name,
      HardwareParamsID: selectedResource,
      gpuMode: gpuMode,
      gpuCount: gpuCount,
    };
    try {
      await startGuidedGenerationParams(params);
      isHyperParamVisible.value = false;
      hyperFormRef.value.resetForm();
      createMessage.success('创建训练任务成功');
      emit('next');
    } catch (e) {
      console.error(e);
      createMessage.error('创建任务失败');
    } finally {
      confirmLoading.value = false;
    }
  };

  // 条件判断：是否应该显示 Tabs
  const shouldShowTabs = computed(() => {
    //如果当前阶段不为标注就显示tab
    return props.currentPipelinePhase == PipelinePhase.ANNOTATING;
  });

  const spinIndicator = h(LoadingOutlined, {
    style: {
      fontSize: '30px',
    },
    spin: true,
  });
</script>

<style scoped lang="less">
  .status-column {
    display: flex;
    align-items: center;
    height: 100%;
  }

  .status-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }

  .status-title {
    font-weight: 500;
    //margin-right: auto;
    margin-left: 30px;
    font-size: 24px;
  }
</style>
