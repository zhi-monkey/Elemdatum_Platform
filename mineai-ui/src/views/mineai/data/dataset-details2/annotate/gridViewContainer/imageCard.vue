<template>
  <div class="image-card">
    <div class="card-header">
      <div class="card-left">
        <span class="card-index">#{{ index }}</span>
        <span class="file-name" :title="file.name">{{ file.name }}</span>
      </div>
      <span class="card-status" :class="statusClass">
        {{ statusText }}
      </span>
    </div>
    <div class="card-body">
      <div class="image-wrapper" ref="imageWrapperRef">
        <!-- 加载状态 -->
        <div v-if="!isFullyLoaded" class="card-loading">
          <Spin size="large" />
          <div class="loading-text">加载中...</div>
        </div>
        <img
          :src="file.url"
          class="card-image"
          :class="{ 'image-hidden': !isFullyLoaded }"
          @load="onImageLoad"
          @error="onImageError"
          ref="imageRef"
        />
        <!-- SVG 标注层 - 使用图片真实尺寸作为viewBox，自动缩放对齐 -->
        <svg
          v-if="imgLoaded && localAnnotations.length > 0"
          class="annotation-overlay"
          :viewBox="`0 0 ${imgSize.width} ${imgSize.height}`"
          preserveAspectRatio="xMidYMid meet"
          @click="handleSvgClick"
        >
          <g
            v-for="annotation in localAnnotations"
            :key="annotation.id"
            class="annotation-group"
            :class="{ selected: selectedAnnotationId === annotation.id }"
            :data-annotation-id="annotation.id"
          >
            <!-- 目标检测：矩形框 -->
            <template v-if="annotation.data.extent">
              <rect
                :x="annotation.data.extent.x0"
                :y="annotation.data.extent.y0"
                :width="annotation.data.extent.x1 - annotation.data.extent.x0"
                :height="annotation.data.extent.y1 - annotation.data.extent.y0"
                :stroke="annotation.data.color || '#00ff00'"
                :fill="annotation.data.color ? annotation.data.color + '30' : '#00ff0030'"
                stroke-width="2"
                vector-effect="non-scaling-stroke"
                class="annotation-rect"
              />
              <!-- Tag样式标签：背景（带边界检测） -->
              <rect
                :x="
                  getLabelPosition(
                    annotation.data.extent.x0,
                    annotation.data.extent.y0,
                    annotation.data.categoryId,
                  ).x
                "
                :y="
                  getLabelPosition(
                    annotation.data.extent.x0,
                    annotation.data.extent.y0,
                    annotation.data.categoryId,
                  ).y
                "
                :width="getLabelWidth(annotation.data.categoryId)"
                :height="22 / svgScale"
                :fill="annotation.data.color || '#00ff00'"
                :rx="3 / svgScale"
                :ry="3 / svgScale"
                opacity="0.9"
                class="annotation-tag-bg"
              />
              <!-- Tag样式标签：文字 -->
              <text
                :x="
                  getLabelPosition(
                    annotation.data.extent.x0,
                    annotation.data.extent.y0,
                    annotation.data.categoryId,
                  ).x +
                  8 / svgScale
                "
                :y="
                  getLabelPosition(
                    annotation.data.extent.x0,
                    annotation.data.extent.y0,
                    annotation.data.categoryId,
                  ).y +
                  16 / svgScale
                "
                fill="#ffffff"
                :font-size="getLabelFontSize(annotation.data.categoryId)"
                font-weight="600"
                class="annotation-label-tag"
              >
                {{ getLabelName(annotation.data.categoryId) }}
              </text>
            </template>

            <!-- 语义分割：多边形 -->
            <template v-else-if="annotation.data.points">
              <polygon
                v-for="(polygon, idx) in normalizePolygons(annotation.data.points)"
                :key="`polygon-${idx}`"
                :points="polygonToSvgPoints(polygon)"
                :stroke="annotation.data.color || '#00ff00'"
                :fill="annotation.data.color ? annotation.data.color + '30' : '#00ff0030'"
                stroke-width="2"
                vector-effect="non-scaling-stroke"
                class="annotation-polygon"
              />
              <!-- Tag样式标签：背景（带边界检测） -->
              <rect
                :x="
                  getLabelPosition(
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minX,
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minY,
                    annotation.data.categoryId,
                  ).x
                "
                :y="
                  getLabelPosition(
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minX,
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minY,
                    annotation.data.categoryId,
                  ).y
                "
                :width="getLabelWidth(annotation.data.categoryId)"
                :height="22 / svgScale"
                :fill="annotation.data.color || '#00ff00'"
                :rx="3 / svgScale"
                :ry="3 / svgScale"
                opacity="0.9"
                class="annotation-tag-bg"
              />
              <!-- Tag样式标签：文字 -->
              <text
                :x="
                  getLabelPosition(
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minX,
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minY,
                    annotation.data.categoryId,
                  ).x +
                  8 / svgScale
                "
                :y="
                  getLabelPosition(
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minX,
                    getPolygonBounds(normalizePolygons(annotation.data.points)).minY,
                    annotation.data.categoryId,
                  ).y +
                  16 / svgScale
                "
                fill="#ffffff"
                :font-size="getLabelFontSize(annotation.data.categoryId)"
                font-weight="600"
                class="annotation-label-tag"
              >
                {{ getLabelName(annotation.data.categoryId) }}
              </text>
            </template>
          </g>
        </svg>
      </div>
    </div>
    <div class="card-footer"></div>
  </div>
