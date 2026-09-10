<template>
  <div class="annotate-container" @keydown="handleGlobalKeydown" tabindex="-1" ref="containerRef">
    <!-- 标注模式 -->
    <transition name="slide-fade">
      <div v-show="state.viewMode === 'annotate'" class="annotate-view">
        <ThumbContainer
          ref="thumbRef"
          :state="state"
          :currentImg="currentImg"
          :updateList="updateList"
          :updateState="updateState"
          :queryNextPage="queryNextPage"
          :isTrack="isTrack"
          :teamLabel="teamLabel"
          :work-space-ref="workspaceRef"
          @change-img="handleChangeImg"
          @list-loading-end="handleListLoadingEnd"
        />
        <WorkSpaceContainer
          ref="workspaceRef"
          :isTrack="isTrack"
          :isSegmentation="isSegmentation"
          :state="state"
          :currentImg="currentImg"
          :drawShapeEnd="drawShapeEnd"
          :createLabel="createLabel"
          :queryLabels="queryLabels"
          :updateState="updateState"
          :getLabelName="getLabelName"
          :deleteAnnotation="deleteAnnotation"
          :handleConfirm="handleConfirm"
          :handlePrev="handlePrev"
          :handleNext="handleNext"
          :handleUp="handleUp"
          :handleDown="handleDown"
          :handleJumpToUnannotated="handleJumpToUnannotated"
          :annotationType="annotationType"
          :deleteable="!teamLabel"
          :toggleViewMode="toggleViewMode"
          :isTeamLabel="!!teamLabel"
          :isBatchConfirming="isBatchConfirming"
          :handleBatchConfirm="handleBatchConfirm"
          @selection="handleSelection"
          @brush-start="handleBrushStart"
          @save="handleSave"
          @remove="handleRemoveFile"
          @select-label="handleSelectLabel"
        />
        <SettingContainer
          :isTrack="isTrack"
          :isSegmentation="isSegmentation"
          :createLabel="createLabel"
          :queryLabels="queryLabels"
          :state="state"
          :updateState="updateState"
          :getColorLabel="getColorLabel"
          :deleteAnnotation="deleteAnnotation"
          :findRowIndex="findRowIndex"
          :annotationType="annotationType"
          @label-created="handleLabelCreated"
        />
      </div>
    </transition>

    <!-- 九宫格查看模式 -->
    <transition name="slide-fade">
      <div v-show="state.viewMode === 'grid'" class="grid-view">
        <GridViewContainer
          ref="gridViewRef"
          :state="state"
          :labels="state.labels"
          :queryNextPage="queryNextPage"
          :teamLabel="teamLabel"
          :startOffset="startOffset"
          :endOffset="endOffset"
          :versionName="versionName"
          @back-to-annotate="handleBackToAnnotate"
        />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, provide, reactive, ref, toRefs, watch } from 'vue';
  import { ElMessage as Message, ElMessageBox } from 'element-plus';
  import { isEmpty, isFunction, isNil, omit } from 'lodash-es';
  import { useUserStore } from '/@/store/modules/user';
  import {
    getPositionRecord,
    isRecordExpired,
    updatePositionRecord,
  } from './utils/annotationPosition';
  import {
    count,
    createLabel as createLabelApi,
    del,
    detail,
    detectFileList,
    getAbsoluteOffsetForAnnotationStatus,
    getEnhanceFileList,
    getNearestUnannotatedFileId,
    getTeamLabelInfo,
    queryDataEnhanceList,
    queryFile,
    queryFileOffset,
    queryFirstImg,
    queryLabels as queryLabelApi,
  } from '../api';
  import {
    bbox2Extent,
    extent2Bbox,
    generateBbox,
    generateUuid,
    noop,
    remove,
    replace,
  } from '/@/utils/dubhe';
  import {
    enhanceSymbol,
    labelsSymbol,
    parseAnnotation,
    stringifyAnnotations,
    transformFiles,
  } from '../util';
  import ThumbContainer from '/@/views/mineai/data/dataset-details2/annotate/thumbContainer/index.vue';
  import WorkSpaceContainer from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/index.vue';
  import SettingContainer from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/index.vue';
  import GridViewContainer from '/@/views/mineai/data/dataset-details2/annotate/gridViewContainer/index.vue';
  import { useRoute } from 'vue-router';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { number } from 'vue-types';

  const limit = 35;
  const { createMessage } = useMessage();

  // 获取用户信息
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const currentUserId = userData?.userId || userData?.id || '';

  const route = useRoute();
  const params = route.params;
  const imgId = history.state?.imgId;
  const taskId = history.state?.taskId;
  const fromHistory = history.state?.fromHistory;
  const subtaskId = history.state?.subtaskId;
  const teamLabel = ref(history.state?.team);
  const startOffset = ref(history.state?.startOffset); //起始的偏移量，用于多人标注
  const endOffset = ref(history.state?.endOffset); //结束的偏移量，用于多人标注
  const versionName = ref(history.state?.versionName); // 版本名称，用于查询特定版本的数据
  const isCurrentVersion = ref(history.state?.isCurrentVersion ?? true); // 是否为当前版本
  const thumbRef = ref(null);
  const workspaceRef = ref(null);
  const gridViewRef = ref(null);

  // 简化的加载控制 - 只保留必要的防重复逻辑
  const loadNextPageFlag = ref(false);

  // 缓存加载锁 - 防止快速按D键时重复触发缓存加载
  const cacheLoadingLock = ref(false);

  // 标注类型: 目标跟踪
  const isTrack = false;
  // 标注类型: 语义分割
  const isSegmentation = route.name === 'Segmentation';

  // 当前图片标注个数
  const curAnnoNum = ref([]);
  const containerRef = ref(null);
  const previousBodyOverflow = ref('');
  const previousHtmlOverflow = ref('');
  const previousAppOverflow = ref('');

  // 全局提交状态管理 - 防止重复提交
  const submitState = reactive({
    isSubmitting: false, // 全局是否正在提交
    submittingImages: new Set(), // 正在提交的图片ID集合
    pendingSubmits: new Map(), // 待提交的图片数据 imageId -> data
  });

  // 一键确认状态管理
  const isBatchConfirming = ref(false);

  // 弹窗状态管理 - 防止重复弹窗
  const dialogState = reactive({
    isConfirmDialogOpen: false, // 确认提交弹窗是否打开
    isDeleteConfirmOpen: false, // 删除文件确认tooltip是否打开
  });

  const state = reactive({
    name: params.datasetName,
    error: null, // 错误信息
    files: [], // 当前数据集图片集合
    addFiles: [], // 新增图片集合
    fileFilterType: [101, 104], // 文件筛选状态
    filterLabelId: [], // 文件加标签筛选条件
    total: 0, // 图片总数
    offset: 0, // 当前图片所处的偏移
    noAnnotation: 0, // 未标注图片数量
    haveAnnotation: 0, // 已标注图片数量
    hasMore: true, // 是否有更多列表
    datasetId: Number(params.datasetId),
    currentImgId: Number(imgId) || undefined, // 当前图片 id
    rawAnnotations: [], // 原始标注集合
    annotations: [], // 标注集合
    fileInfo: null, // 文件信息
    fileId: Number(imgId) || undefined,
    currentAnnotationId: '',
    labels: [],
    enhanceList: [], // 数据增强类型
    lastSelectedLabel: undefined, // 上一次选中的 label
    showScore: false, // 展示置信分
    showTag: !isTrack, // 图片展示标签
    showId: isTrack,
    zoom: 1,
    zoomX: 0,
    zoomY: 0,
    selection: false,
    timestamp: Date.now(),
    datasetInfo: {},
    hasEnhanceRecord: false, // 是否有增强记录
    history: [], // 保存新建的记录
    shapes: [], // 图片分割记录
    annotateStatus: [''], // 详情页的筛选状态
    filterUnfinished: false,
    firstOffset: 4, //虽然不知道为什么offset差出来了4
    imgLoading: true,
    firstQuery: true,
    fromHistory: fromHistory || false,
    unannotatedFileIds: [], // 存储所有未标注文件的ID
    changedList: new Set(), // 存储需要更新标注的图片ID
    viewMode: 'annotate', // 新增：视图模式 'annotate' | 'grid'
    versionName: versionName.value, // 版本名称，用于查询特定版本的数据
    isCurrentVersion: isCurrentVersion.value, // 是否为当前版本
    // 未标注图片缓存机制
    unannotatedCache: [], // 缓存的未标注图片列表（最多60张）
    cacheOffset: 0, // 缓存数据的起始offset
    cachePageNumber: 0, // 缓存数据对应的页码
    recentlyFinishedIds: new Set(), // 记录最近完成的图片ID，防止缓存拉取时的竞态
  });

  // 注入全局 labels
  provide(labelsSymbol, toRefs(state).labels);
  provide(enhanceSymbol, toRefs(state).enhanceList);

  // 标注类型
  const annotationType = isSegmentation ? 'shapes' : 'annotations';
  // 形状
  const shapeField = isSegmentation ? 'points' : 'bbox';

  const getFileOffset = async (fileId, query = {}) => {
    let offset;
    try {
      // 如果有版本名称，添加到查询参数中
      const finalQuery = state.versionName ? { ...query, versionName: state.versionName } : query;
      offset = await queryFileOffset(params.datasetId, fileId, finalQuery);
      return offset;
    } catch (e) {
      Object.assign(state, {
        error: new Error('网络错误，请刷新重试'),
      });
    }
  };

  const getPageByOffset = (globalOffset, pageSize = 35) => {
    if (globalOffset === null || globalOffset === undefined || globalOffset < 0) {
      return null;
    }
    if (teamLabel.value && startOffset.value !== undefined && endOffset.value !== undefined) {
      if (globalOffset < startOffset.value || globalOffset > endOffset.value) {
        return null;
      }
      const relativeOffset = globalOffset - startOffset.value;
      return Math.floor(relativeOffset / pageSize) + 1;
    }
    return Math.floor(globalOffset / pageSize) + 1;
  };

  //多人标注用来获取offset的函数，会默认获取当前用户范围内指定状态的第一张图片的offset
  const getOffsetByAnnoStatusStatus = async (annotationStatus) => {
    return await getAbsoluteOffsetForAnnotationStatus(params.datasetId, {
      annotationStatus: annotationStatus,
      //annotationStatus: [104],
      startOffset: startOffset.value,
      total: state.total,
    });
  };

  //存储标注信息
  const countParams = {
    status: '303',
  };

  // 更新当前数据集已标注/未标注图片数量 - 异步非阻塞
  const countDetails = async () => {
    try {
      // 如果有版本名称，添加到请求参数中
      const params = state.versionName
        ? { ...countParams, versionName: state.versionName }
        : countParams;
      const data = await count(state.datasetId, params);
      Object.assign(state, {
        total: Number(data.noAnnotation) + Number(data.haveAnnotation),
        noAnnotation: Number(data.noAnnotation),
        haveAnnotation: Number(data.haveAnnotation),
      });
    } catch (error) {
      console.warn('更新图片统计失败:', error);
    }
  };

  // 更新当前数据集已标注/未标注图片数量 - 团队标注版本
  const countTeamLabelDeatails = async () => {
    try {
      const data = await getTeamLabelInfo(subtaskId);
      Object.assign(state, {
        total: Number(data.totalCount),
        noAnnotation: Number(data.totalCount) - Number(data.finishedCount),
        haveAnnotation: Number(data.finishedCount),
      });
      if (data?.startOffset !== undefined) {
        startOffset.value = Number(data.startOffset);
      }
      if (data?.endOffset !== undefined) {
        endOffset.value = Number(data.endOffset);
      }
    } catch (error) {
      console.warn('更新团队标注统计失败:', error);
    }
  };
  const handleLabelCreated = () => {
    workspaceRef.value.refreshLabelKeyMap();
  };

  // 从查看模式返回标注模式（保持在当前图片位置）
  const handleBackToAnnotate = () => {
    updateState({
      viewMode: 'annotate',
    });
    Message.success('已返回标注模式');
    // 切换后重新聚焦以确保键盘事件能被捕获
    setTimeout(() => {
      if (containerRef.value) {
        containerRef.value.focus();
      }
    }, 100);
  };

  // 切换视图模式
  const toggleViewMode = async () => {
    const newMode = state.viewMode === 'annotate' ? 'grid' : 'annotate';

    // 如果切换到查看模式，显示loading
    if (newMode === 'grid') {
      const loading = Message({
        message: '正在切换到查看模式...',
        type: 'loading',
        duration: 0,
      });

      try {
        // 保存当前筛选条件
        const originalFilterType = state.fileFilterType;

        // 临时设置为显示所有状态
        const needReload = !originalFilterType.includes(101) || !originalFilterType.includes(104);

        if (needReload) {
          // 更新筛选条件为全部
          updateState({
            fileFilterType: [101, 104],
          });

          // 重新加载列表
          await updateList({
            type: [101, 104],
            labelId: state.filterLabelId,
          });
        }

        // 切换模式
        updateState({
          viewMode: newMode,
        });

        // 刷新网格（延迟一下确保DOM已更新）
        await new Promise((resolve) => setTimeout(resolve, 100));

        // 计算当前图片对应的页码并跳转
        // 使用全局offset而不是当前页内的索引
        if (state.currentImgId && gridViewRef.value) {
          // 先在当前页查找图片的页内索引
          const currentFileIndexInPage = state.files.findIndex((f) => f.id === state.currentImgId);

          if (currentFileIndexInPage !== -1 && thumbRef.value) {
            // 获取当前页码
            const currentPage = thumbRef.value.getCurrentPage();
            const annotatePageSize = 35; // 标注模式每页35张

            // 计算全局offset：(当前页-1) * 每页数量 + 页内索引
            const globalOffset = (currentPage - 1) * annotatePageSize + currentFileIndexInPage;

            // 查看模式每页12张图片
            const gridPageSize = 12;
            const targetPage = Math.floor(globalOffset / gridPageSize) + 1;

            await gridViewRef.value?.goToPage(targetPage);
          } else {
            await gridViewRef.value?.refreshGrid();
          }
        } else {
          await gridViewRef.value?.refreshGrid();
        }

        loading.close();
        Message.success('已切换到查看模式');
      } catch (error) {
        loading.close();
        Message.error('切换模式失败: ' + error.message);
        console.error('切换模式失败:', error);
      }
    } else {
      // 切换回标注模式
      updateState({
        viewMode: newMode,
      });
      Message.success('已切换到标注模式');
      // 切换后重新聚焦以确保键盘事件能被捕获
      setTimeout(() => {
        if (containerRef.value) {
          containerRef.value.focus();
        }
      }, 100);
    }
  };

  // 全局键盘事件处理
  const handleGlobalKeydown = (event) => {
    // 检查是否在输入框中
    const target = event.target;
    const isInputElement =
      target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable;

    if (isInputElement) {
      return; // 在输入框中，不处理快捷键
    }

    // 如果在查看模式，只允许Q键
    if (state.viewMode === 'grid') {
      if (event.key.toLowerCase() === 'q' && !event.ctrlKey && !event.shiftKey && !event.altKey) {
        event.preventDefault();
        toggleViewMode();
      }
      // 禁用其他所有快捷键
      return;
    }

    // 标注模式下，Q键切换模式
    if (event.key.toLowerCase() === 'q' && !event.ctrlKey && !event.shiftKey && !event.altKey) {
      event.preventDefault();
      toggleViewMode();
    }

    // C键：切换删除文件确认tooltip（打开/关闭）
    if (event.key.toLowerCase() === 'c' && !event.ctrlKey && !event.shiftKey && !event.altKey) {
      event.preventDefault();
      if (dialogState.isDeleteConfirmOpen) {
        // 如果弹框已打开，则关闭它
        handleCancelDelete();
      } else {
        // 如果弹框未打开，则打开它
        handleShowDeleteConfirm();
      }
    }

    // V键：确认删除文件（仅在tooltip打开时生效）
    if (event.key.toLowerCase() === 'v' && !event.ctrlKey && !event.shiftKey && !event.altKey) {
      event.preventDefault();
      if (dialogState.isDeleteConfirmOpen) {
        handleConfirmDelete();
      }
    }
  };

  //获取数据集图片集合;
  const queryFiles = async (requestParams = {}) => {
    let requestSendOffset = 0;

    // 将 requestParams.type 转换为普通数组（处理 Proxy 的情况）
    const typeArray = requestParams.type
      ? Array.isArray(requestParams.type)
        ? [...requestParams.type]
        : [requestParams.type]
      : [];

    // 判断逻辑优化
    const isAllTypes =
      typeArray.length === 0 || // 无类型
      (typeArray.includes(101) && typeArray.includes(104)); // 同时包含101和104

    const isType101Only = typeArray.length === 1 && typeArray[0] === 101;
    const isType104Only = typeArray.length === 1 && typeArray[0] === 104;

    if (teamLabel.value && startOffset.value !== undefined && !isNil(requestParams.offset)) {
      // 团队标注：传入的offset是相对offset，需要加上startOffset
      requestSendOffset = requestParams.offset + startOffset.value;
    } else if (!isNil(requestParams.offset)) {
      // 从外部传入的offset，直接使用
      requestSendOffset = requestParams.offset;
    } else if (imgId && state.firstQuery) {
      // 从中间一张图片带着某个图片id跳转过来的情况
      const query = requestParams.type ? { type: requestParams.type } : {};
      requestSendOffset = await getFileOffset(imgId, query);
    } else {
      // 默认从0开始
      requestSendOffset = 0;
    }

    //多人标注的请求需要限制limit
    let filesParams = null;
    // 优先使用传入的limit，否则使用默认的limit
    let requestLimit = requestParams.limit || limit;

    // 多人标注场景：限制limit不超过分配的范围
    if (teamLabel.value && startOffset.value !== undefined && endOffset.value !== undefined) {
      // endOffset是包含的（最后一张图片的索引），所以需要+1来计算总数
      // 剩余数量 = (endOffset + 1) - requestSendOffset
      const remainingInRange = endOffset.value + 1 - requestSendOffset;
      if (remainingInRange > 0) {
        requestLimit = Math.min(requestLimit, remainingInRange);
      } else {
        // 已超出分配范围，不请求数据
        requestLimit = 0;
      }
    }

    //先展开requestParams，再用计算后的值覆盖
    filesParams = { ...requestParams, limit: requestLimit, offset: requestSendOffset };

    // 如果有版本名称，添加到请求参数中
    if (state.versionName) {
      filesParams.versionName = state.versionName;
    }

    const rawFiles = await detectFileList(params.datasetId, filesParams);

    // 进入页面首次获取图片之后，设置标志为false
    if (state.firstQuery) {
      state.firstQuery = false;
    }

    return rawFiles;
  };

  // 查询标签
  const queryLabels = async (requestParams = {}) => {
    // 如果有版本名称，添加到请求参数中
    const finalParams = state.versionName
      ? { ...requestParams, versionName: state.versionName }
      : requestParams;
    const labels = await queryLabelApi(params.datasetId, finalParams);
    return labels || [];
  };

  // 新建标签
  const createLabel = async (labelParams = {}) => {
    // 如果是历史版本，禁止创建标签
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法创建新标签！');
      return null;
    }

    const result = await createLabelApi(params.datasetId, labelParams);
    return result;
  };

  // 根据异步结果更新状态
  const updateState = (params, callback = noop) => {
    // 区分函数式更新和对象更新
    if (typeof params === 'function') {
      const next = params(state);
      Object.assign(state, next);
      callback(state);
      return;
    }

    // 普通更新
    Object.assign(state, params);
    callback(state);
  };

  // 选择当前合适的标注结果
  const selectAnnotation = () => {
    return isSegmentation ? state.shapes : state.annotations;
  };

  // 根据 labelId 获取标签颜色
  const getColorLabel = (labelId) => {
    return (state.labels.find((label) => label.id === labelId) || {}).color || '#000000';
  };

  // 根据 labelId 获取标签名称
  const getLabelName = (labelId) => {
    return (state.labels.find((label) => label.id === labelId) || {}).name || '';
  };

  // 选择标签，更新标注
  const handleSelectLabel = ({ selectedLabel, curAnnotation }) => {
    // 更新 label 之后的标注
    const withLabelAnnotation = {
      ...curAnnotation,
      data: {
        ...curAnnotation.data,
        categoryId: selectedLabel.value,
        color: getColorLabel(selectedLabel.value),
      },
    };
    const curAnnotationIndex = state[annotationType].findIndex((d) => d.id === curAnnotation.id);
    if (curAnnotationIndex !== -1) {
      const updateAnnotations = replace(
        state[annotationType],
        curAnnotationIndex,
        withLabelAnnotation,
      );

      updateState({
        [annotationType]: updateAnnotations,
        lastSelectedLabel: selectedLabel.value,
      });
    }
  };

  // 清理已有标注记录
  const clearHistory = () => {
    updateState({
      history: [],
      rawAnnotations: [],
      annotations: [],
      fileInfo: null, // 当前文件信息
      error: null, // 清空已有错误信息
    });
  };

  // 切换当前图片
  const changeCurrentImg = (file) => {
    if (!file || !file.id) {
      return;
    }

    const { annotation = '[]', id } = file;

    updateState({
      currentImgId: id,
    });

    // 位置记录的更新由watch(state.currentImgId)统一处理
    // 不在这里调用updateImageInfo，由watch(currentImgId)统一处理，避免重复调用info接口
    clearHistory();
  };

  // 修改queryNextPage函数分页 - 支持未标注图片缓存
  const queryNextPage = (requestParams = {}) => {
    const isUnannotatedOnly =
      requestParams.type && requestParams.type.toString() === [101].toString();

    // 计算当前请求的页码
    const currentPageNumber = Math.floor((requestParams.offset || 0) / 35) + 1;

    // 检查是否需要强制跳转到指定offset（不使用缓存）
    const forceOffset = requestParams._forceOffset;

    // 如果需要强制跳转，直接请求指定offset的数据
    if (forceOffset !== undefined && forceOffset >= 0) {
      return queryFiles({
        ...requestParams,
        offset: forceOffset,
        limit: 35,
      })
        .then((res) => {
          const { result = [] } = res;
          const files = transformFiles(result);

          updateState({
            files: files,
            offset: forceOffset,
            hasMore: files.length >= 35,
            unannotatedCache: [], // 清空缓存
            cacheOffset: 0,
            cachePageNumber: 0,
          });

          return {
            files: files,
            raw: result,
          };
        })
        .catch((error) => {
          return {
            files: [],
            raw: [],
            error: true,
          };
        });
    }

    // 如果是未标注筛选模式，使用缓存机制
    if (isUnannotatedOnly) {
      // 检查缓存加载锁，防止重复触发
      if (cacheLoadingLock.value) {
        return Promise.resolve({
          files: state.files,
          raw: state.files,
        });
      }

      // 检查是否翻页了（页码变化）
      const isPageChanged =
        state.cachePageNumber !== 0 && state.cachePageNumber !== currentPageNumber;

      // 翻页时清空缓存，重新加载
      if (isPageChanged) {
        // 设置缓存加载锁
        cacheLoadingLock.value = true;

        // 清空缓存并请求新页的100张数据
        // 根据当前页码计算起始offset：(页码-1) * 每页数量
        const unannotatedOffset = (currentPageNumber - 1) * 35;
        const cacheRequestParams = {
          ...requestParams,
          limit: 100,
          offset: unannotatedOffset, // 使用计算后的offset
        };

        return queryFiles(cacheRequestParams)
          .then((res) => {
            const { result = [] } = res;
            const files = transformFiles(result);

            const displayFiles = files.slice(0, 35);
            const cacheFiles = files.slice(35);

            updateState({
              files: displayFiles,
              offset: requestParams.offset || 0,
              hasMore: result.length >= 100,
              unannotatedCache: cacheFiles,
              cachePageNumber: currentPageNumber,
            });

            return {
              files: displayFiles,
              raw: result.slice(0, 35),
            };
          })
          .catch((error) => {
            return {
              files: [],
              raw: [],
              error: true,
            };
          })
          .finally(() => {
            // 释放缓存加载锁
            cacheLoadingLock.value = false;
          });
      }

      // 同一页且缓存可用
      if (state.cachePageNumber === currentPageNumber && state.unannotatedCache.length > 0) {
        // 从缓存中取35张
        const cachedFiles = state.unannotatedCache.slice(0, 35);

        updateState({
          files: cachedFiles,
          offset: requestParams.offset || 0,
          hasMore: state.unannotatedCache.length >= 35 || state.hasMore,
        });

        return Promise.resolve({
          files: cachedFiles,
          raw: cachedFiles,
        });
      }

      // 首次加载或缓存不可用，请求100张并缓存
      // 设置缓存加载锁
      cacheLoadingLock.value = true;

      // 根据当前页码计算起始offset
      const unannotatedOffset = (currentPageNumber - 1) * 35;
      const cacheRequestParams = {
        ...requestParams,
        limit: 100, // 预加载100张
        offset: unannotatedOffset, // 使用计算后的offset
      };
      console.log('首次/缓存失效请求 - 页码:', currentPageNumber, 'offset:', unannotatedOffset);

      return queryFiles(cacheRequestParams)
        .then((res) => {
          const { result = [] } = res;
          const files = transformFiles(result);

          // 前35张用于显示，剩余的放入缓存
          const displayFiles = files.slice(0, 35);
          const cacheFiles = files.slice(35);

          updateState({
            files: displayFiles,
            offset: requestParams.offset || 0,
            hasMore: result.length >= 100,
            unannotatedCache: cacheFiles, // 缓存剩余的图
            cachePageNumber: currentPageNumber,
          });

          return {
            files: displayFiles,
            raw: result.slice(0, 35),
          };
        })
        .catch((error) => {
          return {
            files: [],
            raw: [],
            error: true,
          };
        })
        .finally(() => {
          // 释放缓存加载锁
          cacheLoadingLock.value = false;
        });
    }

    // 非未标注筛选模式，清空缓存并正常请求
    return queryFiles(requestParams)
      .then((res) => {
        const { result = [] } = res;
        const files = transformFiles(result);

        updateState({
          files: files,
          offset: requestParams.offset || 0,
          hasMore: files.length >= (requestParams.limit || 35),
          unannotatedCache: [], // 清空缓存
          cacheOffset: 0,
          cachePageNumber: 0,
        });

        return {
          files: files,
          raw: result,
        };
      })
      .catch((error) => {
        return {
          files: [],
          raw: [],
          error: true,
        };
      });
  };

  // 请求下一页数据 - 修复offset传递
  const getNextPage = () => {
    // 严格检查加载状态
    if (loadNextPageFlag.value === true) {
      return;
    }

    // 立即设置标志，防止并发
    loadNextPageFlag.value = true;

    queryNextPage({
      offset: state.offset,
      limit: 35,
      type: state.fileFilterType,
      labelId: state.filterLabelId,
    }).finally(() => {
      // 确保总是重置标志
      loadNextPageFlag.value = false;
    });
  };

  // 键盘前后切换图片到底部时请求下一页数据
  // 分页模式下不需要预加载，由list.vue的分页器控制
  const handleNextPage = (index, fileList) => {
    // 分页模式下禁用自动加载
    return;
  };

  // 优化的检查标注变化函数
  const hasAnnotationChanges = () => {
    // 如果curAnnoNum.value未初始化，说明是新加载的图片，没有变化
    if (!curAnnoNum.value || curAnnoNum.value.length === 0) {
      const currentAnnotations = state[annotationType] || [];
      // 如果原来没有标注但现在有标注，说明有变化
      return currentAnnotations.length > 0;
    }

    // 比较当前标注和初始标注
    const currentAnnotations = state[annotationType] || [];
    const originalAnnotations = curAnnoNum.value || [];

    // 首先比较数量
    if (currentAnnotations.length !== originalAnnotations.length) {
      return true;
    }

    // 如果数量相同但都为0，没有变化
    if (currentAnnotations.length === 0) {
      return false;
    }

    // 然后比较内容（去除一些可能变化但不重要的字段）
    const normalizeAnnotation = (ann) => {
      return {
        ...ann,
        data: {
          ...ann.data,
          // 移除一些可能因为重新渲染而变化的字段
        },
      };
    };

    const currentNormalized = currentAnnotations.map(normalizeAnnotation);
    const originalNormalized = originalAnnotations.map(normalizeAnnotation);

    return JSON.stringify(currentNormalized) !== JSON.stringify(originalNormalized);
  };

  // 获取当前图片的状态信息
  const getCurrentImageStatus = (imageId) => {
    const currentFile = state.files.find((f) => f.id === imageId);
    return currentFile ? currentFile.status : null;
  };

  // 修复后的异步提交函数 - 只在状态真正改变时更新计数
  const submitAnnotationAsync = async (imageId, annotationData) => {
    // 检查是否已经在提交中
    if (submitState.submittingImages.has(imageId)) {
      return;
    }

    // 标记为提交中
    submitState.submittingImages.add(imageId);

    // 获取提交前的图片状态
    const beforeStatus = getCurrentImageStatus(imageId);
    const wasUnannotated = beforeStatus === 101; // 101表示未标注，104表示已标注

    try {
      await maHttp.post(
        {
          url: teamLabel.value
            ? `/datasets/team/${params.datasetId}/${imageId}/${taskId}/${subtaskId}/annotations`
            : `datasets/files/${params.datasetId}/${imageId}/annotations/finish`,
          params:
            teamLabel.value && startOffset.value !== undefined && endOffset.value !== undefined
              ? {
                  expectedStartOffset: startOffset.value,
                  expectedEndOffset: endOffset.value,
                }
              : undefined,
          data: annotationData,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );

      // 只有当图片从未标注变为已标注时，才更新统计数据
      if (wasUnannotated) {
        updateState({
          noAnnotation: Math.max(0, state.noAnnotation - 1),
          haveAnnotation: state.haveAnnotation + 1,
          // total 保持不变
        });

        // 更新files数组中对应图片的状态
        const fileIndex = state.files.findIndex((f) => f.id === imageId);
        if (fileIndex > -1) {
          const updatedFiles = [...state.files];
          updatedFiles[fileIndex] = {
            ...updatedFiles[fileIndex],
            status: 104, // 更新为已标注状态
          };
          updateState({
            files: updatedFiles,
          });
        }

        // 提交成功后，如果是未标注筛选模式，从缓存补充新的未标注图片
        if (state.fileFilterType.toString() === ['101'].toString()) {
          // 将当前图片ID加入“最近完成”集合，防止被offset=0的请求重新抓回来
          state.recentlyFinishedIds.add(imageId);
          setTimeout(() => {
            state.recentlyFinishedIds.delete(imageId);
          }, 8000); // 延长到8秒，确保索引同步

          // 优先从缓存补充
          if (state.unannotatedCache.length > 0) {
            // 从缓存中找到第一个未标注状态的图片（status === 101）
            let newFile = null;
            let newFileIndex = -1;

            for (let i = 0; i < state.unannotatedCache.length; i++) {
              const file = state.unannotatedCache[i];
              // 使用status判断是否为未标注图片，而不是annotation是否为空
              // 因为自动标注的图片可能有annotation但status仍为101（未确认）
              if (file.status === 101) {
                newFile = file;
                newFileIndex = i;
                break;
              }
            }

            if (newFile) {
              // 移除已使用的缓存图片
              const remainingCache = [
                ...state.unannotatedCache.slice(0, newFileIndex),
                ...state.unannotatedCache.slice(newFileIndex + 1),
              ];

              // 移除已标注的图片，添加缓存中的新图片
              const updatedFiles = state.files.filter((f) => f.id !== imageId);
              updatedFiles.push(newFile);

              updateState({
                files: updatedFiles,
                unannotatedCache: remainingCache,
              });

              // 检查是否需要预加载：1. 缓存不足20张 2. 后端还有货 3. 未加锁
              if (remainingCache.length < 20 && state.hasMore && !cacheLoadingLock.value) {
                // 立即设置缓存加载锁，防止重复预加载
                cacheLoadingLock.value = true;

                queryFiles({
                  offset: 0, // 始终从0开始拉取
                  limit: 100, // 单次拉取大量数据
                  type: [101],
                  labelId: state.filterLabelId,
                })
                  .then((res) => {
                    const { result = [] } = res;
                    const newFiles = transformFiles(result);

                    // 增强去重逻辑
                    const filteredNewFiles = newFiles.filter((f) => {
                      const isInFiles = state.files.some((existing) => existing.id === f.id);
                      const isInCache = state.unannotatedCache.some(
                        (existing) => existing.id === f.id,
                      );
                      const isRecentlyFinished = state.recentlyFinishedIds.has(f.id);
                      return !isInFiles && !isInCache && !isRecentlyFinished;
                    });

                    // 合并并截断缓存，保持在100张左右
                    const combinedCache = [...state.unannotatedCache, ...filteredNewFiles].slice(
                      0,
                      100,
                    );

                    updateState({
                      unannotatedCache: combinedCache,
                      hasMore: result.length >= 100,
                    });
                  })
                  .catch(() => {
                    // 加载失败不影响当前操作
                  })
                  .finally(() => {
                    // 释放缓存加载锁
                    cacheLoadingLock.value = false;
                  });
              }
            } else {
              // 缓存中没有可用图片
              if (state.hasMore && thumbRef.value && thumbRef.value.reloadCurrentPage) {
                await thumbRef.value.reloadCurrentPage();
              } else {
                // 没货了或不需要刷新，手动从本地列表移除该图片
                const updatedFiles = state.files.filter((f) => f.id !== imageId);
                updateState({ files: updatedFiles });
              }
            }
          } else {
            // 缓存为空
            if (state.hasMore && thumbRef.value && thumbRef.value.reloadCurrentPage) {
              await thumbRef.value.reloadCurrentPage();
            } else {
              // 没货了或不需要刷新，手动从本地列表移除该图片
              const updatedFiles = state.files.filter((f) => f.id !== imageId);
              updateState({ files: updatedFiles });
            }
          }
        }

        // 检查是否需要切换筛选视图
        if (state.filterUnfinished && state.noAnnotation === 0) {
          // 多人标注模式下，不自动切换视图，只给出提示，避免再次触发加载
          if (teamLabel.value) {
            Message.success('当前分配的图片已全部标注完成，请前往提交任务');
          } else {
            Message.success('所有未标注图片均已完成，已为您切换到"全部"视图。');
            if (thumbRef.value) {
              thumbRef.value.changeFilterAndReload([101, 104]);
            }
          }
        }
      } else {
        // 已标注图片的修改不影响统计，但仍然显示成功消息
      }
    } catch (error) {
      if ('任务区间已变更，请刷新页面后重试' != error.message) {
        Message.error(
          `提交失败，可能为提交过快导致失败，请降低速度并刷新页面: ${error.message || '未知错误'}`,
        );
      }
    } finally {
      // 移除提交标记
      submitState.submittingImages.delete(imageId);
    }
  };

  // 列表加载完成后，强制触发标注框重绘
  const handleListLoadingEnd = () => {
    // 通过更新 timestamp 强制触发 watch，重新加载标注信息
    // 这会触发 watch(() => [state.currentImgId, state.timestamp], ...)
    // 从而重新渲染标注框
    setTimeout(() => {
      updateState({
        timestamp: Date.now(),
      });
    }, 100);
  };

  // 完全异步的导航处理 - 绝不等待提交
  // skipConfirmDialog: 是否跳过确认对话框
  // - true: 跳过对话框（s键、d键、其他快捷操作）
  // - false: 不跳过，满足条件时显示对话框（仅用于上一张图片，当当前图片未标注且有标注内容时）
  const handleChangeImg = async (
    oldIndex,
    item,
    index,
    confirmed,
    callback,
    skipConfirmDialog = false,
  ) => {
    // 防止重复弹窗
    if (dialogState.isConfirmDialogOpen) {
      return;
    }

    // 如果正在翻页加载，等待完成
    if (loadNextPageFlag.value && !confirmed) {
      let waitCount = 0;
      while (loadNextPageFlag.value && waitCount < 30) {
        await new Promise((resolve) => setTimeout(resolve, 100));
        waitCount++;
      }
    }

    if (item.id === state.currentImgId) {
      return;
    }

    const currentImg = state.files.find((f) => f.id === state.currentImgId);
    const isCurrentImgInUnannotated = currentImg?.status === 101;

    // 检查是否有未保存的修改
    const hasChanges = hasAnnotationChanges();

    // 保存当前图片ID，防止在切换过程中被更新
    const currentImgId = state.currentImgId;

    // 只有真正有变化时才添加到 changedList
    if (hasChanges) {
      state.changedList.add(currentImgId);
    }

    // 如果是未标注图片且有标注内容，询问是否提交
    // skipConfirmDialog=true 时跳过确认对话框（用于上一张图片）
    // 注意：如果currentImg不存在（跨页情况），跳过此检查
    // 重要：检查的是目标图片(item)的状态，而不是当前图片(currentImg)的状态
    const targetImgInUnannotated = item?.status === 101;

    if (
      currentImg &&
      !skipConfirmDialog &&
      isCurrentImgInUnannotated &&
      !targetImgInUnannotated && // 目标图片不是未标注状态（即已标注或其他状态）
      JSON.stringify(state[annotationType]) === JSON.stringify(curAnnoNum.value) &&
      state[annotationType].length > 0 &&
      !confirmed
    ) {
      // 设置弹窗状态为打开
      dialogState.isConfirmDialogOpen = true;

      ElMessageBox.confirm(
        `是否需要提交当前标注内容?<br>(若需要快速提交可以使用上方完成按钮或快捷键S)`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true,
        },
      )
        .then(() => {
          // 异步提交，不等待结果
          const annotationData = {
            annotation: stringifyAnnotations(state[annotationType].map(rescale)),
          };
          submitAnnotationAsync(state.currentImgId, annotationData);

          // 立即切换图片，不等待提交
          if (hasChanges) {
            state.changedList.add(currentImgId);
          }
          item?.id && changeCurrentImg(item);
          isFunction(callback) && callback(item);
          handleNextPage(index, state.files);
        })
        .catch(() => {
          // 用户取消提交，直接切换
          item?.id && changeCurrentImg(item);
          isFunction(callback) && callback(item);
          handleNextPage(index, state.files);
        })
        .finally(() => {
          // 无论确认还是取消，都要重置弹窗状态
          dialogState.isConfirmDialogOpen = false;
        });
      return;
    }

    // 如果有变化且不是确认状态，异步提交
    if (hasChanges && !confirmed) {
      const msg = checkAnnotationValid();
      if (!msg) {
        const annotationData = {
          annotation: stringifyAnnotations(state[annotationType].map(rescale)),
        };
        // 异步提交，不等待
        submitAnnotationAsync(state.currentImgId, annotationData);
      }
    }

    // 立即执行图片切换，不等待任何请求
    item?.id && changeCurrentImg(item);
    isFunction(callback) && callback(item);
    handleNextPage(index, state.files);

    // 处理确认状态的后续逻辑
    if (confirmed) {
      if (hasChanges) {
        state.changedList.add(currentImgId);
      }
    }

    return null;
  };

  let msgInstance = null;

  const onMessageClose = () => {
    // 清理 message 实例
    msgInstance = null;
  };

  const findImgIndex = () => {
    const { files, currentImgId } = state;
    const currentImgIndex = files.findIndex((d) => d.id === currentImgId);
    return { currentImgIndex, files };
  };

  // 简化的导航函数 - 直接切换，异步处理提交（支持跨页）
  const handleUp = async () => {
    if (!thumbRef.value || !thumbRef.value.getCurrentPage) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是第一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    const { files, currentImgIndex } = findImgIndex();
    const currentPage = thumbRef.value.getCurrentPage();
    const pageSize = 35;

    // 如果找不到当前图片（跨页加载期间state.files和currentImgId不匹配），直接返回
    if (currentImgIndex === -1) {
      return;
    }

    // 判断是否为分配范围内的第一张图片
    // 多人标注：检查是否在第一页且是页内第一张
    // 单人标注：检查是否在第一页且是页内第一张
    // 注意：这里的判断是针对分配范围的第一张，不是数据页的第一张
    const isFirstInRange = currentPage === 1 && currentImgIndex === 0;

    if (isFirstInRange) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是第一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    // 检查是否需要跨页（当前是页内第一张，但不是全局第一张）
    if (currentImgIndex === 0) {
      // 立即设置导航状态（同步），确保后续点击被阻止
      state.isNavigating = true;

      try {
        // 跳转到上一页最后一张
        const success = await thumbRef.value.goToPageWithoutAutoSelect(currentPage - 1);
        if (success && state.files.length > 0) {
          const lastFile = state.files[state.files.length - 1];
          changeCurrentImg(lastFile);
          if (thumbRef.value.scrollToCurrentImage) {
            thumbRef.value.scrollToCurrentImage();
          }
        }
      } finally {
        // 清除导航状态
        state.isNavigating = false;
      }
      return;
    } else {
      // 当前页内切换到上一张
      const targetIndex = currentImgIndex - 1;
      const targetFile = files[targetIndex];
      if (targetFile) {
        changeCurrentImg(targetFile);
      }
    }
  };

  const handleDown = async () => {
    // 如果正在导航中，禁止操作
    if (state.isNavigating) {
      return;
    }

    const { files, currentImgIndex } = findImgIndex();

    if (currentImgIndex < 0) {
      return;
    }

    // 多人标注模式下：当前图片是列表最后一张，且不存在未加载图片（hasMore 为 false），
    // 说明当前用户分配的范围已经全部完成，不再触发翻页加载
    if (teamLabel.value && !state.hasMore && currentImgIndex >= files.length - 1) {
      Message.success('当前分配的图片已全部标注完成，请在前往提交任务');
      return;
    }

    if (!thumbRef.value || !thumbRef.value.getCurrentPage) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是最后一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    const currentPage = thumbRef.value.getCurrentPage();
    const totalPages = thumbRef.value.getTotalPages();

    // 检查是否是全局最后一张：最后一页且当前为该页最后一张
    if (currentPage === totalPages && currentImgIndex >= state.files.length - 1) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是最后一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    // 检查是否需要翻页：当前图片是本页最后一张
    if (currentImgIndex >= state.files.length - 1) {
      if (currentPage < totalPages) {
        // 立即设置导航状态（同步），确保后续点击被阻止
        state.isNavigating = true;

        try {
          const success = await thumbRef.value.goToPageWithoutAutoSelect(currentPage + 1);
          if (success && state.files.length > 0) {
            const firstFile = state.files[0];
            changeCurrentImg(firstFile);
            if (thumbRef.value.scrollToCurrentImage) {
              thumbRef.value.scrollToCurrentImage();
            }
          }
        } finally {
          // 清除导航状态
          state.isNavigating = false;
        }
        return;
      }
    } else {
      // 当前页内切换到下一张
      const targetIndex = currentImgIndex + 1;
      const targetFile = files[targetIndex];
      if (targetFile) {
        changeCurrentImg(targetFile);
      }
    }
  };

  // 辅助函数：使用动态limit加载图片，现在不用了
  const queryNextPageWithDynamicLimit = async (dynamicLimit) => {
    let requestSendOffset = 0;
    const typeArray = state.fileFilterType
      ? Array.isArray(state.fileFilterType)
        ? [...state.fileFilterType]
        : [state.fileFilterType]
      : [];

    const isAllTypes =
      typeArray.length === 0 || (typeArray.includes(101) && typeArray.includes(104));
    const isType101Only = typeArray.length === 1 && typeArray[0] === 101;
    const isType104Only = typeArray.length === 1 && typeArray[0] === 104;

    if (teamLabel.value && startOffset.value !== undefined) {
      requestSendOffset = state.offset + startOffset.value;
    } else {
      requestSendOffset = state.offset;
    }

    let filesParams = null;
    if (teamLabel.value) {
      let newLimit = -1;
      if (isAllTypes) {
        newLimit = Math.min(dynamicLimit, state.total - state.files.length);
      } else if (isType101Only) {
        newLimit = Math.min(dynamicLimit, state.noAnnotation - state.files.length);
      } else if (isType104Only) {
        newLimit = Math.min(dynamicLimit, state.haveAnnotation - state.files.length);
      }

      filesParams = {
        offset: requestSendOffset,
        limit: newLimit,
        type: state.fileFilterType,
        labelId: state.filterLabelId,
      };
    } else {
      filesParams = {
        limit: dynamicLimit,
        offset: requestSendOffset,
        type: state.fileFilterType,
        labelId: state.filterLabelId,
      };
    }

    if (state.versionName) {
      filesParams.versionName = state.versionName;
    }

    const rawFiles = await detectFileList(params.datasetId, filesParams);
    const { result = [] } = rawFiles;
    const addFiles = transformFiles(result);

    // 过滤重复文件
    const currentIds = new Set(state.files.map((f) => f.id));
    const filteredAddFiles = addFiles.filter((f) => !currentIds.has(f.id));

    const newOffset = state.offset + result.length;
    const nextState = {
      addFiles: filteredAddFiles,
      files: state.files.concat(filteredAddFiles),
      offset: newOffset,
      hasMore: result.length >= dynamicLimit,
    };

    // 多人标注范围检查
    if (teamLabel.value) {
      const totalTargetCount = isAllTypes
        ? state.total
        : isType101Only
        ? state.noAnnotation
        : isType104Only
        ? state.haveAnnotation
        : 0;

      const isExceedLimit = nextState.files.length >= totalTargetCount;
      nextState.hasMore = !isExceedLimit && result.length >= dynamicLimit;
    }

    updateState(nextState);
    return nextState;
  };

  const handleJumpToUnannotated = async () => {
    const types = state.fileFilterType;
    const showBtn = Array.isArray(types) && types.includes(101) && types.includes(104);
    if (!showBtn) return;

    // 检查数据是否已加载完成
    if (!state.files || state.files.length === 0 || !state.currentImgId) {
      Message.warning('数据加载中，请稍候再试');
      return;
    }

    if (state.noAnnotation <= 0) {
      Message.info('未找到未标注图片');
      return;
    }

    const currentFile = state.files.find((f) => f.id === state.currentImgId);

    // 如果当前图片已是未标注，则无需跳转
    if (currentFile && currentFile.status === 101) {
      Message.info('当前图片已是未标注');
      return;
    }

    let loadingMessage = null;
    try {
      loadingMessage = Message({
        message: '正在查找并跳转未标注图片...',
        type: 'info',
        duration: 0,
        iconClass: 'el-icon-loading',
      });

      const apiParams = {
        currentFileId: state.currentImgId,
        labelId: state.filterLabelId,
      };

      if (state.versionName) {
        apiParams.versionName = state.versionName;
      }

      // 获取最近未标注图片的ID
      const result = await getNearestUnannotatedFileId(params.datasetId, apiParams);

      if (!result || !result.fileId) {
        // 多人标注场景：如果后端API没有返回结果，遍历所有分配的页面查找未标注图片
        if (teamLabel.value && thumbRef.value) {
          const totalAssignedCount = endOffset.value - startOffset.value + 1;
          const totalPages = Math.ceil(totalAssignedCount / 35);
          const currentPage = thumbRef.value.getCurrentPage();

          // 从当前页的下一页开始查找，然后循环回到第一页
          for (let i = 1; i <= totalPages; i++) {
            const pageToCheck = ((currentPage - 1 + i) % totalPages) + 1;

            // 加载该页数据
            const success = await thumbRef.value.goToPageWithoutAutoSelect(pageToCheck);
            if (success && state.files.length > 0) {
              const unannotated = state.files.find((f) => f.status === 101);
              if (unannotated) {
                loadingMessage.close();
                changeCurrentImg(unannotated);
                if (thumbRef.value.scrollToCurrentImage) {
                  thumbRef.value.scrollToCurrentImage();
                }
                Message.success('已跳转至未标注图片');
                return;
              }
            }
          }
        }

        loadingMessage.close();
        Message.info('未找到未标注图片');
        return;
      }

      const targetFileId = result.fileId;

      // 获取目标图片的全局偏移量
      const offsetParams = {
        type: state.fileFilterType,
        labelId: state.filterLabelId,
      };

      if (teamLabel.value && startOffset.value !== undefined) {
        offsetParams.startOffset = startOffset.value;
        offsetParams.endOffset = endOffset.value;
      }

      if (state.versionName) {
        offsetParams.versionName = state.versionName;
      }

      const targetOffset = await queryFileOffset(params.datasetId, targetFileId, offsetParams);

      if (targetOffset === null || targetOffset === undefined) {
        loadingMessage.close();
        Message.error('无法获取图片位置信息');
        return;
      }

      // 计算目标图片所在的页码（多人标注下需将全局offset转换为区间内相对offset）
      const pageSize = 35;
      const targetPage = getPageByOffset(targetOffset, pageSize);
      if (!targetPage) {
        loadingMessage.close();
        Message.warning('目标图片不在当前分配区间内');
        return;
      }

      // 加载目标页的数据
      if (thumbRef.value && thumbRef.value.goToPageWithoutAutoSelect) {
        const success = await thumbRef.value.goToPageWithoutAutoSelect(targetPage);
        console.log('success', success, state.files, state.files.length);
        if (success && state.files.length > 0) {
          // 在新加载的页面数据中查找目标图片
          const targetFile = state.files.find((f) => f.id === targetFileId);

          if (targetFile) {
            loadingMessage.close();
            changeCurrentImg(targetFile);

            // 滚动列表到目标图片
            if (thumbRef.value.scrollToCurrentImage) {
              thumbRef.value.scrollToCurrentImage();
            }

            Message.success('已跳转至下一个未标注图片');
            return;
          }
        }
      }

      loadingMessage.close();
      Message.error('跳转失败，请重试');
    } catch (error) {
      if (loadingMessage) {
        loadingMessage.close();
      }
      console.error('获取最近未标注图片失败:', error);
      Message.error('跳转失败，请重试');
    }
  };

  const handlePrev = async () => {
    // 如果正在导航中，禁止操作
    if (state.isNavigating) {
      return;
    }

    if (!thumbRef.value || !thumbRef.value.getCurrentPage) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是第一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    const { files, currentImgIndex } = findImgIndex();
    const currentPage = thumbRef.value.getCurrentPage();

    // 如果找不到当前图片（跨页加载期间state.files和currentImgId不匹配），直接返回
    if (currentImgIndex === -1) {
      return;
    }

    // 判断是否为分配范围内的第一张图片
    // 多人标注：检查是否在第一页且是页内第一张
    // 单人标注：检查是否在第一页且是页内第一张
    // 注意：这里的判断是针对分配范围的第一张，不是数据页的第一张
    const isFirstInRange = currentPage === 1 && currentImgIndex === 0;

    if (isFirstInRange) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是第一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    if (currentImgIndex === 0) {
      // 当前是页内第一张，需要跳转到上一页
      const targetPage = currentPage - 1;

      // 立即设置导航状态（同步），确保后续点击被阻止
      state.isNavigating = true;

      try {
        const success = await thumbRef.value.goToPageWithoutAutoSelect(targetPage);
        if (success && state.files.length > 0) {
          const lastFile = state.files[state.files.length - 1];
          changeCurrentImg(lastFile);
          if (thumbRef.value.scrollToCurrentImage) {
            thumbRef.value.scrollToCurrentImage();
          }
        }
      } finally {
        // 清除导航状态
        state.isNavigating = false;
      }
      return;
    } else {
      const targetIndex = currentImgIndex - 1;
      const targetFile = files[targetIndex];
      if (targetFile) {
        handleChangeImg(currentImgIndex, targetFile, targetIndex, false, undefined, false);
      }
    }
  };

  // 统一的下一张处理函数（支持跨页）
  // shouldConfirm: true表示s键（已经提交过了），false表示d键（需要在切换时提交）
  const handleNext = async (shouldConfirm = false) => {
    // 如果正在导航中，禁止操作
    if (state.isNavigating) {
      return;
    }

    const { files, currentImgIndex } = findImgIndex();

    if (currentImgIndex < 0) {
      return;
    }

    // 多人标注模式下：当前图片是列表最后一张，且不存在未加载图片（hasMore 为 false），
    // 说明当前用户分配的范围已经全部完成，不再触发翻页加载
    if (teamLabel.value && !state.hasMore && currentImgIndex >= files.length - 1) {
      Message.success('当前分配的图片已全部标注完成，请前往提交任务');
      return;
    }

    if (!thumbRef.value || !thumbRef.value.getCurrentPage) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是最后一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    const currentPage = thumbRef.value.getCurrentPage();
    const totalPages = thumbRef.value.getTotalPages();

    // 检查是否是全局最后一张：最后一页且当前为该页最后一张
    if (currentPage === totalPages && currentImgIndex >= state.files.length - 1) {
      if (!msgInstance) {
        msgInstance = Message.warning({
          message: '当前图片已是最后一张',
          onClose: onMessageClose,
        });
      }
      return;
    }

    // 检查是否需要翻页：当前图片是本页最后一张
    if (currentImgIndex >= state.files.length - 1) {
      // 跳转到下一页第一张
      if (currentPage < totalPages) {
        // 立即设置导航状态（同步），确保后续点击被阻止
        state.isNavigating = true;

        try {
          const success = await thumbRef.value.goToPageWithoutAutoSelect(currentPage + 1);
          if (success && state.files.length > 0) {
            const firstFile = state.files[0];
            changeCurrentImg(firstFile);
            if (thumbRef.value.scrollToCurrentImage) {
              thumbRef.value.scrollToCurrentImage();
            }
          }
        } finally {
          // 清除导航状态
          state.isNavigating = false;
        }
        return;
      }
    } else {
      // 当前页内切换到下一张
      const targetIndex = currentImgIndex + 1;
      const targetFile = files[targetIndex];
      if (targetFile) {
        // shouldConfirm=true 表示已经提交过了（S键），传递confirmed=true避免重复提交
        // shouldConfirm=false 表示需要在切换时提交（D键），传递confirmed=false让handleChangeImg处理提交
        handleChangeImg(currentImgIndex, targetFile, targetIndex, shouldConfirm, undefined, true);
      }
    }
  };

  // 修改updateList - 增强防并发和状态检查，支持筛选切换后恢复图片位置
  const updateList = async (requestParams) => {
    // 检查是否正在加载
    if (loadNextPageFlag.value === true) {
      // 等待当前加载完成
      let waitCount = 0;
      while (loadNextPageFlag.value === true && waitCount < 50) {
        await new Promise((resolve) => setTimeout(resolve, 100));
        waitCount++;
      }
    }

    // 重置加载标志（updateList通常意味着重新开始）
    loadNextPageFlag.value = false;

    // 提取恢复图片ID和状态（用于筛选切换后尝试恢复位置）
    const restoreImageId = requestParams._restoreImageId;
    const restoreImageStatus = requestParams._restoreImageStatus; // 从调用方传入的图片状态
    // 移除内部参数，避免传递给API
    const cleanParams = { ...requestParams };
    delete cleanParams._restoreImageId;
    delete cleanParams._restoreImageStatus;

    try {
      // 如果有恢复图片ID，先尝试获取该图片在新筛选条件下的位置
      let targetPage = Math.floor((state.offset || 0) / 35) + 1;
      let fallbackToFirstPage = false;
      let shouldRestorePosition = false;

      if (restoreImageId) {
        // 使用传入的图片状态（因为此时state.files可能已被清空）
        const currentImageStatus = restoreImageStatus; // 101=未标注, 104=已标注

        // 获取新的筛选类型
        const newTypeArray = cleanParams.type
          ? Array.isArray(cleanParams.type)
            ? [...cleanParams.type]
            : [cleanParams.type]
          : [];
        const isNewTypeUnannotatedOnly = newTypeArray.length === 1 && newTypeArray[0] === 101;
        const isNewTypeAnnotatedOnly = newTypeArray.length === 1 && newTypeArray[0] === 104;

        // 已标注图片切到未标注筛选，或未标注图片切到已标注筛选，不再强制跳第一页
        const shouldSkipRestore =
          (currentImageStatus === 104 && isNewTypeUnannotatedOnly) || // 已标注图片 -> 未标注筛选
          (currentImageStatus === 101 && isNewTypeAnnotatedOnly); // 未标注图片 -> 已标注筛选

        if (!shouldSkipRestore) {
          try {
            // 获取该图片在新筛选条件下的offset
            const query = cleanParams.type ? { type: cleanParams.type } : {};
            if (cleanParams.labelId) {
              query.labelId = cleanParams.labelId;
            }
            const imageOffset = await queryFileOffset(params.datasetId, restoreImageId, query);

            if (imageOffset !== null && imageOffset !== undefined && imageOffset >= 0) {
              // 图片在新筛选条件下存在
              const page = getPageByOffset(imageOffset, 35);
              if (page) {
                targetPage = page;
                shouldRestorePosition = true;
              } else if (teamLabel.value) {
                // 多人标注下，图片不在分配区间，回退到第一页
                targetPage = 1;
                fallbackToFirstPage = true;
              }
            }
          } catch (e) {
            // 图片在新筛选条件下不存在，保留当前页，若无数据再兜底第一页
            console.warn('图片在新筛选条件下不存在，保留当前页:', e);
          }
        }
      }

      // 加载目标页数据
      let usedPage = targetPage;
      let rawFile = await queryFiles({
        ...cleanParams,
        offset: (usedPage - 1) * 35,
        limit: 35,
      });
      let { result: files } = rawFile;
      let nextFiles = transformFiles(files);

      // 当前页没有数据时再兜底到第一页，避免无脑重置分页
      if (nextFiles.length === 0 && usedPage > 1) {
        usedPage = 1;
        fallbackToFirstPage = true;
        rawFile = await queryFiles({
          ...cleanParams,
          offset: 0,
          limit: 35,
        });
        ({ result: files } = rawFile);
        nextFiles = transformFiles(files);
      }

      // 确定当前选中的图片
      let currentImgId = nextFiles.length ? nextFiles[0].id : undefined;

      if (shouldRestorePosition && restoreImageId && nextFiles.length > 0) {
        // 在加载的页面中查找目标图片
        const foundIndex = nextFiles.findIndex((f) => f.id === restoreImageId);
        if (foundIndex !== -1) {
          currentImgId = restoreImageId;
        }
      }

      // 检测新列表内部是否有重复，如果有则去重
      const uniqueFiles = [];
      const seenIds = new Set();

      nextFiles.forEach((file) => {
        if (!seenIds.has(file.id)) {
          seenIds.add(file.id);
          uniqueFiles.push(file);
        }
      });

      const finalFiles = uniqueFiles.length < nextFiles.length ? uniqueFiles : nextFiles;

      // 只在必要时更新统计
      const isFilterChange = cleanParams && (cleanParams.type || cleanParams.labelId);
      if (isFilterChange) {
        if (!teamLabel.value) {
          await countDetails();
        } else {
          await countTeamLabelDeatails();
        }
      }

      // 需要更新的状态
      const nextState = {
        files: finalFiles,
        error: !currentImgId ? new Error('图片不存在') : null,
        currentImgId,
        timestamp: Date.now(), // 强制更新
        hasMore: finalFiles.length >= limit,
        offset: (usedPage - 1) * 35,
      };

      // 对于多人标注，检查是否到达分配范围的末尾
      if (teamLabel.value) {
        const typeArray = cleanParams.type
          ? Array.isArray(cleanParams.type)
            ? [...cleanParams.type]
            : [cleanParams.type]
          : [];
        const isAllTypes =
          typeArray.length === 0 || (typeArray.includes(101) && typeArray.includes(104));
        const isType101Only = typeArray.length === 1 && typeArray[0] === 101;
        const isType104Only = typeArray.length === 1 && typeArray[0] === 104;

        const totalTargetCount = isAllTypes
          ? state.total
          : isType101Only
          ? state.noAnnotation
          : isType104Only
          ? state.haveAnnotation
          : 0;

        const isExceedLimit = finalFiles.length >= totalTargetCount;
        nextState.hasMore = !isExceedLimit;
      }

      // 更新图片集合
      updateState(nextState);

      // 如果恢复了位置，需要通知list组件更新页码
      if (shouldRestorePosition && usedPage > 1 && thumbRef.value) {
        // 延迟一下确保状态已更新
        setTimeout(() => {
          if (thumbRef.value && thumbRef.value.goToPageWithoutAutoSelect) {
            thumbRef.value.goToPageWithoutAutoSelect(usedPage);
          }
        }, 50);
      } else if (fallbackToFirstPage && !!thumbRef.value) {
        // 仅在确实fallback到第一页时同步分页器
        setTimeout(() => {
          if (thumbRef.value && thumbRef.value.goToPageWithoutAutoSelect) {
            thumbRef.value.goToPageWithoutAutoSelect(1);
          }
        }, 50);
      }
    } catch (error) {
      console.error('更新列表失败:', error);
      throw error;
    }
  };

  // 移除前端缓存的缩略图列表中被删除的图片
  const removeLocalImg = async () => {
    const { files, currentImgIndex } = findImgIndex();
    const removedImgIndex = currentImgIndex;

    // 获取被删除图片的状态，用于本地更新统计
    const removedFile = files[currentImgIndex];
    const wasUnannotated = removedFile?.status === 101;

    // 本地更新统计数据（不修改files，等重新加载）
    const newState = {
      total: state.total - 1,
    };

    // 根据删除的图片状态更新对应的统计
    if (wasUnannotated) {
      newState.noAnnotation = Math.max(0, state.noAnnotation - 1);
    } else {
      newState.haveAnnotation = Math.max(0, state.haveAnnotation - 1);
    }

    updateState(newState);

    // 重新加载当前页，保持35张图片并且序号正确
    if (thumbRef.value && thumbRef.value.reloadCurrentPage) {
      await thumbRef.value.reloadCurrentPage();

      // 重新加载后，选中合适的图片
      if (state.files.length > 0) {
        // 如果删除的不是最后一张，选中原位置的图片（现在是下一张）
        if (removedImgIndex < state.files.length) {
          changeCurrentImg(state.files[removedImgIndex]);
        } else {
          // 如果删除的是最后一张，选中新的最后一张
          changeCurrentImg(state.files[state.files.length - 1]);
        }
      } else {
        // 当前页为空，跳转到上一页
        const currentPage = thumbRef.value.getCurrentPage();
        if (currentPage > 1) {
          await thumbRef.value.goToPageWithoutAutoSelect(currentPage - 1);
          if (state.files.length > 0) {
            changeCurrentImg(state.files[0]);
          }
        } else {
          // 已经是第1页且为空
          if (!msgInstance) {
            msgInstance = Message.warning({
              message: '已无图片',
              onClose: onMessageClose,
            });
          }
        }
      }
    }
  };

  // 保存标注
  const saveAnnotation = async (data) => {
    await maHttp
      .post(
        {
          url: `datasets/files/${params.datasetId}/${state.currentImgId}/annotations`,
          data,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then(() => {
        // 清空历史记录
        Object.assign(state, { history: [] });
        Message.success({ message: '保存成功', duration: 800 });
      });
  };

  // 显示删除文件确认tooltip
  const handleShowDeleteConfirm = () => {
    // 如果是历史版本，禁止删除
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法删除文件！');
      return;
    }

    if (teamLabel.value) return;

    // 检查是否有当前文件
    if (!state.currentImgId) {
      Message.warning('没有可删除的文件');
      return;
    }

    // 打开删除确认tooltip
    dialogState.isDeleteConfirmOpen = true;
    // 通知toolbar组件显示popconfirm
    if (workspaceRef.value) {
      workspaceRef.value.showDeleteConfirm();
    }
  };

  // 确认删除文件
  const handleConfirmDelete = async () => {
    // 关闭tooltip
    dialogState.isDeleteConfirmOpen = false;

    // 通知toolbar组件关闭popconfirm
    if (workspaceRef.value) {
      workspaceRef.value.hideDeleteConfirm();
    }

    // 如果是历史版本，禁止删除
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法删除文件！');
      return;
    }

    if (teamLabel.value) return;

    const params = {
      fileIds: [state.currentImgId],
      datasetId: state.datasetId,
    };
    await del(params)
      .then(() => {
        Message.success('文件删除成功');
        removeLocalImg();
      })
      .catch((err) => {
        Message.error(err.message || '删除失败');
      });
  };

  // 取消删除文件
  const handleCancelDelete = () => {
    // 关闭tooltip
    dialogState.isDeleteConfirmOpen = false;

    // 通知toolbar组件关闭popconfirm
    if (workspaceRef.value) {
      workspaceRef.value.hideDeleteConfirm();
    }
  };

  // 删除当前文件（原有方法，保持兼容性）
  const handleRemoveFile = async () => {
    await handleConfirmDelete();
  };

  // 选择框
  const handleSelection = (boolean) => {
    state.selection = boolean;
  };

  // 开始画框
  const handleBrushStart = () => {
    // 每次开始画框清空当前标注
    Object.assign(state, {
      currentAnnotationId: '',
    });
  };

  const mapPolygon = (annotation) => {
    const { points } = annotation.data;
    const { dimension } = workspaceRef.value;

    // SVG 尺寸现在始终等于图片缩放后的尺寸，所以直接根据缩放比例转换坐标
    const _points = points.map((d) => {
      const __point = {};
      for (const k in d) {
        // 根据图片缩放比例进行调整
        __point[k] = d[k] / (dimension.scale || 1);
      }
      return __point;
    });
    const updatedAnnotation = {
      ...annotation,
      data: {
        ...annotation.data,
        points: _points,
      },
    };
    return updatedAnnotation;
  };

  // 将绝对路径映射为相对图片路径
  const mapBrushToBbox = (annotation) => {
    const { bbox, _debug } = annotation.data;
    const { dimension } = workspaceRef.value;

    // 临时变量
    let temp_bbox = {};
    // 解析 bbox 值
    const _bbox = {};

    // 如果有调试信息，直接使用调试信息中的调整后坐标
    if (_debug && _debug.adjustedStart && _debug.adjustedEnd) {
      // 直接使用已经调整过的坐标
      const { adjustedStart, adjustedEnd } = _debug;
      temp_bbox = {
        x: Math.min(adjustedStart.x, adjustedEnd.x),
        y: Math.min(adjustedStart.y, adjustedEnd.y),
        width: Math.abs(adjustedEnd.x - adjustedStart.x),
        height: Math.abs(adjustedEnd.y - adjustedStart.y),
      };
    } else {
      // SVG 尺寸现在始终等于图片缩放后的尺寸，直接使用 bbox
      temp_bbox = { ...bbox };
    }

    // 应用图片缩放比例
    for (const k in temp_bbox) {
      // 根据图片缩放比例进行调整
      _bbox[k] = temp_bbox[k] / (dimension.scale || 1);
    }

    const updatedAnnotation = {
      ...annotation,
      data: {
        ...annotation.data,
        bbox: _bbox,
        extent: bbox2Extent(_bbox),
      },
    };
    return updatedAnnotation;
  };

  // 保存的时候生成新的位置信息
  const rescale = (annotation) => {
    const { extent, points } = annotation.data;
    const shapeInfo = isSegmentation ? points : extent2Bbox(extent);
    const updatedAnnotation = {
      ...annotation,
      data: {
        ...annotation.data,
        [shapeField]: shapeInfo,
      },
    };
    // _type 仅供绘画使用
    return omit(updatedAnnotation, ['__type']);
  };

  // 改造标注数据
  const transformShape = (raw) => {
    return isSegmentation ? mapPolygon(raw) : mapBrushToBbox(raw);
  };

  // 手动画框结束
  const drawShapeEnd = (shape) => {
    // 如果是历史版本，禁止画框
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法添加新标注！');
      return;
    }

    // 检查是否有原始坐标信息
    const hasOriginalCoordinates =
      shape.originalCoordinates && shape.originalCoordinates.start && shape.originalCoordinates.end;

    // 使用原始坐标创建标注
    if (hasOriginalCoordinates) {
      const { start, end, imageOffset, zoom, scale } = shape.originalCoordinates;

      // 创建一个新的形状对象，用于生成bbox
      const adjustedShape = {
        start: { ...start },
        end: { ...end },
      };

      // 如果是分割模式，处理points；否则处理bbox
      const shapeInfo = isSegmentation
        ? { points: shape.points }
        : { bbox: generateBbox(adjustedShape) };

      // 记录上一次选中的 selectLabel
      const otherProps = state.lastSelectedLabel
        ? {
            categoryId: state.lastSelectedLabel,
            color: getColorLabel(state.lastSelectedLabel),
          }
        : {};

      const rawAnnotation = {
        id: generateUuid(),
        shapeType: isSegmentation ? 'polygon' : 'rect',
        __type: 0, // 标识为新创建的标注
        data: {
          score: 1,
          ...shapeInfo,
          ...otherProps,
          // 保存原始绘制信息，便于调试
          _debug: {
            originalStart: { ...start },
            originalEnd: { ...end },
            adjustedStart: { ...adjustedShape.start },
            adjustedEnd: { ...adjustedShape.end },
            imageOffset: { ...imageOffset },
            zoom,
            scale,
          },
        },
      };

      // 转换成标准地址（extent/bbox）
      const annotation = transformShape(rawAnnotation);

      // 更新框选位置坐标
      const newAnnotation = (state[annotationType] || []).concat(annotation);
      Object.assign(state, {
        [annotationType]: newAnnotation,
        history: state.history.concat(annotation),
        currentAnnotationId: annotation.id,
      });
    } else {
      // 兼容旧的处理方式
      const shapeInfo = isSegmentation ? { points: shape.points } : { bbox: generateBbox(shape) };

      // 记录上一次选中的 selectLabel
      const otherProps = state.lastSelectedLabel
        ? {
            categoryId: state.lastSelectedLabel,
            color: getColorLabel(state.lastSelectedLabel),
          }
        : {};

      const rawAnnotation = {
        id: generateUuid(),
        shapeType: isSegmentation ? 'polygon' : 'rect',
        __type: 0, // 标识为新创建的标注
        data: {
          score: 1,
          ...shapeInfo,
          ...otherProps,
        },
      };

      // 转换成标准地址（extent/bbox）
      const annotation = transformShape(rawAnnotation);

      // 更新框选位置坐标
      const newAnnotation = (state[annotationType] || []).concat(annotation);
      Object.assign(state, {
        [annotationType]: newAnnotation,
        history: state.history.concat(annotation),
        currentAnnotationId: annotation.id,
      });
    }

    // 画框结束后重新聚焦容器，确保键盘快捷键（如Q键）能继续工作
    setTimeout(() => {
      if (containerRef.value) {
        containerRef.value.focus();
      }
    }, 50);
  };

  const validateField = ({ data }) => {
    return !(!data[shapeField] || !data.categoryId);
  };

  // 校验 annotation
  const checkAnnotationValid = () => {
    // 如果是分割，需要做校验
    if (isSegmentation) {
      const validateStatus = workspaceRef.value.segmentationRef.validate();
      if (!validateStatus) {
        return '当前标注未完成，不可保存';
      }
    }
    const unValid = state[annotationType].find((d) => !validateField(d));
    if (unValid) {
      return '标注格式异常，请检查字段';
    }
    return '';
  };

  // 保存标注
  const handleSave = async () => {
    // 如果是历史版本，禁止保存
    if (!state.isCurrentVersion) {
      return Message.warning('历史版本为只读模式，无法保存标注！');
    }

    const msg = checkAnnotationValid();
    if (msg) {
      return Message.warning(msg || '标注格式异常');
    }
    await saveAnnotation({
      id: state.currentImgId,
      // 保存的时候忽略掉__type, 仅供内部使用
      annotation: stringifyAnnotations(state[annotationType].map(rescale)),
    }).then(() => {
      // 保存成功后才添加到变更列表
      state.changedList.add(state.currentImgId);
      // 保存标注不会改变图片的标注状态（101/104），所以不需要更新统计
    });
    return null;
  };

  // 人工确认 - 严格防重复，但不阻塞导航
  const handleConfirm = () => {
    // 如果是历史版本，禁止提交
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法提交标注！');
      return Promise.resolve();
    }

    // 严格检查是否已经在提交中
    if (submitState.submittingImages.has(state.currentImgId)) {
      Message.warning('当前图片正在提交中，请稍候...');
      return Promise.resolve(); // 返回resolved的Promise，不阻塞后续操作
    }

    // 提交时移除未标注集合里的已标注文件
    if (state.unannotatedFileIds.includes(state.currentImgId)) {
      state.unannotatedFileIds = state.unannotatedFileIds.filter((id) => id !== state.currentImgId);
    }

    const msg = checkAnnotationValid();
    if (msg) {
      Message.warning(msg || '标注格式异常');
      return Promise.resolve(); // 返回resolved的Promise，不阻塞后续操作
    }

    const annotationData = {
      annotation: stringifyAnnotations(state[annotationType].map(rescale)),
    };

    // 异步提交，立即返回
    submitAnnotationAsync(state.currentImgId, annotationData);

    // 只有在真正有变化时才添加到 changedList
    if (hasAnnotationChanges()) {
      state.changedList.add(state.currentImgId);
    }

    // 清空历史记录
    Object.assign(state, { history: [] });

    // 返回立即resolved的Promise，不阻塞任何操作
    return Promise.resolve();
  };

  // 一键确认 - 仅用于多人标注
  const handleBatchConfirm = async () => {
    // 如果是历史版本，禁止操作
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法进行一键确认！');
      return;
    }

    // 只有多人标注模式才能使用
    if (!teamLabel.value) {
      Message.warning('一键确认仅在多人标注模式下可用！');
      return;
    }

    // 检查是否正在处理
    if (isBatchConfirming.value) {
      Message.warning('正在处理中，请稍候...');
      return;
    }

    try {
      // 设置加载状态
      isBatchConfirming.value = true;

      const loading = Message({
        message: '正在检查标注状态...',
        type: 'loading',
        duration: 0,
      });

      // 调用后端接口检查是否所有图片都已标注
      const checkResult = await maHttp.get(
        {
          url: `/datasets/team/${subtaskId}/check-all-annotated`,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );

      loading.close();

      if (!checkResult.allAnnotated) {
        Message.warning(
          `还有 ${checkResult.unannotatedCount} 张图片未标注，请先完成所有图片的标注后再进行一键确认！`,
        );
        return;
      }

      // 确认操作
      ElMessageBox.confirm(
        '确认要一键提交所有已标注的图片吗？此操作将把当前进度设置为已完成。',
        '一键确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )
        .then(async () => {
          const submitLoading = Message({
            message: '正在提交...',
            type: 'loading',
            duration: 0,
          });

          try {
            // 调用后端接口进行一键确认
            await maHttp.post(
              {
                url: `/datasets/team/${taskId}/${subtaskId}/batch-confirm`,
                headers: {},
              },
              { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
            );

            submitLoading.close();
            Message.success('一键确认成功！');

            // 刷新统计信息
            if (teamLabel.value) {
              await countTeamLabelDeatails();
            }
          } catch (error) {
            submitLoading.close();
            Message.error(`一键确认失败: ${error.message || '未知错误'}`);
          }
        })
        .catch(() => {
          // 用户取消
        });
    } catch (error) {
      Message.error(`检查失败: ${error.message || '未知错误'}`);
    } finally {
      // 重置加载状态
      isBatchConfirming.value = false;
    }
  };

  // 根据 files 获取第一个文件的信息
  const getFirstChild = (files = []) => (files.length ? files[0] : {});

  // 获取选中文件详情
  const getActiveImg = (files, id) => files.find((d) => d.id === id) || {};

  // 获取当前 row 索引
  const findRowIndex = (rowId) => {
    const annotations = selectAnnotation();
    return (annotations || []).findIndex((d) => d.id === rowId);
  };

  // 删除标注确认
  const deleteAnnotation = (rowId) => {
    // 如果是历史版本，禁止删除标注
    if (!state.isCurrentVersion) {
      Message.warning('历史版本为只读模式，无法删除标注！');
      return;
    }

    // 当前的标注类型
    const curAnnotation = selectAnnotation();
    const removedIndex = findRowIndex(rowId);
    if (removedIndex > -1) {
      const removedList = remove(curAnnotation, removedIndex);
      updateState({
        [annotationType]: removedList,
        currentAnnotationId: '',
      });

      // 删除标注后重新聚焦容器，确保键盘快捷键能继续工作
      setTimeout(() => {
        if (containerRef.value) {
          containerRef.value.focus();
        }
      }, 50);
    }
  };

  const reportError = (msg) => {
    Object.assign(state, {
      error: new Error(msg),
    });
  };

  // 优化的图片信息更新函数 - 避免不必要的接口调用
  const updateImageInfo = async (fileId, labels) => {
    if (!fileId) {
      reportError('文件不存在');
      return { file: null, annotations: [] };
    }

    // 优先从缓存中获取文件
    let file = state.files.find((f) => f.id === fileId);

    // 只有在changedList中或缓存中没有文件时才调用接口
    const needRefresh = state.changedList.has(fileId) || !file || isEmpty(file);

    if (needRefresh) {
      try {
        const freshFile = await queryFile(params.datasetId, fileId, state.versionName);
        const freshAnnotations = freshFile.annotation
          ? parseAnnotation(freshFile.annotation, labels)
          : [];

        // 更新files数组中对应的文件
        const fileIndex = state.files.findIndex((f) => f.id === fileId);
        if (fileIndex > -1) {
          const transformedFile = transformFiles([freshFile])[0];
          Object.assign(state.files[fileIndex], transformedFile);
        }

        // 清除changedList标记
        if (state.changedList.has(fileId)) {
          state.changedList.delete(fileId);
        }

        return { file: freshFile, annotations: freshAnnotations };
      } catch (error) {
        // 清除changedList标记，避免重复尝试
        state.changedList.delete(fileId);
        reportError('获取文件信息失败，请刷新重试');
        return { file: null, annotations: [] };
      }
    } else {
      // 从缓存获取，不调用接口
      const annotations = file && file.annotation ? parseAnnotation(file.annotation, labels) : [];
      return { file, annotations };
    }
  };

  onMounted(async () => {
    // 如果查看的是历史版本，显示提示
    if (!state.isCurrentVersion && state.versionName) {
      Message.warning({
        message: `您正在查看历史版本（${state.versionName}）的标注数据，此模式为只读模式，无法进行编辑和保存操作。`,
        duration: 5000,
        showClose: true,
      });
    }

    // 判断当前数据集不存在
    let datasetInfo = {};
    try {
      // 获取数据集信息
      datasetInfo = await detail(params.datasetId);
    } catch (err) {
      Object.assign(state, {
        error: new Error('获取数据集信息出错，请刷新重试'),
      });
      return;
    }

    Object.assign(state, {
      datasetInfo,
    });

    // 获取添加的标签
    const labels = await queryLabels();
    // 如果当前页面跳转时不带有指定imgId
    if (!imgId || imgId === '0') {
      const firstImgId = await queryFirstImg(params.datasetId, state.versionName);
      if (typeof firstImgId === 'number') {
        Object.assign(state, {
          currentImgId: firstImgId,
        });
      } else {
        Object.assign(state, {
          error: new Error('请检查当前数据集是否存在图片'),
          labels, // 不存在文件的时候也需要渲染添加的标签
        });
        return;
      }
    }

    // 跳转时有imgId，赋值渲染添加的标签
    Object.assign(state, {
      labels,
    });

    // 获取图片信息 - 必须同步执行，确保统计数据正确
    if (!teamLabel.value) {
      await countDetails();
    } else {
      await countTeamLabelDeatails();
    }

    // 根据未标注图片数量设置初始筛选条件
    const hasUnannotated = state.noAnnotation > 0;
    let initialFileFilterType = null;
    if (teamLabel.value) {
      //如果是多人标注默认全选
      initialFileFilterType = [101, 104];
    } else {
      initialFileFilterType = hasUnannotated ? [101] : [101, 104]; // 有未标注时只选未标注，否则全选
    }

    const initialFilterUnfinished = hasUnannotated;

    updateState({
      fileFilterType: initialFileFilterType,
      filterUnfinished: initialFilterUnfinished,
    });

    // 获取数据集图片集合 - 使用分页模式
    const [rawFile = {}] = await Promise.allSettled([
      queryFiles({
        type: initialFileFilterType,
        offset: 0,
        limit: 35,
      }),
    ]);

    // 获取数据增强类型
    const enhanceListResult = await queryDataEnhanceList();
    const { dictDetails = [] } = enhanceListResult || {};
    const enhanceList = dictDetails.map((d) => ({
      label: d.label,
      value: Number(d.value),
    }));

    if (rawFile.status === 'rejected') {
      Object.assign(state, {
        error: rawFile.reason,
      });
      throw rawFile.reason;
    }
    let { result: files } = rawFile.value;

    // 如果在当前筛选条件下没有任何图片，则显示提示信息并终止后续操作
    if (isEmpty(files)) {
      updateState({
        error: new Error('当前筛选条件下没有图片'),
        files: [],
        enhanceList,
        imgLoading: false,
      });
      return;
    }

    // 4. 将返回结果的第一张图片ID设为当前活动图片的ID
    const activeFileId = files[0].id;
    const addFiles = transformFiles(files);

    // 一次性更新所有相关状态
    updateState({
      files: addFiles,
      addFiles,
      enhanceList,
      currentImgId: activeFileId,
      hasMore: addFiles.length >= limit,
      offset: 0,
      // 确保筛选条件正确设置
      fileFilterType: initialFileFilterType,
      filterUnfinished: initialFilterUnfinished,
    });

    const firstEnhanceList = !teamLabel.value
      ? await getEnhanceFileList(params.datasetId, activeFileId)
      : null;

    const { file, annotations } = await updateImageInfo(activeFileId, labels);
    updateState({
      currentImgId: file.id,
      fileInfo: file,
      rawAnnotations: annotations,
      [annotationType]: annotations,
      hasEnhanceRecord: !isNil(firstEnhanceList),
      imgLoading: false,
    });
    // 重要：记录图片的初始标注状态
    curAnnoNum.value = JSON.parse(JSON.stringify(annotations));

    // 自动聚焦容器以接收键盘事件
    if (containerRef.value) {
      containerRef.value.focus();
    }

    if (currentUserId) {
      const positionRecord = getPositionRecord(state.datasetId, currentUserId);

      if (positionRecord && !isRecordExpired(positionRecord)) {
        const lastFileId = positionRecord.fileId;

        // 检查上次的图片是否还存在（不是当前显示的图片）
        if (lastFileId && lastFileId !== file.id) {
          // 延迟显示弹框，确保页面已完全加载
          setTimeout(async () => {
            try {
              // 先验证图片是否还存在
              const checkFile = await queryFile(
                params.datasetId,
                lastFileId,
                state.versionName,
              ).catch(() => null);

              if (checkFile && checkFile.id) {
                ElMessageBox.confirm(
                  '检测到您上次在此数据集标注过图片，是否跳转到上次标注的位置？',
                  '恢复标注位置',
                  {
                    confirmButtonText: '跳转',
                    cancelButtonText: '从头开始',
                    type: 'info',
                  },
                )
                  .then(async () => {
                    // 用户选择跳转
                    try {
                      // 获取目标图片在当前筛选条件下的offset
                      const query = { type: state.fileFilterType };
                      if (state.filterLabelId && state.filterLabelId.length > 0) {
                        query.labelId = state.filterLabelId;
                      }
                      const targetOffset = await queryFileOffset(
                        params.datasetId,
                        lastFileId,
                        query,
                      );

                      if (
                        targetOffset !== null &&
                        targetOffset !== undefined &&
                        targetOffset >= 0
                      ) {
                        // 计算目标页码
                        const targetPage = getPageByOffset(targetOffset, 35);
                        if (!targetPage) {
                          Message.warning('上次标注图片不在当前分配区间');
                          return;
                        }

                        // 加载目标页数据
                        if (thumbRef.value && thumbRef.value.goToPageWithoutAutoSelect) {
                          const success = await thumbRef.value.goToPageWithoutAutoSelect(
                            targetPage,
                          );

                          if (success) {
                            // 等待一下确保state.files已经更新
                            await new Promise((resolve) => setTimeout(resolve, 100));

                            // 在新加载的页面中查找目标图片
                            const targetFile = state.files.find((f) => f.id === lastFileId);

                            if (targetFile) {
                              changeCurrentImg(targetFile);

                              // 滚动到目标图片
                              if (thumbRef.value.scrollToCurrentImage) {
                                thumbRef.value.scrollToCurrentImage();
                              }

                              Message.success('已跳转到上次标注的位置');
                            } else {
                              Message.warning('上次标注的图片在当前筛选条件下不可见');
                            }
                          }
                        }
                      } else {
                        Message.warning('上次标注的图片在当前筛选条件下不可见');
                      }
                    } catch (e) {
                      console.warn('跳转到上次标注位置失败:', e);
                      Message.warning('跳转失败，请手动查找');
                    }
                  })
                  .catch(() => {
                    // 用户选择从头开始，不做任何操作
                  })
                  .finally(() => {
                    // 无论用户选择什么，都开始记录位置
                    shouldRecordPosition.value = true;
                  });
              } else {
                // 图片不存在，开始记录位置
                shouldRecordPosition.value = true;
              }
            } catch (e) {
              // 图片不存在或查询失败，不显示弹框
              console.warn('检查上次标注图片失败:', e);
              shouldRecordPosition.value = true;
            }
          }, 500);
        } else {
          // lastFileId不存在或等于当前图片，开始记录位置
          shouldRecordPosition.value = true;
        }
      } else {
        // 没有位置记录或记录已过期，开始记录位置
        shouldRecordPosition.value = true;
      }
    } else {
      // 不满足检查条件（团队标注或没有用户ID等），开始记录位置
      shouldRecordPosition.value = true;
    }
  });

  watch(
    () => [state.currentImgId, state.timestamp],
    async () => {
      const curImgId = state.currentImgId;
      updateState({
        rawAnnotations: [],
        annotations: [],
        fileInfo: null,
      });

      if (curImgId) {
        updateState({
          error: null,
          fileId: curImgId,
        });

        const { annotations, file } = await updateImageInfo(curImgId, state.labels);

        // 防止异步竞态条件：只有当前图片ID仍然是请求的图片ID时才更新状态
        if (state.currentImgId === curImgId) {
          updateState({
            rawAnnotations: annotations,
            [annotationType]: annotations,
            fileInfo: file,
            imgLoading: false,
          });

          // 重要：记录图片的初始标注状态，用于后续比较是否有变化
          curAnnoNum.value = JSON.parse(JSON.stringify(annotations));
        }
      } else {
        updateState({
          imgLoading: false,
        });
        curAnnoNum.value = [];
      }
    },
  );

  // 监听currentImgId变化，更新localStorage中的标注位置记录
  // 使用一个标志来控制是否应该记录位置（初始化完成后才开始记录）
  const shouldRecordPosition = ref(false);

  watch(
    () => state.currentImgId,
    (newImgId, oldImgId) => {
      // 只有在初始化完成后、图片ID真正变化时才更新记录
      if (
        shouldRecordPosition.value &&
        newImgId &&
        newImgId !== oldImgId &&
        currentUserId &&
        state.datasetId
      ) {
        updatePositionRecord(state.datasetId, currentUserId, newImgId, state.fileFilterType);
      }
    },
  );

  // 当前文件对象
  const currentImg = computed(() => getActiveImg(state.files, state.currentImgId));

  // 是否应屏蔽全局交互：导航中/下一页加载中/图片加载中
  const isUIBlocked = computed(() => {
    return (
      state.isNavigating === true || loadNextPageFlag.value === true || state.imgLoading === true
    );
  });

  // 注入 UI 屏蔽状态，供子组件在各自区域内渲染遮罩（只覆盖导航区域，不影响标注区域）
  provide('isUIBlocked', isUIBlocked);

  // 组件卸载时清理
  onUnmounted(() => {
    // 清理加载状态
    loadNextPageFlag.value = false;
  });
</script>

<style lang="scss">
  .annotate-container {
    display: flex;
    height: calc(100vh - 48px); // 减去全局 Header 高度
    position: relative;
    outline: none;
  }

  .annotate-view {
    display: flex;
    width: 100%;
    height: 100%;
    position: relative;
  }

  .grid-view {
    display: flex;
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }

  .workspace-container {
    flex: 1;
    max-width: calc(100vw - 20% - 160px);
  }

  .workspace-settings {
    width: 20%;
  }

  // 过渡动画
  .slide-fade-enter-active,
  .slide-fade-leave-active {
    transition: all 0.3s ease;
  }

  .slide-fade-enter-from {
    transform: translateX(20px);
    opacity: 0;
  }

  .slide-fade-leave-to {
    transform: translateX(-20px);
    opacity: 0;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.2s ease;
  }

  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }

  // 局部屏蔽层：用于在导航区域内拦截点击，避免加载/导航中点击穿透
  // 由各子组件在自己的区域内使用此样式
  .nav-blocker {
    position: absolute;
    inset: 0; // top:0; right:0; bottom:0; left:0;
    z-index: 1000;
    background: transparent; // 透明，不遮挡视觉
    pointer-events: auto; // 接收并吞掉点击
  }
</style>
