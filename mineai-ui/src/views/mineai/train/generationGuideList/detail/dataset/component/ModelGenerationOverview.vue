<template>
  <ACard style="margin-bottom: 20px; padding: 20px; background: transparent !important">
    <template #title>
      <h2 style="font-size: 18px; margin: 0">引导式数据集概览</h2>
    </template>
    <a-skeleton :loading="isLoading" active :paragraph="{ rows: 3 }">
      <ARow :gutter="16" style="margin-bottom: 16px">
        <ACol :span="8">
          <AStatistic
            title="绑定数据集版本数"
            :value="overviewData?.boundVersionsCount || 0"
            :value-style="{ color: '#1890ff', fontSize: '18px' }"
            suffix="个"
          />
        </ACol>
        <ACol :span="8">
          <AStatistic
            title="数据集图片总数"
            :value="totalDatasetImages"
            :value-style="{ color: '#52c41a', fontSize: '18px' }"
            suffix="张"
          />
        </ACol>
        <ACol :span="8">
          <AStatistic
            title="标注数量"
            :value="totalLabelsCount"
            :value-style="{ color: '#722ed1', fontSize: '18px' }"
            suffix="个"
          />
        </ACol>
      </ARow>

      <!-- 标签展示区域 -->
      <div v-if="overviewData?.labels && overviewData.labels.length > 0" class="labels-section">
        <div class="labels-header">
          <span class="labels-title">算法包标签：</span>
          <AButton
            v-if="overviewData.labels.length > maxVisibleLabels"
            type="link"
            size="small"
            @click="showAllLabels = !showAllLabels"
            style="padding: 0; height: auto; margin-left: 8px"
          >
            {{ showAllLabels ? '收起' : `展开全部(${overviewData.labels.length})` }}
          </AButton>
        </div>

        <div class="labels-container" :class="{ expanded: showAllLabels }">
          <ATag
            v-for="(label, index) in visibleLabels"
            :key="label.id || index"
            :color="getLabelColor(index)"
            class="label-tag"
          >
            {{ index }}.{{ label.name || label.labelName || `标签${index + 1}` }}
          </ATag>

          <!-- 溢出指示器 -->
          <ATag
            v-if="!showAllLabels && overviewData.labels.length > maxVisibleLabels"
            class="overflow-indicator"
            @click="showAllLabels = true"
          >
            +{{ overviewData.labels.length - maxVisibleLabels }}
          </ATag>
        </div>
      </div>

      <!-- 无标签提示 -->
      <div v-else-if="!isLoading" class="no-labels">
        <AEmpty
          description="暂无算法包标签"
          :image="AEmpty.PRESENTED_IMAGE_SIMPLE"
          style="margin: 16px 0"
        />
      </div>
    </a-skeleton>
  </ACard>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, watch, unref, toRaw } from 'vue';
  import {
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Skeleton as ASkeleton,
    Statistic as AStatistic,
    Tag as ATag,
    Button as AButton,
    Empty as AEmpty,
  } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  interface LabelDTO {
    id?: number;
    name?: string;
    labelName?: string;
  }

  interface ModelGenerationOverviewDTO {
    labels: LabelDTO[];
    boundVersionsCount: number;
  }

  const props = defineProps<{
    modelGenerationId: number;
    boundVersions: any[];
    totalImageCount: number;
  }>();

  const getModelGenerationOverview = (modelGenerationId: number) => {
    return maHttp.get(
      {
        url: 'modelGeneration/getModelGenerationOverview',
        params: { modelGenerationId },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  };

  const isLoading = ref(true);
  const overviewData = ref<ModelGenerationOverviewDTO | null>(null);
  const showAllLabels = ref(false);
  const maxVisibleLabels = 8; // 最多显示8个标签，超出则折叠

  // 计算数据集图片总数（DatasetStatus中的数量 + 绑定版本中的数量）
  const totalDatasetImages = computed(() => {
    let boundVersionsImageCount = 0;

    if (props.boundVersions && props.boundVersions.length > 0) {
      props.boundVersions.forEach((item) => {
        if (item.datasetVersions && Array.isArray(item.datasetVersions)) {
          item.datasetVersions.forEach((version) => {
            boundVersionsImageCount += version.importableImageCount || 0;
          });
        }
      });
    }

    return boundVersionsImageCount + props.totalImageCount;
  });

  const totalLabelsCount = computed(() => {
    let labelCount = 0;
    if (props.boundVersions && props.boundVersions.length > 0) {
      props.boundVersions.forEach((item) => {
        if (item.datasetVersions && Array.isArray(item.datasetVersions)) {
          item.datasetVersions.forEach((version) => {
            labelCount += Object.values(version.labelCountMap).reduce(
              (acc, count) => acc + count,
              0,
            );
          });
        }
      });
    }
    return labelCount;
  });

  // 根据是否展开显示不同数量的标签
  const visibleLabels = computed(() => {
    if (!overviewData.value?.labels) return [];

    if (showAllLabels.value) {
      return overviewData.value.labels;
    }

    return overviewData.value.labels.slice(0, maxVisibleLabels);
  });

  // 获取标签颜色（循环使用预定义颜色）
  const getLabelColor = (index: number) => {
    const colors = [
      'blue',
      'green',
      'orange',
      'red',
      'purple',
      'cyan',
      'magenta',
      'volcano',
      'gold',
      'lime',
    ];
    return colors[index % colors.length];
  };

  // 获取总览数据
  const fetchOverviewData = async () => {
    if (!props.modelGenerationId) return;

    try {
      isLoading.value = true;
      const response = await getModelGenerationOverview(props.modelGenerationId);
      overviewData.value = response.data || response;
    } catch (error) {
      console.error('获取模型生成任务总览失败:', error);
      overviewData.value = null;
    } finally {
      isLoading.value = false;
    }
  };

  // 监听modelGenerationId变化
  watch(
    () => props.modelGenerationId,
    (newId) => {
      if (newId) {
        fetchOverviewData();
      }
    },
    { immediate: true },
  );

  defineExpose({
    fetchOverviewData,
  });

  onMounted(() => {
    fetchOverviewData();
  });
</script>

<style scoped lang="scss">
  .labels-section {
    border-top: 1px solid #545454;
    padding-top: 16px;

    .labels-header {
      display: flex;
      align-items: center;
      margin-bottom: 12px;

      .labels-title {
        font-weight: 500;
        color: #a9a9a9;
        font-size: 14px;
      }
    }

    .labels-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      max-height: 80px;
      overflow: hidden;
      transition: all 0.3s ease;

      &.expanded {
        max-height: none;
      }

      .label-tag {
        font-size: 12px;
        padding: 4px 8px;
        border-radius: 4px;
        margin: 0;
        font-weight: 500;
        cursor: default;
        border: 1px solid transparent;
        transition: all 0.2s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
      }

      .overflow-indicator {
        background: #f5f5f5;
        color: #8c8c8c;
        border: 1px dashed #d9d9d9;
        cursor: pointer;
        font-size: 12px;
        padding: 4px 8px;
        border-radius: 4px;
        margin: 0;
        transition: all 0.2s ease;

        &:hover {
          background: #e6f7ff;
          color: #1890ff;
          border-color: #1890ff;
          transform: translateY(-1px);
        }
      }
    }
  }

  .no-labels {
    border-top: 1px solid #f0f0f0;
    padding-top: 16px;
    text-align: center;
  }

  /* 响应式处理 */
  @media (max-width: 768px) {
    .labels-container {
      max-height: 120px;
    }

    :deep(.ant-statistic-title) {
      font-size: 12px;
    }

    :deep(.ant-statistic-content-value) {
      font-size: 16px !important;
    }
  }

  /* 自定义滚动条（如果需要） */
  .labels-container::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }

  .labels-container::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 2px;
  }

  .labels-container::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 2px;
  }

  .labels-container::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
</style>