</template>

<script setup>
  import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
  import { Spin } from 'ant-design-vue';

  const props = defineProps({
    file: Object,
    annotations: Array,
    labels: Array,
    index: Number,
    annotationsLoaded: Boolean,
  });

  const imgLoaded = ref(false);
  const imgSize = ref({ width: 0, height: 0 });
  const imageWrapperRef = ref(null);
  const imageRef = ref(null);
  const svgScale = ref(1); // SVG缩放比例

  // 卡片完全加载完成（图片+标注都加载完）
  const isFullyLoaded = computed(() => {
    return imgLoaded.value && props.annotationsLoaded;
  });

  // 本地标注顺序（用于控制显示层级）
  const localAnnotations = ref([]);
  const selectedAnnotationId = ref(null);

  // 状态类
  const statusClass = computed(() => {
    return props.file.status === 104 ? 'status-completed' : 'status-pending';
  });

  // 状态文本 - 合并状态和标注数
  const statusText = computed(() => {
    if (props.file.status === 104) {
      return `已标注:${localAnnotations.value.length}`;
    } else {
      return '未标注';
    }
  });

  // 获取标签名称
  const getLabelName = (labelId) => {
    const label = props.labels.find((l) => l.id === labelId);
    return label?.name || '';
  };

  // 计算标签字体大小 - 根据SVG缩放比例调整
  const getLabelFontSize = (labelId) => {
    const baseSize = 16; // 增大基准字体大小
    const minSize = 14; // 提高最小字体大小

    // 根据SVG缩放比例计算实际字体大小
    // svgScale.value 越小，说明图片被缩得越小，需要更大的字体
    const scaledSize = baseSize / svgScale.value;

    // 限制最小字体大小
    return Math.max(minSize, scaledSize);
  };

  // 计算标签宽度（根据缩放比例调整）
  const getLabelWidth = (labelId) => {
    const name = getLabelName(labelId);
    let width = 0;
    const charWidth = 8 / svgScale.value; // 根据缩放比例调整字符宽度
    const cnCharWidth = 14 / svgScale.value;

    for (let i = 0; i < name.length; i++) {
      // 判断是否为中文字符
      if (name.charCodeAt(i) > 255) {
        width += cnCharWidth; // 中文字符宽度
      } else {
        width += charWidth; // 英文字符宽度
      }
    }
    return width + 16 / svgScale.value; // 加上左右padding
  };

  // 计算标签位置（带边界检测）
  const getLabelPosition = (x, y, labelId, isTop = true) => {
    const labelWidth = getLabelWidth(labelId);
    const labelHeight = 22 / svgScale.value;
    const offset = 24 / svgScale.value;

    let labelX = x;
    let labelY = isTop ? y - offset : y + offset;

    // 边界检测 - X轴
    if (labelX < 0) {
      labelX = 0;
    } else if (labelX + labelWidth > imgSize.value.width) {
      labelX = imgSize.value.width - labelWidth;
    }

    // 边界检测 - Y轴
    if (labelY < 0) {
      // 如果上方超出边界，放到框下方
      labelY = y + 2 / svgScale.value;
    } else if (labelY + labelHeight > imgSize.value.height) {
      // 如果下方超出边界，放到框上方
      labelY = y - offset;
    }

    return { x: labelX, y: labelY };
  };

  // 标准化多边形数据 - 确保返回数组的数组格式
  const normalizePolygons = (points) => {
    if (!points || !Array.isArray(points)) return [];

    // 检查第一个元素，判断是单个多边形还是多个多边形
    if (points.length > 0 && points[0] && typeof points[0] === 'object' && 'x' in points[0]) {
      // 单个多边形：[{x, y}, {x, y}, ...]
      return [points];
    }

    // 多个多边形：[[{x, y}, ...], [{x, y}, ...]]
    return points;
  };

  // 将多边形点数组转换为SVG points字符串
  const polygonToSvgPoints = (polygon) => {
    if (!Array.isArray(polygon)) {
      console.warn('polygon is not an array:', polygon);
      return '';
    }
    return polygon.map((point) => `${point.x},${point.y}`).join(' ');
  };

  // 计算多边形的边界框
  const getPolygonBounds = (points) => {
    if (!points || points.length === 0) {
      return { minX: 0, minY: 0, maxX: 0, maxY: 0 };
    }

    let minX = Infinity;
    let minY = Infinity;
    let maxX = -Infinity;
    let maxY = -Infinity;

    points.forEach((polygon) => {
      polygon.forEach((point) => {
        minX = Math.min(minX, point.x);
        minY = Math.min(minY, point.y);
        maxX = Math.max(maxX, point.x);
        maxY = Math.max(maxY, point.y);
      });
    });

    return { minX, minY, maxX, maxY };
  };

  // 图片加载完成
  const onImageLoad = (event) => {
    const img = event.target;
    imgSize.value = {
      width: img.naturalWidth,
      height: img.naturalHeight,
    };
    imgLoaded.value = true;

    // 延迟计算SVG缩放比例，确保DOM已渲染
    requestAnimationFrame(() => {
      calculateSvgScale();
    });
  };

  // 计算SVG缩放比例
  const calculateSvgScale = () => {
    if (!imageWrapperRef.value || !imgSize.value.width) return;

    const wrapperRect = imageWrapperRef.value.getBoundingClientRect();
    const wrapperWidth = wrapperRect.width;
    const wrapperHeight = wrapperRect.height;

    // 如果容器尺寸为0，说明DOM还没准备好，延迟计算
    if (wrapperWidth === 0 || wrapperHeight === 0) {
      requestAnimationFrame(() => {
        calculateSvgScale();
      });
      return;
    }

    // 计算实际显示时的缩放比例
    const scaleX = wrapperWidth / imgSize.value.width;
    const scaleY = wrapperHeight / imgSize.value.height;

    // 使用较小的缩放比例（object-fit: contain的逻辑）
    const scale = Math.min(scaleX, scaleY);
    svgScale.value = Math.max(scale, 0.01); // 防止除以0或过小的值
  };

  // 图片加载失败
  const onImageError = () => {
    imgLoaded.value = true;
  };

  // SVG点击事件处理 - 检测所有重叠的框
  const handleSvgClick = (event) => {
    const svg = event.currentTarget;
    const rect = svg.getBoundingClientRect();

    const clickX = ((event.clientX - rect.left) / rect.width) * imgSize.value.width;
    const clickY = ((event.clientY - rect.top) / rect.height) * imgSize.value.height;

    const clickedAnnotations = [];
    for (let i = localAnnotations.value.length - 1; i >= 0; i--) {
      const annotation = localAnnotations.value[i];
      if (isPointInAnnotation(annotation, clickX, clickY)) {
        clickedAnnotations.push(annotation);
      }
    }

    if (clickedAnnotations.length === 0) return;

    if (clickedAnnotations.length === 1) {
      const annotation = clickedAnnotations[0];
      const index = localAnnotations.value.findIndex((a) => a.id === annotation.id);
      if (index !== -1 && index !== localAnnotations.value.length - 1) {
        const movedAnnotation = localAnnotations.value.splice(index, 1)[0];
        localAnnotations.value.push(movedAnnotation);
      }
      selectAnnotation(annotation);
      return;
    }

    const topAnnotation = clickedAnnotations[0];
    const index = localAnnotations.value.findIndex((a) => a.id === topAnnotation.id);
    if (index !== -1) {
      const movedAnnotation = localAnnotations.value.splice(index, 1)[0];

      const allOverlappingIds = clickedAnnotations.map((a) => a.id);
      let insertIndex = 0;
      for (let i = 0; i < localAnnotations.value.length; i++) {
        if (allOverlappingIds.includes(localAnnotations.value[i].id)) {
          insertIndex = i;
          break;
        }
      }

      localAnnotations.value.splice(insertIndex, 0, movedAnnotation);
      const newTopAnnotation = clickedAnnotations[1];
      selectAnnotation(newTopAnnotation);
    }
  };

  // 选中标注
  const selectAnnotation = (annotation) => {
    selectedAnnotationId.value = annotation.id;
    setTimeout(() => {
      selectedAnnotationId.value = null;
    }, 1000);
  };

  // 计算标注框的面积
  const getAnnotationArea = (annotation) => {
    if (annotation.data.extent) {
      const { extent } = annotation.data;
      return (extent.x1 - extent.x0) * (extent.y1 - extent.y0);
    }
    if (annotation.data.points) {
      const bounds = getPolygonBounds(normalizePolygons(annotation.data.points));
      return (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY);
    }
    return 0;
  };

  // 检测点击位置是否在框内
  const isPointInAnnotation = (annotation, clickX, clickY) => {
    if (annotation.data.extent) {
      const { extent } = annotation.data;
      return (
        clickX >= extent.x0 && clickX <= extent.x1 && clickY >= extent.y0 && clickY <= extent.y1
      );
    }
    if (annotation.data.points) {
      const polygons = normalizePolygons(annotation.data.points);
      return polygons.some((polygon) => isPointInPolygon(clickX, clickY, polygon));
    }
    return false;
  };

  // 射线法判断点是否在多边形内
  const isPointInPolygon = (x, y, polygon) => {
    let inside = false;
    for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
      const xi = polygon[i].x;
      const yi = polygon[i].y;
      const xj = polygon[j].x;
      const yj = polygon[j].y;

      const intersect = yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
      if (intersect) inside = !inside;
    }
    return inside;
  };

  // 监听窗口大小变化，重新计算缩放比例
  onMounted(() => {
    window.addEventListener('resize', calculateSvgScale);
    // 组件挂载后也计算一次，确保初始状态正确
    if (imgLoaded.value) {
      requestAnimationFrame(() => {
        calculateSvgScale();
      });
    }
  });

  onUnmounted(() => {
    window.removeEventListener('resize', calculateSvgScale);
  });

  // 监听props.annotations变化，更新本地顺序（按面积从大到小排序，小框在上层）
  watch(
    () => props.annotations,
    (newAnnotations) => {
      if (newAnnotations && newAnnotations.length > 0) {
        localAnnotations.value = [...newAnnotations].sort((a, b) => {
          return getAnnotationArea(b) - getAnnotationArea(a);
        });
        // 标注数据变化时重新计算缩放比例
        if (imgLoaded.value) {
          requestAnimationFrame(() => {
            calculateSvgScale();
          });
        }
      } else {
        localAnnotations.value = [];
      }
    },
    { immediate: true },
  );
