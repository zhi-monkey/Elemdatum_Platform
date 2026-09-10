<!-- 大屏概览页面 -->
<template>
  <div class="overview-container" ref="containerRef">
    <!-- 背景层 -->
    <div class="bg-layer">
      <!-- 整体背景图 -->
      <img class="bg-overall" src="/resource/overview/bg_overall.png" alt="" />
      <!-- 底部硬盘 -->
      <!--      <img class="bg-disk" src="/resource/overview/bg_disk_bottom.png" alt="" />-->
      <!--      &lt;!&ndash; 佛光效果 - 背后的半圆环 &ndash;&gt;-->
      <!--      <img class="bg-bracket" src="/resource/overview/bg_bracket.png" alt="" />-->
      <!-- 底盘 -->
      <!-- <img class="bg-plate" src="/resource/overview/bg_plate.png" alt="" /> -->
      <!-- 大脑 -->
      <!-- <img class="bg-brain" src="/resource/overview/bg_brain.png" alt="" /> -->
      <!-- ai Icon -->
      <!-- <img class="ai" src="/resource/overview/ai_brain.svg" alt="" /> -->
      <!-- 立方体 -->
      <!-- <img class="bg-earth" src="/resource/overview/bg_earth.png" alt="" /> -->

      <!-- 中心球体容器 - 可调整位置 -->
      <div
        class="center-ball-container"
        :style="{
          '--ball-x': '10%',
          '--ball-y': '-3%',
        }"
      >
        <!-- 外层动效 -->
        <img class="ball-gif" src="/resource/overview/bg_ball_gif-ezgif.com-effects.gif" alt="" />
        <!-- 内层球体 -->
        <img class="ball-static" src="/resource/overview/bg_ball_new_icon.png" alt="" />
      </div>

      <!-- 闪烁效果 -->
      <img class="bg-twinkle" src="/resource/overview/bg_twinkle.gif" alt="" />
    </div>

    <!-- 内容层 -->
    <div class="content-layer">
      <!-- 顶部标题区域 -->
      <div class="header">
        <div class="title-left-section">
          <img class="title-left-line" src="/resource/overview/bg_title_left_line.png" alt="" />
          <div class="title-left-text">
            在线人数：{{ onlineUserCount }}
            <ExclamationCircleOutlined
              class="online-info-icon"
              @mouseenter="showOnlineList = true"
              @mouseleave="showOnlineList = false"
            />
            <div
              v-show="showOnlineList && onlineUsers.length"
              class="online-info-pop"
              @mouseenter="showOnlineList = true"
              @mouseleave="showOnlineList = false"
            >
              <div class="online-pop-title">在线用户</div>
              <div class="online-pop-list">
                <div class="online-pop-item" v-for="(u, idx) in onlineUsers" :key="u.id || idx">
                  <span class="name">{{ u.username || '-' }}</span>
                  <span class="duration">{{ formatDuration(u.duration || 0) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="title-center">
          <img src="/resource/overview/bg_title_center.png" alt="" />
          <span class="title-text">天地自动化AI训练平台</span>
        </div>
        <div class="title-right-section">
          <img class="title-right-line" src="/resource/overview/bg_title_right_line.png" alt="" />
          <div class="title-right-text">{{ currentDateTime }}</div>
        </div>
      </div>

      <!-- 泡泡装饰层 -->
      <div class="bubbles-layer">
        <!-- 泡泡 -->
        <OverviewBubble
          v-for="(bubble, index) in generatedBubbles"
          :key="'bubble-' + index"
          :text="bubble.text"
          :position="bubble.position"
          :animation-delay="bubble.animationDelay"
          :animation-duration="bubble.animationDuration"
          :animation-offset="bubble.animationOffset"
          :applications="bubble.applications"
        />
      </div>

      <!-- 主体布局区域 - 左中右三栏 -->
      <div class="main-layout">
        <!-- 左侧大括号 -->
        <!--        <img class="bg-brace-left" src="/resource/overview/bg_right_brace_left.png" alt="" />-->

        <!-- 左侧区域 -->
        <div class="left-panel">
          <!-- 用户中心 -->
          <div class="rect-box">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">角色分布</div>
            <div class="rect-content">
              <OverviewUserCenter />
            </div>
          </div>
          <!-- 数据信息统计 -->
          <div class="rect-box">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">数据统计</div>
            <div class="rect-content">
              <OverviewDataStatistics />
            </div>
          </div>
          <!-- 训练任务 -->
          <div class="rect-box training-up">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">训练任务</div>
            <div class="rect-content">
              <OverviewTrainingTasks />
            </div>
          </div>
        </div>

        <!-- 中间区域 -->
        <div class="center-panel">
          <div class="center-top"></div>
          <!-- GPU利用率 -->
          <div class="rect-box rect-box-horizontal gpu-up">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">显卡使用情况</div>
            <div class="rect-content" style="padding: 6% 4% 4% 4%">
              <OverviewGpuUsage />
            </div>
          </div>
        </div>

        <!-- 右侧区域 -->
        <div class="right-panel">
          <!-- 算法应用 -->
          <div class="rect-box">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">算法应用</div>
            <div class="rect-content">
              <OverviewAlgorithmStats />
            </div>
          </div>
          <!-- 资源使用（内存和磁盘并排） -->
          <div class="rect-box">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">资源使用</div>
            <div class="rect-content resource-content">
              <div class="resource-item">
                <OverviewMemoryUsage />
              </div>
              <div class="resource-item">
                <OverviewDiskUsage />
              </div>
            </div>
          </div>
          <!-- 系统信息 -->
          <div class="rect-box system-up">
            <img class="rect-bg" src="/resource/overview/rect_bg.png" alt="" />
            <div class="rect-title">系统信息</div>
            <div class="rect-content">
              <OverviewSystemInfo />
            </div>
          </div>
        </div>

        <!-- 右侧大括号 -->
        <!--        <img class="bg-brace-right" src="/resource/overview/bg_left_brace_right.png" alt="" />-->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import OverviewUserCenter from '/@/views/mineai/overview/components/OverviewUserCenter.vue';
  import OverviewDataStatistics from '/@/views/mineai/overview/components/OverviewDataStatistics.vue';
  import OverviewTrainingTasks from '/@/views/mineai/overview/components/OverviewTrainingTasks.vue';
  import OverviewMemoryUsage from '/@/views/mineai/overview/components/OverviewMemoryUsage.vue';
  import OverviewDiskUsage from '/@/views/mineai/overview/components/OverviewDiskUsage.vue';
  import OverviewSystemInfo from '/@/views/mineai/overview/components/OverviewSystemInfo.vue';
  import OverviewGpuUsage from '/@/views/mineai/overview/components/OverviewGpuUsage.vue';
  import OverviewBubble from '/@/views/mineai/overview/components/OverviewBubble.vue';
  import OverviewAlgorithmStats from '/@/views/mineai/overview/components/OverviewAlgorithmStats.vue';
  import { getAllScenesWithApplications } from '/@/views/mineai/application/message/sceneDetail/SceneManagementApi';
  import { getOnlineUsersCount } from './data';

  const containerRef = ref<HTMLElement | null>(null);

  // 动态样式表引用，用于在组件卸载时移除
  let dynamicStyleSheet: HTMLStyleElement | null = null;

  // 在线人数（从后端获取）
  const onlineUserCount = ref(0);
  const onlineUsers = ref<Array<{ id?: number; username?: string; duration?: number }>>([]);
  const showOnlineList = ref(false);

  // 当前日期时间
  const currentDateTime = ref('');

  // 格式化日期时间
  const formatDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    currentDateTime.value = `${year}年${month}月${day}日 ${hours}:${minutes}`;
  };

  // 可放置泡泡的区域配置（用百分比定义）
  interface BubbleZone {
    left: string; // 左边界
    top: string; // 上边界
    width: string; // 宽度
    height: string; // 高度
  }

  // 第一个矩形：覆盖用户中心右侧到内存利用率左侧，标题下方到底部硬盘上方
  // 左右各缩短5%：left从22%变成27%，width从56%变成46%
  // 底部与第二个矩形底部重合：第二个矩形底部是25% + 36% = 61%，所以height = 61% - 10% = 51%
  const allowedZone = ref<BubbleZone>({
    left: '27%', // 左侧面板右侧 + 5%
    top: '10%', // 标题下方
    width: '46%', // 56% - 10% = 46%
    height: '51%', // 61% - 10% = 51%（与第二个矩形底部重合）
  });

  // 第二个矩形：覆盖底盘和大脑位置（排除区域）
  // 宽度保留当前的三分之一居中：width从50%变成16.67%，居中left约41.67%
  // 左右各扩展约2%（约40px），top与蓝色区域平齐
  const excludedZone = ref<BubbleZone>({
    left: '39.67%', // 41.67% - 2%，向左扩展
    top: '10%', // 与蓝色区域顶部平齐
    width: '20.67%', // 16.67% + 4%，左右各扩展2%
    height: '51%', // 与蓝色区域底部平齐
  });

  // 泡泡文字内容（从后端加载，失败时用默认）
  const defaultBubbleTexts = [
    '主运',
    '探放水',
    '掘进',
    '回采',
    '通风',
    '排水',
    '供电',
    '运输',
    '安全监测',
    '皮带运输',
    '瓦斯监测',
    '设备管理',
    '环境监测',
    '人员定位',
  ];

  const bubbleTexts = ref<string[]>([]);

  // 检查点是否在排除区域内
  const isInExcludedZone = (
    x: number,
    y: number,
    containerWidth: number,
    containerHeight: number,
  ): boolean => {
    const excludedLeft = (parseFloat(excludedZone.value.left) / 100) * containerWidth;
    const excludedTop = (parseFloat(excludedZone.value.top) / 100) * containerHeight;
    const excludedWidth = (parseFloat(excludedZone.value.width) / 100) * containerWidth;
    const excludedHeight = (parseFloat(excludedZone.value.height) / 100) * containerHeight;
    const excludedRight = excludedLeft + excludedWidth;
    const excludedBottom = excludedTop + excludedHeight;

    return x >= excludedLeft && x <= excludedRight && y >= excludedTop && y <= excludedBottom;
  };

  // 检查两个泡泡是否重叠
  const checkBubbleOverlap = (
    x1: number,
    y1: number,
    size1: number,
    x2: number,
    y2: number,
    size2: number,
  ): boolean => {
    const center1X = x1 + size1 / 2;
    const center1Y = y1 + size1 / 2;
    const center2X = x2 + size2 / 2;
    const center2Y = y2 + size2 / 2;
    const distance = Math.sqrt(Math.pow(center2X - center1X, 2) + Math.pow(center2Y - center1Y, 2));
    const minDistance = (size1 + size2) / 2 + 10; // 最小间距10px
    return distance < minDistance;
  };

  // 检查泡泡位置是否有效（不在排除区域，不与其他泡泡重叠）
  const isValidBubblePosition = (
    x: number,
    y: number,
    size: number,
    containerWidth: number,
    containerHeight: number,
    existingBubbles: Array<{ x: number; y: number; size: number }>,
  ): boolean => {
    // 检查是否在允许区域内
    const allowedLeft = (parseFloat(allowedZone.value.left) / 100) * containerWidth;
    const allowedTop = (parseFloat(allowedZone.value.top) / 100) * containerHeight;
    const allowedWidth = (parseFloat(allowedZone.value.width) / 100) * containerWidth;
    const allowedHeight = (parseFloat(allowedZone.value.height) / 100) * containerHeight;

    if (
      x < allowedLeft ||
      y < allowedTop ||
      x + size > allowedLeft + allowedWidth ||
      y + size > allowedTop + allowedHeight
    ) {
      return false;
    }

    // 检查泡泡中心是否在排除区域内
    const bubbleCenterX = x + size / 2;
    const bubbleCenterY = y + size / 2;
    if (isInExcludedZone(bubbleCenterX, bubbleCenterY, containerWidth, containerHeight)) {
      return false;
    }

    // 检查是否与其他泡泡重叠
    for (const existing of existingBubbles) {
      if (checkBubbleOverlap(x, y, size, existing.x, existing.y, existing.size)) {
        return false;
      }
    }

    return true;
  };

  // 泡泡数据（包含场景名称和应用信息）
  interface BubbleData {
    text: string;
    applications: Array<{
      applicationTaskName?: string;
      device?: string;
    }>;
  }

  const bubbleDataList = ref<BubbleData[]>([]);

  async function loadSceneBubbles() {
    try {
      const res = await getAllScenesWithApplications();
      // 按你的 Msg 结构取数据，这里假设为 { code, data }
      const scenes = res?.data || res; // 视你实际包装而定，如果不确定可以 console.log 看一眼

      const dataList: BubbleData[] = (scenes || [])
        .filter((s: any) => s.sceneName ?? s.name)
        .map((s: any) => ({
          text: s.sceneName ?? s.name,
          applications: (s.applications || []).map((app: any) => ({
            applicationTaskName: app.applicationTaskName,
            device: app.device,
          })),
        }));

      // 如果没有返回有效数据，就用默认
      if (dataList.length > 0) {
        bubbleDataList.value = dataList;
        bubbleTexts.value = dataList.map((d) => d.text);
      } else {
        bubbleTexts.value = defaultBubbleTexts;
        bubbleDataList.value = defaultBubbleTexts.map((t) => ({ text: t, applications: [] }));
      }
    } catch (e) {
      // 出错时兜底为默认
      bubbleTexts.value = defaultBubbleTexts;
      bubbleDataList.value = defaultBubbleTexts.map((t) => ({ text: t, applications: [] }));
    }

    // 依赖 bubbleTexts 的泡泡布局重新生成
    if (containerRef.value) {
      generateBubbles();
    }
  }

  // 生成泡泡数据
  const generatedBubbles = ref<
    Array<{
      text: string;
      position: { left: string; top: string };
      animationDelay: number;
      animationDuration: number;
      animationOffset: { x: number; y: number };
      applications: Array<{ applicationTaskName?: string; device?: string }>;
    }>
  >([]);

  // 根据区域和文字生成泡泡（只在左右两个区域均匀分布）
  const generateBubbles = () => {
    if (!containerRef.value) return;

    const containerWidth = containerRef.value.offsetWidth;
    const containerHeight = containerRef.value.offsetHeight;

    // 解析允许区域和排除区域
    const allowedLeft = (parseFloat(allowedZone.value.left) / 100) * containerWidth;
    const allowedTop = (parseFloat(allowedZone.value.top) / 100) * containerHeight;
    const allowedWidth = (parseFloat(allowedZone.value.width) / 100) * containerWidth;
    const allowedHeight = (parseFloat(allowedZone.value.height) / 100) * containerHeight;
    const allowedRight = allowedLeft + allowedWidth;

    const excludedLeft = (parseFloat(excludedZone.value.left) / 100) * containerWidth;
    const excludedRight =
      excludedLeft + (parseFloat(excludedZone.value.width) / 100) * containerWidth;

    // 计算所有文字的泡泡大小，并关联 applications 数据
    const textSizes = bubbleTexts.value.map((text, index) => {
      const bubbleData = bubbleDataList.value[index];
      return {
        text,
        size: Math.min(Math.max(70, text.length * 14 + 30), 140),
        applications: bubbleData?.applications || [],
      };
    });

    // 目标数量
    const targetCount = Math.min(textSizes.length, 14);

    // 随机选择文字
    const shuffledTexts = [...textSizes].sort(() => Math.random() - 0.5);
    const selectedTexts = shuffledTexts.slice(0, targetCount);

    // 只定义左右两个区域
    const leftZone = {
      left: allowedLeft,
      top: allowedTop,
      width: excludedLeft - allowedLeft,
      height: allowedHeight,
    };

    const rightZone = {
      left: excludedRight,
      top: allowedTop,
      width: allowedRight - excludedRight,
      height: allowedHeight,
    };

    // 左右各分配一半的泡泡
    const leftCount = Math.ceil(targetCount / 2);
    const rightCount = targetCount - leftCount;

    const leftTexts = selectedTexts.slice(0, leftCount);
    const rightTexts = selectedTexts.slice(leftCount);

    const bubbles: Array<{
      text: string;
      position: { left: string; top: string };
      x: number;
      y: number;
      size: number;
      applications: Array<{ applicationTaskName?: string; device?: string }>;
    }> = [];
    const existingBubbles: Array<{ x: number; y: number; size: number }> = [];

    // 在指定区域内均匀放置泡泡
    const placeBubblesInZone = (
      zone: { left: number; top: number; width: number; height: number },
      texts: typeof textSizes,
    ) => {
      const rows = Math.ceil(Math.sqrt(texts.length));
      const cols = Math.ceil(texts.length / rows);
      const cellWidth = zone.width / cols;
      const cellHeight = zone.height / rows;

      texts.forEach((item, index) => {
        const row = Math.floor(index / cols);
        const col = index % cols;

        // 在单元格内随机偏移，但保持在单元格范围内
        const cellLeft = zone.left + col * cellWidth;
        const cellTop = zone.top + row * cellHeight;
        const maxOffsetX = Math.max(0, cellWidth - item.size);
        const maxOffsetY = Math.max(0, cellHeight - item.size);

        let placed = false;
        let attempts = 0;
        const maxAttempts = 50;

        while (!placed && attempts < maxAttempts) {
          const randomX = cellLeft + Math.random() * maxOffsetX;
          const randomY = cellTop + Math.random() * maxOffsetY;

          // 检查是否与其他泡泡重叠
          let overlaps = false;
          for (const existing of existingBubbles) {
            const dist = Math.sqrt(
              Math.pow(randomX + item.size / 2 - existing.x - existing.size / 2, 2) +
                Math.pow(randomY + item.size / 2 - existing.y - existing.size / 2, 2),
            );
            if (dist < (item.size + existing.size) / 2 + 10) {
              overlaps = true;
              break;
            }
          }

          if (!overlaps) {
            bubbles.push({
              text: item.text,
              position: {
                left: `${(randomX / containerWidth) * 100}%`,
                top: `${(randomY / containerHeight) * 100}%`,
              },
              x: randomX,
              y: randomY,
              size: item.size,
              applications: item.applications,
            });
            existingBubbles.push({ x: randomX, y: randomY, size: item.size });
            placed = true;
          }
          attempts++;
        }

        // 如果多次尝试失败，强制放置
        if (!placed) {
          const randomX = cellLeft + maxOffsetX / 2;
          const randomY = cellTop + maxOffsetY / 2;
          bubbles.push({
            text: item.text,
            position: {
              left: `${(randomX / containerWidth) * 100}%`,
              top: `${(randomY / containerHeight) * 100}%`,
            },
            x: randomX,
            y: randomY,
            size: item.size,
            applications: item.applications,
          });
          existingBubbles.push({ x: randomX, y: randomY, size: item.size });
        }
      });
    };

    // 在左右区域分别放置泡泡
    placeBubblesInZone(leftZone, leftTexts);
    placeBubblesInZone(rightZone, rightTexts);

    // 为每个泡泡生成随机的动画参数
    generatedBubbles.value = bubbles.map((b) => {
      const posX = parseFloat(b.position.left);
      const centerX = 50;

      // 左侧泡泡向左飘动，右侧泡泡向右飘动
      const dirX = posX < centerX ? -1 : 1;

      // 生成运动偏移
      const baseOffset = 20 + Math.random() * 15;

      return {
        text: b.text,
        position: b.position,
        animationDelay: Math.random() * 3,
        animationDuration: 10 + Math.random() * 6,
        animationOffset: {
          x: dirX * baseOffset,
          y: (Math.random() - 0.5) * 30,
        },
        applications: b.applications,
      };
    });
  };

  // 不再偏移容器，让大屏在内容区域内显示
  const adjustPosition = () => {
    // 保留空函数以便后续扩展
  };

  // 监听窗口变化
  let resizeObserver: ResizeObserver | null = null;
  const timers: number[] = [];

  const formatDuration = (ms: number) => {
    if (!ms || ms < 0) return '0分钟';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    if (days > 0) return `${days}天${hours % 24}小时`;
    if (hours > 0) return `${hours}小时${minutes % 60}分钟`;
    if (minutes > 0) return `${minutes}分钟`;
    return `${totalSeconds}秒`;
  };

  // 创建动态样式表来应用总览页的特殊样式
  const createDynamicStyleSheet = () => {
    if (typeof document === 'undefined') return;

    // 如果已存在，先移除
    if (dynamicStyleSheet) {
      dynamicStyleSheet.remove();
    }

    // 创建新的样式表
    dynamicStyleSheet = document.createElement('style');
    dynamicStyleSheet.id = 'overview-page-styles';
    dynamicStyleSheet.textContent = `
      .vben-layout-content,
      .vben-layout-content.fixed,
      .vben-page-wrapper,
      .vben-page-wrapper-content,
      .app-container,
      .wrapper,
      .wrapper.fixed,
      .vben-layout-main,
      .vben-layout {
        max-width: none !important;
        padding: 0 !important;
        overflow: hidden !important;
        height: 100vh !important;
        max-height: 100vh !important;
      }

      html,
      body {
        overflow: hidden !important;
      }
    `;
    document.head.appendChild(dynamicStyleSheet);
  };

  // 移除动态样式表
  const removeDynamicStyleSheet = () => {
    if (dynamicStyleSheet) {
      dynamicStyleSheet.remove();
      dynamicStyleSheet = null;
    }
    // 同时移除可能存在的旧样式表（兼容处理）
    const oldStyleSheet = document.getElementById('overview-page-styles');
    if (oldStyleSheet) {
      oldStyleSheet.remove();
    }
  };

  const fetchOnlineCount = async () => {
    try {
      // 新结构：{ code?, data: { count, users, durations, userIds } }
      const resp = await getOnlineUsersCount();
      const payload = resp?.data ?? resp ?? {};

      onlineUserCount.value = payload.count ?? 0;
      const durations = payload.durations || {};
      const users = payload.users || [];
      onlineUsers.value = Array.isArray(users)
        ? users.map((u: any) => ({
            id: u?.id,
            username: u?.username,
            duration: typeof u?.id !== 'undefined' ? durations?.[u.id] : undefined,
          }))
        : [];
    } catch (e) {
      console.error('获取在线人数失败', e);
      onlineUserCount.value = 0;
      onlineUsers.value = [];
    }
  };

  // 轮询间隔配置（30秒）
  const POLLING_INTERVAL = 30000;
  // 接口调用间隔（1秒）
  const API_CALL_INTERVAL = 1000;
  let pollingTimer: number | null = null;

  // 定义所有需要轮询的数据刷新函数
  const refreshAllData = () => {
    let delay = 0;

    // 1. 在线用户数
    setTimeout(() => {
      fetchOnlineCount();
    }, delay);
    delay += API_CALL_INTERVAL;

    // 2. 场景列表（泡泡数据）
    setTimeout(() => {
      loadSceneBubbles();
    }, delay);
    delay += API_CALL_INTERVAL;

    // 3. 通过事件通知子组件刷新数据
    // 使用自定义事件来通知各个子组件刷新
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-user-center'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-data-statistics'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-training-tasks'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-gpu-usage'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-memory-usage'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-disk-usage'));
    }, delay);
    delay += API_CALL_INTERVAL;

    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('overview-refresh-system-info'));
    }, delay);
  };

  // 启动轮询
  const startPolling = () => {
    // 立即执行一次
    refreshAllData();
    // 然后每30秒执行一次
    pollingTimer = window.setInterval(() => {
      refreshAllData();
    }, POLLING_INTERVAL);
  };

  // 停止轮询
  const stopPolling = () => {
    if (pollingTimer !== null) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  };

  onMounted(() => {
    // 创建动态样式表
    createDynamicStyleSheet();

    adjustPosition();
    timers.push(window.setTimeout(adjustPosition, 100));
    timers.push(window.setTimeout(adjustPosition, 300));

    // 初始化日期时间
    formatDateTime();
    // 每秒更新一次时间
    const timeInterval = setInterval(() => {
      formatDateTime();
    }, 1000);

    // 初始化在线人数（首次加载）
    fetchOnlineCount();

    // 加载场景列表（首次加载）
    loadSceneBubbles();

    // 启动轮询机制
    startPolling();

    timers.push(timeInterval as unknown as number);

    // 延迟生成泡泡，确保容器已完全初始化
    timers.push(
      window.setTimeout(() => {
        generateBubbles();
      }, 100),
    );

    // 监听父容器大小变化（菜单展开/收起）
    resizeObserver = new ResizeObserver(() => {
      adjustPosition();
      // 窗口大小变化时重新生成泡泡位置
      if (containerRef.value) {
        generateBubbles();
      }
    });

    if (containerRef.value?.parentElement) {
      resizeObserver.observe(containerRef.value.parentElement);
    }

    window.addEventListener('resize', () => {
      adjustPosition();
      if (containerRef.value) {
        generateBubbles();
      }
    });
  });

  onUnmounted(() => {
    // 停止轮询
    stopPolling();

    // 清理定时器
    timers.forEach((id) => clearTimeout(id));

    if (resizeObserver) {
      resizeObserver.disconnect();
    }
    window.removeEventListener('resize', adjustPosition);

    // 移除动态样式表，恢复页面滚动
    removeDynamicStyleSheet();
  });
