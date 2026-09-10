<template>
  <PageWrapper title="数据集增强" style="padding-bottom: 30px">
    <template #extra>
      <div style="display: flex; gap: 20px">
        <AButton @click="emit('prev')">上一步</AButton>
        <AButton type="primary" @click="emit('next')" disabled>完成数据标注</AButton>
      </div>
    </template>

    <template #footer>
      <ARow :gutter="24">
        <ACol :span="14">
          <DataEnhancePanel />
        </ACol>
        <ACol :span="10">
          <DatasetStatus :createdDatasetId="props.createdDatasetId" />
        </ACol>
      </ARow>
    </template>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { PageWrapper } from '/@/components/Page';
  import { useModal } from '/@/components/Modal';
  import { Button as AButton, Col as ACol, Row as ARow } from 'ant-design-vue';
  import DataEnhancePanel from '/@/views/mineai/train/generationGuideList/detail/dataset/component/DataEnhancePanel.vue';
  import DatasetStatus from '/@/views/mineai/train/generationGuideList/detail/dataset/component/DatasetStatus.vue';

  import { defineProps } from 'vue';

  const props = defineProps<{ createdDatasetId: number }>();

  const emit = defineEmits(['next', 'prev']);
  const [registerModal] = useModal();
</script>
