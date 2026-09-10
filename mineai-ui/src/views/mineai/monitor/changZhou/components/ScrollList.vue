<template>
  <div class="scroll-list" @mouseenter="pauseAutoScroll" @mouseleave="resumeAutoScroll">
    <!-- 表头 -->
    <div class="header" :style="{ gridTemplateColumns: gridTemplateColumns }">
      <div
        v-for="(column, index) in columns"
        :key="index"
        class="col"
        :class="column.className"
        :style="{
          textAlign: column.align || 'center',
          fontWeight: column.fontWeight || 'bold',
        }"
      >
        {{ column.title }}
      </div>
    </div>

    <!-- 滚动内容区域 -->
    <div
      class="scroll-container-wrapper"
      ref="wrapperRef"
      :style="{ height: containerHeight + 'px' }"
    >
      <div
        class="scroll-content"
        ref="scrollContainer"
        :style="{ transform: `translateY(-${currentScrollPosition}px)` }"
      >
        <div class="data-list">
          <div
            v-for="(item, index) in displayData"
            :key="`${index}-${getItemKey(item)}`"
            class="data-item"
            :class="getRowClass(item, index % data.length)"
            :style="{ gridTemplateColumns: gridTemplateColumns, height: itemHeight + 'px' }"
          >
            <div
              v-for="(column, colIndex) in columns"
              :key="colIndex"
              class="col"
              :class="column.className"
              :style="{
                textAlign: column.align || 'center',
                fontWeight: column.fontWeight || 'normal',
              }"
            >
              <slot
                :name="`cell-${column.key}`"
                :item="item"
                :value="getValueByKey(item, column.key)"
                :index="index % data.length"
                :column="column"
              >
                {{ formatValue(getValueByKey(item, column.key), column) }}
              </slot>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态显示 -->
    <div v-if="!data.length" class="empty-state"> 暂无数据 </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onBeforeUnmount, computed, watch, nextTick } from 'vue';

  interface ColumnConfig {
    key: string;
    title: string;
    className?: string;
    align?: 'left' | 'center' | 'right';
    fontWeight?: 'normal' | 'bold';
    formatter?: (value: any, item: any) => string;
    width?: string;
  }

  interface RowConfig {
    className?: string | ((item: any, index: number) => string);
    hoverClass?: string;
  }

  interface Props {
    data: any[];
    columns: ColumnConfig[];
    rowConfig?: RowConfig;
    scrollSpeed?: number;
    scrollInterval?: number;
    autoScroll?: boolean;
    itemHeight?: number;
    containerHeight?: number;
    maxReplications?: number; // 新增：最大复制份数限制
  }

  const props = withDefaults(defineProps<Props>(), {
    scrollSpeed: 1,
    scrollInterval: 16,
    autoScroll: true,
    itemHeight: 56,
    rowConfig: () => ({}),
    containerHeight: 400,
    maxReplications: 10, // 默认最大复制20份，避免性能问题
  });

  const gridTemplateColumns = computed(() => {
    return props.columns.map((col) => col.width || '1fr').join(' ');
  });

  const wrapperRef = ref<HTMLDivElement | null>(null);
  const currentScrollPosition = ref(0);
  const isAutoScrollPaused = ref(false);
  let animationFrameId: number | null = null;
  let lastTimestamp = 0;

  // 显示数据：复制足够份数直到可以填满容器高度
  const displayData = computed(() => {
    if (!props.data.length) return [];

    const singleListHeight = props.data.length * props.itemHeight;

    // 如果单组数据高度已经大于容器高度，复制一份即可（无缝滚动需要）
    if (singleListHeight >= props.containerHeight) {
      return [...props.data, ...props.data];
    }

    // 计算需要复制的份数：容器高度的1.5倍 ÷ 单组数据高度，向上取整
    const minRequiredHeight = props.containerHeight * 1.5;
    const requiredCopies = Math.ceil(minRequiredHeight / singleListHeight);

    // 应用最大复制份数限制
    const actualCopies = Math.min(requiredCopies, props.maxReplications);

    // 生成足够份数的数据
    const result = [];
    for (let i = 0; i < actualCopies; i++) {
      result.push(...props.data);
    }

    return result;
  });

  // 计算内容总高度
  const contentHeight = computed(() => {
    return displayData.value.length * props.itemHeight;
  });

  // 检查是否可以滚动（内容高度需要大于容器高度）
  const canScroll = computed(() => {
    return props.data.length > 0 && contentHeight.value > props.containerHeight;
  });

  const getItemKey = (item: any) => {
    return item.id || JSON.stringify(item);
  };

  const getValueByKey = (item: any, key: string) => {
    if (key.includes('.')) {
      return key.split('.').reduce((obj, k) => obj?.[k], item);
    }
    return item[key];
  };

  const formatValue = (value: any, column: ColumnConfig) => {
    if (column.formatter) {
      return column.formatter(value, value);
    }
    return value || '';
  };

  const getRowClass = (item: any, index: number) => {
    if (typeof props.rowConfig.className === 'function') {
      return props.rowConfig.className(item, index);
    }
    return props.rowConfig.className || '';
  };

  // 滚动动画函数
  const scrollAnimation = (timestamp: number) => {
    if (!lastTimestamp) lastTimestamp = timestamp;

    const elapsed = timestamp - lastTimestamp;

    if (elapsed >= props.scrollInterval && !isAutoScrollPaused.value && canScroll.value) {
      currentScrollPosition.value += props.scrollSpeed;

      // 无缝滚动逻辑：当滚动完一组数据后回到起点
      const singleListHeight = props.data.length * props.itemHeight;
      if (currentScrollPosition.value >= singleListHeight) {
        currentScrollPosition.value = 0;
      }

      lastTimestamp = timestamp;
    }

    animationFrameId = requestAnimationFrame(scrollAnimation);
  };

  const startAutoScroll = () => {
    // 只有开启了自动滚动、有数据且可以滚动时才启动
    if (!props.autoScroll || !props.data.length || !canScroll.value) return;

    stopAutoScroll(); // 先停止现有的滚动

    lastTimestamp = 0;
    animationFrameId = requestAnimationFrame(scrollAnimation);
  };

  const stopAutoScroll = () => {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }
  };

  const pauseAutoScroll = () => {
    isAutoScrollPaused.value = true;
  };

  const resumeAutoScroll = () => {
    isAutoScrollPaused.value = false;
    lastTimestamp = 0;
  };

  const init = async () => {
    await nextTick();

    // 确保数据存在且可以滚动
    if (canScroll.value && wrapperRef.value) {
      currentScrollPosition.value = 0;
      startAutoScroll();
    }
  };

  onMounted(() => {
    setTimeout(() => {
      init();
    }, 100);
  });

  onBeforeUnmount(() => {
    stopAutoScroll();
  });

  // 监听数据变化
  watch(
    () => props.data,
    (newData) => {
      if (newData.length > 0) {
        currentScrollPosition.value = 0;
        // 短暂延迟确保DOM更新完成
        setTimeout(() => {
          startAutoScroll();
        }, 50);
      }
    },
    { deep: true },
  );

  // 监听自动滚动开关
  watch(
    () => props.autoScroll,
    (newVal) => {
      if (newVal) {
        startAutoScroll();
      } else {
        stopAutoScroll();
      }
    },
  );

  // 监听数据量变化
  watch(
    () => props.data.length,
    () => {
      currentScrollPosition.value = 0;
      setTimeout(() => {
        startAutoScroll();
      }, 50);
    },
  );

  // 监听容器高度变化
  watch(
    () => props.containerHeight,
    () => {
      currentScrollPosition.value = 0;
      setTimeout(() => {
        startAutoScroll();
      }, 50);
    },
  );