</script>

<!-- 
  注意：总览页的特殊样式已改为动态样式表，在组件挂载时添加，卸载时移除
  这样可以避免影响其他页面的滚动功能
  样式定义已移至 createDynamicStyleSheet() 函数中
-->

<style scoped lang="less">
  .overview-container {
    position: relative;
    width: 100%;
    height: 100vh;
    margin: 0;
    padding: 0;
    overflow: hidden !important;
    background: #0a0e27;
  }

  // 背景层
  .bg-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    pointer-events: none;

    .bg-overall {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .bg-disk {
      position: absolute;
      top: 45%;
      left: 50%;
      transform: translateX(-50%);
      width: 87%;
      height: auto;
      object-fit: contain;
      z-index: 2;
    }

    .bg-bracket {
      position: absolute;
      top: 20%;
      left: 50%;
      transform: translateX(-50%);
      height: 50%;
      width: auto;
      object-fit: contain;
      z-index: 2;
    }

    // .bg-plate {
    //   position: absolute;
    //   top: 45%;
    //   left: 50%;
    //   transform: translateX(-50%);
    //   height: 20%;
    //   object-fit: contain;
    //   z-index: 3;
    // }

    // .bg-brain {
    //   position: absolute;
    //   top: 25%;
    //   left: 50%;
    //   transform: translateX(-50%);
    //   height: 25%;
    //   object-fit: contain;
    //   z-index: 4;
    // }

    .ai {
      position: absolute;
      top: 25%;
      left: calc(50% + 20px);
      transform: translate(-58%, -30%) scale(0.6);
      transform-origin: center;
      z-index: 4;
    }

    .bg-earth {
      position: absolute;
      top: 50%;
      left: calc(50% + 20px);
      transform: translate(-55%, -70%);
      height: auto;
      max-height: 50%;
      width: auto;
      max-width: 50%;
      object-fit: contain;
      // 调整为蓝色 #004ac3
      filter: hue-rotate(-5deg) saturate(1.6) brightness(0.8) contrast(1.8);
      z-index: 3;
    }

    /* ========== 中心球体位置调整 ========== */
    /* 修改 --ball-x 和 --ball-y 来调整位置 */
    /* 使用百分比，正值向右/下，负值向左/上 */

    .center-ball-container {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(calc(-50% + var(--ball-x, 0%)), calc(-50% + var(--ball-y, 0%)));
      scale: 1.3;
      width: 40%;
      height: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 3;

      .ball-gif {
        position: absolute;
        width: 100%;
        height: 100%;
        object-fit: contain;
        z-index: 2;
        scale: 0.7;
        transform: translate(0%, -12%);
      }

      .ball-static {
        position: absolute;
        width: 70%;
        height: 70%;
        object-fit: contain;
        z-index: 1;
      }
    }

    .bg-twinkle {
      position: absolute;
      top: 20%;
      left: 50%;
      transform: translateX(-50%) rotate(90deg);
      width: 50%;
      height: 40%;
      object-fit: contain;
      opacity: 0.25;
      filter: blur(4px);
      mix-blend-mode: screen;
      z-index: 4;
    }
  }

  // 大括号 - 对齐机器人中间位置
  .bg-brace-left {
    height: 85%;
    width: auto;
    object-fit: contain;
    flex-shrink: 0;
    pointer-events: none;
    margin-left: -16px;
    margin-top: -120px;
    align-self: center;
    z-index: 20;
  }

  .bg-brace-right {
    height: 85%;
    width: auto;
    object-fit: contain;
    flex-shrink: 0;
    pointer-events: none;
    margin-right: -16px;
    margin-top: -120px;
    align-self: center;
    z-index: 20;
  }

  // 内容层
  .content-layer {
    position: relative;
    z-index: 10;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  // 泡泡装饰层
  .bubbles-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 6;
    pointer-events: none;
  }

  // 可放置区域标注（调试用）
  .bubble-zone-marker {
    position: absolute;
    border: 3px dashed;
    pointer-events: none;
    z-index: 100;
  }

  .bubble-zone-allowed {
    border-color: rgba(64, 158, 255, 0.8);
    background: rgba(64, 158, 255, 0.2);
  }

  .bubble-zone-excluded {
    border-color: rgba(255, 80, 80, 0.8);
    background: rgba(255, 80, 80, 0.25);
  }

  // 顶部标题区域
  .header {
    height: 70px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    width: 100%;

    .title-left-section,
    .title-right-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      position: relative;
    }

    .title-left-line,
    .title-right-line {
      width: 100%;
      height: 70px;
      object-fit: fill;
    }

    .title-left-text {
      position: absolute;
      top: 30px;
      left: 40px;
      font-size: 18px;
      color: #fff;
      font-weight: 500;
      text-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
      white-space: nowrap;
      display: flex;
      align-items: center;
      gap: 6px;

      .online-info-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 18px;
        height: 18px;
        border-radius: 50%;
        border: 1px solid rgba(255, 255, 255, 0.6);
        color: #fff;
        font-size: 12px;
        cursor: default;
        background: rgba(255, 255, 255, 0.1);
        box-shadow: 0 0 6px rgba(64, 158, 255, 0.6);
      }

      .online-info-pop {
        position: absolute;
        top: 28px;
        left: 0;
        margin-top: 4px;
        padding: 8px 10px;
        width: 220px;
        background: rgba(10, 14, 39, 0.9);
        border: 1px solid rgba(64, 158, 255, 0.4);
        border-radius: 8px;
        box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35);
        z-index: 99999;
        backdrop-filter: blur(4px);

        .online-pop-title {
          font-size: 12px;
          color: #fff;
          margin-bottom: 6px;
          opacity: 0.85;
        }

        .online-pop-list {
          max-height: 180px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 4px;

          &::-webkit-scrollbar {
            width: 4px;
          }

          &::-webkit-scrollbar-thumb {
            background: rgba(64, 158, 255, 0.6);
            border-radius: 2px;
          }
        }

        .online-pop-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 12px;
          color: #fff;
          background: rgba(255, 255, 255, 0.04);
          padding: 4px 6px;
          border-radius: 4px;

          .name {
            max-width: 120px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .duration {
            color: rgba(64, 158, 255, 0.9);
            margin-left: 6px;
            flex-shrink: 0;
          }
        }
      }
    }

    .title-right-text {
      position: absolute;
      top: 30px;
      right: 20px;
      font-size: 16px;
      color: #fff;
      font-weight: 500;
      text-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
      white-space: nowrap;
    }

    .title-center {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 4;

      img {
        width: 100%;
        height: 70px;
        object-fit: fill;
      }

      .title-text {
        position: absolute;
        font-size: 32px;
        font-weight: bold;
        color: #fff;
        letter-spacing: 6px;
        text-shadow: 0 0 20px rgba(64, 158, 255, 0.8);
        white-space: nowrap;
      }
    }
  }

  // 主体布局
  .main-layout {
    flex: 1;
    display: flex;
    gap: 16px;
    padding: 8px 16px 16px 16px;
    min-height: 0;
    overflow: hidden;

    .left-panel,
    .right-panel {
      flex: 0 0 24%;
      max-width: 24%;
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 0;
      min-height: 0;
    }

    .center-panel {
      flex: 1.2;
      min-width: 0;
      min-height: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .center-top {
        flex: 1; // 缩短顶部占位，提高下方模块位置
        min-height: 0;
      }

      .rect-box-horizontal {
        flex: 0 0 calc((100% - 24px) / 3);
        min-height: 0;
      }
    }

    // 矩形框样式
    .rect-box {
      flex: 1 1 0;
      position: relative;
      min-height: 0;
      max-height: 100%;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .rect-bg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: fill;
        z-index: 1;
      }

      .rect-title {
        position: absolute;
        top: 0.5%;
        left: 50%;
        transform: translateX(-50%);
        z-index: 2;
        font-size: clamp(18px, 1.5vw, 26px);
        font-weight: bold;
        color: #fff;
        text-align: center;
        text-shadow: 0 0 10px rgba(64, 158, 255, 0.8);
        letter-spacing: 2px;
        white-space: nowrap;
        pointer-events: none;
      }

      .rect-content {
        position: relative;
        z-index: 2;
        flex: 1;
        padding: 8% 4% 4% 4%;
        min-height: 0;
        overflow: hidden;
        display: flex;
        flex-direction: column;

        &.resource-content {
          flex-direction: row;
          gap: 12px;
          padding: 8% 2% 4% 2%;
        }

        &.algorithm-content {
          flex-direction: row;
          justify-content: space-around;
          align-items: center;
          padding: 12% 4% 4% 4%;
          gap: 20px;
        }
      }

      .resource-item {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      .algorithm-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
      }

      .algorithm-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        max-width: 80px;
        aspect-ratio: 1;
      }

      .icon-circle {
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background: linear-gradient(
          135deg,
          rgba(64, 158, 255, 0.3) 0%,
          rgba(64, 158, 255, 0.1) 100%
        );
        border: 2px solid rgba(64, 158, 255, 0.6);
        box-shadow: 0 0 20px rgba(64, 158, 255, 0.4), inset 0 0 20px rgba(64, 158, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.3s ease;

        &:hover {
          border-color: rgba(64, 158, 255, 1);
          box-shadow: 0 0 30px rgba(64, 158, 255, 0.6), inset 0 0 30px rgba(64, 158, 255, 0.3);
          transform: scale(1.05);
        }
      }

      .algorithm-label {
        font-size: clamp(12px, 1vw, 16px);
        color: rgba(255, 255, 255, 0.9);
        font-weight: 500;
        text-align: center;
        text-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
        white-space: nowrap;
      }
    }

    // 单个模块上移调整
    .training-up {
      margin-top: -20px;
      min-height: 34%; // 显著提高训练任务模块高度
    }

    .gpu-up {
      margin-top: -30px;
      flex: 0 0 75%; // 再提高 GPU 模块高度，靠近两侧高度
      min-height: 42%; // 再加一些高度，给图表留空间
      overflow: visible; // 允许GPU模块内容溢出，避免被裁剪
      .rect-content {
        padding-left: 0;
        padding-right: 0;
        padding-top: 8%; // 减少顶部padding，给图表更多空间
        padding-bottom: 2%; // 减少底部padding，避免图表下半部分被遮挡
        overflow: visible; // 允许内容溢出，避免被裁剪
      }

      margin-bottom: 25px;
    }

    .system-up {
      margin-top: -12px;
      min-height: 34%; // 提高系统信息模块高度
    }
  }
</style>
