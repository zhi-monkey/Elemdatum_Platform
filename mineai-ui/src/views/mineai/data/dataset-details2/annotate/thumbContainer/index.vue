<template>
  <div class="thumb-wrapper">
    <!-- 导航区域局部遮罩：在加载期间阻止点击缩略图列表 -->
    <div v-if="isUIBlocked" class="nav-blocker"></div>
    <div
      class="flex flex-row justify-start gap-x-1 mb-2 pl-1 pr-1"
      style="border-bottom-width: 1px; border-bottom-color: #767676"
    >
      <div
        style="
          width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          line-height: 40px;
          height: 40px;
        "
      >
        <span style="color: #c9d1d9; font-size: medium; font-weight: bold">数据集</span>
        <span style="color: #f5b956; font-size: medium; font-weight: bold" :title="state.name">
          {{ state.name }}
        </span>
      </div>
    </div>
    <div class="file-infobar flex flex-col">
      <div class="flex flex-wrap text-center text-sm text-left font-bold">
        <div class="w-full break-all mb-1">
          <span>图片总数: </span>
          <span>{{ state.total }} 张</span>
        </div>
        <div class="w-full break-all">
          <span>未标注: </span>
          <span>{{ state.noAnnotation }} 张</span>
        </div>
        <div class="w-full break-all">
          <span>已标注: </span>
          <span>{{ state.haveAnnotation }} 张</span>
        </div>
      </div>
      <div class="mb-1 w-full flex flex-col">
        <SearchBox
          ref="searchBoxRef"
          :formItems="thumbState.formItems"
          :handleFilter="handleFilter"
          :initialValue="initialValue"
          :noAnnotations="state.noAnnotation"
          :haveAnnotations="state.haveAnnotation"
          :teamLabel="teamLabel"
        />
        <Button
          type="primary"
          size="small"
          @click="switchMode"
          style="margin-left: 8px; margin-right: 8px"
        >
          切换列表样式
        </Button>
      </div>
    </div>
    <List
      ref="listRef"
      v-bind="$attrs"
      :list-mode="listMode"
      :updateState="updateState"
      :list="state.files"
      :addFiles="state.addFiles"
      :hasMore="state.hasMore"
      :total="state.total"
      :noAnnotation="state.noAnnotation"
      :haveAnnotation="state.haveAnnotation"
      :offset="state.offset"
      :type="thumbState.type"
      :labelId="thumbState.labelId"
      :history="state.history"
      @loading-end="$emit('listLoadingEnd')"
    />
  </div>
</template>