</script>

<style scoped lang="less">
  @text-color: #ffffff;
  @secondary-color: rgba(255, 255, 255, 0.8);
  @primary-color: #409eff;
  @glass-bg: rgba(255, 255, 255, 0.05);
  @glass-border: rgba(255, 255, 255, 0.15);
  @glass-shadow: rgba(0, 0, 0, 0.05);

  .scroll-list {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    background: rgba(26, 30, 48, 0.2);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    border-radius: 0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    margin-bottom: 0px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    position: relative;

    .header {
      display: grid;
      align-items: center;
      justify-items: start;
      padding: 10px 12px;
      background: rgba(12, 23, 52, 0.15);
      backdrop-filter: blur(5px);
      -webkit-backdrop-filter: blur(5px);
      color: @secondary-color;
      font-size: 14px;
      border-bottom: 1px solid @glass-border;
      flex-shrink: 0;
      border-radius: 0;

      .col {
        word-break: break-all;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding: 0 4px;
      }
    }

    .scroll-container-wrapper {
      flex: 1;
      min-height: 0;
      overflow: hidden;
      position: relative;
      transition: height 0.3s ease;
    }

    .scroll-content {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      will-change: transform;
    }

    .data-list {
      display: flex;
      flex-direction: column;
    }

    .data-item {
      display: grid;
      align-items: center;
      justify-items: start;
      padding: 10px 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      color: rgba(255, 255, 255, 0.85) !important;
      font-size: 13px;
      transition: background-color 0.3s ease;
      background: rgba(255, 255, 255, 0.02);
      box-sizing: border-box;

      &:hover {
        background: rgba(255, 255, 255, 0.05);
        backdrop-filter: blur(3px);
        -webkit-backdrop-filter: blur(3px);
      }

      .col {
        word-break: break-word;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: normal;
        padding: 0 4px;
        line-height: 1.4;
        color: rgba(255, 255, 255, 0.85) !important;
      }
    }

    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100px;
      color: @secondary-color;
      font-size: 14px;
      background: rgba(255, 255, 255, 0.02);
    }
  }

  @media (max-width: 768px) {
    .scroll-list {
      .header,
      .data-item {
        grid-template-columns: 1fr;
        grid-template-rows: auto;
        grid-row-gap: 8px;
      }
    }
  }
</style>
