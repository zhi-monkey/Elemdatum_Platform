<template>
  <div class="workspace-container flex flex-col" ref="workspaceRef" tabindex="-1">
    <div v-if="!isMoveMode" class="drawing-mode-indicator">
      <span class="icon"><EditOutlined /></span>
      绘图模式 - 按 W 键切换
    </div>
    <div v-else class="drawing-mode-indicator">
      <span class="icon"><DragOutlined /></span>
      移动模式 - 按 W 键切换
    </div>

    <div v-hotkey.stop="keymap" class="workspace-stage flex flex-col f1">
      <ToolBar
        ref="toolbarRef"
        :zoomIn="zoomIn"
        :zoomOut="zoomOut"
        :zoomReset="resetZoom"
        :clearSelection="clearSelection"
        :api="api"
        :setApi="setApi"
        :confirm="confirm"
        :is-confirming="isConfirming"
        :isSegmentation="isSegmentation"
        :deleteable="deleteable"
        :action-disabled="props.state.fromHistory"
        :has-file="!state.error"
        :show-jump-unannotated="showJumpUnannotated"
        :toggleViewMode="props.toggleViewMode"
        :isTeamLabel="props.isTeamLabel"
        :isBatchConfirming="props.isBatchConfirming?.value || false"
        :batchConfirm="handleBatchConfirm"
        @save="attrs.onSave"
        @selection="attrs.onSelection"
        @remove="attrs.onRemove"
        @prev="handleClickUp"
        @next="handleClickDown"
        @jump-unannotated="handleClickJumpUnannotated"
      />
      <div
        v-if="!state.error"
        id="stage"
        ref="imgWrapperRef"
        v-loading="!imgInfo.loaded"
        class="f1 rel"
        @mousedown="handleStageMouseDown"
        @mousemove="handleStageMouseMove"
        @mouseup="handleStageMouseUp"
        @mouseleave="handleStageMouseUp"
      >
        <ZoomContainer
          ref="zoomRef"
          :controlled="true"
          :filter="filter"
          v-bind="zoom"
          :onZoom="handleZoom"
        >
          <div class="zoom-content">
            <div
              class="zoom-content-bound rel"
              :style="{ ...dimension.marginStyle, pointerEvents: 'none' }"
            >
              <div
                ref="contentRef"
                class="content-container"
                :style="{
                  transform: `translate(${imageOffset.x}px, ${imageOffset.y}px)`,
                  pointerEvents: isMoveMode ? 'auto' : 'none',
                  cursor: isDraggingImage ? 'grabbing' : isMoveMode ? 'grab' : 'default',
                }"
              >
                <!--                图片和标注放一起-->
                <div
                  class="imgWrapper"
                  :style="{
                    ...dimension.imgScaleStyle,
                    cursor: isDraggingImage ? 'grabbing' : isMoveMode ? 'grab' : 'default',
                  }"
                  :class="'imgScale'"
                >
                  <img ref="imgRef" :src="currentImg.url" class="usn" />
                </div>
                <!-- svg 宽高要根据图片自适应 -->
                <div class="annotation-element-group abs" :style="dimension.annotationGroupStyle">
                  <svg ref="svgRef" class="canvas" :style="{ ...dimension.svg }">
                    <g v-if="!isSegmentation">
                      <g class="annotation-group">
                        <BboxWrapper
                          v-for="annotate in sortedAnnotations"
                          :key="annotate.id"
                          :annotate="annotate"
                          :action-disabled="props.state.fromHistory"
                          :brush="brush"
                          :offset="offset"
                          :transformer="transformer"
                          :svg="dimension.svg"
                          :scale="dimension.scale"
                          :bounds="dimension.img"
                          :onDragStart="onDragStart"
                          :onDragMove="onDragMove"
                          :onDragEnd="onDragEnd"
                          :onBrushHandleChange="onBrushHandleChange"
                          :onBrushHandleEnd="onBrushHandleEnd"
                          :currentAnnotationId="state.currentAnnotationId"
                          :setCurAnnotation="setCurAnnotation"
                          :getZoom="getZoom"
                          :is-move-mode="isMoveMode"
                          :onRightClick="handleAnnotationRightClick"
                        />
                      </g>
                      <Brush
                        :stageWidth="dimension.svg.width"
                        :stageHeight="dimension.svg.height"
                        :onBrushStart="handleBrushStart"
                        :onBrushMove="handleBrushMove"
                        :onBrushEnd="handleBrushEnd"
                        :action-disabled="props.state.fromHistory"
                        :transformZoom="transformZoom"
                        :is-move-mode="isMoveMode"
                        :imageOffset="imageOffset"
                        :style="{ zIndex: 10 }"
                      />
                    </g>
                    <Segmentation
                      v-else
                      ref="segmentationRef"
                      :stageWidth="dimension.svg.width"
                      :stageHeight="dimension.svg.height"
                      :shapes="api.shapes"
                      :offset="offsetBbox"
                      :hide-tooltip="hideTooltip"
                      :current-annotation-id="state.currentAnnotationId"
                      :set-cur-annotation="setCurAnnotation"
                      :updateState="updateState"
                      :transformZoom="transformZoom"
                      :scale="dimension.scale"
                      :getZoom="getZoom"
                      :bounds="dimension.img"
                      :is-move-mode="isMoveMode"
                      :onRightClick="handleAnnotationRightClick"
                      @change="handleShapesChange"
                    />
                  </svg>
                  <div class="annotation-extra">
                    <div v-if="false" class="annotation-score-group">
                      <Score
                        v-for="annotate in api.annotations"
                        :key="annotate.id"
                        :annotate="annotate"
                        :currentAnnotationId="state.currentAnnotationId"
                        :brush="brush"
                        :offset="offset"
                        :transformer="transformer"
                      />
                    </div>
                    <div v-if="state.showTag" class="annotation-tag-group">
                      <Tag
                        v-for="annotate in props.state[annotationType]"
                        :key="annotate.id"
                        :annotate="annotate"
                        :currentAnnotationId="state.currentAnnotationId"
                        :isMoving="isSegmentation ? transformer.isDragging : brush.isBrushing"
                        :offset="offset"
                        :transformer="transformer"
                        :getLabelName="getLabelName"
                        :annotationType="annotationType"
                        :scale="zoom.zoom"
                      />
                    </div>
                    <div v-if="state.showId && isTrack" class="annotation-tag-group">
                      <AnnotationId
                        v-for="annotate in api.annotations"
                        :key="annotate.id"
                        :annotate="annotate"
                        :currentAnnotationId="state.currentAnnotationId"
                        :brush="brush"
                        :offset="offset"
                        :transformer="transformer"
                        :scale="dimension.scale"
                        :getLabelName="getLabelName"
                        :imgBounding="api.imgBounding"
                      />
                    </div>
                    <!-- 新建标注展示尺寸信息 -->
                    <BrushTip
                      v-if="brush.isBrushing && brush.extent"
                      :brush="brush"
                      :dimension="dimension"
                      :image-offset="imageOffset"
                      :zoom="zoom.zoom"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </ZoomContainer>
        <DropDownLabel
          v-if="tooltipData.visible"
          v-bind="tooltipData"
          :value="api.label.id"
          :handleChange="handleSelectChange"
          :labels="labels"
        />
        <div id="left-button">
          <Button 
            type="primary" 
            shape="circle" 
            size="large" 
            @click="handleClickPrev"
          >
            <template #icon>
              <LeftOutlined />
            </template>
          </Button>
        </div>
        <div id="right-button">
          <Button type="primary" shape="circle" size="large" @click="handleClickNext">
            <template #icon>
              <RightOutlined />
            </template>
          </Button>
        </div>
      </div>
      <Exception v-else>
        <template #image>
          <ContainerOutlined />
        </template>
        <template #desc>
          {{ (state.error || {}).message || 'error' }}
        </template>
      </Exception>
    </div>
  </div>
