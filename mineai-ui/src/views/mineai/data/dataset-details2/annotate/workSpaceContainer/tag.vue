<template>
  <div class="annotation-label image-tag usn" :style="outerStyle" v-if="ifShow2" ref="tagRef">
    <div class="tag-inner">
      <el-tag :color="tagColor" disable-transitions :style="tagStyle">
        {{ tagName }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
  import { isNil, throttle } from 'lodash-es';
  import { addSuffix, chroma, colorByLuminance } from '/@/utils/dubhe';
  import { defaultColor } from '../../util';
  import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue';

  const props = defineProps({
    annotate: Object,
    offset: Function,
    transformer: Object,
    getLabelName: Function,
    isMoving: {
      type: Boolean,
      default: false,
    },
    currentAnnotationId: String,
    annotationType: String,
    scale: {
      type: Number,
      default: 1,
    },
  });

  // eslint-disable-next-line vue/no-setup-props-destructure
  const { annotate = {}, getLabelName, offset, transformer } = props;
  const { data = {}, id } = annotate;
  const tagRef = ref(null);

  // 响应式的缩放比例
  const effectiveScale = ref(props.scale);

  // 监听父组件传递的 scale 变化
  watch(
    () => props.scale,
    (newScale) => {
      effectiveScale.value = newScale;
    },
  );

  // 动态计算外部定位样式
  const outerStyle = computed(() => {
    const pos = offset(props.annotate);

    const style = {
      left: addSuffix(pos.x),
      top: addSuffix(pos.y),
    };

    // 如果当前标注正在移动，添加位移
    if (annotate.id === transformer.id) {
      style.transform = `translate(${transformer.dx}px, ${transformer.dy}px)`;
    }

    return style;
  });

  // 动态计算标签样式
  const tagStyle = computed(() => {
    const scale = effectiveScale.value;
    // 设置字体大小的合理范围
    const minFontSize = 10; // 最小字体大小
    const maxFontSize = 14; // 最大字体大小
    const baseFontSize = 14; // 基础字体大小

    // 计算缩放后的字体大小，限制在最小和最大值之间
    let fontSize = baseFontSize / scale;
    fontSize = Math.min(maxFontSize, Math.max(minFontSize, fontSize));

    return {
      color: colorByLuminance(props.annotate.data.color || defaultColor),
      fontSize: `${fontSize}px`,
      padding: '2px 6px',
      lineHeight: '1.2',
      border: 'none',
      transform: `scale(${1 / scale})`,
      transformOrigin: 'top left',
      display: 'inline-block',
      whiteSpace: 'nowrap',
    };
  });

  // 标签背景色
  const tagColor = computed(() =>
    chroma(props.annotate.data.color || defaultColor)
      .alpha(0.8)
      .toString(),
  );

  // 标签文本
  const tagName = computed(() => getLabelName(props.annotate.data.categoryId));

  // 是否显示标签
  const ifShow2 = computed(() => {
    return !(props.currentAnnotationId === id && props.isMoving);
  });

  // 添加 resize 监听器确保响应窗口变化
  const handleResize = throttle(() => {
    if (tagRef.value) {
      // 强制重新计算样式
      effectiveScale.value = props.scale;
    }
  }, 100);

  onMounted(() => {
    window.addEventListener('resize', handleResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });
</script>

<style scoped lang="less">
  .annotation-label {
    position: absolute;
    z-index: 10;
    pointer-events: none;
    transform-origin: top left;

    .tag-inner {
      display: inline-block;
      transition: transform 0.1s ease;
    }
  }
</style>