</script>

<style lang="scss" scoped>
  .image-card {
    background: #2a2d3e;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s ease;
    border: 2px solid transparent;
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #1f2233;

    .card-left {
      display: flex;
      align-items: center;
      flex: 1;
      min-width: 0;

      .card-index {
        color: #909399;
        font-size: 12px;
        font-weight: 500;
        margin-right: 8px;
        flex-shrink: 0;
      }

      .file-name {
        color: #e1e1e1;
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        flex: 1;
        min-width: 0;
      }
    }

    .card-status {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 500;
      flex-shrink: 0;
      margin-left: 8px;

      &.status-completed {
        background: #67c23a20;
        color: #67c23a;
      }

      &.status-pending {
        background: #e6a23c20;
        color: #e6a23c;
      }
    }
  }

  .card-body {
    aspect-ratio: 16 / 8;
    position: relative;
    overflow: hidden;
    background: #1a1d2e;
  }

  .image-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
  }

  .card-loading {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background: #1a1d2e;
    z-index: 10;

    .loading-text {
      margin-top: 12px;
      color: #909399;
      font-size: 14px;
    }
  }

  .card-image {
    width: 100%;
    height: 100%;
    object-fit: contain;
    display: block;
    transition: opacity 0.3s ease;

    &.image-hidden {
      opacity: 0;
    }
  }

  .annotation-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    cursor: pointer;
  }

  .annotation-group {
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 1 !important;

      .annotation-rect,
      .annotation-polygon {
        stroke-width: 3;
      }

      .annotation-tag-bg {
        opacity: 1 !important;
      }
    }

    &.selected {
      .annotation-rect,
      .annotation-polygon {
        stroke-width: 3;
        filter: drop-shadow(0 0 6px currentColor);
      }

      .annotation-tag-bg {
        opacity: 1 !important;
        filter: drop-shadow(0 0 4px rgba(0, 0, 0, 0.5));
      }
    }
  }

  .annotation-rect,
  .annotation-polygon {
    transition: stroke-width 0.2s ease, filter 0.2s ease;
  }

  .annotation-tag-bg {
    min-width: 40px;
    min-height: 22px;
    transition: opacity 0.2s ease, filter 0.2s ease;
  }

  .annotation-label-tag {
    user-select: none;
    pointer-events: none;
    text-rendering: optimizeLegibility;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  .card-footer {
    padding: 8px 12px;
    background: #1f2233;
    border-top: 1px solid #2a2d3e;

    .annotation-info {
      color: #909399;
      font-size: 12px;
      display: flex;
      align-items: center;
    }
  }
</style>
