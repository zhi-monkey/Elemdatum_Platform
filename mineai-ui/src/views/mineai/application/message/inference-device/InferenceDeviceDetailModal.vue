<template>
  <Modal
    v-model:visible="innerVisible"
    title="模型接收地址详情"
    :footer="null"
    width="760px"
    :body-style="{ padding: '20px 24px' }"
    @cancel="handleClose"
  >
    <div class="detail-content">
      <Descriptions :column="{ xxl: 2, xl: 2, lg: 2, md: 2, sm: 1, xs: 1 }" bordered size="middle">
        <DescriptionsItem label="地址ID">{{ displayValue(record?.id) }}</DescriptionsItem>
        <DescriptionsItem label="地址名称">{{ displayValue(record?.deviceName) }}</DescriptionsItem>
        <DescriptionsItem label="接收地址IP">{{
          displayValue(record?.inferenceIp)
        }}</DescriptionsItem>
        <DescriptionsItem label="端口号">{{
          displayValue(record?.inferencePort)
        }}</DescriptionsItem>
        <DescriptionsItem label="地址状态">{{
          displayValue(record?.deviceStatus)
        }}</DescriptionsItem>
        <DescriptionsItem label="加载算法应用">
          {{ displayValue(record?.loadedAlgorithmApp) }}
        </DescriptionsItem>
        <DescriptionsItem label="创建人">{{ displayValue(record?.createdBy) }}</DescriptionsItem>
        <DescriptionsItem label="创建时间">{{ displayValue(record?.createdAt) }}</DescriptionsItem>
        <DescriptionsItem label="授权码">{{ authCodeDisplayText }}</DescriptionsItem>
        <DescriptionsItem label="备注信息" :span="2">{{
          displayValue(record?.remark)
        }}</DescriptionsItem>
      </Descriptions>
    </div>
  </Modal>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { Descriptions, DescriptionsItem, Modal } from 'ant-design-vue';
  import type { InferenceDeviceItem } from './api';

  const props = defineProps<{ visible: boolean; record: InferenceDeviceItem | null }>();
  const emit = defineEmits<{
    (e: 'update:visible', value: boolean): void;
  }>();

  const innerVisible = computed({
    get: () => props.visible,
    set: (value: boolean) => emit('update:visible', value),
  });

  const authCodeDisplayText = computed(() => {
    const authCode = props.record?.authCode;
    return authCode && String(authCode).trim() ? '已设置' : '--';
  });

  const displayValue = (value: unknown) => {
    if (value === null || value === undefined) {
      return '--';
    }
    const text = String(value).trim();
    return text ? text : '--';
  };

  const handleClose = () => {
    emit('update:visible', false);
  };
</script>

<style scoped lang="less">
  .detail-content {
    padding: 4px;
  }

  :deep(.ant-descriptions-item-label) {
    width: 140px;
    //padding: 14px 16px !important;
    text-align: left;
    white-space: nowrap;
  }

  :deep(.ant-descriptions-item-content) {
    //padding: 14px 16px !important;
    text-align: left;
    word-break: break-word;
  }
</style>
