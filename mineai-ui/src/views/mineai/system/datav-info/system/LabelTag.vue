<template>
  <div class="label-tag">
    <template v-if="state.mergedConfig">
      <div v-for="(label, i) in state.mergedConfig.data" :key="label" class="label-item">
        {{ label }}
        <div
          :style="`background-color: ${
            state.mergedConfig.colors[i % state.mergedConfig.colors.length]
          };`"
        ></div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, watch } from 'vue';

  const props = defineProps({
    config: {
      type: Object,
      default: () => ({}),
    },
  });

  const state = reactive({
    defaultConfig: {
      /**
       * @description Label data
       * @type {Array<String>}
       * @default data = []
       * @example data = ['label1', 'label2']
       */
      data: [],
      /**
       * @description Label color (Hex|Rgb|Rgba|color keywords)
       * @type {Array<String>}
       * @default colors = ['#00baff', '#3de7c9', '#fff', '#ffc530', '#469f4b']
       * @example colors = ['#666', 'rgb(0, 0, 0)', 'rgba(0, 0, 0, 1)', 'red']
       */
      colors: ['#00baff', '#3de7c9', '#fff', '#ffc530', '#469f4b'],
    },

    mergedConfig: null,
  });

  watch(
    () => props.config,
    () => {
      mergeConfig();
    },
  );

  function mergeConfig() {
    state.mergedConfig = deepMerge(deepClone(state.defaultConfig, true), props.config || {});
  }

  function deepMerge(target: any, merged: any) {
    for (const key in merged) {
      if (target[key] && typeof target[key] === 'object') {
        deepMerge(target[key], merged[key]);

        continue;
      }

      if (typeof merged[key] === 'object') {
        target[key] = deepClone(merged[key], true);

        continue;
      }

      target[key] = merged[key];
    }

    return target;
  }

  /**
   * @description Clone an object or array
   * @param {Object|Array} object Cloned object
   * @param {Boolean} recursion   Whether to use recursive cloning
   * @return {Object|Array} Clone object
   */
  function deepClone(object: any, recursion: boolean) {
    if (!object) return object;
    const { parse, stringify } = JSON;
    if (!recursion) return parse(stringify(object));
    const clonedObj: Record<string, any> = object instanceof Array ? [] : {};

    if (object && typeof object === 'object') {
      for (const key in object) {
        if (Object.prototype.hasOwnProperty.call(object, key)) {
          if (object[key] && typeof object[key] === 'object')
            clonedObj[key] = deepClone(object[key], true);
          else clonedObj[key] = object[key];
        }
      }
    }

    return clonedObj;
  }

  onMounted(() => {
    mergeConfig();
  });
</script>

<style lang="less">
  .label-tag {
    display: flex;
    justify-content: center;
    align-items: center;

    .label-item {
      margin: 5px;
      font-size: 15px;
      display: flex;
      align-items: center;

      div {
        width: 12px;
        height: 12px;
        margin-left: 5px;
      }
    }
  }
</style>
