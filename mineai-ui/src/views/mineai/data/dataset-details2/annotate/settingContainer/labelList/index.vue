<template>
  <div class="mb-10">
    <div class="label-header">
      <el-form-item v-show="showLabel" :label="labelsTitle" class="label-title" />
    </div>
    <!-- 优化后的标签列表 - 自适应布局 -->
    <div class="label-list-container">
      <div class="label-flex-wrapper">
        <div
          v-for="(item, index) in _state.labelData"
          :key="item.id"
          class="label-item"
          :class="{ 'active-label': item.id === _state.lastSelectedLabel }"
          :title="item.name"
          :style="getItemStyle(item)"
          @click="(event) => handleEditAnnotation(item, event)"
        >
          <span class="label-index">{{ index + 1 }}</span>
          <span class="label-name">{{ item.name }}</span>
          <Edit
            v-if="!item.labelGroupId"
            class="edit-icon"
            :getStyle="getStyle"
            :item="item"
            @handle-ok="handleEditLabel"
            @click.stop
          />
        </div>
        <div class="label-item-slot" v-if="!props.actionDisabled">
          <slot name="addLabel"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { computed, reactive, ref, watch } from 'vue';
  import { chroma, colorByLuminance, replace } from '/@/utils/dubhe';
  import Edit from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/labelList/edit.vue';

  export default {
    name: 'LabelList',
    components: {
      Edit,
    },
    props: {
      labels: {
        type: Array,
        default: () => [],
      },
      currentAnnotationId: {
        type: String,
        default: undefined,
      },
      editLabel: Function,
      annotations: Array,
      annotationType: String,
      updateState: Function,
      getColorLabel: Function,
      findRowIndex: Function,
      actionDisabled: Boolean,
      state: Object,
    },
    setup(props) {
      const {
        annotations: rawAnnotations,
        updateState,
        getColorLabel,
        findRowIndex,
        editLabel,
        annotationType,
      } = props;
      const searchRef = ref(null);

      // 主题配置：背景色，支持亮色/暗色主题切换
      // 将来切换主题时，只需修改这个值即可
      const themeConfig = reactive({
        backgroundColor: '#181d31', // 暗色主题背景
        // backgroundColor: '#f5f5f5', // 亮色主题背景（备用）
      });

      const _state = reactive({
        ...props.state,
        annotations: rawAnnotations,
        labelData: props.labels,
        currentAnnotationId: props.currentAnnotationId,
      });

      // 根据亮度来决定颜色
      const getStyle = (item) => {
        const color = colorByLuminance(item.color);
        return {
          color,
        };
      };

      // 根据背景色计算合适的文字颜色
      const getTextColorForBackground = (bgColor) => {
        const luminance = chroma(bgColor).luminance();
        // 深色背景使用浅色文字，浅色背景使用深色文字
        return luminance < 0.5 ? '#fff' : '#333';
      };

      const getItemStyle = (item) => {
        const isActive = item.id === _state.lastSelectedLabel;

        // 选中状态：根据标签颜色智能计算文字颜色（标签色作为背景）
        // 未选中状态：根据容器背景色智能计算文字颜色
        const textColor = isActive
          ? colorByLuminance(item.color) // 标签色作为背景时的文字色
          : getTextColorForBackground(themeConfig.backgroundColor); // 容器背景色下的文字色

        return {
          '--label-color': item.color,
          '--label-color-light': `${item.color}1a`,
          '--text-color': textColor,
        };
      };

      // 查询分类标签
      const handleSearch = (label) => {
        if (label) {
          _state.labelData = props.labels.filter((d) => d.name.includes(label));
        } else {
          _state.labelData = props.labels;
        }
      };

      const labelsTitle = computed(() => {
        return `全部标签(${props.labels.length})`;
      });

      const showLabel = computed(() => {
        if (!searchRef.value) return true;
        return !searchRef.value._state.open;
      });

      const handleEditAnnotation = (item, event) => {
        if (event.target.closest('.edit-icon')) return;

        updateState({
          lastSelectedLabel: item.id,
        });

        if (_state.currentAnnotationId) {
          const updateIndex = findRowIndex(_state.currentAnnotationId);
          if (updateIndex > -1) {
            const curItem = props.annotations[updateIndex];
            const nextItem = {
              ...curItem,
              data: {
                ...curItem.data,
                categoryId: item.id,
                color: getColorLabel(item.id),
              },
            };
            const updateList = replace(props.annotations, updateIndex, nextItem);
            updateState({
              [annotationType]: updateList,
            });
          }
        }
      };

      const handleEditLabel = (field, item) => {
        editLabel(item.id, field);
      };

      watch(
        () => props.labels,
        (next) => {
          _state.labelData = next;
        },
      );

      watch(
        () => props.currentAnnotationId,
        (next) => {
          _state.currentAnnotationId = next;
        },
      );

      watch(
        () => props.state.lastSelectedLabel,
        (next) => {
          _state.lastSelectedLabel = next;
        },
      );

      return {
        _state,
        searchRef,
        labelsTitle,
        handleEditAnnotation,
        handleEditLabel,
        getStyle,
        getItemStyle,
        showLabel,
        handleSearch,
        props,
      };
    },
  };
