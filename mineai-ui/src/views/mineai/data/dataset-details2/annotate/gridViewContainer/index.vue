<template>
  <div class="grid-view-container">
    <!-- 顶部工具栏 -->
    <div class="grid-toolbar">
      <div class="toolbar-left">
        <span class="mode-title">
          <EyeOutlined class="mr-2" />
          查看模式 - 按 Q 键返回标注模式
        </span>
      </div>
      <div class="toolbar-right">
        <!-- 跳转到指定图片序号 -->
        <div class="jump-to-page">
          <span style="margin-right: 8px; color: #e1e1e1">跳转到第</span>
          <input
            type="number"
            v-model.number="jumpImageNumber"
            @keydown.enter="handleJumpToImage"
            :min="1"
            :max="displayTotal"
            class="page-input"
            placeholder="图片序号"
          />
          <span style="margin-left: 4px; margin-right: 8px; color: #e1e1e1">张</span>
          <AButton @click="handleJumpToImage" class="action-btn" size="small">跳转</AButton>
        </div>

        <!-- 跳转到指定页码 -->
        <div class="jump-to-page">
          <span style="margin-right: 8px; color: #e1e1e1">跳转到第</span>
          <input
            type="number"
            v-model.number="jumpPageNumber"
            @keydown.enter="handleJumpToPage"
            :min="1"
            :max="totalPages"
            class="page-input"
            placeholder="页码"
          />
          <span style="margin-left: 4px; margin-right: 8px; color: #e1e1e1">页</span>
          <AButton @click="handleJumpToPage" class="action-btn" size="small">跳转</AButton>
        </div>

        <!-- 跳回标注模式按钮 -->
        <div class="back-to-annotate">
          <AButton @click="handleBackToAnnotate" class="action-btn">
            <template #icon>
              <EditOutlined />
            </template>
            返回标注模式
          </AButton>
        </div>

        <!-- 翻页按钮 -->
        <AButton @click="prevPage" :disabled="currentPage <= 1">
          <template #icon>
            <LeftOutlined />
          </template>
          上一页
        </AButton>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <AButton @click="nextPage" :disabled="currentPage >= totalPages" style="margin-right: 12px">
          下一页
          <template #icon>
            <RightOutlined />
          </template>
        </AButton>

        <AButton @click="refreshGrid" type="primary">
          <template #icon>
            <ReloadOutlined />
          </template>
          刷新
        </AButton>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="grid-content" ref="gridContentRef">
      <!-- 加载状态 -->
      <div v-if="loading && displayFiles.length === 0" class="loading-state">
        <Spin size="large">
          <template #tip>
            <div class="loading-tip">
              <div>正在加载第 {{ currentPage }} 页数据...</div>
              <div class="loading-progress" v-if="loadingProgress > 0">
                加载进度: {{ loadingProgress }}%
              </div>
            </div>
          </template>
        </Spin>
      </div>

      <!-- 网格内容 - 4列2行布局 -->
      <div v-else class="grid-layout">
        <ImageCard
          v-for="(file, index) in displayFiles"
          :key="file.id"
          :file="file"
          :annotations="fileAnnotationsMap.get(file.id) || []"
          :labels="labels"
          :index="(currentPage - 1) * pageSize + index + 1"
          :annotationsLoaded="annotationsLoaded"
        />
      </div>
    </div>

    <!-- 底部分页器 -->
    <div class="grid-footer">
      <div class="pagination-info">
        第 {{ currentPage }} / {{ totalPages }} 页，每页 {{ pageSize }} 张，共
        {{ displayTotal }}
        张图片{{ teamLabel ? '（您分配的部分）' : '' }}
      </div>
      <Pagination
        v-model:current="currentPage"
        :pageSize="pageSize"
        :total="displayTotal"
        :show-size-changer="false"
        :show-quick-jumper="true"
        @change="handlePageChange"
        size="small"
      >
        <template #itemRender="{ type, originalElement }">
          <a v-if="type === 'prev'">
            <LeftOutlined />
            上一页 (←)
          </a>
          <a v-else-if="type === 'next'">
            下一页 (→)
            <RightOutlined />
          </a>
          <component v-else :is="originalElement" />
        </template>
      </Pagination>
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, ref } from 'vue';
  import {
    EditOutlined,
    EyeOutlined,
    LeftOutlined,
    ReloadOutlined,
    RightOutlined,
  } from '@ant-design/icons-vue';
  import { Button as AButton, Pagination, Spin } from 'ant-design-vue';
  import ImageCard from './imageCard.vue';
  import { parseAnnotation, transformFiles } from '../../util';
  import { detectFileList } from '../../api';
  import { ElMessage as Message } from 'element-plus';

  const props = defineProps({
    state: Object,
    labels: Array,
    queryNextPage: Function,
    teamLabel: Object, // 是否为多人标注
    startOffset: Object, // 多人标注的起始offset
    endOffset: Object, // 多人标注的结束offset
    versionName: String,
  });

  const gridContentRef = ref(null);
  const loading = ref(false);
  const loadingProgress = ref(0); // 加载进度
  const fileAnnotationsMap = ref(new Map());
  const annotationsLoaded = ref(false); // 标注是否已加载

  // 分页相关
  const currentPage = ref(1);
  const jumpImageNumber = ref(null); // 跳转图片序号输入
  const jumpPageNumber = ref(null); // 跳转页码输入
  // 固定每页显示数量 - 改为8张（4列×2行）
  const pageSize = computed(() => {
    return 12;
  });

  // 当前页的文件数据（直接存储，不从state.files中取）
  const currentPageFiles = ref([]);

  // 显示的总数 - 对于多人标注，显示分配的数量；对于普通标注，显示全部数量
  const displayTotal = computed(() => {
    return props.state.total || 0;
  });

  // 总页数
  const totalPages = computed(() => {
    return Math.ceil(displayTotal.value / pageSize.value);
  });

  // 当前页显示的文件列表
  const displayFiles = computed(() => {
    return currentPageFiles.value;
  });

  // 获取标签名称
  const getLabelName = (labelId) => {
    const label = props.labels.find((l) => l.id === labelId);
    return label?.name || '';
  };

  // 直接加载指定页的数据
  const loadPageData = async (page) => {
    loading.value = true;
    loadingProgress.value = 0;
    annotationsLoaded.value = false; // 重置标注加载状态

    try {
      // 计算offset和limit - 考虑多人标注的情况
      let offset = (page - 1) * pageSize.value;
      let limit = pageSize.value;

      // 如果是多人标注，需要加上起始offset，并限制limit
      if (props.teamLabel && props.startOffset !== null) {
        const start = props.startOffset;
        const end = props.endOffset ? props.endOffset : start;

        // 用户分配的总图片数
        const userTotalCount = end - start + 1;

        // 当前页相对于用户范围的起始位置
        const relativeOffset = (page - 1) * pageSize.value;

        // 实际的全局offset
        offset = start + relativeOffset;

        // 限制limit，不能超出用户的分配范围
        const remainingCount = userTotalCount - relativeOffset;
        limit = Math.min(pageSize.value, Math.max(0, remainingCount));
      }

      loadingProgress.value = 10;

      // 直接请求第n页的数据
      const filesParams = {
        offset: offset,
        limit: limit,
        type: [101, 104], // 获取所有状态的图片
        labelId: props.state.filterLabelId,
        versionName: props.versionName,
      };

      loadingProgress.value = 30;

      const rawFiles = await detectFileList(props.state.datasetId, filesParams);

      loadingProgress.value = 60;

      // 转换文件格式（必须使用transformFiles转换URL等信息）
      const fileList = rawFiles.result || [];
      const transformedFiles = transformFiles(fileList);
      currentPageFiles.value = transformedFiles;

      loadingProgress.value = 80;

      // 加载标注信息
      await loadAnnotations();

      // 标注加载完成
      annotationsLoaded.value = true;

      loadingProgress.value = 100;
    } catch (error) {
      console.error('加载页面数据失败:', error);
      currentPageFiles.value = [];
      annotationsLoaded.value = true; // 即使失败也标记为已加载
    } finally {
      loading.value = false;
      loadingProgress.value = 0;
    }
  };

  // 加载文件的标注信息
  const loadAnnotations = async () => {
    const map = new Map();

    // 查看模式下直接使用 detectFileList 返回的 annotation 字段，不需要再调用 info 请求
    for (const file of currentPageFiles.value) {
      try {
        if (file.annotation) {
          const annotations = parseAnnotation(file.annotation, props.labels);
          map.set(file.id, annotations);
        } else {
          map.set(file.id, []);
        }
      } catch (error) {
        console.error(`解析图片 ${file.id} 的标注信息失败:`, error);
        map.set(file.id, []);
      }
    }

    fileAnnotationsMap.value = map;
  };

  // 刷新九宫格
  const refreshGrid = () => {
    loadPageData(currentPage.value);
  };

  // 翻页处理
  const handlePageChange = async (page) => {
    // 检查页数是否有效
    if (page < 1) {
      page = 1;
    }
    if (totalPages.value > 0 && page > totalPages.value) {
      page = totalPages.value;
    }

    currentPage.value = page;

    // 直接加载该页的数据
    await loadPageData(page);

    // 滚动到顶部
    if (gridContentRef.value) {
      gridContentRef.value.scrollTop = 0;
    }
  };

  // 上一页
  const prevPage = () => {
    if (currentPage.value > 1) {
      handlePageChange(currentPage.value - 1);
    }
  };

  // 下一页
  const nextPage = () => {
    if (currentPage.value < totalPages.value) {
      handlePageChange(currentPage.value + 1);
    }
  };

  // 跳转到指定页
  const goToPage = async (page) => {
    if (page < 1 || page > totalPages.value) {
      return;
    }
    await handlePageChange(page);
  };

  // 处理跳转到指定图片序号
  const handleJumpToImage = () => {
    if (!jumpImageNumber.value) {
      return;
    }

    let imageNum = jumpImageNumber.value;
    // 验证输入范围
    if (imageNum < 1) {
      imageNum = 1;
      Message.warning('图片序号不能小于1');
    }
    if (imageNum > displayTotal.value) {
      imageNum = displayTotal.value;
      Message.warning(`图片序号不能大于${displayTotal.value}`);
    }

    // 根据图片序号计算页码（每页8张）
    const targetPage = Math.ceil(imageNum / pageSize.value);
    goToPage(targetPage);
    jumpImageNumber.value = null; // 清空输入框
  };

  // 处理跳转到指定页码
  const handleJumpToPage = () => {
    if (!jumpPageNumber.value) {
      return;
    }

    let page = jumpPageNumber.value;
    // 验证输入范围
    if (page < 1) {
      page = 1;
      Message.warning('页码不能小于1');
    }
    if (page > totalPages.value) {
      page = totalPages.value;
      Message.warning(`页码不能大于${totalPages.value}`);
    }

    goToPage(page);
    jumpPageNumber.value = null; // 清空输入框
  };

  // 返回标注模式
  const emit = defineEmits(['back-to-annotate']);
  const handleBackToAnnotate = () => {
    emit('back-to-annotate');
  };

  onMounted(() => {
    // 挂载时加载第一页数据
    loadPageData(1);
  });

  defineExpose({
    refreshGrid,
    goToPage,
  });
