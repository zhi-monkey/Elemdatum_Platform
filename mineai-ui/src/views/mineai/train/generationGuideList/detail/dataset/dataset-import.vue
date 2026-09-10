<template>
  <PageWrapper title="数据导入" style="padding-bottom: 30px">
    <template #extra>
      <div style="display: flex; gap: 20px">
        <a-button type="primary" @click="emit('next')" :disabled="!canProceed"> 下一步 </a-button>
      </div>
    </template>
    <template #footer>
      <ARow :gutter="24">
        <ACol :span="14">
          <ImportPanel
            :createdDatasetId="props.createdDatasetId"
            @dataset-created="handleDatasetCreated"
            :model-generation-id="props.modelGenerationId"
            :bound-versions="props.boundVersions"
            :annotate-type="props.annotateType"
            :annotation-format="props.annotationFormat"
            @bind-success="handleBindSuccess"
          />
        </ACol>
        <ACol :span="10">
          <!-- 新增：模型生成任务总览组件 -->
          <ModelGenerationOverview
            ref="modelGenerationOverviewRef"
            :model-generation-id="props.modelGenerationId"
            :bound-versions="props.boundVersions"
            :total-image-count="totalImageCount"
          />

          <DatasetStatus
            :createdDatasetId="props.createdDatasetId"
            v-model:status="datasetStatus"
            v-model:total-image-count="totalImageCount"
            v-model:total-video-count="totalVideoCount"
          />

          <!-- 新增：已绑定数据集版本组件 -->
          <BoundVersions :bound-versions="props.boundVersions" />
        </ACol>
      </ARow>
    </template>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { Button as AButton, Col as ACol, Row as ARow } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import ImportPanel from '/@/views/mineai/train/generationGuideList/detail/dataset/component/ImportPanel.vue';
  import DatasetStatus from '/@/views/mineai/train/generationGuideList/detail/dataset/component/DatasetStatus.vue';
  import BoundVersions from '/@/views/mineai/train/generationGuideList/detail/dataset/component/BoundVersions.vue';
  import ModelGenerationOverview from '/@/views/mineai/train/generationGuideList/detail/dataset/component/ModelGenerationOverview.vue';
  import { ref, computed } from 'vue';

  const props = defineProps<{
    createdDatasetId: number;
    modelGenerationId: number;
    boundVersions: object[];
    annotateType?: number; // 标注类型：102=目标检测，103=语义分割
    annotationFormat?: string; // 标注格式：YOLO, Segment-YOLO, COCO等
  }>();

  const emit = defineEmits(['next', 'prev', 'bind-success']);

  const datasetStatus = ref<number>();
  const datasetCreated = ref(false);
  const totalImageCount = ref(0);
  const totalVideoCount = ref(0);
  const modelGenerationOverviewRef = ref();

  // 处理数据集创建事件
  const handleDatasetCreated = () => {
    datasetCreated.value = true;
  };

  const handleBindSuccess = () => {
    emit('bind-success');
    modelGenerationOverviewRef.value.fetchOverviewData();
  };

  // 计算属性判断是否可以继续
  const canProceed = computed(() => {
    const hasImagesOrVideos = totalImageCount.value > 0 || totalVideoCount.value > 0;
    const hasBoundVersions = Array.isArray(props.boundVersions) && props.boundVersions.length > 0;

    return (
      props.createdDatasetId > 0 &&
      datasetStatus.value !== undefined &&
      (hasImagesOrVideos || hasBoundVersions)
    );
  });
</script>
