<template>
  <div
    class="bubble-item"
    :class="{ 'is-hovered': isHovered }"
    :style="{
      left: position.left,
      top: position.top,
      width: bubbleSize + 'px',
      height: bubbleSize + 'px',
      '--animation-delay': animationDelay + 's',
      '--animation-duration': animationDuration + 's',
      '--offset-x': animationOffset.x + 'px',
      '--offset-y': animationOffset.y + 'px',
    }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <img class="bubble-img" :class="bubbleColorClass" :src="bubbleImageSrc" alt="" />
    <div class="bubble-text" :style="{ fontSize: fontSize }">{{ text }}</div>

    <!-- Tooltip -->
    <div v-if="isHovered && applications.length > 0" class="bubble-tooltip">
      <div class="tooltip-title">{{ text }}</div>
      <div class="tooltip-list">
        <div v-for="(app, idx) in applications" :key="idx" class="tooltip-item">
          <div v-if="app.applicationTaskName" class="tooltip-row">
            <span class="tooltip-label">算法应用：</span>
            <span class="tooltip-value">{{ app.applicationTaskName }}</span>
          </div>
          <div v-if="app.device" class="tooltip-row">
            <span class="tooltip-label">支持设备：</span>
            <span class="tooltip-value">{{ app.device }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, withDefaults } from 'vue';

  // 三种颜色的bubble图片
  const BUBBLE_COLORS = ['red', 'yellow', 'blue'] as const;
  type BubbleColor = typeof BUBBLE_COLORS[number];

  interface Props {
    text: string;
    position: {
      left: string;
      top: string;
    };
    animationDelay?: number;
    animationDuration?: number;
    animationOffset?: {
      x: number;
      y: number;
    };
    applications?: Array<{
      applicationTaskName?: string;
      device?: string;
    }>;
    bubbleColor?: BubbleColor; // 可选指定颜色，不指定则随机
  }

  const props = withDefaults(defineProps<Props>(), {
    animationDelay: 0,
    animationDuration: 10,
    animationOffset: () => ({ x: 0, y: 0 }),
    applications: () => [],
  });

  const isHovered = ref(false);

  // 随机选择一个颜色（组件创建时确定，不会变化）
  const randomColor = BUBBLE_COLORS[Math.floor(Math.random() * BUBBLE_COLORS.length)];
  const selectedColor = computed(() => props.bubbleColor || randomColor);

  // 根据颜色返回对应的图片路径
  const bubbleImageSrc = computed(() => {
    return `/resource/overview/${selectedColor.value}_bubble.png`;
  });

  // 根据颜色返回对应的class
  const bubbleColorClass = computed(() => {
    return `bubble-${selectedColor.value}`;
  });

  const handleMouseEnter = () => {
    isHovered.value = true;
  };

  const handleMouseLeave = () => {
    isHovered.value = false;
  };

  // 根据文字长度计算泡泡大小
  const bubbleSize = computed(() => {
    const baseSize = 70; // 基础大小
    const charCount = props.text.length;
    // 每个字符大约需要12-15px宽度，加上内边距
    const minSize = Math.max(baseSize, charCount * 14 + 30);
    // 最大不超过140px
    return Math.min(minSize, 140);
  });

  // 根据文字长度计算字体大小
  const fontSize = computed(() => {
    const charCount = props.text.length;
    if (charCount <= 2) return '16px';
    if (charCount <= 4) return '14px';
    return '12px';
  });
</script>

<style scoped lang="less">
  .bubble-item {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: auto;
    z-index: 5;
    cursor: pointer;
    animation: bubbleFloat var(--animation-duration, 10s) ease-in-out infinite;
    animation-delay: var(--animation-delay, 0s);

    &.is-hovered {
      animation-play-state: paused;
      z-index: 100;
    }
  }

  @keyframes bubbleFloat {
    0% {
      transform: translate(0, 0);
    }
    20% {
      transform: translate(calc(var(--offset-x, 0px) * 0.6), calc(var(--offset-y, 0px) * -0.4));
    }
    40% {
      transform: translate(calc(var(--offset-x, 0px) * 0.8), calc(var(--offset-y, 0px) * 0.6));
    }
    60% {
      transform: translate(calc(var(--offset-x, 0px) * -0.5), calc(var(--offset-y, 0px) * 0.8));
    }
    80% {
      transform: translate(calc(var(--offset-x, 0px) * -0.7), calc(var(--offset-y, 0px) * -0.3));
    }
    100% {
      transform: translate(0, 0);
    }
  }

  .bubble-img {
    width: 100%;
    height: 100%;
    object-fit: contain;

    // 红色bubble - 添加发光效果
    &.bubble-red {
      filter: drop-shadow(0 0 8px rgba(255, 80, 80, 0.6));
    }

    // 黄色bubble - 添加发光效果
    &.bubble-yellow {
      filter: drop-shadow(0 0 8px rgba(255, 200, 50, 0.6));
    }

    // 蓝色bubble - 添加发光效果
    &.bubble-blue {
      filter: drop-shadow(0 0 8px rgba(0, 150, 255, 0.6));
    }
  }

  .bubble-text {
    position: absolute;
    color: #fff;
    font-weight: 500;
    text-align: center;
    white-space: nowrap;
    text-shadow: 0 0 4px rgba(0, 0, 0, 0.8);
    max-width: 80%;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1.2;
  }

  .bubble-tooltip {
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    margin-top: 8px;
    background: rgba(10, 14, 39, 0.95);
    border: 1px solid rgba(64, 158, 255, 0.6);
    border-radius: 6px;
    padding: 10px 14px;
    min-width: 180px;
    max-width: 280px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
    z-index: 101;

    &::before {
      content: '';
      position: absolute;
      top: -6px;
      left: 50%;
      transform: translateX(-50%);
      border-left: 6px solid transparent;
      border-right: 6px solid transparent;
      border-bottom: 6px solid rgba(64, 158, 255, 0.6);
    }
  }

  .tooltip-title {
    font-size: 14px;
    font-weight: 600;
    color: #00d4ff;
    margin-bottom: 8px;
    padding-bottom: 6px;
    border-bottom: 1px solid rgba(64, 158, 255, 0.3);
  }

  .tooltip-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .tooltip-item {
    display: flex;
    flex-direction: column;
    gap: 4px;

    &:not(:last-child) {
      padding-bottom: 6px;
      border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
    }
  }

  .tooltip-row {
    display: flex;
    align-items: flex-start;
    gap: 4px;
    font-size: 12px;
  }

  .tooltip-label {
    color: rgba(255, 255, 255, 0.6);
    flex-shrink: 0;
  }

  .tooltip-value {
    color: #fff;
    word-break: break-all;
  }
</style>
