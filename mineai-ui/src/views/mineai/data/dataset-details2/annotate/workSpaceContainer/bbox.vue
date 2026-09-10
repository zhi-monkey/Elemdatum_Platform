<template>
  <g :class="classnames" v-if="isShow">
    <rect
      :fill="fill"
      :stroke="annotate.data.color || defaultColor"
      :stroke-width="strokeWidth"
      :x="pos.x"
      :y="pos.y"
      :width="pos.width"
      :height="pos.height"
      :transform="transform"
      :style="style"
      @mousemove="dragMove"
      @mouseup="dragEnd"
      @mousedown="dragStart"
      @contextmenu="handleContextMenu"
    />
  </g>
</template>

<script setup>
  import { isNil } from 'lodash-es';
  import { defaultColor, defaultFill } from '/@/views/mineai/data/dataset-details2/util';
  import { chroma } from '/@/utils/dubhe';
  import cx from 'classnames';
  import { computed } from 'vue';

  const props = defineProps({
    annotate: Object,
    brush: Object,
    scale: {
      type: Number,
      default: 1,
    },
    pos: {
      type: Object,
      default: () => ({}),
    },
    dragStart: Function,
    dragMove: Function,
    dragEnd: Function,
    currentAnnotationId: String,
    transformer: Object,
    imgRef: HTMLImageElement,
    style: Object,
    onContextmenu: Function, // 添加右键事件回调
  });

  const strokeWidth = computed(() => {
    const baseWidth = 3; // 基础线宽
    const minWidth = 0.5; // 最小线宽 (图片放大时)
    const maxWidth = 4; // 最大线宽 (图片缩小时)

    let width = baseWidth / (props.scale || 1);
    return Math.min(maxWidth, Math.max(minWidth, width));
  });

  const isShow = computed(() => !isNil(props.annotate.data.bbox));
  const isActive = computed(() => props.currentAnnotationId === props.annotate.id);
  const colorAlpha = computed(() => (isActive.value ? 0.4 : 0.1));
  const fill = computed(() =>
    chroma(props.annotate.data.color || defaultFill).alpha(colorAlpha.value),
  );

  // 匹配当前标注
  const transform = computed(() =>
    props.annotate.id === props.transformer.id
      ? `translate(${props.transformer.dx}, ${props.transformer.dy})`
      : null,
  );

  const classnames = cx('bbox-group', {
    active: isActive,
  });

  // 添加右键事件处理函数
  const handleContextMenu = (event) => {
    event.preventDefault(); // 阻止默认右键菜单
    event.stopPropagation(); // 阻止事件冒泡

    // 调用父组件传入的右键处理函数
    if (typeof props.onContextmenu === 'function') {
      props.onContextmenu(event);
    }
  };
</script>

<style scoped lang="less"></style>
