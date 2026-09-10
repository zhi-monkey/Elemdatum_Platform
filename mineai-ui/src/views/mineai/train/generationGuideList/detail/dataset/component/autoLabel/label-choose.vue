<template>
  <div class="label-selector-container">
    <!-- 搜索栏 -->
    <div class="search-header">
      <a-input-search
        v-model:value="searchText"
        placeholder="搜索标签名称..."
        allow-clear
        class="search-input"
      />
      <div class="action-buttons">
        <a-button type="link" size="small" @click="toggleSelectAll">
          {{ isAllSelected ? '取消全选' : '全选' }}
        </a-button>
        <a-tag color="blue" class="selected-count">
          已选择: {{ selectedLabels.length }} / {{ filteredLabels.length }}
        </a-tag>
      </div>
    </div>

    <a-divider style="margin: 8px 0" />

    <!-- 加载状态 -->
    <a-spin :spinning="loading" tip="加载标签数据中...">
      <!-- 空状态 -->
      <a-empty
        v-if="!loading && filteredLabels.length === 0"
        :description="searchText ? '未找到匹配的标签' : '暂无标签数据'"
        class="empty-state"
      />

      <!-- 标签网格 -->
      <div class="label-tags-container" v-else>
        <a-checkable-tag
          v-for="(label, index) in filteredLabels"
          :key="label.id || label.name"
          :checked="isLabelSelected(label.name)"
          :style="{
            backgroundColor: isLabelSelected(label.name) ? getLabelColor(index) : 'transparent',
            borderColor: getLabelColor(index),
            color: isLabelSelected(label.name) ? '#fff' : getLabelColor(index),
          }"
          class="label-tag"
          @change="(checked) => handleTagChange(label.name, checked)"
        >
          {{ label.name }}
        </a-checkable-tag>
      </div>
    </a-spin>

    <!-- 底部统计信息 -->
    <div class="footer-info" v-if="!loading && filteredLabels.length > 0">
      <a-text type="secondary" style="font-size: 12px">
        共 {{ labels.length }} 个标签
        <span v-if="searchText">，筛选出 {{ filteredLabels.length }} 个</span>
      </a-text>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, watch, onMounted } from 'vue';
  import {
    InputSearch as AInputSearch,
    Button as AButton,
    Divider as ADivider,
    Empty as AEmpty,
    Spin as ASpin,
    Tag as ATag,
    Typography as ATypography,
  } from 'ant-design-vue';
  import {
    getLabelsByModelGenerationIdApi,
    getLabelsIByImageUrlApiAndDatasetId,
  } from '/@/views/mineai/data/dataset-details2/api';

  const { Text: AText } = ATypography;

  const { CheckableTag: ACheckableTag } = ATag;

  // 定义标签接口
  interface Label {
    id: number | null;
    name: string;
    color?: string;
  }

  // 事件发射
  const emit = defineEmits<{
    'update:selected': [labels: string[]];
  }>();

  // Props
  const props = defineProps<{
    modelGenerationId: number;
    defaultSelected?: string[]; // 默认选中的标签名称
    autoLabelImageUrl: string;
    datasetId?: number;
  }>();

  // 暴露给父组件的方法
  defineExpose({
    getSelectedLabels,
    clearSelection,
    setSelectedLabels,
  });

  // 响应式数据
  const labels = ref<Label[]>([]);
  const selectedLabels = ref<string[]>([]);
  const searchText = ref('');
  const loading = ref(false);

  // 计算属性
  const filteredLabels = computed(() => {
    if (!labels.value.length) return [];

    if (!searchText.value.trim()) {
      return labels.value;
    }

    const searchLower = searchText.value.toLowerCase().trim();
    return labels.value.filter(
      (label) => label.name && label.name.toLowerCase().includes(searchLower),
    );
  });

  const isAllSelected = computed(() => {
    if (!filteredLabels.value.length) return false;
    return filteredLabels.value.every((label) => selectedLabels.value.includes(label.name));
  });

  // 定义高对比度颜色集合
  const PREDEFINED_COLORS = [
    // 'red',
    // 'blue',
    // 'pink',
    // 'orange',
    '#cccccc',
    // '#1890ff', // 蓝色
    // '#52c41a', // 绿色
    // '#fa541c', // 橙色
    // '#722ed1', // 紫色
    // '#eb2f96', // 粉色
    // '#faad14', // 黄色
    // '#13c2c2', // 青色
    // '#f5222d', // 红色
    // '#2f54eb', // 靛蓝
    // '#a0d911', // 青绿
    // '#fa8c16', // 橙黄
    // '#722ed1', // 深紫
  ];

  // 获取标签颜色
  const getLabelColor = (index: number) => {
    return PREDEFINED_COLORS[index % PREDEFINED_COLORS.length];
  };
  const fetchLabels = async () => {
    if (!props.modelGenerationId && !props.autoLabelImageUrl) return;

    try {
      loading.value = true;
      let data;
      if (props.modelGenerationId) {
        data = await getLabelsByModelGenerationIdApi(props.modelGenerationId);
      }
      if (props.autoLabelImageUrl) {
        data = await getLabelsIByImageUrlApiAndDatasetId(props.autoLabelImageUrl, props.datasetId);
      }

      if (Array.isArray(data)) {
        labels.value = data.filter((item) => item && item.name); // 过滤掉无效数据
      } else {
        labels.value = [];
        console.warn('获取的标签数据格式不正确:', data);
      }
    } catch (error) {
      console.error('获取标签数据失败:', error);
      labels.value = [];
    } finally {
      loading.value = false;
    }
  };

  const isLabelSelected = (labelName: string): boolean => {
    return selectedLabels.value.includes(labelName);
  };

  const handleTagChange = (labelName: string, checked: boolean) => {
    if (checked) {
      if (!selectedLabels.value.includes(labelName)) {
        selectedLabels.value.push(labelName);
      }
    } else {
      const index = selectedLabels.value.indexOf(labelName);
      if (index > -1) {
        selectedLabels.value.splice(index, 1);
      }
    }

    // 实时通知父组件选择状态变化
    emit('update:selected', [...selectedLabels.value]);
  };

  const toggleSelectAll = () => {
    if (isAllSelected.value) {
      // 取消全选 - 从已选中列表中移除当前筛选的标签
      const filteredNames = filteredLabels.value.map((label) => label.name);
      selectedLabels.value = selectedLabels.value.filter((name) => !filteredNames.includes(name));
    } else {
      // 全选 - 添加当前筛选的标签到已选中列表
      const filteredNames = filteredLabels.value.map((label) => label.name);
      const newSelections = filteredNames.filter((name) => !selectedLabels.value.includes(name));
      selectedLabels.value.push(...newSelections);
    }

    // 通知父组件选择状态变化
    emit('update:selected', [...selectedLabels.value]);
  };

  // 暴露给父组件的方法
  function getSelectedLabels(): string[] {
    return [...selectedLabels.value];
  }

  function clearSelection() {
    selectedLabels.value = [];
  }

  function setSelectedLabels(labelNames: string[]) {
    selectedLabels.value = [...labelNames];
  }

  // 监听器
  watch(
    () => props.modelGenerationId || props.autoLabelImageUrl,
    (newId) => {
      if (newId) {
        // 不重置选择状态，保持用户选择
        searchText.value = '';
        fetchLabels();
      }
    },
    { immediate: true },
  );

  // 设置默认选中
  watch(
    () => props.defaultSelected,
    (newDefault) => {
      if (newDefault && Array.isArray(newDefault)) {
        selectedLabels.value = [...newDefault];
      }
    },
    { immediate: true },
  );

  onMounted(() => {
    if (props.modelGenerationId || props.autoLabelImageUrl) {
      fetchLabels();
    }
  });
</script>

<style scoped lang="less">
  .label-selector-container {
    padding: 16px;
    border-radius: 6px;
    min-height: 200px;

    .search-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .search-input {
        flex: 1;
        max-width: 300px;
        margin-right: 16px;
      }

      .action-buttons {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;

        .selected-count {
          font-size: 12px;
          margin: 0;
        }
      }
    }

    .empty-state {
      padding: 40px 20px;
      text-align: center;
    }

    .label-tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      max-height: 350px;
      overflow-y: auto;
      border-radius: 6px;
      padding: 16px;

      .label-tag {
        font-size: 14px;
        padding: 6px 12px;
        border-radius: 5px;
        border-width: 1px;
        border-style: solid;
        cursor: pointer;
        transition: all 0.3s ease;
        user-select: none;
        font-weight: 500;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }

        &:active {
          transform: translateY(0);
        }
      }
    }

    .footer-info {
      margin-top: 8px;
      text-align: center;
      padding: 8px 0;
    }
  }

  // 滚动条样式
  .label-tags-container::-webkit-scrollbar {
    width: 6px;
  }

  .label-tags-container::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  .label-tags-container::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
  }

  .label-tags-container::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
</style>
