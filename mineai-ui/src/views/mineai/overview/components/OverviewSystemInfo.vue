<template>
  <div class="system-info-container">
    <div class="info-list-wrapper" v-if="systemInfo.length > 0">
      <div class="info-list" :style="{ transform: `translateY(-${scrollPosition}px)` }">
        <div v-for="(item, index) in displayData" :key="`${index}-${item.time}`" class="info-item">
          <div class="info-time">{{ item.time }}</div>
          <div class="info-content">{{ item.info }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">暂无数据</div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted, computed } from 'vue';
  // 暂时使用假数据，真实API接口保留在注释中
  import { getModelApplicationList, getAllPublicDatasets } from '../data';
  import { dayjs } from 'element-plus';

  interface SystemInfo {
    time: string;
    info: string;
  }

  const systemInfo = ref<SystemInfo[]>([]);
  const scrollPosition = ref(0);
  const scrollSpeed = 0.5; // 滚动速度
  let animationFrameId: number | null = null;
  let lastTimestamp = 0;
  const itemHeight = 70; // 每个消息框的高度（包括间距）

  // 使用假数据
  const generateMockData = () => {
    const mockData: SystemInfo[] = [
      {
        time: '2024-01-15 14:30:25',
        info: '张三 发布了 图像分类数据集 数据集',
      },
      {
        time: '2024-01-15 13:20:15',
        info: '李四 发布了 目标检测模型 应用任务',
      },
      {
        time: '2024-01-15 12:10:05',
        info: '王五 发布了 自然语言处理数据集 数据集',
      },
      {
        time: '2024-01-15 11:05:30',
        info: '赵六 发布了 语音识别模型 应用任务',
      },
      {
        time: '2024-01-15 10:00:20',
        info: '孙七 发布了 计算机视觉数据集 数据集',
      },
      {
        time: '2024-01-15 09:15:10',
        info: '周八 发布了 深度学习模型 应用任务',
      },
      {
        time: '2024-01-15 08:30:45',
        info: '吴九 发布了 数据挖掘数据集 数据集',
      },
      {
        time: '2024-01-15 07:45:30',
        info: '郑十 发布了 机器学习模型 应用任务',
      },
    ];
    return mockData;
  };

  // 获取真实数据的函数（保留接口，暂时不使用）
  const fetchSystemInfo = async () => {
    try {
      const datasetRes = await getAllPublicDatasets({});
      const appRes = await getModelApplicationList({});
      const datasetTasks = datasetRes.result.map((dataset) => ({
        time: dayjs(dataset.createTime).format('YYYY-MM-DD HH:mm:ss'),
        info: `${dataset.creatorName} 发布了 ${dataset.name} 数据集`,
      }));

      const applicationTasks = appRes.content
        .filter((task) => task.isReleased === '已发布')
        .map((task) => ({
          time: dayjs(task.releaseTime).format('YYYY-MM-DD HH:mm:ss'),
          info: `${task.publisherId} 发布了 ${task.applicationTaskName} 应用任务`,
        }));

      const combinedTasks = [...datasetTasks, ...applicationTasks];
      combinedTasks.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());

      systemInfo.value = combinedTasks.slice(0, 10);

      if (systemInfo.value.length > 0) {
        // 数据加载完成后再启动滚动
        setTimeout(() => {
          startAutoScroll();
        }, 100);
      }
    } catch (error) {
      console.error('获取数据失败:', error);
    }
  };

  // 显示数据：复制数据以实现无缝滚动
  const displayData = computed(() => {
    if (systemInfo.value.length === 0) return [];
    // 复制数据以实现无缝滚动
    return [...systemInfo.value, ...systemInfo.value];
  });

  // 滚动动画
  const scrollAnimation = (timestamp: number) => {
    if (!lastTimestamp) lastTimestamp = timestamp;

    const elapsed = timestamp - lastTimestamp;

    if (elapsed >= 16) {
      scrollPosition.value += scrollSpeed;

      // 无缝滚动：当滚动完一组数据后回到起点
      const singleListHeight = systemInfo.value.length * itemHeight;
      if (scrollPosition.value >= singleListHeight) {
        scrollPosition.value = 0;
      }

      lastTimestamp = timestamp;
    }

    animationFrameId = requestAnimationFrame(scrollAnimation);
  };

  const startAutoScroll = () => {
    if (systemInfo.value.length === 0) return;
    stopAutoScroll();
    lastTimestamp = 0;
    animationFrameId = requestAnimationFrame(scrollAnimation);
  };

  const stopAutoScroll = () => {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }
  };

  // 监听刷新事件
  const handleRefresh = () => {
    fetchSystemInfo();
  };

  onMounted(() => {
    // 暂时使用假数据
    // systemInfo.value = generateMockData();

    // 真实数据接口保留，需要时可以取消注释
    fetchSystemInfo();
    window.addEventListener('overview-refresh-system-info', handleRefresh);
  });

  onUnmounted(() => {
    stopAutoScroll();
    window.removeEventListener('overview-refresh-system-info', handleRefresh);
  });
</script>

<style scoped lang="less">
  .system-info-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 12px 8px;
    position: relative;
  }

  .info-list-wrapper {
    flex: 1;
    overflow: hidden;
    position: relative;
    min-height: 0;
  }

  .info-list {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    will-change: transform;
    padding: 8px 0;
  }

  .info-item {
    display: flex;
    flex-direction: column;
    padding: 8px 10px;
    margin-bottom: 8px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 4px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.3s ease;
    min-height: 54px;
    box-sizing: border-box;

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(64, 158, 255, 0.5);
    }
  }

  .info-time {
    font-family: 'Courier New', monospace;
    font-size: 11px;
    color: rgba(160, 200, 255, 0.8);
    margin-bottom: 4px;
    font-weight: 500;
  }

  .info-content {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.9);
    line-height: 1.4;
    word-break: break-word;
  }

  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: rgba(255, 255, 255, 0.5);
    font-size: 14px;
  }

  // 滚动条样式
  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 2px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(64, 158, 255, 0.3);
    border-radius: 2px;

    &:hover {
      background: rgba(64, 158, 255, 0.5);
    }
  }
</style>
