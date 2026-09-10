<!-- index.vue -->
<template>
  <div class="container">
    <!-- 视频背景 -->
    <div class="video-background">
      <video ref="backgroundVideo" autoplay muted loop playsinline class="background-video">
        <source src="/zip/bg.mp4" type="video/mp4" />
      </video>
      <div class="video-overlay"></div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 顶部标题栏 -->
      <div class="top-header">
        <div class="header-center">
          <h1 class="header-title">天地自动化AI训练平台</h1>
        </div>
      </div>

      <!-- 主体布局 - 左中右三栏结构 -->
      <div class="main-layout">
        <!-- 左侧区域 -->
        <div class="left-panel">
          <!-- 左上 - 用户中心 -->
          <div class="panel-item panel-item-half">
            <ComponentWrapper title="用户中心">
              <UserCenter />
            </ComponentWrapper>
          </div>

          <!-- 左中 - 数据信息统计 -->
          <div class="panel-item panel-item-half">
            <ComponentWrapper title="数据信息统计">
              <DataStatistics />
            </ComponentWrapper>
          </div>

          <!-- 左下 - 公共数据集 -->
          <div class="panel-item panel-item-half">
            <ComponentWrapper title="公共数据集">
              <DataSetList />
            </ComponentWrapper>
          </div>
        </div>

        <!-- 中间区域 -->
        <div class="center-panel">
          <!-- 计算集群监控 - 占中上区域较大空间 -->
          <div class="panel-item panel-item-large">
            <ComponentWrapper title="计算集群监控">
              <ClusterCenter />
            </ComponentWrapper>
          </div>

          <!-- 算法应用商城 - 占中下区域较小空间 -->
          <div class="panel-item panel-item-small">
            <ComponentWrapper title="算法应用商城">
              <AlgorithmStore />
            </ComponentWrapper>
          </div>
        </div>

        <!-- 右侧区域 -->
        <div class="right-panel">
          <!-- 右上 - 训练任务 -->
          <div class="panel-item panel-item-two-thirds">
            <ComponentWrapper title="训练任务">
              <TrainingTasks />
            </ComponentWrapper>
          </div>

          <!-- 右下 - 系统信息 -->
          <div class="panel-item panel-item-one-third">
            <ComponentWrapper title="系统信息">
              <SystemMessage />
            </ComponentWrapper>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue';
  import ClusterCenter from './components/ClusterCenter.vue';
  import AlgorithmStore from './components/AlgorithmStore.vue';
  import TrainingTasks from './components/TrainingTasks.vue';
  import DataStatistics from './components/DataStatistics.vue';
  import UserCenter from './components/UserCenter.vue';
  import DataSetList from './components/DataSetList.vue';
  import SystemMessage from './components/SystemMessage.vue';
  import ComponentWrapper from './components/ComponentWrapper.vue';

  const backgroundVideo = ref<HTMLVideoElement | null>(null);
  let observer: any = null;

  onMounted(() => {
    // 确保视频播放
    if (backgroundVideo.value) {
      backgroundVideo.value.play().catch(console.error);
    }

    // 强制移除所有可能的宽度限制 - 更彻底的方法
    const forceFullWidth = () => {
      // 查找所有可能限制宽度的父级容器
      const selectors = [
        '.vben-layout-content',
        '.vben-layout-content.fixed',
        '.vben-page-wrapper',
        '.vben-page-wrapper-content',
        '.app-container',
        '.wrapper',
        '.wrapper.fixed',
        '.vben-layout',
        '.vben-layout-main',
      ];

      selectors.forEach((selector) => {
        const elements = document.querySelectorAll(selector);
        elements.forEach((el: any) => {
          if (el) {
            el.style.setProperty('width', '100%', 'important');
            el.style.setProperty('max-width', 'none', 'important');
            el.style.setProperty('min-width', 'auto', 'important');
            el.style.setProperty('margin', '0', 'important');
            el.style.setProperty('margin-left', '0', 'important');
            el.style.setProperty('margin-right', '0', 'important');
            el.style.setProperty('padding', '0', 'important');
            el.style.setProperty('padding-left', '0', 'important');
            el.style.setProperty('padding-right', '0', 'important');
          }
        });
      });

      // 强制container本身和其父级元素撑满
      const container = document.querySelector('.container');
      if (container) {
        // 设置container自身
        (container as any).style.setProperty('width', '100%', 'important');
        (container as any).style.setProperty('max-width', 'none', 'important');
        (container as any).style.setProperty('min-width', '100%', 'important');

        // 设置所有父级元素
        let parent = container.parentElement;
        while (parent && parent !== document.body) {
          parent.style.setProperty('width', '100%', 'important');
          parent.style.setProperty('max-width', 'none', 'important');
          parent.style.setProperty('margin', '0', 'important');
          parent.style.setProperty('padding', '0', 'important');
          parent = parent.parentElement;
        }
      }

      // 强制三栏布局撑满
      const mainLayout = document.querySelector('.main-layout');
      if (mainLayout) {
        (mainLayout as any).style.setProperty('width', '100%', 'important');
        (mainLayout as any).style.setProperty('max-width', 'none', 'important');
      }

      // 输出调试信息
      const containerWidth = container?.clientWidth || 0;
      const mainLayoutWidth = mainLayout?.clientWidth || 0;
      const windowWidth = window.innerWidth;
      console.log(
        `布局宽度已强制设置 - Container: ${containerWidth}px, Main-layout: ${mainLayoutWidth}px, 窗口: ${windowWidth}px`,
      );
    };

    // 立即执行
    forceFullWidth();

    // 延迟执行确保DOM完全加载
    setTimeout(forceFullWidth, 0);
    setTimeout(forceFullWidth, 100);
    setTimeout(forceFullWidth, 300);
    setTimeout(forceFullWidth, 500);
    setTimeout(forceFullWidth, 1000);
  });

  onUnmounted(() => {
    if (observer) {
      observer.disconnect();
    }
  });