<script>
  import { computed, defineExpose, inject, nextTick, onUnmounted, reactive, ref, watch } from 'vue';
  import { ElMessage as Message } from 'element-plus';
  import { Button } from 'ant-design-vue';
  import { debounce } from 'lodash-es';
  import SearchBox from '/@/views/mineai/data/dataset-details2/components/SearchBox/index.vue';
  import { fileCodeMap, getFileFromMinIO, withDimensionFile } from '../../util';
  import { detectFileList, queryFileOffset, submit } from '../../api';
  import List from '/@/views/mineai/data/dataset-details2/annotate/thumbContainer/list.vue';
  import { useRoute } from 'vue-router';
  import { useGo } from '/@/hooks/web/usePage';

  export default {
    name: 'ThumbContainer',
    components: {
      List,
      SearchBox,
      Button,
    },
    inheritAttrs: false,
    props: {
      state: Object,
      updateList: Function,
      updateState: Function,
      isTrack: Boolean,
      teamLabel: Boolean,
      workSpaceRef: Object,
    },
    emits: ['listLoadingEnd'],
    setup(props) {
      const route = useRoute();
      const go = useGo();

      // 注入 UI 屏蔽状态
      const isUIBlocked = inject('isUIBlocked', ref(false));

      const uploaderRef = ref(null);
      const listRef = ref(null);
      const listMode = ref('text');
      const searchBoxRef = ref(null);
      // eslint-disable-next-line vue/no-setup-props-destructure
      const { datasetId } = props.state;
      const detectOptions = [
        { label: '未标注', value: 101 },
        { label: '已标注', value: 104 },
      ];
      const trackOptions = detectOptions.concat({ label: '目标跟踪完成', value: 201 });
      const rawFormItems = [
        {
          label: '筛选',
          prop: 'annotateStatus',
          type: 'checkboxGroup',
          options: props.isTrack ? trackOptions : detectOptions,
        },
      ];

      // 简化的状态管理
      const thumbState = reactive({
        type: props.state.fileFilterType,
        labelId: props.state.filterLabelId,
        showDialog: false,
        gotoNumber: 1,
        formItems: rawFormItems,
        filterUnfinished: props.state.filterUnfinished,
        // 简化：只保留必要的状态
        lastImageId: null,
        isUpdating: false,
      });

      watch(
        () => [props.state.fileFilterType, props.state.filterLabelId],
        ([newType, newLabelId]) => {
          // 同步更新本地状态
          thumbState.type = newType;
          thumbState.labelId = newLabelId;
        },
        { immediate: true, deep: true },
      );

      // 重置后的选项值
      const initialValue = computed(() => ({
        annotateStatus: props.state.fileFilterType,
        labelId: [],
      }));

      // 创建防抖的滚动函数 - 恢复防抖，但减少延迟
      const debouncedScrollToActiveItem = debounce(async () => {
        await nextTick();

        const listComponent = listRef.value;
        if (!listComponent || !listComponent.$refs.listWrapper) {
          return;
        }

        const scrollContainer = listComponent.$refs.listWrapper;
        const selector =
          listMode.value === 'image' ? '.thumb-list-item.active' : '.text-link.focus';
        const activeItem = scrollContainer.querySelector(selector);

        if (activeItem) {
          // 使用 scrollIntoViewIfNeeded (非标准但广泛支持) 或设置 block: 'nearest'
          if (activeItem.scrollIntoViewIfNeeded) {
            // Chrome/Safari 支持
            activeItem.scrollIntoViewIfNeeded(true);
          } else {
            // 标准方法，设置 block: 'nearest' 避免不必要的页面滚动
            activeItem.scrollIntoView({
              behavior: 'smooth',
              block: 'nearest', // 关键：使用 'nearest' 而不是 'center'
              inline: 'nearest',
            });
          }
        }
      }, 100);

      // 简化的索引计算函数
      const calculateImageIndex = async (imageId) => {
        if (!imageId) return 1;

        // 优先使用本地计算
        const currentFileIndex = props.state.files.findIndex((file) => file.id === imageId);

        if (currentFileIndex !== -1) {
          // 基于当前加载的文件列表计算索引
          const calculatedIndex =
            props.state.offset - props.state.files.length + currentFileIndex + 1;
          return Math.max(1, calculatedIndex);
        } else {
          // 本地找不到才调用API
          try {
            let currentImgIndex = await queryFileOffset(datasetId, imageId, {
              type: thumbState.type,
              labelId: thumbState.labelId,
            });

            // 对于多人标注，需要转换为相对于用户起始位置的索引
            if (
              props.teamLabel &&
              props.state.firstOffset !== undefined &&
              props.state.firstOffset !== 0
            ) {
              const relativeIndex = Math.max(0, currentImgIndex - props.state.firstOffset);
              return Math.max(1, relativeIndex + 1);
            } else {
              // 普通标注直接使用API返回的索引
              return Math.max(1, currentImgIndex + 1);
            }
          } catch (error) {
            console.warn('获取图片偏移量失败:', error);
            return thumbState.gotoNumber; // 返回当前值
          }
        }
      };

      // 状态重置函数
      const resetThumbState = () => {
        thumbState.lastImageId = null;
        thumbState.isUpdating = false;
        thumbState.gotoNumber = 1;
      };

      const handleFilter = (form) => {
        // 保存当前图片ID，用于切换筛选后尝试恢复位置
        const currentImageId = props.state.currentImgId;
        // 保存当前图片的状态（在files被清空之前）
        const currentFile = props.state.files.find((f) => f.id === currentImageId);
        const currentImageStatus = currentFile?.status;

        resetThumbState();

        const isUnfinishedOnly =
          JSON.stringify(form.annotateStatus.sort()) === JSON.stringify([fileCodeMap.UNANNOTATED]);

        Object.assign(thumbState, {
          type: form.annotateStatus,
          labelId: form.labelId,
          gotoNumber: 1,
        });

        props.updateState({
          files: [],
          annotations: [],
          rawAnnotations: [],
          fileInfo: null,
          fileFilterType: form.annotateStatus,
          filterLabelId: form.labelId,
          offset: 0, // 筛选后重置为0
          imgLoading: true,
          filterUnfinished: isUnfinishedOnly,
          // 传递当前图片ID，用于筛选后尝试恢复位置
          _pendingRestoreImageId: currentImageId,
        });

        props
          .updateList({
            type: form.annotateStatus,
            labelId: form.labelId,
            // 传递当前图片ID和状态，用于筛选后尝试恢复位置
            _restoreImageId: currentImageId,
            _restoreImageStatus: currentImageStatus,
          })
          .then(() => {
            setTimeout(() => {
              if (props.workSpaceRef) {
                props.workSpaceRef.focusWorkspace();
              }
            }, 100);
          });

        const listWrapper = listRef.value?.$refs?.listWrapper;
        if (listWrapper) {
          listWrapper.scrollTo({
            top: 0,
          });
        }
      };

      const handleStatusChange = (val) => {
        searchBoxRef.value.changeOption(
          'annotateStatus',
          val ? [fileCodeMap.UNANNOTATED, fileCodeMap.UNRECOGNIZED] : [''],
        );
        searchBoxRef.value.handleOk();
        props.updateState({ filterUnfinished: val });
      };

      const handleClose = () => {
        thumbState.showDialog = false;
      };

      const uploadSuccess = async (res) => {
        const files = getFileFromMinIO(res);
        submit(datasetId, files).then(() => {
          Message.success('上传成功');
          props.updateList({ type: thumbState.type });
        });
      };

      const uploadError = (err) => {
        Message.error('上传失败', err);
        console.error(err.message || err);
      };

      const handleUpload = () => {
        thumbState.showDialog = true;
      };

      const uploadParams = {
        datasetId: datasetId,
        objectPath: `dataset/${datasetId}/origin`,
      };

      const handleKeyup = async ({ target, keyCode }) => {
        let { value } = target;
        if (keyCode === 13) {
          // 输入验证
          const minValue = parseInt(target.min, 10);
          const maxValue = parseInt(target.max, 10);

          if (parseInt(target.value, 10) < minValue) {
            thumbState.gotoNumber = minValue;
            value = minValue;
          }
          if (parseInt(target.value, 10) > maxValue) {
            thumbState.gotoNumber = maxValue;
            value = maxValue;
          }

          try {
            // 计算实际的offset - 关键修改
            let actualOffset = value - 1; // 用户输入的是1基索引，API需要0基索引

            if (
              props.teamLabel &&
              props.state.firstOffset !== undefined &&
              props.state.firstOffset !== 0
            ) {
              // 多人标注：相对位置转换为绝对位置
              actualOffset = actualOffset + props.state.firstOffset;
            } else {
              // 普通标注：直接使用相对位置
            }

            const res = await detectFileList(datasetId, {
              offset: actualOffset,
              limit: 1,
              type: thumbState.type,
              labelId: thumbState.labelId,
            });

            const endStrReg = /(\/file\/)(\d+)$/;
            if (res.result.length > 0) {
              if (props.state.currentImgId === res.result[0].id) {
                return;
              }
              let nextPath = route.fullPath;
              nextPath = nextPath.replace(endStrReg, `$1${res.result[0].id}`);
              go(nextPath);
            } else {
              // 如果没有找到图片，可能是超出了用户的分配范围
              if (props.teamLabel) {
                Message.warning('跳转位置超出分配范围');
              } else {
                Message.warning('跳转位置无效');
              }
            }
          } catch (error) {
            console.error('跳转失败:', error);
            Message.error('跳转失败，请重试');
          }
        }
      };

      const switchMode = async () => {
        if (listMode.value === 'text') {
          listMode.value = 'image';
        } else {
          listMode.value = 'text';
        }

        // 切换模式后，滚动到当前选中的图片位置
        // 使用 setTimeout 确保 DOM 完全更新后再滚动
        await nextTick();
        setTimeout(async () => {
          const listComponent = listRef.value;
          if (!listComponent || !listComponent.$refs.listWrapper) {
            return;
          }

          const scrollContainer = listComponent.$refs.listWrapper;
          const selector =
            listMode.value === 'image' ? '.thumb-list-item.active' : '.text-link.focus';
          const activeItem = scrollContainer.querySelector(selector);

          if (activeItem) {
            // 切换模式时使用 'center' 确保选中项在视图中央
            activeItem.scrollIntoView({
              behavior: 'smooth',
              block: 'center',
              inline: 'nearest',
            });
          }
        }, 150);
      };

      // 优化的 watch 监听器，配合主文件的锁机制
      const watchCurrentImageId = watch(
        () => props.state.currentImgId,
        debounce(async (next, prev) => {
          // 基本检查
          if (!next || next === prev || next === thumbState.lastImageId) {
            return;
          }

          // 简单的重复检查
          if (thumbState.isUpdating) {
            return;
          }

          try {
            thumbState.isUpdating = true;
            thumbState.lastImageId = next;

            // 计算新的索引（已经在calculateImageIndex中处理了多人标注逻辑）
            const newIndex = await calculateImageIndex(next);

            // 更新索引
            if (newIndex !== thumbState.gotoNumber) {
              thumbState.gotoNumber = newIndex;
            }

            // 滚动到活动项
            await debouncedScrollToActiveItem();
          } catch (error) {
            console.error('更新图片索引时出错:', error);
          } finally {
            thumbState.isUpdating = false;
          }
        }, 150),
        {
          immediate: true,
        },
      );

      const watchFilesLength = watch(
        () => props.state.files.length,
        (newLength, oldLength) => {
          if (thumbState.isUpdating) {
            thumbState.isUpdating = false;
          }

          if (newLength === 0 && oldLength > 0) {
            resetThumbState();
          }
        },
      );

      // 监听过滤条件变化
      const watchFilterConditions = watch(
        () => [props.state.fileFilterType, props.state.filterLabelId],
        ([newType, newLabelId], [oldType, oldLabelId]) => {
          if (
            JSON.stringify(newType) !== JSON.stringify(oldType) ||
            JSON.stringify(newLabelId) !== JSON.stringify(oldLabelId)
          ) {
            resetThumbState();
          }
        },
      );

      const watchFilterUnfinished = watch(
        () => props.state.filterUnfinished,
        (next) => {
          Object.assign(thumbState, {
            filterUnfinished: next,
          });
        },
      );

      const changeFilterAndReload = (newStatus) => {
        resetThumbState();

        if (searchBoxRef.value) {
          searchBoxRef.value.changeOption('annotateStatus', newStatus);
          searchBoxRef.value.handleOk();
        }
      };

      // 组件卸载时清理
      onUnmounted(() => {
        // 清理防抖函数
        if (debouncedScrollToActiveItem) {
          debouncedScrollToActiveItem.cancel();
        }

        // 手动停止 watchers
        if (watchCurrentImageId) {
          watchCurrentImageId();
        }
        if (watchFilesLength) {
          watchFilesLength();
        }
        if (watchFilterConditions) {
          watchFilterConditions();
        }
        if (watchFilterUnfinished) {
          watchFilterUnfinished();
        }
      });

      // 暴露list.vue的分页方法给父组件使用
      const getCurrentPage = () => {
        return listRef.value?.getCurrentPage?.() || 1;
      };

      const getTotalPages = () => {
        return listRef.value?.getTotalPages?.() || 1;
      };

      const goToPageWithoutAutoSelect = async (page) => {
        if (listRef.value?.goToPageWithoutAutoSelect) {
          return await listRef.value.goToPageWithoutAutoSelect(page);
        }
        return false;
      };

      const scrollToCurrentImage = () => {
        if (listRef.value?.scrollToCurrentImage) {
          listRef.value.scrollToCurrentImage();
        }
      };

      const reloadCurrentPage = async () => {
        if (listRef.value?.reloadCurrentPage) {
          return await listRef.value.reloadCurrentPage();
        }
        return false;
      };

      defineExpose({
        changeFilterAndReload,
        resetThumbState,
        getCurrentPage,
        getTotalPages,
        goToPageWithoutAutoSelect,
        scrollToCurrentImage,
        reloadCurrentPage,
      });

      return {
        isUIBlocked,
        listRef,
        searchBoxRef,
        thumbState,
        withDimensionFile,
        uploadParams,
        handleUpload,
        handleClose,
        uploadSuccess,
        uploadError,
        uploaderRef,
        handleKeyup,
        initialValue,
        handleFilter,
        handleStatusChange,
        listMode,
        switchMode,
        changeFilterAndReload,
        resetThumbState,
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
  @import 'src/assets/scss/variables.scss';
  @import 'src/assets/scss/mixin.scss';

  .thumb-wrapper {
    margin-left: 10px;
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    width: 160px;
    min-width: 160px;
    flex-shrink: 0;
    height: 100%;
    padding-top: 4px;
    text-align: center;
    background-color: #181d31;
    box-shadow: 2px 0 6px 0 rgba(0, 0, 0, 0.25);

    // 局部屏蔽层：拦截缩略图列表区域的点击
    .nav-blocker {
      position: absolute;
      inset: 0;
      z-index: 1000;
      background: transparent;
      pointer-events: auto;
    }

    .file-infobar {
      padding: 0 8px;
      font-size: 16px;
      border-bottom: 1px solid #767676;

      .el-dropdown {
        display: inline-block;
        width: 66%;
        white-space: nowrap;
      }
    }

    .infinite-list-wrapper {
      flex: 1;
      min-height: 0;
      overflow: hidden;
    }

    .annotate-pagination {
      span {
        display: inline-block;
        margin-left: 0;
      }

      .el-icon-question {
        line-height: 28px;
      }
    }
  }
</style>