</script>

<style lang="scss" scoped>
  .grid-view-container {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    background: #1a1d2e;
  }

  .grid-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 48px;
    padding: 0 20px;
    background: #181d31;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.4);

    .toolbar-left {
      .mode-title {
        color: #e1e1e1;
        font-size: 16px;
        font-weight: 500;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;

      .back-to-annotate {
        margin-right: 16px;
      }

      .jump-to-page {
        display: flex;
        align-items: center;
        margin-right: 16px;

        .page-input {
          width: 60px;
          height: 28px;
          padding: 4px 8px;
          background: #2a2d3e;
          border: 1px solid #3a3d4e;
          border-radius: 4px;
          color: #e1e1e1;
          font-size: 14px;
          text-align: center;
          outline: none;

          &:hover {
            border-color: #409eff;
          }

          &:focus {
            border-color: #409eff;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
          }

          &::placeholder {
            color: #909399;
          }

          /* 隐藏数字输入框的上下箭头 */
          &::-webkit-inner-spin-button,
          &::-webkit-outer-spin-button {
            -webkit-appearance: none;
            margin: 0;
          }
        }
      }

      .page-info {
        margin: 0 12px;
        color: #e1e1e1;
        font-size: 14px;
        font-weight: 500;
        min-width: 60px;
        text-align: center;
      }

      // 自定义按钮样式，跟完成按钮一样
      :deep(.action-btn) {
        background-color: transparent;
        border-color: #3a3d4e;
        color: #e1e1e1;

        &:hover {
          border-color: #409eff;
          color: #409eff;
          background-color: transparent;
        }

        &:focus {
          border-color: #409eff;
          color: #409eff;
          background-color: transparent;
        }

        &:active {
          border-color: #3a8ee6;
          color: #3a8ee6;
          background-color: rgba(64, 158, 255, 0.1);
        }
      }
    }
  }

  .grid-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    /* 确保占满剩余空间 */
    min-height: 0;
  }

  .grid-layout {
    display: grid;
    /* 改为4列2行布局 */
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: repeat(3, 1fr);
    gap: 12px;
    margin-bottom: 20px;
  }

  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;

    .loading-tip {
      margin-top: 12px;
      text-align: center;
      color: #e1e1e1;
      font-size: 14px;

      .loading-progress {
        margin-top: 8px;
        color: #909399;
        font-size: 12px;
      }
    }
  }

  .grid-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: #181d31;
    border-top: 1px solid #2a2d3e;

    .pagination-info {
      color: #909399;
      font-size: 14px;
      font-weight: 500;
    }

    :deep(.ant-pagination) {
      display: flex;
      align-items: center;

      .ant-pagination-item,
      .ant-pagination-prev,
      .ant-pagination-next,
      .ant-pagination-jump-prev,
      .ant-pagination-jump-next {
        background: #2a2d3e;
        border-color: #3a3d4e;

        a {
          color: #e1e1e1;
        }

        &:hover {
          border-color: #409eff;

          a {
            color: #409eff;
          }
        }
      }

      .ant-pagination-item-active {
        background: #409eff;
        border-color: #409eff;

        a {
          color: #fff;
        }
      }

      .ant-pagination-disabled {
        opacity: 0.4;

        &:hover {
          border-color: #3a3d4e;

          a {
            color: #e1e1e1;
          }
        }
      }

      .ant-pagination-options-quick-jumper {
        color: #e1e1e1;

        input {
          background: #2a2d3e;
          border-color: #3a3d4e;
          color: #e1e1e1;

          &:hover,
          &:focus {
            border-color: #409eff;
          }
        }
      }
    }
  }

  /* 响应式设计：在小屏幕上调整为2列4行 */
  @media (max-width: 1200px) {
    .grid-layout {
      grid-template-columns: repeat(2, 1fr);
      grid-template-rows: repeat(4, 1fr);
      height: auto;
      min-height: 600px;
    }
  }

  /* 在更小的屏幕上调整为1列8行 */
  @media (max-width: 768px) {
    .grid-layout {
      grid-template-columns: 1fr;
      grid-template-rows: repeat(8, 1fr);
      height: auto;
      min-height: 800px;
    }
  }
</style>