</script>

<style lang="less">
  // 覆盖父级容器的宽度限制 - 非scoped样式
  // 使用最高优先级强制覆盖所有可能的宽度限制

  // 主要布局容器 - 移除1200px的固定宽度限制
  .vben-layout-content,
  .vben-layout-content.fixed,
  div.vben-layout-content.fixed,
  body .vben-layout-content.fixed {
    width: 100% !important;
    max-width: none !important;
    min-width: auto !important;
    margin: 0 !important;
    margin-left: 0 !important;
    margin-right: 0 !important;
    padding: 0 !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    flex: 1 1 auto !important;
  }

  // 页面包装器
  .vben-page-wrapper,
  .vben-page-wrapper-content,
  div.vben-page-wrapper,
  div.vben-page-wrapper-content {
    width: 100% !important;
    max-width: none !important;
    min-width: auto !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  // App容器 - 移除1080px的限制
  .app-container,
  div.app-container {
    width: 100% !important;
    max-width: none !important;
    min-width: auto !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  // Wrapper容器
  .wrapper,
  .wrapper.fixed,
  div.wrapper,
  div.wrapper.fixed,
  body .wrapper.fixed {
    width: 100% !important;
    max-width: none !important;
    min-width: auto !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  // 主布局容器
  .vben-layout-main,
  div.vben-layout-main {
    width: 100% !important;
    max-width: none !important;
    flex: 1 1 auto !important;
  }

  // 通用覆盖 - 使用属性选择器
  [class*='layout-content'] {
    max-width: none !important;
    width: 100% !important;
  }

  [class*='page-wrapper'] {
    max-width: none !important;
    width: 100% !important;
  }

  // 确保所有可能的父级容器都没有宽度限制
  .vben-layout,
  div.vben-layout {
    max-width: none !important;
  }
</style>

<style scoped lang="less">
  // 全局变量定义
  @bg-color: #0a1628;
  @header-bg: #0c1734;
  @card-bg: #1a1e30;
  @border-color: #0d2b5a;
  @text-color: #ffffff;
  @secondary-color: #bfbfbf;
  @primary-color: #409eff;
  @success-color: #67c23a;

  .container {
    width: 100% !important;
    max-width: none !important;
    min-width: 100% !important;
    height: 100vh;
    margin: 0 !important;
    padding: 0;
    box-sizing: border-box;
    color: @text-color;
    overflow: hidden;
    position: relative;
    display: flex;
    flex-direction: column;
  }

  // 视频背景样式
  .video-background {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 1;
    overflow: hidden;

    .background-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
    }

    .video-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.1);
      z-index: 1;
    }
  }

  // 内容包装器
  .content-wrapper {
    position: relative;
    z-index: 5;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    background: transparent;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }

  // 顶部标题栏样式
  .top-header {
    height: 60px;
    min-height: 60px;
    max-height: 60px;
    flex-shrink: 0;
    background: rgba(12, 23, 52, 0.3);
    backdrop-filter: blur(5px);
    -webkit-backdrop-filter: blur(5px);
    border-bottom: 2px solid rgba(13, 43, 90, 0.3);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    position: relative;
    box-sizing: border-box;
    z-index: 10;

    &::after {
      content: '';
      position: absolute;
      bottom: -2px;
      left: 0;
      right: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent 0%, @primary-color 50%, transparent 100%);
    }
  }

  .header-center {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: @text-color;
    letter-spacing: 1px;
    background: linear-gradient(90deg, #ffffff 0%, #a0cfff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  // 主体布局样式
  .main-layout {
    flex: 1;
    display: flex;
    gap: 12px;
    padding: 12px;
    overflow: hidden;
    min-height: 0;
    box-sizing: border-box;
    width: 100% !important;
    max-width: none !important;
    position: relative;
    z-index: 8;

    > div {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 0; // 防止 flex 子元素溢出
      min-height: 0;
      height: 100%;
      box-sizing: border-box;
    }

    .left-panel {
      flex: 1;
      min-width: 0;
      max-width: none;
    }

    .center-panel {
      flex: 1;
      min-width: 0;
      max-width: none;
    }

    .right-panel {
      flex: 1;
      min-width: 0;
      max-width: none;
    }
  }

  // 面板项样式
  .panel-item {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    min-height: 0;
    flex-shrink: 0;
    box-sizing: border-box;
    position: relative;
    z-index: 12;

    // 使用深度选择器确保 ComponentWrapper 填满容器
    :deep(.component-wrapper) {
      height: 100%;
      display: flex;
      flex-direction: column;
      margin-bottom: 0;
      box-sizing: border-box;
      background: rgba(26, 30, 48, 0.3);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border: 1px solid rgba(45, 55, 72, 0.5);

      .component-content {
        flex: 1;
        overflow: hidden;
        min-height: 0;
        box-sizing: border-box;
      }
    }
  }

  // 不同高度的面板项 - 重新调整布局
  .panel-item-half {
    flex: 1; // 50%
    min-height: 0;
  }

  .panel-item-two-thirds {
    flex: 2; // 占2/3高度
    min-height: 0;
    :deep(.component-wrapper) {
      .component-content {
        overflow: auto !important; // 只对这个组件开启滚动
      }
    }
  }

  .panel-item-one-third {
    flex: 1; // 占1/3高度
    min-height: 0;
  }

  .panel-item-large {
    flex: 2; // 占较大空间（约65%）
    min-height: 0;
    :deep(.component-wrapper) {
      .component-content {
        overflow: auto !important;
      }
    }
  }

  .panel-item-small {
    flex: 1; // 占较小空间（约35%）
    min-height: 0;
    :deep(.component-wrapper) {
      .component-content {
        overflow: auto !important;
      }
    }
  }

  .panel-item-full {
    flex: 1; // 100% - 占满整个中间区域
    min-height: 0;
    height: 100%;

    // 确保计算集群监控组件占满整个高度
    :deep(.component-wrapper) {
      height: 100%;
      min-height: calc(100vh - 80px); // 减去标题栏高度

      .component-content {
        height: 100%;
        min-height: calc(100vh - 140px); // 减去标题栏和组件标题高度
        overflow: hidden;
      }
    }
  }

  .placeholder {
    padding: 20px;
    text-align: center;
    color: @secondary-color;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1;
    min-height: 0;
    height: 100%;
    font-size: 14px;
    box-sizing: border-box;
  }

  // 响应式设计 - 大屏幕优化
  @media screen and (min-width: 1920px) {
    .main-layout {
      gap: 16px;
      padding: 16px;

      > div {
        gap: 16px;
      }
    }
  }

  @media screen and (max-width: 1600px) {
    .header-title {
      font-size: 20px;
    }

    .main-layout {
      gap: 10px;
      padding: 10px;

      > div {
        gap: 10px;
      }
    }
  }

  @media screen and (max-width: 1200px) {
    .main-layout {
      gap: 8px;
      padding: 8px;

      > div {
        gap: 8px;
      }
    }
  }

  // 滚动条样式优化
  :deep(*::-webkit-scrollbar) {
    width: 6px;
    height: 6px;
  }

  :deep(*::-webkit-scrollbar-track) {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 3px;
  }

  :deep(*::-webkit-scrollbar-thumb) {
    background: rgba(64, 158, 255, 0.3);
    border-radius: 3px;
    transition: background 0.3s;

    &:hover {
      background: rgba(64, 158, 255, 0.5);
    }
  }

  // 强制表格字体颜色为白色
  :deep(.ant-table) {
    .ant-table-thead > tr > th {
      color: #ffffff !important;
    }

    .ant-table-tbody > tr > td {
      color: #ffffff !important;
    }

    .ant-table-tbody > tr:hover > td {
      color: #ffffff !important;
    }
  }

  :deep(.el-table) {
    .el-table__header-wrapper th {
      color: #ffffff !important;
    }

    .el-table__body-wrapper td {
      color: #ffffff !important;
    }

    .el-table__row td {
      color: #ffffff !important;
    }
  }

  :deep(table) {
    th,
    td {
      color: #ffffff !important;
    }
  }

  // 保留训练成功、失败等特殊状态的颜色
  :deep(.success-status),
  :deep(.status-success) {
    color: #67c23a !important;
  }

  :deep(.error-status),
  :deep(.status-error),
  :deep(.status-fail),
  :deep(.status-failed) {
    color: #f56c6c !important;
  }

  :deep(.warning-status),
  :deep(.status-warning) {
    color: #e6a23c !important;
  }

  // 强制滚动列表的所有文字为白色
  :deep(.scroll-list) {
    .data-item {
      color: rgba(255, 255, 255, 0.85) !important;

      .col {
        color: rgba(255, 255, 255, 0.85) !important;
      }

      * {
        color: inherit !important;
      }
    }

    .header {
      color: rgba(255, 255, 255, 0.8) !important;

      .col {
        color: rgba(255, 255, 255, 0.8) !important;
      }
    }
  }
</style>