</script>

<style lang="scss" scoped>
  // 标签头部布局
  .label-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    padding: 0 4px;

    .label-title {
      flex: 1;
      padding: 0;
      margin-bottom: 0;
    }
  }

  // 容器样式
  .label-list-container {
    max-height: 200px;
    overflow: auto;
    padding: 4px;
  }

  // Flex 布局包裹器 - 自动换行
  .label-flex-wrapper {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: flex-start;
  }

  // 标签项 - 自适应宽度
  .label-item {
    display: inline-flex;
    align-items: center;
    min-width: 80px; // 最小宽度，确保即使很短的标签也有合适的点击区域
    max-width: calc(50% - 4px); // 最大宽度不超过50%，确保每行至少2个（减去gap的一半）
    flex-shrink: 0; // 防止被压缩
    padding: 6px 28px 6px 8px; // 右侧留出编辑图标的空间
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;

    // 未选中：边框使用标签颜色，文字使用智能颜色（确保可读性）
    background: transparent;
    border: 2px solid var(--label-color);
    color: var(--text-color);

    // 未选中状态下的文字颜色
    .label-index,
    .label-name {
      color: var(--text-color);
    }

    // 悬停效果
    &:hover:not(.active-label) {
      background: var(--label-color-light);

      .edit-icon {
        opacity: 1;
      }
    }
  }

  // 插槽容器 - 与标签项对齐
  .label-item-slot {
    display: inline-flex;
    align-items: center;
    min-width: 80px;
    max-width: calc(50% - 4px); // 与标签项保持一致
    flex-shrink: 0;
  }

  // 选中状态 - 纯色背景+智能文字颜色
  .active-label {
    background: var(--label-color);
    border: 2px solid var(--label-color);
    color: var(--text-color); // 使用智能计算的文字颜色

    .label-index {
      color: var(--text-color);
      opacity: 0.9;
    }

    .label-name {
      color: var(--text-color);
    }

    .edit-icon {
      color: var(--text-color);
      opacity: 0.8;
    }

    &:hover .edit-icon {
      opacity: 1;
    }
  }

  // 标签序号 - 极简样式
  .label-index {
    flex-shrink: 0;
    font-size: 12px;
    opacity: 0.7;
    min-width: 16px;
    margin-right: 6px;
    text-align: center;
  }

  // 标签名称 - 自适应显示
  .label-name {
    flex: 1 1 auto; // 允许增长和收缩
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    line-height: 1.4;
    min-width: 0; // 允许文本收缩
  }

  // 编辑图标 - 精简
  .edit-icon {
    position: absolute;
    right: 6px;
    top: 50%;
    transform: translateY(-50%);
    opacity: 0;
    transition: opacity 0.2s ease;
    padding: 2px;
    font-size: 14px;

    &:hover {
      opacity: 1 !important;
    }
  }

  // 响应式优化 - 小屏幕下的适配
  @media (max-width: 1366px) {
    .label-item {
      min-width: 70px;
      padding: 5px 24px 5px 6px;
      font-size: 12px;

      .label-index {
        font-size: 11px;
        min-width: 14px;
        margin-right: 4px;
      }

      .label-name {
        font-size: 12px;
      }

      .edit-icon {
        right: 4px;
        font-size: 12px;
      }
    }
  }

  @media (max-width: 1024px) {
    .label-item {
      min-width: 60px;
    }
  }
</style>
