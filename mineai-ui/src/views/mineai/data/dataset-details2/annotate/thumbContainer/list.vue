<template>
  <div ref="listWrapper" class="infinite-list-wrapper">
    <ul style="flex: 1; overflow: auto; padding-top: 2px">
      <ListItem
        v-for="(item, index) in currentPageFiles"
        :key="item.id"
        :item="item"
        :index="(currentPage - 1) * pageSize + index + 1"
        :handleClick="handleClick"
        :currentImgId="currentImg.id ?? null"
        :mode="listMode"
      />
      <li v-if="currentPageFiles.length === 0 && !state.loading" class="f14 g6">暂无数据</li>
    </ul>

    <!-- 分页器 -->
    <div v-if="total > 0" class="pagination-bar">
      <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)"
        >&lt;
      </button>
      <input
        type="number"
        class="page-input"
        :style="{ width: inputWidth }"
        v-model.number="inputPage"
        @keyup.enter="jumpToPage"
        :min="1"
        :max="totalPages"
      />
      <span class="page-info">/ {{ totalPages }}</span>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages"
        @click="goToPage(currentPage + 1)"
        >&gt;
      </button>
    </div>

    <div v-loading="state.loading" element-loading-spinner="el-icon-loading"></div>
  </div>
</template>
<script>
  import { computed, reactive, ref, watch } from 'vue';
  import ListItem from '/@/views/mineai/data/dataset-details2/annotate/thumbContainer/listitem.vue';

  export default {
    name: 'Scroller',
    components: {
      ListItem,
    },
    props: {
      list: {
        type: Array,
        default: () => [],
      },
      type: {
        type: Array,
        default: () => [],
      },
      labelId: {
        type: Array,
        default: () => [],
      },
      addFiles: {
        type: Array,
        default: () => [],
      },
      hasMore: Boolean,
      history: {
        type: Array,
        default: () => [],
      },
      total: Number,
      noAnnotation: Number,
      haveAnnotation: Number,
      offset: Number,
      currentImg: {
        type: Object,
        default: () => ({}),
      },
      queryNextPage: Function,
      updateState: Function,
      listMode: {
        type: String,
        default: () => 'text',
      },
    },
    emits: ['changeImg', 'loadingEnd'],
    setup(props, ctx) {
      const listWrapper = ref(null);
      const pageSize = 35; // 每页35张
      const currentPage = ref(1);
      const inputPage = ref(1);
      const currentPageFiles = ref([]);

      const state = reactive({
        loading: false,
      });

      // 根据筛选条件计算实际的总数
      const actualTotal = computed(() => {
        const typeArray = props.type || [];
        const isAllTypes =
          typeArray.length === 0 || (typeArray.includes(101) && typeArray.includes(104));
        const isType101Only = typeArray.length === 1 && typeArray[0] === 101;
        const isType104Only = typeArray.length === 1 && typeArray[0] === 104;

        if (isType101Only) {
          return props.noAnnotation || 0;
        } else if (isType104Only) {
          return props.haveAnnotation || 0;
        } else {
          return props.total || 0;
        }
      });

      // 总页数 - 基于筛选后的实际总数
      const totalPages = computed(() => Math.ceil(actualTotal.value / pageSize));

      // 根据总页数计算输入框宽度
      const inputWidth = computed(() => {
        const digits = String(totalPages.value).length;
        return `${Math.max(32, digits * 12 + 16)}px`;
      });

      // 加载指定页的数据
      const loadPageData = async (page, forceOffset = false) => {
        // 放宽页码检查，允许在totalPages还没更新时也能加载
        if (page < 1) return;

        // 如果totalPages为0或page超出范围，但我们仍然尝试加载（可能是数据还没同步）
        const shouldLoad = totalPages.value === 0 || page <= totalPages.value;
        if (!shouldLoad && totalPages.value > 0) {
          console.warn(`页码 ${page} 超出范围 (totalPages: ${totalPages.value})`);
          return;
        }

        // 立即清空当前列表，避免显示旧数据
        currentPageFiles.value = [];
        state.loading = true;

        try {
          const offset = (page - 1) * pageSize;
          const requestParams = {
            offset: offset,
            limit: pageSize,
            type: props.type,
            labelId: props.labelId,
          };

          // 如果需要强制跳转，添加_forceOffset参数
          if (forceOffset) {
            requestParams._forceOffset = offset;
          }

          const result = await props.queryNextPage(requestParams);

          // 从props.list同步数据，确保响应式更新
          if (props.list && props.list.length > 0) {
            currentPageFiles.value = [...props.list];
          } else if (result && result.files) {
            currentPageFiles.value = [...result.files];
          }
        } catch (error) {
          console.error('加载页面数据失败:', error);
          currentPageFiles.value = [];
        } finally {
          state.loading = false;
          // 列表加载完成后，通知父组件触发标注框重绘
          ctx.emit('loadingEnd');
        }
      };

      // 跳转到指定页
      const goToPage = async (page) => {
        if (page < 1 || page > totalPages.value || page === currentPage.value) return;
        currentPage.value = page;
        inputPage.value = page;
        await loadPageData(page);

        // 滚动到顶部
        const ul = listWrapper.value?.querySelector('.list-container');
        if (ul) ul.scrollTop = 0;

        // 自动选中新页的第一张图片
        if (currentPageFiles.value.length > 0) {
          const firstFile = currentPageFiles.value[0];
          ctx.emit('changeImg', -1, firstFile, 0, false);
        }
      };

      // 输入框跳转
      const jumpToPage = () => {
        const page = inputPage.value;
        if (page >= 1 && page <= totalPages.value) {
          goToPage(page);
        } else {
          inputPage.value = currentPage.value;
        }
      };

      const handleClick = (item) => {
        if (item.id === props.currentImg.id) return;

        // 确保点击的图片在当前页面中
        const nextImgIndex = currentPageFiles.value.findIndex((d) => d.id === item.id);
        if (nextImgIndex === -1) {
          return;
        }

        // 在props.list（state.files）中查找当前图片的索引
        const currentImgIndex = props.list.findIndex((d) => d.id === props.currentImg.id);
        ctx.emit('changeImg', currentImgIndex, item, nextImgIndex, false);
      };

      // 使用一个标志来跟踪是否已经初始化
      const isInitialized = ref(false);

      // 监听筛选条件变化
      watch(
        () => props.type,
        (newType, oldType) => {
          // 初始化时直接加载
          if (!isInitialized.value && actualTotal.value > 0) {
            isInitialized.value = true;
            currentPage.value = 1;
            inputPage.value = 1;
            loadPageData(1);
            return;
          }

          // 只有在筛选条件真正改变时才处理
          if (isInitialized.value && JSON.stringify(newType) !== JSON.stringify(oldType)) {
            // 筛选条件变化时，清空当前页面文件列表
            // 父组件的updateList会更新state.files，然后通过props.list的watch来更新currentPageFiles
            currentPageFiles.value = [];
          }
        },
        { deep: true, immediate: true },
      );

      // 监听offset变化，同步更新当前页码（用于筛选切换后恢复位置）
      watch(
        () => props.offset,
        (newOffset) => {
          if (newOffset !== undefined && newOffset >= 0) {
            const newPage = Math.floor(newOffset / pageSize) + 1;
            if (newPage >= 1) {
              // 更新页码，即使超出当前totalPages（因为totalPages可能还没更新）
              currentPage.value = newPage;
              inputPage.value = newPage;
            }
          }
        },
      );

      // 监听props.list变化，同步更新currentPageFiles（用于标注状态实时更新和筛选切换）
      watch(
        () => props.list,
        (newList) => {
          if (newList && newList.length > 0) {
            // 更新当前页面文件列表
            currentPageFiles.value = [...newList];

            // 如果还没初始化，标记为已初始化
            if (!isInitialized.value) {
              isInitialized.value = true;
            }
          }
        },
        { deep: true, immediate: true },
      );

      // 获取当前页码
      const getCurrentPage = () => currentPage.value;

      // 获取总页数
      const getTotalPages = () => totalPages.value;

      // 滚动到当前选中的图片
      const scrollToCurrentImage = () => {
        // 直接滚动到顶部即可，因为跨页后当前图片要么是第一张要么是最后一张
        setTimeout(() => {
          const ul = listWrapper.value?.querySelector('.list-container');
          if (!ul) return;

          const currentImgId = props.currentImg?.id;
          if (!currentImgId) return;

          // 查找当前图片在列表中的位置
          const currentIndex = currentPageFiles.value.findIndex((f) => f.id === currentImgId);
          if (currentIndex < 0) return;

          // 如果是最后一张，滚动到底部
          if (currentIndex === currentPageFiles.value.length - 1) {
            ul.scrollTop = ul.scrollHeight;
          } else {
            // 否则滚动到顶部
            ul.scrollTop = 0;
          }
        }, 150);
      };

      // 跳转到指定页（不自动选中第一张图片）
      const goToPageWithoutAutoSelect = async (page) => {
        // 放宽页码检查，允许在totalPages还没更新时也能加载
        if (page < 1) return false;

        // 如果totalPages为0，仍然尝试加载（可能是数据还没同步）
        if (totalPages.value > 0 && page > totalPages.value) {
          console.warn(
            `goToPageWithoutAutoSelect: 页码 ${page} 超出范围 (totalPages: ${totalPages.value})`,
          );
          return false;
        }

        // 即使是当前页也要刷新，因为可能是跨页后需要更新显示
        currentPage.value = page;
        inputPage.value = page;
        // 使用forceOffset=true强制跳转到指定offset，不使用缓存
        await loadPageData(page, true);

        // 滚动到顶部
        const ul = listWrapper.value?.querySelector('.list-container');
        if (ul) ul.scrollTop = 0;

        return true;
      };

      // 重新加载当前页（用于删除图片后刷新）
      const reloadCurrentPage = async () => {
        if (currentPage.value > 0) {
          await loadPageData(currentPage.value);
          return true;
        }
        return false;
      };

      return {
        state,
        handleClick,
        listWrapper,
        currentPage,
        inputPage,
        pageSize,
        totalPages,
        currentPageFiles,
        goToPage,
        jumpToPage,
        inputWidth,
        getCurrentPage,
        getTotalPages,
        goToPageWithoutAutoSelect,
        scrollToCurrentImage,
        reloadCurrentPage,
      };
    },
  };
</script>

<style lang="scss" scoped>
  .infinite-list-wrapper {
    display: flex;
    flex-direction: column;
  }

  .pagination-bar {
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 4px 8px;
    background: #181d31;
    border-top: 1px solid #3a3d4e;
  }

  .page-btn {
    width: 28px;
    height: 28px;
    padding: 0;
    background: #2a2d3e;
    border: 1px solid #3a3d4e;
    border-radius: 3px;
    color: #e1e1e1;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      border-color: #409eff;
      color: #409eff;
      background: rgba(64, 158, 255, 0.1);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  .page-input {
    height: 28px;
    padding: 0 6px;
    background: #2a2d3e;
    border: 1px solid #3a3d4e;
    border-radius: 3px;
    color: #e1e1e1;
    font-size: 12px;
    text-align: center;

    &:focus {
      outline: none;
      border-color: #409eff;
    }

    /* 隐藏数字输入框的上下箭头 */
    &::-webkit-inner-spin-button,
    &::-webkit-outer-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
  }

  .page-info {
    color: #909399;
    font-size: 12px;
    white-space: nowrap;
  }

  .f14 {
    font-size: 14px;
  }

  .g6 {
    color: #909399;
    text-align: center;
    padding: 20px;
  }
</style>