</template>

<script>
  import {
    computed,
    defineExpose,
    inject,
    onBeforeUnmount,
    onMounted,
    reactive,
    ref,
    unref,
    watch,
  } from 'vue';
  import { isNil } from 'lodash-es';
  import { event as d3Event } from 'd3-selection';
  import { ElMessage as Message } from 'element-plus';
  import { labelsSymbol } from '../../util';
  import { useBrush, useImage, useTooltip, useZoom } from '/@/hooks/dubhe';
  import {
    extent2Bbox,
    getBounding,
    getZoomPosition,
    parsePolygon2Bbox,
    replace,
  } from '/@/utils/dubhe';
  import { Brush } from '/@/views/mineai/data/dataset-details2/components/svg/index.jsx';
  import ZoomContainer from '/@/views/mineai/data/dataset-details2/components/ZoomContainer/index.vue';
  import Exception from '/@/views/mineai/data/dataset-details2/components/Exception/index.vue';
  import ToolBar from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/toolbar.vue';
  import BboxWrapper from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/bboxWrapper.jsx';
  import Score from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/score.jsx';
  import Tag from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/tag.vue';
  import AnnotationId from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/annotationId.jsx';
  import DropDownLabel from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/dropdownLabel/index.jsx';
  import BrushTip from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/brushTip.jsx';
  import Segmentation from '/@/views/mineai/data/dataset-details2/annotate/workSpaceContainer/segmentation/index.vue';
  import {
    ContainerOutlined,
    DragOutlined,
    EditOutlined,
    LeftOutlined,
    RightOutlined,
  } from '@ant-design/icons-vue';
  import { Button } from 'ant-design-vue';

  const FooterHeight = 0;

  export const ThumbWidth = 160; // 侧边栏宽度

  export default {
    name: 'WorkSpaceContainer',
    components: {
      Button,
      LeftOutlined,
      RightOutlined,
      EditOutlined,
      DragOutlined,
      ZoomContainer,
      Segmentation,
      Exception,
      Brush,
      ToolBar,
      DropDownLabel,
      Score,
      Tag,
      AnnotationId,
      BboxWrapper,
      BrushTip,
      ContainerOutlined,
    },
    props: {
      state: Object,
      currentImg: {
        type: Object,
        default: () => null,
      },
      drawShapeEnd: Function,
      createLabel: Function,
      queryLabels: Function,
      getLabelName: Function,
      updateState: Function,
      handleConfirm: Function,
      deleteAnnotation: Function,
      isTrack: Boolean,
      isSegmentation: Boolean,
      annotationType: String,
      handlePrev: Function,
      handleNext: Function,
      handleUp: Function,
      handleDown: Function,
      handleJumpToUnannotated: Function,
      deleteable: Boolean,
      toggleViewMode: Function,
      isTeamLabel: Boolean,
      isBatchConfirming: Object,
      handleBatchConfirm: Function,
      imageOffset: {
        type: Object,
        default: () => ({ x: 0, y: 0 }),
      },
    },
    emits: ['selectLabel'],
    setup(props, ctx) {
      const imgWrapperRef = ref(null);
      const svgRef = ref(null);
      const resizerRef = ref(null); // 事件句柄
      const imgRef = ref(null); // 图片
      const segmentationRef = ref(null);
      const zoomRef = ref(null);
      const isMoveMode = ref(true);
      const isInfiniteDrawMode = ref(false); // 是否为无限绘制模式
      const modeText = computed(() => {
        if (isMoveMode.value) return '移动模式';
        return isInfiniteDrawMode.value ? '绘图模式(无限绘制)' : '绘图模式(自动切换)';
      });
      const showJumpUnannotated = computed(() => {
        const t = props.state.fileFilterType || [];
        return Array.isArray(t) && t.includes(101) && t.includes(104);
      });
      // 移动图片需要的
      const isDraggingImage = ref(false);
      const dragStart = reactive({ x: 0, y: 0 });
      const imageOffset = reactive({ x: 0, y: 0 });
      const tempImageOffset = reactive({ x: 0, y: 0 });
      const contentRef = ref(null);
      const workspaceRef = ref(null);
      const toolbarRef = ref(null);

      // 严格的确认状态管理 - 防重复提交
      const isConfirming = ref(false);
      const confirmingImageId = ref(null); // 记录正在确认的图片ID
      const lastConfirmTime = ref(0); // 最后一次确认时间
      const processingWarningHandler = ref(null); // 限制“处理中”提示
      const allLabels = ref([]);

      // 当前所有标签信息
      const labels = inject(labelsSymbol);
      // 注入 UI 屏蔽状态，用于禁用导航快捷键
      const isUIBlocked = inject('isUIBlocked', ref(false));
      // 初始化状态
      const api = reactive({
        annotations: props.state.annotations,
        shapes: props.state.shapes,
        label: {}, // 一个页面当前只能存在一个标签
        bounding: null, // 容器位置信息
        isCenter: false, // 图片是否已居中
        imgBounding: null, // 图片的位置，给 bbox 位置定位使用
        active: '', // 当前选中
      });

      // 对标注进行合并
      const annotations = computed(() => api[props.annotationType]);

      // 计算标注框面积的辅助函数
      const getAnnotationArea = (annotate) => {
        const { data = {} } = annotate;
        const { extent = {} } = data;
        const width = Math.abs((extent.x1 || 0) - (extent.x0 || 0));
        const height = Math.abs((extent.y1 || 0) - (extent.y0 || 0));
        return width * height;
      };

      // 按面积从大到小排序的标注列表（用于渲染，小框在上层优先响应点击）
      const sortedAnnotations = computed(() => {
        if (!api.annotations || api.annotations.length === 0) return [];
        return [...api.annotations].sort((a, b) => getAnnotationArea(b) - getAnnotationArea(a));
      });

      // 标注偏移
      const transformer = reactive({
        id: undefined,
        dx: 0,
        dy: 0,
        x: undefined,
        y: undefined,
      });

      const handleStageMouseDown = (event) => {
        // 确保工作区获得焦点以响应快捷键
        if (workspaceRef.value) {
          workspaceRef.value.focus();
        }

        if (!isMoveMode.value) return;
        // 使用更可靠的方式检查点击目标
        if (
          event.target === contentRef.value ||
          event.target === imgRef.value ||
          event.target.closest('.imgWrapper') === imgRef.value.parentElement
        ) {
          event.preventDefault();
          event.stopPropagation();
          // 清除其他交互
          clearSelection();
          if (props.isSegmentation) {
            cancelSelection();
          }

          // 记录起始位置
          isDraggingImage.value = true;
          dragStart.x = event.clientX;
          dragStart.y = event.clientY;
          tempImageOffset.x = imageOffset.x;
          tempImageOffset.y = imageOffset.y;
        }
      };
      const handleStageMouseMove = (event) => {
        if (!isDraggingImage.value) return;
        // 使用当前缩放因子计算移动距离
        const dx = (event.clientX - dragStart.x) / zoom.zoom;
        const dy = (event.clientY - dragStart.y) / zoom.zoom;
        imageOffset.x = tempImageOffset.x + dx;
        imageOffset.y = tempImageOffset.y + dy;
      };
      const handleStageMouseUp = () => {
        isDraggingImage.value = false;
      };

      // 重置图片位置
      const resetImagePosition = () => {
        imageOffset.x = 0;
        imageOffset.y = 0;
      };

      const { attrs } = ctx;
      const { brush, onBrushStart, onBrushMove, updateBrush, getExtent, onBrushReset } = useBrush();

      const initialZoom = {
        zoom: unref(props.state.zoom),
        zoomX: unref(props.state.zoomX),
        zoomY: unref(props.state.zoomY),
      };

      // 初始放大和缩小函数
      const {
        zoomIn,
        zoomOut,
        setZoom,
        reset: resetZoom,
        zoom,
        getZoom,
      } = useZoom(initialZoom, imgWrapperRef);

      // tooltip
      const { tooltipData, showTooltip, hideTooltip } = useTooltip();
      const handleAnnotationRightClick = (event, annotation) => {
        event.preventDefault();
        event.stopPropagation();

        // 设置当前标注
        setCurAnnotation(annotation);

        // 获取当前标注的标签ID
        const labelId = annotation.data.categoryId;

        // 更新api.label为当前标注的标签
        api.label = {
          ...api.label,
          id: labelId,
          value: labelId,
        };

        // 展示tooltip，传递当前标注的标签ID
        if (labels.value.length >= 1) {
          showTooltip(
            labelId, // 传递当前标注的标签ID
            event,
            { el: imgWrapperRef.value },
          );
        }
      };

      // 图片尺寸
      const { imgInfo = {}, setImg, startLoad, finishLoad } = useImage();

      const filter = () => {
        // 不允许通过鼠标拖拽来更改缩放
        return d3Event.type !== 'mousedown';
      };

      const setApi = (params) => {
        Object.assign(api, params);
      };

      // 更新标注偏移
      const setTransformer = (params) => {
        Object.assign(transformer, params);
      };

      // 转换 zoom 位置
      const transformZoom = (point) => {
        return getZoomPosition(zoomRef.value.wrapperRef, point);
      };

      const defaultDimension = {
        svg: {},
        img: {},
        wrapper: {},
        margin: {},
        scale: 1,
      };

      // 根据图片大小重新计算 svg 位置信息
      const dimension = computed(() => {
        // 当 dom 元素加载完毕来计算 svg 尺寸
        if (!isNil(api.bounding) && !!imgInfo.loaded && svgRef.value) {
          // 实时获取容器尺寸，补偿浏览器缩放
          const currentBounding = imgWrapperRef.value
            ? getBounding(imgWrapperRef.value)
            : api.bounding;

          // 检测浏览器缩放并补偿
          let browserZoom = 1;
          if (imgWrapperRef.value) {
            const element = imgWrapperRef.value;
            browserZoom = element.offsetWidth > 0 ? currentBounding.width / element.offsetWidth : 1;
          }

          // 容器的最大宽度、高度（补偿浏览器缩放）
          const { width: rawCw, height: rawCh, left, top } = currentBounding;
          const cw = rawCw / browserZoom;
          const ch = rawCh / browserZoom;
          // 图片的宽度，高度
          const iw = imgInfo.width;
          const ih = imgInfo.height;

          const wrapperDimension = {
            width: cw,
            height: ch - FooterHeight,
            left,
            top,
          };

          // 图片自适应填满容器（无论放大还是缩小）
          const imgScale = Math.min(
            wrapperDimension.width / imgInfo.width,
            wrapperDimension.height / imgInfo.height,
          );

          // SVG 尺寸应该与图片实际显示尺寸一致
          const svgDimension = {
            width: iw * imgScale,
            height: ih * imgScale,
          };

          // 标注相关元素的容器 - 始终使用缩放后的图片尺寸
          const annotationGroupStyle = {
            left: 0,
            top: 0,
            width: `${iw * imgScale}px`,
            height: `${ih * imgScale}px`,
          };

          // 上面已经通过margin: 0 auto 做过宽度处理
          const ml = Math.max(wrapperDimension.width - iw * imgScale, 0) / 2;
          const mt = Math.max(wrapperDimension.height - ih * imgScale, 0) / 2;

          const marginStyle = {};
          if (ml > 0) {
            marginStyle['padding-left'] = `${ml}px`;
            marginStyle['padding-right'] = `${ml}px`;
          }
          if (mt > 0) {
            marginStyle['padding-top'] = `${mt}px`;
            marginStyle['padding-bottom'] = `${mt}px`;
          }
          const margin = {
            left: ml,
            top: mt,
          };

          const imgDimension = {
            width: iw,
            height: ih,
          };

          // 始终应用缩放样式（无论放大还是缩小）
          const imgScaleStyle = {
            width: `${imgInfo.width * imgScale}px`,
            height: `${imgInfo.height * imgScale}px`,
            transition: 'opacity 0.4s',
            opacity: 1,
          };
          // 居中
          api.isCenter = true;
          return {
            svg: svgDimension,
            img: imgDimension,
            wrapper: wrapperDimension,
            margin,
            marginStyle,
            scale: imgScale,
            imgScaleStyle,
            annotationGroupStyle,
          };
        }
        if (!imgInfo.loaded || (imgInfo.width === 0 && imgInfo.height === 0)) {
          // 加一个过渡效果，避免太唐突
          return {
            ...defaultDimension,
            imgScaleStyle: {
              opacity: 0,
            },
          };
        }
        return defaultDimension;
      });

      // 删除注释
      const removeAnnotation = () => {
        hideTooltip();
        const currentAnnotationId = props.state.currentAnnotationId;
        if (currentAnnotationId) {
          props.deleteAnnotation(currentAnnotationId);
          // 添加历史记录
          props.state.history.push({});

          // 删除后恢复焦点，确保快捷键能继续工作
          setTimeout(() => {
            if (workspaceRef.value) {
              workspaceRef.value.focus();
            }
          }, 50);
        }
      };

      // 一键清空所有标注
      const removeAllAnnotations = () => {
        hideTooltip();
        // 针对不同的标注类型清空
        const annotationList = props.isSegmentation ? props.state.shapes : props.state.annotations;
        annotationList.forEach((e) => {
          props.deleteAnnotation(e.id);
        });
        // 添加历史记录
        props.state.history.push({});

        // 清空后恢复焦点，确保快捷键能继续工作
        setTimeout(() => {
          if (workspaceRef.value) {
            workspaceRef.value.focus();
          }
        }, 50);
      };

      // 取消选择（目前只针对分割有效）
      const cancelSelection = () => {
        segmentationRef.value?.reset();
      };

      // 完成绘制（目前只针对分割有效）
      const finishDraw = () => {
        segmentationRef.value?.finishDraw();
      };

      // 选中标注，开始画框
      const selection = () => {
        setApi({ active: 'selection' });
        // 关闭已有的 dropdown
        hideTooltip();
        attrs.onSelection(true);
      };

      watch(
        () => api.isCenter,
        (isCenter) => {
          if (isCenter) {
            const { width: boundingWidth, height: boundingHeight } = api.bounding;
            const { width: imgWidth, height: imgHeight } = getBounding(imgRef.value);
            // 图片始终自适应填满容器，使用容器尺寸计算边界
            const mw = boundingWidth;
            const mh = boundingHeight - FooterHeight;
            Object.assign(api, {
              imgBounding: [(mw - imgWidth) / 2, (mh - imgHeight) / 2],
            });
          }
        },
      );
      const toggleMode = () => {
        isMoveMode.value = !isMoveMode.value;

        // 重置所有交互状态
        brush.isBrushing = false;
        transformer.isDragging = false;
        hideTooltip();

        // 如果是分割标注，取消选择
        if (props.isSegmentation) {
          segmentationRef.value?.reset();
        }

        const modeMsg = isMoveMode.value
          ? '移动'
          : isInfiniteDrawMode.value
          ? '绘图(无限绘制)'
          : '绘图(自动切换)';
        Message.info(`已切换到${modeMsg}模式`);
      };

      // 切换无限绘制模式
      const toggleInfiniteDrawMode = () => {
        isInfiniteDrawMode.value = !isInfiniteDrawMode.value;
        const modeMsg = isInfiniteDrawMode.value
          ? '已开启无限绘制模式'
          : '已关闭无限绘制模式，绘制后自动切换到移动模式';
        Message.info(modeMsg);
      };

      const labelKeymap = computed(() => {
        return allLabels.value.reduce((acc, label, index) => {
          const key = index + 1;
          if (key < 10) {
            acc[key.toString()] = () => {
              if (allLabels.value[index]) {
                props.updateState({
                  lastSelectedLabel: allLabels.value[index].id,
                });
                handleSelectChange(allLabels.value[index].id);
              }
            };
          }
          return acc;
        }, {});
      });

      // 简化的快捷键
      const keymap = computed(() => {
        // 如果在查看模式，禁用所有快捷键（q键在主容器处理）
        if (props.state.viewMode === 'grid') {
          return {};
        }

        // 导航快捷键（A/D/S）：在加载期间禁用，防止重复跳页
        const navKeyA = {
          keyup: () => {
            if (isUIBlocked.value) return; // 加载中禁用
            hideTooltip();
            props.handlePrev();
          },
        };
        const navKeyD = {
          keyup: () => {
            if (isUIBlocked.value) return; // 加载中禁用
            hideTooltip();
            props.handleNext(false);
          },
        };
        const navKeyS = () => {
          if (isUIBlocked.value) return; // 加载中禁用
          confirm();
        };

        if (props.deleteable)
          return {
            a: navKeyA,
            d: navKeyD,
            w: toggleMode,
            e: toggleInfiniteDrawMode,
            // q键在主容器处理，用于切换标注/查看模式
            delete: removeAnnotation,
            esc: cancelSelection,
            f: finishDraw,
            r: removeAllAnnotations,
            s: navKeyS,
            ...labelKeymap.value,
          };
        else
          return {
            a: navKeyA,
            d: navKeyD,
            w: toggleMode,
            e: toggleInfiniteDrawMode,
            // q键在主容器处理，用于切换标注/查看模式
            delete: removeAnnotation,
            esc: cancelSelection,
            f: finishDraw,
            r: removeAllAnnotations,
            s: navKeyS,
            ...labelKeymap.value,
          };
      });

      // 简化的点击导航函数
      const handleClickPrev = () => {
        hideTooltip();
        props.handlePrev();
      };

      const handleClickNext = () => {
        hideTooltip();
        props.handleNext(false);
      };

      const handleClickUp = () => {
        hideTooltip();
        props.handleUp();
      };

      const handleClickDown = () => {
        hideTooltip();
        props.handleDown(false);
      };

      const handleClickJumpUnannotated = () => {
        hideTooltip();
        props.handleJumpToUnannotated && props.handleJumpToUnannotated();
      };

      // 选中标注
      const setCurAnnotation = (annotation = {}) => {
        props.updateState({
          currentAnnotationId: annotation.id || '',
        });
      };

      // 开始绘制
      const handleBrushStart = (start) => {
        // 关闭已有的 dropdown
        hideTooltip();
        const { x, y } = start;
        onBrushStart({ x, y });
        // 重置当前选中的标注
        setCurAnnotation(undefined);
      };

      const handleBrushMove = (state) => {
        const { x, y } = state.end || {};
        onBrushMove({ x, y });
      };

      const handleBrushEnd = (state, event, options = {}) => {
        const { prevState = {} } = options;
        // 确认是 move 之后触发
        if (state.end && !!prevState.isDragging) {
          // 创建一个新的状态对象，不直接修改原始坐标
          // 重要：保存原始坐标和图片偏移信息，以便后续处理
          const finalState = {
            ...state,
            // 存储调整后的坐标（减去图片偏移量）
            start: {
              x: state.start.x - imageOffset.x,
              y: state.start.y - imageOffset.y,
            },
            end: {
              x: state.end.x - imageOffset.x,
              y: state.end.y - imageOffset.y,
            },
            // 同时保留原始坐标和图片偏移信息
            originalCoordinates: {
              start: { ...state.start },
              end: { ...state.end },
              imageOffset: { ...imageOffset },
              zoom: zoom.zoom,
              scale: dimension.value.scale,
            },
            // 更新extent以反映调整后的坐标
            extent: {
              x0: Math.min(state.start.x - imageOffset.x, state.end.x - imageOffset.x),
              y0: Math.min(state.start.y - imageOffset.y, state.end.y - imageOffset.y),
              x1: Math.max(state.start.x - imageOffset.x, state.end.x - imageOffset.x),
              y1: Math.max(state.start.y - imageOffset.y, state.end.y - imageOffset.y),
            },
          };

          // 展示tooltip，如果仅有一个标签，则不弹tooltip
          if (labels.value.length >= 1)
            showTooltip(props.state.lastSelectedLabel ?? null, event, {
              el: imgWrapperRef.value,
            });

          // 回调，传递完整的状态信息
          props.drawShapeEnd && props.drawShapeEnd(finalState, event);
          onBrushReset();

          // 如果不是无限绘制模式，绘制完成后自动切换到移动模式
          if (!isInfiniteDrawMode.value && !isMoveMode.value) {
            isMoveMode.value = true;
            Message.info('已自动切换到移动模式');
          }

          // 画框结束后恢复焦点，确保快捷键能继续工作
          setTimeout(() => {
            if (workspaceRef.value) {
              workspaceRef.value.focus();
            }
          }, 50);
          return;
        }
        onBrushReset();
      };

      // 清除选择框
      const clearSelection = () => attrs.onSelection(false);

      // 判断标签是否已存在
      const islabelExists = (value) => {
        return !!(labels.value || []).find((label) => label.id === Number(value));
      };

      // 基于检测生成 bbox
      const offsetBbox = (bbox) => {
        return {
          x: bbox.x * dimension.value.scale,
          y: bbox.y * dimension.value.scale,
          width: bbox.width * dimension.value.scale,
          height: bbox.height * dimension.value.scale,
        };
      };

      // 标注偏移
      const offset = (annotate) => {
        const { data = {} } = annotate;
        const _bbox = props.isSegmentation
          ? parsePolygon2Bbox(data.points)
          : extent2Bbox(data.extent);
        return offsetBbox(_bbox);
      };

      // handle 变更
      const onBrushHandleChange = (brush, annotation) => {
        // 同步 brush
        const pos = offset(annotation);
        // 将屏幕坐标转换回原始图片坐标
        const scale = dimension.value.scale;
        const originalExtent = {
          x0: brush.extent.x0 / scale,
          y0: brush.extent.y0 / scale,
          x1: brush.extent.x1 / scale,
          y1: brush.extent.y1 / scale,
        };

        props.updateState((prev) => {
          const index = prev.annotations.findIndex((d) => d.id === annotation.id);
          if (index > -1) {
            const selectedItem = prev.annotations[index];
            const _nextItem = {
              ...selectedItem,
              data: {
                ...selectedItem.data,
                extent: originalExtent,
              },
            };

            const nextAnnotations = replace(prev.annotations, index, _nextItem);
            return {
              ...prev,
              annotations: nextAnnotations,
            };
          }
          return prev;
        });

        // 更新brush
        updateBrush((prevBrush) => {
          return {
            ...prevBrush,
            isBrushing: true,
            extent: {
              x0: pos.x,
              x1: pos.x + pos.width,
              y0: pos.y,
              y1: pos.y + pos.height,
            },
          };
        });
      };

      // handle 拖拽完成
      const onBrushHandleEnd = (brush, annotation) => {
        // 同步 brush
        const pos = offset(annotation);
        // 更新brush
        updateBrush((prevBrush) => {
          return {
            ...prevBrush,
            isBrushing: false,
            extent: {
              x0: pos.x,
              x1: pos.x + pos.width,
              y0: pos.y,
              y1: pos.y + pos.height,
            },
          };
        });
      };

      // 限制“处理中”提示的展示次数
      const showProcessingWarning = () => {
        if (processingWarningHandler.value) return;
        processingWarningHandler.value = Message.warning({
          message: '正在处理中，请稍候...',
          duration: 2000,
          onClose: () => {
            processingWarningHandler.value = null;
          },
        });
      };

      // 严格防重复的确认函数
      // 统一使用handleNext(true)来处理s键逻辑，与d键使用相同的切换机制，但会先提交
      const confirm = () => {
        const currentTime = Date.now();
        const currentImageId = props.state.currentImgId;

        // 多重检查防止重复提交
        if (
          isConfirming.value ||
          confirmingImageId.value === currentImageId ||
          currentTime - lastConfirmTime.value < 500 // 500ms内不允许重复操作
        ) {
          showProcessingWarning();
          return;
        }

        // 立即标记为确认中
        isConfirming.value = true;
        confirmingImageId.value = currentImageId;
        lastConfirmTime.value = currentTime;
        hideTooltip();

        // 先提交当前图片
        props
          .handleConfirm()
          .then(() => {
            // 提交成功后，使用统一的handleNext(true)切换到下一张
            // 这样可以使用缓存，避免每次都重新加载标注信息
            // shouldConfirm=true表示s键，会在最后一张时只提交不切换
            props.handleNext(true);
          })
          .catch((error) => {
            console.error(`图片 ${currentImageId} 确认失败:`, error);
            Message.error(`保存失败: ${error.message || '未知错误'}`);
          })
          .finally(() => {
            // 重置状态，延迟稍短以提高响应性
            setTimeout(() => {
              isConfirming.value = false;
              confirmingImageId.value = null;
            }, 200); // 减少到200ms
          });
      };

      // 一键确认函数 - 仅用于多人标注
      const handleBatchConfirm = () => {
        if (props.handleBatchConfirm) {
          props.handleBatchConfirm();
        }
      };

      // 选中selectItem
      const handleSelectChange = async (value) => {
        let labelVal = value;
        // 首先判断标签是否已存在
        // 如果没有，就先创建
        if (!islabelExists(value) && props.state.datasetInfo.isGuided === false) {
          labelVal = await props.createLabel({ name: value });
          const nextLabels = await props.queryLabels();
          // 更新全局 provide
          props.updateState({
            labels: nextLabels,
          });
        }
        const annotationInfo = props.isSegmentation ? props.state.shapes : props.state.annotations;
        const selectedLabel = {
          ...api.label,
          value: labelVal,
        };

        props.updateState({
          lastSelectedLabel: labelVal,
        });
        // 引导式不新建不更新标签
        if (!(!islabelExists(value) && props.state.datasetInfo.isGuided === true)) {
          Object.assign(api, {
            label: selectedLabel,
          });
          const curAnnotation =
            annotationInfo.find((d) => d.id === props.state.currentAnnotationId) || {};
          // 触发标注对应标签变更事件
          ctx.emit('selectLabel', { selectedLabel, curAnnotation });
          // 选择标签完成关闭选择器
          hideTooltip();

          // 选择标签后恢复焦点，确保快捷键能继续工作
          setTimeout(() => {
            if (workspaceRef.value) {
              workspaceRef.value.focus();
            }
          }, 50);
        }
      };

      const handleZoom = (nextZoomTransform) => {
        if (!nextZoomTransform) return;
        setZoom({
          zoomX: nextZoomTransform.x,
          zoomY: nextZoomTransform.y,
          zoom: nextZoomTransform.k,
        });
      };

      const focusWorkspace = () => {
        if (workspaceRef.value) {
          workspaceRef.value.focus();
        }
      };
      const refreshLabelKeyMap = async () => {
        allLabels.value = await props.queryLabels();
      };

      // 显示删除确认tooltip
      const showDeleteConfirm = () => {
        if (toolbarRef.value) {
          toolbarRef.value.showDeletePopConfirm();
        }
      };

      // 隐藏删除确认tooltip
      const hideDeleteConfirm = () => {
        if (toolbarRef.value) {
          toolbarRef.value.hideDeletePopConfirm();
        }
      };

      defineExpose({
        focusWorkspace,
        refreshLabelKeyMap,
        showDeleteConfirm,
        hideDeleteConfirm,
      });

      // 拖拽开始
      const onDragStart = (draw, annotation, event) => {
        if (!isMoveMode.value) {
          event.sourceEvent.stopPropagation(); // 阻止事件传播
          return; // 绘图模式下不执行拖动
        }
        // 不再使用 raise 调整数组顺序，因为渲染时已按面积排序（小框在上层）
        // 同步当前标注
        setCurAnnotation(annotation);

        // 同步 brush
        const pos = offset(annotation);
        updateBrush((prevBrush) => {
          const start = {
            x: pos.x,
            y: pos.y,
          };
          const end = {
            x: pos.x + pos.width,
            y: pos.y + pos.height,
          };
          return {
            ...prevBrush,
            start,
            end,
            extent: getExtent(start, end),
          };
        });
      };

      // 拖拽 boxing 更新位置
      const onDragMove = (draw, annotation) => {
        hideTooltip();
        const { drag = {} } = draw;

        // 使用 bboxWrapper 已经计算好的 validDx 和 validDy
        // 这些值已经除以 zoom 并且经过边界检查
        const validDx = drag.validDx !== undefined ? drag.validDx : 0;
        const validDy = drag.validDy !== undefined ? drag.validDy : 0;

        // 更新 brush 位置
        updateBrush((prevBrush) => {
          const { x: x0, y: y0 } = prevBrush.start;
          const { x: x1, y: y1 } = prevBrush.end;
          return {
            ...prevBrush,
            isBrushing: true,
            extent: {
              ...prevBrush.extent,
              x0: x0 + validDx,
              x1: x1 + validDx,
              y0: y0 + validDy,
              y1: y1 + validDy,
            },
          };
        });

        setTransformer({
          isDragging: true,
          id: annotation.id,
          x: drag.x,
          y: drag.y,
          dx: validDx,
          dy: validDy,
        });
      };

      // 拖拽 boxing 结束，更新位置
      const onDragEnd = (draw, annotation) => {
        const { drag = {} } = draw;

        // 在重置前保存当前的位移值
        const { dx, dy } = transformer;

        // 重置标注 transform
        setTransformer({
          isDragging: false,
          id: annotation.id,
          x: drag.x,
          y: drag.y,
          dx: 0,
          dy: 0,
        });

        // 将屏幕坐标系的位移转换为原始图片坐标系
        const scale = dimension.value.scale || 1;
        const originalDx = dx / scale;
        const originalDy = dy / scale;

        props.updateState((prev) => {
          const index = prev.annotations.findIndex((d) => d.id === annotation.id);
          if (index > -1) {
            const selectedItem = prev.annotations[index];
            // 使用转换后的 dx 和 dy 值来更新位置
            const _nextItem = {
              ...selectedItem,
              data: {
                ...selectedItem.data,
                extent: {
                  x0: selectedItem.data.extent.x0 + originalDx,
                  y0: selectedItem.data.extent.y0 + originalDy,
                  x1: selectedItem.data.extent.x1 + originalDx,
                  y1: selectedItem.data.extent.y1 + originalDy,
                },
              },
            };

            const nextAnnotations = replace(prev.annotations, index, _nextItem);
            return {
              ...prev,
              annotations: nextAnnotations,
            };
          }
          return prev;
        });

        // 更新 brush 位置
        updateBrush((prevBrush) => {
          return {
            ...prevBrush,
            isBrushing: false,
            start: {
              ...prevBrush.start,
              x: Math.min(prevBrush.extent.x0, prevBrush.extent.x1),
              y: Math.min(prevBrush.extent.y0, prevBrush.extent.y1),
            },
            end: {
              ...prevBrush.end,
              x: Math.max(prevBrush.extent.x0, prevBrush.extent.x1),
              y: Math.max(prevBrush.extent.y0, prevBrush.extent.y1),
            },
          };
        });
      };

      const handleShapesChange = ({ type, state: shapeState, options = {} }) => {
        const { shape, event } = options;
        switch (type) {
          case 'DRAW_END': {
            // 更新当前选中的 shape
            if (labels.value.length >= 1)
              event &&
                showTooltip(props.state.lastSelectedLabel ?? null, event, {
                  el: imgWrapperRef.value,
                });
            // 回调，进行标注格式组装
            props.drawShapeEnd(shape);

            // 绘制结束后恢复焦点，确保快捷键能继续工作
            setTimeout(() => {
              if (workspaceRef.value) {
                workspaceRef.value.focus();
              }
            }, 50);
            break;
          }
          case 'DRAG_START': {
            if (labels.value.length >= 1) {
              event &&
                showTooltip(shape, event, {
                  el: imgWrapperRef.value,
                });
            }
            break;
          }
          default: {
            const { transformer } = segmentationRef.value;
            // 更新transformer
            setTransformer(transformer);
            props.updateState({
              shapes: shapeState.shapes,
            });
          }
        }
      };

      const handleLeftClick = (event) => {
        // 检查是否点击在标注框上
        const isOnAnnotation =
          event.target.closest('.bbox-group') || event.target.closest('.annotation-group');

        // 检查是否点击在下拉选择框上
        const isOnSelect =
          event.target.closest('.el-select') ||
          event.target.closest('.el-popper') ||
          event.target.closest('.el-select-dropdown') ||
          event.target.closest('.el-select-dropdown__item');
        // 如果点击在标签处
        const isOnTag = event.target.closest('.el-tag__content');
        if (!isOnAnnotation && !isOnSelect && !isOnTag) {
          hideTooltip();
          setCurAnnotation(undefined);
        }
      };

      onMounted(async () => {
        allLabels.value = await props.queryLabels();
        document.body.addEventListener('click', (e) => {
          // 如果不在画布内，直接清空
          // 过滤右侧设置标注 table
          const { target } = e;
          if (
            !target.closest('.annotation-table') &&
            !target.closest('#stage') &&
            !target.closest('.label-list')
          ) {
            // 清空选中的注释
            props.updateState({
              currentAnnotationId: '',
            });
            hideTooltip();
          }
        });
        document.body.addEventListener('contextmenu', (e) => {
          if (e.target.closest('#stage')) {
            e.preventDefault(); // 在画布区域内阻止默认右键菜单
          }
        });

        // 初始化执行一次
        api.bounding = getBounding(imgWrapperRef.value);
        document.addEventListener('mousedown', handleLeftClick);

        // 初始化时设置焦点，确保快捷键可用
        setTimeout(() => {
          if (workspaceRef.value) {
            workspaceRef.value.focus();
          }
        }, 100);
      });

      onBeforeUnmount(() => {
        resizerRef.value && resizerRef.value.remove();
        resizerRef.value = null;
        document.removeEventListener('mousedown', handleLeftClick);
      });

      // 监听 currentImage 变化 - 重置确认状态
      watch(
        () => props.currentImg,
        (nextImg) => {
          // 每次切换图片重置 zoom
          resetZoom();
          // 复原图片位置
          resetImagePosition();

          // 重置确认状态
          isConfirming.value = false;
          confirmingImageId.value = null;

          // 重置选中的标签和位置
          Object.assign(api, {
            label: {},
            isCenter: false,
            imgBounding: null,
          });
          if (nextImg?.url) {
            setImg(nextImg.url);
          }
          // 首先清空标注信息
          setApi({
            [props.annotationType]: [],
          });
          // 清理选择（分割）
          cancelSelection();
          // 若标签仅有一个，则修改lastSelectedLabel，画框结束时在drawShapeEnd方法中设置标注框标签值，不弹出tooltip框选择
          // 如果已经有选中的标签，保持不变；否则使用第一个标签
          if (labels.value.length > 0) {
            // 检查 lastSelectedLabel 是否有效
            const lastLabel = props.state.lastSelectedLabel;
            const isLastLabelValid =
              lastLabel && labels.value.some((label) => label.id === lastLabel);

            // 如果上次选择的标签仍然有效，使用它；否则使用第一个标签
            const targetLabelId = isLastLabelValid ? lastLabel : labels.value[0].id;

            const selectedLabel = {
              ...api.label,
              value: targetLabelId,
            };
            Object.assign(api, {
              label: selectedLabel,
            });

            // 使用 updateState 方法来更新状态，确保响应式更新
            if (!isLastLabelValid) {
              props.updateState({
                lastSelectedLabel: targetLabelId,
              });
            }
          }
        },
      );

      watch(
        () => props.state.annotations,
        (nextProps) => {
          api[props.annotationType] = nextProps || [];
        },
      );

      watch(
        () => props.state.shapes,
        (nextProps) => {
          api[props.annotationType] = nextProps || [];
        },
      );

      watch(
        () => props.state.imgLoading,
        (newV) => {
          if (!newV) {
            finishLoad();
          } else {
            startLoad();
          }
        },
      );

      return {
        attrs,
        imgWrapperRef,
        labels,
        brush,
        clearSelection,
        filter,
        // zoom
        zoom,
        zoomIn,
        zoomOut,
        resetZoom,
        handleZoom,
        zoomRef,
        // tooltip
        tooltipData,
        hideTooltip,
        // img
        imgInfo,
        dimension,
        svgRef,
        imgRef,
        // annotations
        api,
        setApi,
        // event
        handleClickPrev,
        handleClickNext,
        handleClickUp,
        handleClickDown,
        handleClickJumpUnannotated,
        handleSelectChange,
        confirm,
        onDragStart,
        onDragMove,
        onDragEnd,
        keymap,
        // brush 事件
        handleBrushStart,
        handleBrushMove,
        handleBrushEnd,
        // 标注偏移
        offset,
        // 针对锚点偏移
        offsetBbox,
        transformer,
        setTransformer,
        onBrushHandleChange,
        onBrushHandleEnd,
        // 缩放情况下将绝对位置转换为相对路径
        transformZoom,
        getZoom,
        setCurAnnotation,
        segmentationRef,
        handleShapesChange,
        annotations,
        sortedAnnotations,
        props,
        isMoveMode,
        isInfiniteDrawMode,
        modeText,
        handleAnnotationRightClick,
        imageOffset,
        isDraggingImage,
        handleStageMouseDown,
        handleStageMouseMove,
        handleStageMouseUp,
        isConfirming,
        workspaceRef,
        focusWorkspace,
        refreshLabelKeyMap,
        showDeleteConfirm,
        hideDeleteConfirm,
        toolbarRef,
        showJumpUnannotated,
      };
    },
  };
