<template>
  <div class="card-selector-container">
    <a-spin :spinning="loading">
      <!-- 搜索框 - 仅在引导式模式下显示 -->
      <div v-if="type === 'guided'" class="search-container">
        <!-- 筛选类型下拉选择 -->
        <a-select
          v-model:value="searchType"
          size="small"
          style="width: 100px"
          @change="handleSearchTypeChange"
        >
          <a-select-option value="name">按名称</a-select-option>
          <a-select-option value="tag">按标签</a-select-option>
        </a-select>

        <!-- 搜索输入框 -->
        <a-input-search
          v-if="searchType === 'name'"
          v-model:value="searchText"
          placeholder="请输入训练任务名称"
          enter-button="搜索"
          size="small"
          allowClear
          @search="handleSearch"
          @clear="handleClear"
          class="search-input"
        />

        <a-select
          v-else
          v-model:value="searchText"
          placeholder="请选择标签名称"
          size="small"
          style="
            width: 232px;
            background-color: #2a2a2a;
            font-size: 14px;
            letter-spacing: 0.4px;
            font-weight: 300;
          "
          allowClear
          :loading="labelLoading"
          @dropdown-visible-change="handleLabelDropdown"
          :show-search="true"
        >
          <a-select-option v-for="label in allLabels" :key="label" :value="label">
            {{ label }}
          </a-select-option>
        </a-select>
        <a-button
          v-if="searchType === 'tag'"
          type="primary"
          style="font-weight: 300"
          size="small"
          class="search-btn"
          @click="handleSearch"
        >
          搜索
        </a-button>
      </div>
      <div v-else class="search-container">
        <a-select
          v-model:value="searchText"
          placeholder="请选择标签名称"
          size="small"
          style="
            width: 232px;
            background-color: #2a2a2a;
            font-size: 14px;
            letter-spacing: 0.4px;
            font-weight: 300;
          "
          allowClear
          :loading="labelLoading"
          :disabled="!props.requestParam"
          @dropdown-visible-change="handleLabelDropdownStandard"
          :show-search="true"
        >
          <a-select-option v-for="label in allLabelsStandard" :key="label" :value="label">
            {{ label }}
          </a-select-option>
        </a-select>
        <a-button
          type="primary"
          style="font-weight: 300"
          size="small"
          class="search-btn"
          @click="handleSearch"
        >
          搜索
        </a-button>
      </div>

      <!-- 卡片列表 -->
      <div v-if="cardList.length > 0" class="card-grid">
        <a-card
          v-for="card in cardList"
          :key="card.key"
          hoverable
          class="info-card"
          :class="{ selected: selectedCardKey === card.key }"
          @click="handleCardClick(card)"
        >
          <!-- 卡片头部：任务名称和选中标记 -->
          <div class="card-header">
            <div class="card-title-wrapper">
              <span class="card-name" :title="card.name">{{ card.name }}</span>
              <a-tag v-if="selectedCardKey === card.key" color="success" class="selected-tag">
                <check-circle-outlined />
                <span>已选择</span>
              </a-tag>
            </div>
          </div>

          <!-- 卡片内容：模型信息和标签 -->
          <div class="card-content">
            <!-- 模型信息 -->
            <div class="metrics-container">
              <div class="metric-item">
                <div class="metric-title">Accuracy</div>
                <div class="metric-value">
                  {{ card.modelInfo.accuracy < 0 ? '--' : card.modelInfo.accuracy.toFixed(2) }}%
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-title">recall</div>
                <div class="metric-value">
                  {{ card.modelInfo.loss < 0 ? '--' : card.modelInfo.loss.toFixed(2) }}
                </div>
              </div>
              <div class="metric-item">
                <div class="metric-title">图片数</div>
                <div class="metric-value">
                  {{ card.modelInfo.imgCount <= 0 ? '--' : card.modelInfo.imgCount }}张
                </div>
              </div>
            </div>

            <!-- 标签信息 -->
            <div class="label-section">
              <div class="section-title">分类标签</div>
              <div v-if="card.labelNames && card.labelNames.length > 0" class="label-tags">
                <a-tag
                  v-for="(labelName, index) in card.labelNames"
                  :key="index"
                  :color="LABEL_COLORS[index % LABEL_COLORS.length]"
                  class="label-tag"
                >
                  {{ labelName }}
                </a-tag>
              </div>
              <div v-else class="no-labels">
                <a-tag class="no-label-tag">暂无标签</a-tag>
              </div>
            </div>
          </div>
        </a-card>
      </div>
      <!-- 空状态 -->
      <a-empty v-else-if="!loading" description="暂无可用的模型任务" class="empty-container" />

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-container">
        <a-pagination
          v-model:current="pagination.current"
          v-model:pageSize="pagination.pageSize"
          :total="total"
          size="small"
          show-less-items
          @change="handlePageChange"
        />
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import type { PropType } from 'vue';
  import {
    Spin,
    Card,
    Empty,
    Pagination,
    Tag,
    Input,
    Select,
    SelectOption,
    Button,
  } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { CheckCircleOutlined } from '@ant-design/icons-vue';

  const ASpin = Spin;
  const ACard = Card;
  const AEmpty = Empty;
  const APagination = Pagination;
  const ATag = Tag;
  const AInputSearch = Input.Search;
  const ASelect = Select;
  const ASelectOption = Select.Option;
  const AButton = Button;

  const props = defineProps({
    // 'standard' 或 'guided'
    type: {
      type: String,
      required: true,
    },
    // 标准化训练名称的ID，或 数据集ID
    requestParam: {
      type: [Number, String],
      default: null,
    },
    // 用于回显已选中的卡片
    selectedValue: {
      type: [String, Number],
      default: null,
    },
    applicationName: {
      type: String,
      default: '',
    },
    deviceFirmware: {
      type: String,
      default: '',
    },
    requiredLabels: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
  });

  const emits = defineEmits(['update:selected']);

  const { createMessage } = useMessage();
  const loading = ref(false);
  const cardList = ref<any[]>([]);
  const total = ref(0);
  const pagination = ref({
    current: 1,
    pageSize: 8, // 修改为8个卡片每页
  });
  const selectedCardKey = ref<string | number | null>(props.selectedValue);

  // 搜索相关状态
  const searchText = ref('');
  const searchType = ref('name');
  const allLabels = ref<string[]>([]);
  const allLabelsStandard = ref<string[]>([]);
  const labelLoading = ref(false);

  // 处理标准化模式下的标签下拉框
  const handleLabelDropdownStandard = async (open: boolean) => {
    if (open && props.requestParam) {
      allLabelsStandard.value = [];
      labelLoading.value = true;
      try {
        // 注意：这里的API路径可能需要根据你的实际情况调整
        // 标准化训练模式下获取标签的API可能与guided模式不同
        const res = await maHttp.get(
          {
            url: `/modelGeneration/getStandardAutoLabelNames/${props.requestParam}`,
            headers: { ignoreCancelToken: true },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );

        if (res && Array.isArray(res)) {
          allLabelsStandard.value = res;
        } else {
          allLabelsStandard.value = [];
          console.warn('获取标签API返回数据格式不正确:', res);
        }
      } catch (error) {
        console.error('获取标准化训练标签数据失败:', error);
        allLabelsStandard.value = [];
        createMessage.error('获取标签列表失败');
      } finally {
        labelLoading.value = false;
      }
    }
  };

  // 处理引导式下拉框
  const handleLabelDropdown = async (open: boolean) => {
    // 当下拉框打开时获取标签
    if (open && props.requestParam && searchType.value === 'tag') {
      allLabels.value = [];
      labelLoading.value = true;
      try {
        const res = await maHttp.get(
          {
            url: `/modelGeneration/getAutoLabelNames/${props.requestParam}`,
            headers: { ignoreCancelToken: true },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );

        if (res && Array.isArray(res)) {
          allLabels.value = res;
        } else {
          allLabels.value = [];
          console.warn('获取标签API返回数据格式不正确:', res);
        }
      } catch (error) {
        console.error('获取标签数据失败:', error);
        allLabels.value = [];
        createMessage.error('获取标签列表失败');
      } finally {
        labelLoading.value = false;
      }
    }
  };

  function handleSearchTypeChange() {
    searchText.value = undefined as any;
  }

  // 优化后的标签颜色（在深色背景下更鲜明）
  const LABEL_COLORS = ['blue', 'green', 'orange', 'purple', 'red', 'cyan', 'pink'];

  function matchesAnyRequiredLabel(labelNames: string[] = []) {
    const requiredLabels = (props.requiredLabels || [])
      .map((label) => String(label || '').trim().toLowerCase())
      .filter(Boolean);
    if (!requiredLabels.length) {
      return true;
    }
    const candidateSet = new Set(
      (labelNames || [])
        .map((label) => String(label || '').trim().toLowerCase())
        .filter(Boolean),
    );
    return requiredLabels.some((label) => candidateSet.has(label));
  }

  async function fetchData() {
    const hasApplicationGuidedContext =
      props.type === 'guided' && props.applicationName && props.deviceFirmware;
    if (!props.requestParam && !hasApplicationGuidedContext) {
      resetState();
      return;
    }

    loading.value = true;
    try {
      let url = '';
      let params: any = {
        page: pagination.value.current - 1,
        pageSize: pagination.value.pageSize,
      };
      if (props.type === 'standard') {
        url = '/modelGeneration/findModelJobInfoCardsByGenerationId';
        params.modelGenerationId = props.requestParam;
        params.labels = searchText.value;
      } else if (props.type === 'guided') {
        url = hasApplicationGuidedContext
          ? '/modelGeneration/findGuidedTrainingInfoCardsByApplication'
          : `/modelGeneration/findGuidedTrainingInfoCardsByDatasetId/${props.requestParam}`;
        // guided模式下，参数在URL路径中，并添加搜索参数
        params = {
          page: pagination.value.current - 1,
          pageSize: pagination.value.pageSize,
          applicationName: hasApplicationGuidedContext ? props.applicationName : undefined,
          deviceFirmware: hasApplicationGuidedContext ? props.deviceFirmware : undefined,
          name: searchType.value === 'name' ? searchText.value : undefined,
          labels: searchType.value === 'tag' ? searchText.value : undefined,
        };
      } else {
        loading.value = false;
        return;
      }

      const res = await maHttp.get(
        { url, params, headers: { ignoreCancelToken: true } },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      // 适配两种不同的DTO结构
      const cards = (res.content || []).map((item) => ({
        key: props.type === 'standard' ? item.jobName : item.generationId,
        name: props.type === 'standard' ? item.jobName : item.generationName,
        modelInfo: item.modelInfo,
        labelNames: item.labelNames,
      }));
      const filteredCards = cards.filter((card) => matchesAnyRequiredLabel(card.labelNames));
      cardList.value = filteredCards;
      total.value =
        props.requiredLabels && props.requiredLabels.length > 0 && props.type === 'standard'
          ? filteredCards.length
          : res.totalElements;
    } catch (error) {
      console.error('获取卡片数据失败:', error);
      createMessage.error('获取模型任务列表失败');
      resetState();
    } finally {
      loading.value = false;
    }
  }

  function handlePageChange(page: number, pageSize: number) {
    pagination.value.current = page;
    pagination.value.pageSize = pageSize;
    fetchData();
  }

  function handleCardClick(card) {
    selectedCardKey.value = card.key;
    // 将整个卡片对象和选择的key都传递出去
    emits('update:selected', { key: card.key, card: card });
  }

  function handleSearch() {
    pagination.value.current = 1; // 重置到第一页
    fetchData();
  }

  function handleClear() {
    searchText.value = undefined as any;
    pagination.value.current = 1; // 重置到第一页
    fetchData();
  }

  function resetState() {
    cardList.value = [];
    total.value = 0;
    pagination.value.current = 1;
    selectedCardKey.value = null;
    searchText.value = undefined as any;
    emits('update:selected', null);
  }

  // 清空搜索和选择的方法
  const clearSearch = () => {
    searchText.value = '';
    searchType.value = 'name';
    pagination.value.current = 1;
    selectedCardKey.value = null;
    allLabels.value = [];
    allLabelsStandard.value = [];
  };

  // 暴露方法给父组件
  defineExpose({
    clearSearch,
  });

  // 监听请求参数变化，如果变化则重置并重新拉取数据
  watch(
    () => props.requestParam,
    (newVal) => {
      resetState();
      if (newVal || (props.type === 'guided' && props.applicationName && props.deviceFirmware)) {
        fetchData();
      }
    },
    { immediate: true }, // 立即执行一次
  );
  watch(
    () => [props.applicationName, props.deviceFirmware, props.requiredLabels],
    () => {
      resetState();
      if (props.requestParam || (props.type === 'guided' && props.applicationName && props.deviceFirmware)) {
        fetchData();
      }
    },
  );
</script>

<style scoped lang="scss">
  .card-selector-container {
    min-height: 200px;
    padding: 12px;
    background-color: #1f1f1f;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

    .search-container {
      margin-bottom: 16px;
      display: flex;
      justify-content: flex-end;

      .search-input {
        width: 280px;

        :deep(.ant-input) {
          background: #2a2a2a;
          border-color: #3a3a3a;
          color: #f0f0f0;

          &::placeholder {
            color: #888;
          }

          &:focus,
          &:hover {
            border-color: #1890ff;
            box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
          }
        }
        :deep(.ant-input-search-button) {
          margin-left: 6px;
        }

        :deep(.ant-input-group-addon) {
          background: #2a2a2a;
          border-color: #3a3a3a;

          .ant-btn {
            background: #1575ce;
            border-color: #1575ce;
            color: #d9d9d9;

            &:hover {
              background: #40a9ff;
              border-color: #40a9ff;
            }
          }
        }
      }
    }

    .card-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr); // 4列布局
      gap: 8px; // 减小间距
      margin-bottom: 16px;
    }

    .info-card {
      background: linear-gradient(145deg, #2a2a2a, #252525);
      border: 1px solid #3a3a3a;
      border-radius: 6px; // 减小圆角
      overflow: hidden;
      transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
      //max-height: 200px; // 固定高度，确保卡片统一

      // 覆盖Ant Design卡片默认的24px padding
      :deep(.ant-card-body) {
        padding: 0 !important;
      }

      &:hover {
        transform: translateY(-2px); // 减小hover效果
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        border-color: #555;
      }

      &.selected {
        border-color: #1890ff;
        box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.3), 0 4px 16px rgba(24, 144, 255, 0.2);
      }

      .card-header {
        padding: 6px 8px; // 减小padding
        border-bottom: 1px solid #3a3a3a;
        background-color: rgba(40, 40, 40, 0.7);

        .card-title-wrapper {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .card-name {
            font-size: 12px; // 减小字体
            font-weight: 600;
            color: #f0f0f0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: calc(100% - 60px); // 调整最大宽度
            letter-spacing: 0.3px;
          }

          .selected-tag {
            display: flex;
            align-items: center;
            gap: 2px; // 减小间距
            font-size: 10px; // 减小字体
            padding: 1px 4px; // 减小padding
            border-radius: 8px;
            background: rgba(24, 144, 255, 0.15);
            border: none;
            color: #1890ff;
            font-weight: 500;
          }
        }
      }

      .card-content {
        padding: 8px; // 减小padding
        height: calc(180px - 30px); // 计算剩余高度
        display: flex;
        flex-direction: column;

        .metrics-container {
          display: flex;
          gap: 6px; // 减小间距
          margin-bottom: 8px; // 减小margin
          padding-bottom: 6px; // 减小padding
          border-bottom: 1px solid #3a3a3a;

          .metric-item {
            flex: 1;
            text-align: center;
            padding: 4px 3px; // 减小padding
            background: rgba(40, 40, 40, 0.6);
            border-radius: 4px; // 减小圆角
            transition: all 0.3s ease;

            &:hover {
              background: rgba(50, 50, 50, 0.7);
              transform: translateY(-1px); // 减小hover效果
            }

            .metric-title {
              font-size: 10px; // 减小字体
              color: #a0a0a0;
              margin-bottom: 2px; // 减小间距
              font-weight: 500;
            }

            .metric-value {
              font-size: 11px; // 减小字体
              font-weight: 600;
              color: #f0f0f0;
              letter-spacing: 0.3px;
            }
          }
        }

        .label-section {
          flex: 1;
          display: flex;
          flex-direction: column;
          min-height: 0; // 确保flex子项可以缩小

          .section-title {
            font-size: 10px; // 减小字体
            color: #a0a0a0;
            margin-bottom: 4px; // 减小间距
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 4px; // 减小间距
            flex-shrink: 0; // 标题不缩小

            &::before {
              content: '';
              display: block;
              width: 2px; // 减小宽度
              height: 8px; // 减小高度
              background: #1890ff;
              border-radius: 1px;
            }
          }

          .label-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 4px; // 稍微增大间距
            flex: 1;
            align-content: flex-start;
            overflow-y: auto; // 添加垂直滚动
            overflow-x: hidden; // 隐藏水平滚动
            max-height: 60px; // 限制最大高度，约3行标签的空间
            padding-right: 2px; // 为滚动条留出空间

            // 自定义滚动条样式
            &::-webkit-scrollbar {
              width: 3px;
            }

            &::-webkit-scrollbar-track {
              background: rgba(60, 60, 60, 0.3);
              border-radius: 2px;
            }

            &::-webkit-scrollbar-thumb {
              background: rgba(136, 136, 136, 0.6);
              border-radius: 2px;

              &:hover {
                background: rgba(136, 136, 136, 0.8);
              }
            }

            .label-tag {
              margin: 0;
              padding: 2px 6px; // 稍微增大padding
              font-size: 10px; // 增大字体
              border: none;
              color: white;
              font-weight: 500;
              border-radius: 10px;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
              flex-shrink: 0; // 标签不缩小
              line-height: 1.2;
            }
          }

          .no-labels {
            flex: 1;
            display: flex;
            align-items: flex-start;

            .no-label-tag {
              background: #3a3a3a;
              color: #a0a0a0;
              border: none;
              padding: 2px 6px; // 增大padding
              border-radius: 10px;
              font-size: 10px; // 增大字体
              margin: 0;
            }
          }
        }
      }
    }

    .empty-container {
      padding: 30px 0;

      :deep(.ant-empty-description) {
        color: #888;
        font-size: 14px;
      }

      :deep(.ant-empty-image) {
        opacity: 0.3;
      }
    }

    .pagination-container {
      display: flex;
      justify-content: center;
      padding-top: 8px;

      :deep(.ant-pagination) {
        .ant-pagination-item {
          background: #2a2a2a;
          border-color: #3a3a3a;

          a {
            color: #d0d0d0;
          }

          &:hover,
          &-active {
            border-color: #1890ff;

            a {
              color: #1890ff;
            }
          }
        }

        .ant-pagination-item-link {
          background: #2a2a2a;
          border-color: #3a3a3a;

          .anticon {
            color: #d0d0d0;
          }

          &:hover {
            border-color: #1890ff;

            .anticon {
              color: #1890ff;
            }
          }
        }

        .ant-pagination-options-quick-jumper {
          color: #d0d0d0;

          input {
            background: #2a2a2a;
            border-color: #3a3a3a;
            color: #f0f0f0;

            &:focus {
              border-color: #1890ff;
              box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
            }
          }
        }
      }
    }

    // 响应式布局：在小屏幕上调整为2列
    @media (max-width: 1200px) {
      .card-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    // 在更小屏幕上调整为1列
    @media (max-width: 768px) {
      .card-grid {
        grid-template-columns: 1fr;
      }
    }
  }
</style>
