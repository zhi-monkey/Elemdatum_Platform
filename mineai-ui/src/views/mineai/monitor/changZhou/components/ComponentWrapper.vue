<!-- ComponentWrapper.vue -->
<template>
  <div class="component-wrapper">
    <div class="component-header">
      <div class="header-icon"></div>
      <div class="header-title">{{ title }}</div>
    </div>
    <div class="component-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  defineProps({
    title: {
      type: String,
      required: true,
    },
  });
</script>

<style scoped lang="less">
  .component-wrapper {
    position: relative;
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 10px;
    margin-bottom: 0;
    background: rgba(26, 30, 48, 0.3);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(64, 158, 255, 0.4);
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
      transform: translateY(-2px);
    }
  }

  .component-header {
    padding: 14px 20px;
    background: rgba(12, 23, 52, 0.2);
    backdrop-filter: blur(5px);
    -webkit-backdrop-filter: blur(5px);
    border-bottom: 1px solid rgba(64, 158, 255, 0.15);
    display: flex;
    align-items: center;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: linear-gradient(180deg, #409eff 0%, #1890ff 100%);
      box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
    }
  }

  .header-icon {
    width: 10px;
    height: 10px;
    background: linear-gradient(135deg, #409eff 0%, #1890ff 100%);
    border-radius: 50%;
    margin-right: 12px;
    box-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
    animation: iconPulse 3s ease-in-out infinite;
  }

  @keyframes iconPulse {
    0%,
    100% {
      box-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
    }
    50% {
      box-shadow: 0 0 12px rgba(64, 158, 255, 0.9);
    }
  }

  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: #e6f7ff;
    letter-spacing: 0.5px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .component-content {
    padding: 0;
    min-height: 0;
  }
</style>