</script>

<style lang="scss">
  @import 'src/assets/scss/atomic.scss';

  #left-button {
    z-index: 500;
    position: absolute;
    left: 1%;
    top: 45%;
    pointer-events: auto;
  }

  #right-button {
    z-index: 500;
    position: absolute;
    right: 1%;
    top: 45%;
    pointer-events: auto;
  }

  #stage {
    max-height: 100%;
  }

  .workspace-stage {
    flex: 1;
    overflow: hidden;
    cursor: grab;
    user-select: none;

    .imgWrapper {
      touch-action: none;
      user-select: none;

      &.imgScale {
        margin: 0 auto;

        img {
          display: inline-block;
          width: 100%;
          height: 100%;
          user-select: none;
        }
      }
    }

    .canvas {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: calc(100vh - 50px - 48px);
      pointer-events: none;
    }

    .annotation-score-group {
      pointer-events: none;

      .annotation-score-row {
        position: absolute;
        color: #fff;

        .score {
          display: inline-block;
          min-width: 48px;
          font-size: 16px;
          line-height: 24px;
          user-select: none;

          .unit {
            margin-left: 2px;
            font-size: 0.8em;
          }
        }
      }
    }

    .annotation-tag-group {
      pointer-events: none;

      .annotation-label {
        position: absolute;
        color: #fff;
      }
    }

    .bbox-group {
      cursor: pointer;
    }

    .brush-tooltip {
      position: absolute;
      font-size: 12px;
      line-height: 1em;
      color: #fff;
      pointer-events: none;

      .tooltip-item-row {
        display: flex;
        white-space: nowrap;
      }

      .tooltip-content {
        display: inline-block;
        background-color: $dark;
        color: #fff;
        transform-origin: top left;
        white-space: nowrap;
        border-radius: 4px;
      }
    }
  }

  .annotation-element-group {
    transition: none !important;
  }

  .content-container {
    position: relative;
    transition: none !important;
  }

  .drawing-mode-indicator {
    position: absolute;
    top: 6%;
    left: 72%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.7);
    color: white;
    padding: 5px 15px;
    border-radius: 4px;
    z-index: 1000;
  }
</style>
