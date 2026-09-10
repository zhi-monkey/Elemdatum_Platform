<template>
  <path
    v-if="show"
    class="interactive"
    :d="line(nextData)"
    :stroke="bgColor"
    :fill="bgColor"
    :stroke-width="2"
    v-bind="attrs"
    :style="props.style"
    @mousedown="props.onMousedown"
    @mousemove="props.onMousemove"
    @mouseup="props.onMouseup"
  />
</template>

<script setup>
  import { isFunction, isNil } from 'lodash-es';
  import { line as d3Line } from 'd3/dist/d3.js';
  import { defaultColor } from '/@/views/mineai/data/dataset-details2/util';
  import { computed, ref } from 'vue';
  import { useAttrs } from 'vue';

  const props = defineProps({
    points: Array,
    curve: Function,
    fill: String,
    offset: Function,
    style: Object,
    onMousedown: Function,
    onMousemove: Function,
    onMouseup: Function,
  });

  const show = ref(true);

  if (isNil(props.points)) {
    show.value = false;
  }
  const attrs = useAttrs();

  const line = d3Line()
    .x((d) => d.x)
    .y((d) => d.y);

  if (props.curve) {
    line.curve(props.curve);
  }

  const bgColor = computed(() => {
    return props.fill || defaultColor;
  });

  // const restProps = {
  //   attrs: context.data.attrs,
  //   on: context.data.on,
  // };

  const nextData = computed(() => {
    return isFunction(props.offset) ? props.points.map(props.offset) : props.points;
  });
</script>

<style scoped lang="less"></style>
