<!-- 父组件 -->
<template>
  <PageWrapper title="数据集标注" style="padding-bottom: 30px">
    <template #extra>
      <div style="display: flex; gap: 20px">
        <AButton @click="emit('prev')">上一步</AButton>
        <template v-if="showConfirmAnnotation">
          <AButton type="primary" @click="handleConfirmAnnotation"> 前往确认自动标注结果 </AButton>
        </template>
        <template v-else>
          <AButton type="primary" @click="handleFinish" :disabled="!canStartTraining">
            开始训练
          </AButton>
        </template>
      </div>
    </template>

    <template #footer>
      <ARow :gutter="24">
        <ACol :span="14">
          <AnnotationPanel
            :createdDatasetId="props.createdDatasetId"
            :application-name="props.applicationName"
            :device-firmware="props.deviceFirmware"
          />
        </ACol>
        <ACol :span="10">
          <DatasetStatus :createdDatasetId="props.createdDatasetId" v-model:status="status" />
        </ACol>
      </ARow>
    </template>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { PageWrapper } from '/@/components/Page';
  import { Button as AButton, Col as ACol, Row as ARow } from 'ant-design-vue';
  import AnnotationPanel from '/@/views/mineai/train/generationGuideList/detail/dataset/component/AnnotationPanel.vue';
  import DatasetStatus from '/@/views/mineai/train/generationGuideList/detail/dataset/component/DatasetStatus.vue';

  import { defineProps, ref, computed } from 'vue';

  const emit = defineEmits(['next', 'prev', 'finish', 'confirm-annotation']);
  const props = defineProps<{
    createdDatasetId: number;
    boundVersions?: any[];
    applicationName?: string;
    deviceFirmware?: string;
  }>();

  const status = ref<Number>();

  // 计算属性：是否存在绑定的数据集版本
  const hasBoundVersions = computed(() => {
    return Array.isArray(props.boundVersions) && props.boundVersions.length > 0;
  });

  // 计算属性，判断是否可以开始训练
  const canStartTraining = computed(() => {
    // 允许开始训练的状态码
    const allowedStatuses = [105]; // 105表示"已标注"状态

    // 情况1：正常流程，标注完成
    if (status.value && allowedStatuses.includes(status.value)) {
      return true;
    }

    // 情况2：主数据集可以是空，但如果已经绑定了数据集版本，也允许直接开始训练
    if (hasBoundVersions.value) {
      return true;
    }

    return false;
  });

  // 计算属性，判断是否显示确认自动标注结果按钮
  const showConfirmAnnotation = computed(() => {
    // 需要显示确认自动标注结果按钮的状态码
    const confirmStatuses = [104];
    return status.value && confirmStatuses.includes(status.value);
  });

  const handleConfirmAnnotation = () => {
    emit('confirm-annotation');
  };

  const handleFinish = () => {
    if (!canStartTraining.value) return;
    emit('finish', status.value);
  };
</script>
