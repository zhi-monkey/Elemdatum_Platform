<template>
  <div class="training-tasks-container">
    <!-- 训练任务列表按钮 -->
    <div
      class="button-wrapper"
      ref="buttonRef"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <div class="task-list-button" @click="toggleFixed">
        <img class="button-bg" src="/resource/overview/button.png" alt="" />
        <span class="button-text">训练任务列表</span>
      </div>
    </div>
    <!-- 悬停显示框 - 使用Teleport渲染到body -->
    <Teleport to="body">
      <div
        v-show="showPopup"
        ref="popupRef"
        class="popup-box"
        :class="{ 'is-fixed': isFixed }"
        :style="popupStyle"
        @mouseenter="handlePopupEnter"
        @mouseleave="handlePopupLeave"
      >
        <div class="popup-content">
          <!-- 训练中列表 -->
          <div class="task-list-section" ref="trainingListRef">
            <div class="list-title">训练中</div>
            <div class="task-list">
              <div
                v-for="(task, index) in trainingTasks"
                :key="'training-' + index"
                class="task-item"
              >
                <div class="task-info">
                  <div class="task-name">{{ task.name }}</div>
                  <div class="task-details">
                    <div class="task-progress">
                      <span class="progress-label">进度:</span>
                      <span class="progress-value">{{ Math.round(task.progress || 0) }}%</span>
                    </div>
                    <div class="task-creator" v-if="task.creator">
                      <span class="creator-label">创建者:</span>
                      <span class="creator-value">{{ task.creator }}</span>
                    </div>
                  </div>
                </div>
                <div class="task-right">
                  <div class="task-time">{{ task.createTime }}</div>
                  <div class="task-resource">{{ task.resourceUsage || '未配置' }}</div>
                </div>
              </div>
            </div>
          </div>
          <!-- 排队中列表 -->
          <div class="task-list-section" ref="queuingListRef">
            <div class="list-title">排队中</div>
            <div class="task-list">
              <div
                v-for="(task, index) in queuingTasks"
                :key="'queuing-' + index"
                class="task-item"
              >
                <div class="task-info">
                  <div class="task-name">{{ task.name }}</div>
                  <div class="task-details">
                    <div class="task-queuing-time">
                      <span class="queuing-label">排队:</span>
                      <span class="queuing-value">{{
                        formatQueuingTime(task.queuingTime || 0)
                      }}</span>
                    </div>
                    <div class="task-creator" v-if="task.creator">
                      <span class="creator-label">创建者:</span>
                      <span class="creator-value">{{ task.creator }}</span>
                    </div>
                  </div>
                </div>
                <div class="task-right">
                  <div class="task-time">{{ task.createTime }}</div>
                  <div class="task-resource">{{ task.resourceUsage || '未配置' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
    <div class="stats-wrapper">
      <div class="stat-item train-item">
        <div class="stat-value training">{{ trainingCount }}</div>
        <div class="stat-label">训练中</div>
      </div>
      <div class="stat-item queuing-item">
        <div class="stat-value queuing">{{ queuingCount }}</div>
        <div class="stat-label">排队中</div>
      </div>
      <div class="stat-item success-item">
        <div class="stat-value success">{{ successCount }}</div>
        <div class="stat-label">训练成功</div>
      </div>
      <div class="stat-item failed-item">
        <div class="stat-value failed">{{ failedCount }}</div>
        <div class="stat-label">训练失败</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import { getOverviewTrainingStats } from '../data';

  const trainingCount = ref(0);
  const queuingCount = ref(0);
  const successCount = ref(0);
  const failedCount = ref(0);

  // 控制弹窗显示
  const showPopup = ref(false);
  const isFixed = ref(false); // 是否固定弹窗
  const buttonRef = ref<HTMLElement | null>(null);
  const popupRef = ref<HTMLElement | null>(null);
  const trainingListRef = ref<HTMLElement | null>(null);
  const queuingListRef = ref<HTMLElement | null>(null);
  const popupStyle = ref<{ top: string; left: string; transform?: string }>({
    top: '0px',
    left: '0px',
  });
  let autoScrollIntervals: number[] = [];

  // 格式化时间（年月日 小时分钟）
  const formatTime = (date: Date) => {
    // 检查日期是否有效
    if (!date || isNaN(date.getTime())) {
      return '';
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}年${month}月${day}日 ${hours}:${minutes}`;
  };

  // 定义任务接口
  interface TrainingTask {
    id: number;
    name: string;
    status: number;
    createTime: string | Date;
    progress: number;
    resourceUsage: string;
    queuingTime: number;
    creator?: string;
  }

  const trainingTasks = ref<Array<TrainingTask>>([]);
  const queuingTasks = ref<Array<TrainingTask>>([]);

  // 格式化时间（处理 Date 对象、字符串、时间戳等）
  const formatTimeStr = (time: string | Date | number | null | undefined) => {
    if (!time) return '';
    let d: Date;

    try {
      if (time instanceof Date) {
        d = time;
      } else if (typeof time === 'number') {
        // 处理时间戳（可能是毫秒或秒）
        d = new Date(time > 1000000000000 ? time : time * 1000);
      } else if (typeof time === 'string') {
        // 处理字符串格式
        let timeStr = time.trim();

        // 如果是 "yyyy-MM-dd HH:mm:ss" 格式，需要特殊处理
        if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(timeStr)) {
          // 将 "yyyy-MM-dd HH:mm:ss" 转换为 "yyyy/MM/dd HH:mm:ss"
          timeStr = timeStr.replace(/-/g, '/');
          d = new Date(timeStr);
        } else if (timeStr.includes('T') || timeStr.includes('Z')) {
          // ISO 8601 格式
          d = new Date(timeStr);
        } else {
          // 其他格式，尝试替换 - 为 /
          d = new Date(timeStr.replace(/-/g, '/'));
        }
      } else {
        return '';
      }

      // 检查日期是否有效
      if (isNaN(d.getTime())) {
        console.warn('Invalid date:', time);
        return '';
      }

      return formatTime(d);
    } catch (error) {
      console.error('Error formatting time:', time, error);
      return '';
    }
  };

  // 格式化排队时间（毫秒转换为可读格式）
  const formatQueuingTime = (milliseconds: number) => {
    if (!milliseconds || milliseconds <= 0) return '0分钟';

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
      return `${days}天${hours % 24}小时`;
    } else if (hours > 0) {
      return `${hours}小时${minutes % 60}分钟`;
    } else if (minutes > 0) {
      return `${minutes}分钟`;
    } else {
      return `${seconds}秒`;
    }
  };

  const fetchOverviewTrainingStats = async () => {
    try {
      const res = await getOverviewTrainingStats();
      if (!res) return;

      trainingCount.value = (res as any).trainingCount || 0;
      queuingCount.value = (res as any).queuingCount || 0;
      successCount.value = (res as any).trainSucceededCount || 0;
      failedCount.value = (res as any).trainFailedCount || 0;

      const tJobs = ((res as any).trainingJobs || []) as Array<TrainingTask>;
      const qJobs = ((res as any).queuingJobs || []) as Array<TrainingTask>;

      trainingTasks.value = tJobs.map((j) => {
        const formattedTime = formatTimeStr(j.createTime);
        // 如果格式化失败，尝试直接使用原始值
        return {
          ...j,
          createTime: formattedTime || (typeof j.createTime === 'string' ? j.createTime : ''),
        };
      });

      queuingTasks.value = qJobs.map((j) => {
        const formattedTime = formatTimeStr(j.createTime);
        // 如果格式化失败，尝试直接使用原始值
        return {
          ...j,
          createTime: formattedTime || (typeof j.createTime === 'string' ? j.createTime : ''),
        };
      });
    } catch (error) {
      console.error('获取训练任务统计失败:', error);
    }
  };

  // 计算弹窗位置（弹窗左下角在按钮右侧50px，上方40px）
  const updatePopupPosition = () => {
    if (!buttonRef.value) return;
    const rect = buttonRef.value.getBoundingClientRect();
    const popupHeight = 350; // 弹窗高度（已更新）

    // 弹窗左下角位置：
    // left = 按钮右边缘 + 50px
    // top = 按钮上边缘 - 40px - 弹窗高度（因为要左下角对齐）
    popupStyle.value = {
      left: `${rect.right + 50}px`,
      top: `${rect.top - 40 - popupHeight}px`,
      transform: 'none',
    };
  };

  // 自动滚动函数
  const startAutoScroll = () => {
    if (isFixed.value) return; // 固定时不滚动

    const scrollList = (listRef: HTMLElement | null) => {
      if (!listRef) return;
      const list = listRef.querySelector('.task-list') as HTMLElement;
      if (!list) return;

      const scrollHeight = list.scrollHeight;
      const clientHeight = list.clientHeight;
      if (scrollHeight <= clientHeight) return; // 内容不够，不需要滚动

      let scrollTop = list.scrollTop || 0;
      const maxScroll = scrollHeight - clientHeight;

      const scroll = () => {
        if (isFixed.value) return; // 如果固定了，停止滚动
        scrollTop += 1;
        if (scrollTop >= maxScroll) {
          scrollTop = 0; // 滚动到底部后回到顶部
        }
        list.scrollTop = scrollTop;
      };

      return setInterval(scroll, 50); // 每50ms滚动1px
    };

    // 为两个列表分别启动滚动
    const interval1 = scrollList(trainingListRef.value);
    const interval2 = scrollList(queuingListRef.value);

    // 保存所有interval以便清理
    if (interval1) autoScrollIntervals.push(interval1);
    if (interval2) autoScrollIntervals.push(interval2);
  };

  // 停止自动滚动
  const stopAutoScroll = () => {
    autoScrollIntervals.forEach((interval) => {
      clearInterval(interval);
    });
    autoScrollIntervals = [];
  };

  // 鼠标进入按钮
  const handleMouseEnter = () => {
    updatePopupPosition();
    showPopup.value = true;
    if (!isFixed.value) {
      // 延迟一下再开始滚动，让弹窗先显示
      setTimeout(() => {
        startAutoScroll();
      }, 300);
    }
  };

  // 鼠标离开按钮
  const handleMouseLeave = () => {
    if (!isFixed.value) {
      showPopup.value = false;
      stopAutoScroll();
    }
  };

  // 鼠标进入弹窗
  const handlePopupEnter = () => {
    if (!isFixed.value) {
      startAutoScroll();
    }
  };

  // 鼠标离开弹窗
  const handlePopupLeave = () => {
    if (!isFixed.value) {
      showPopup.value = false;
      stopAutoScroll();
    }
  };

  // 切换固定状态
  const toggleFixed = () => {
    isFixed.value = !isFixed.value;
    if (isFixed.value) {
      stopAutoScroll();
    } else {
      // 取消固定时，如果鼠标还在按钮或弹窗上，继续显示
      showPopup.value = false;
    }
  };

  // 处理点击外部区域关闭弹窗
  const handleClickOutside = (event: MouseEvent) => {
    if (!showPopup.value) return;

    const target = event.target as HTMLElement;

    // 检查点击是否在弹窗内部
    if (popupRef.value && popupRef.value.contains(target)) {
      return;
    }

    // 检查点击是否在按钮内部
    if (buttonRef.value && buttonRef.value.contains(target)) {
      return;
    }

    // 点击在外部，关闭弹窗
    showPopup.value = false;
    stopAutoScroll();
  };

  // 监听窗口滚动和大小变化
  const handleScroll = () => {
    if (showPopup.value) {
      updatePopupPosition();
    }
  };

  // 监听刷新事件
  const handleRefresh = () => {
    fetchOverviewTrainingStats();
  };

  onMounted(() => {
    window.addEventListener('scroll', handleScroll, true);
    window.addEventListener('resize', updatePopupPosition);
    // 添加点击事件监听，使用捕获阶段确保能捕获到所有点击
    document.addEventListener('click', handleClickOutside, true);
    window.addEventListener('overview-refresh-training-tasks', handleRefresh);
    fetchOverviewTrainingStats();
  });

  onUnmounted(() => {
    stopAutoScroll();
    window.removeEventListener('scroll', handleScroll, true);
    window.removeEventListener('resize', updatePopupPosition);
    // 移除点击事件监听
    document.removeEventListener('click', handleClickOutside, true);
    window.removeEventListener('overview-refresh-training-tasks', handleRefresh);
  });

  // 获取真实数据的函数（保留接口，暂时不使用）
  // const fetchJobList = async () => {
  //   try {
  //     const response = await getModelJobList({});
  //     if (response) {
  //       const filteredResponse = response.filter((job: any) => job.isDelete === false);
  //
  //       // 训练中：status === 1
  //       trainingCount.value = filteredResponse.filter((job: any) => job.status === 1).length;
  //
  //       // 排队中：status === 0 (创建成功，等待训练)
  //       queuingCount.value = filteredResponse.filter((job: any) => job.status === 0).length;
  //     }
  //   } catch (error) {
  //     console.error('获取训练任务数据失败:', error);
  //   }
  // };

  // onMounted(() => {
  //   fetchJobList();
  // });
</script>

<style scoped lang="less">
  .training-tasks-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 20px;
    gap: 16px;
  }

  .button-wrapper {
    position: relative;
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .task-list-button {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    min-width: 180px;
    height: 40px;
  }

  .button-bg {
    position: absolute;
    width: 100%;
    height: 100%;
    object-fit: fill;
  }

  .button-text {
    position: relative;
    z-index: 1;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    text-shadow: 0 0 8px rgba(64, 158, 255, 0.8);
  }

  .popup-box {
    position: fixed;
    width: 800px;
    height: 350px;
    background: rgba(10, 14, 39, 0.95);
    border: 2px solid rgba(64, 158, 255, 0.6);
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    padding: 12px;
    z-index: 99999;
    display: flex;
    flex-direction: column;

    &.is-fixed {
      overflow-y: auto;
    }
  }

  .popup-content {
    display: flex;
    flex-direction: row;
    gap: 16px;
    height: 100%;
  }

  .task-list-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-width: 0;
    overflow: hidden;
  }

  .list-title {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    padding-bottom: 6px;
    border-bottom: 1px solid rgba(64, 158, 255, 0.3);
    flex-shrink: 0;
  }

  .task-list {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    overflow-y: auto;
    padding-right: 4px;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 2px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(64, 158, 255, 0.5);
      border-radius: 2px;

      &:hover {
        background: rgba(64, 158, 255, 0.7);
      }
    }
  }

  .task-item {
    display: flex;
    flex-direction: row;
    align-items: stretch;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 10px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 4px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.2s ease;
    flex-shrink: 0;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(64, 158, 255, 0.5);
    }
  }

  .task-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
  }

  .task-name {
    font-size: 13px;
    color: #fff;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .task-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.7);
  }

  .task-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
    min-width: 120px;
    height: 100%;
  }

  .task-progress,
  .task-queuing-time,
  .task-creator {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .progress-label,
  .queuing-label,
  .creator-label {
    color: rgba(255, 255, 255, 0.6);
  }

  .progress-value {
    color: #ff9800;
    font-weight: 600;
  }

  .queuing-value {
    color: #409eff;
    font-weight: 600;
  }

  .creator-value {
    color: rgba(255, 255, 255, 0.8);
    font-weight: 500;
  }

  .task-resource {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.5);
    text-align: right;
    white-space: nowrap;
    margin-top: auto;
    margin-bottom: auto;
  }

  .task-time {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
    white-space: nowrap;
    text-align: right;
    flex-shrink: 0;
    margin-bottom: auto;
  }

  .stats-wrapper {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 16px;
    width: 100%;
    justify-content: center;
    align-items: stretch;
  }

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: transparent;
    transition: all 0.3s ease;
    flex: 0 0 calc(50% - 8px);
    min-width: 0;
    max-width: calc(50% - 8px);
    position: relative;

    // 胶囊边框基础样式
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      border-radius: 50px;
      border: 2px solid;
      box-shadow: inset 0 0 15px, 0 0 10px;
      z-index: -1;
    }

    // 内部发光层
    &::after {
      content: '';
      position: absolute;
      top: 6px;
      left: 6px;
      right: 6px;
      bottom: 6px;
      border-radius: 44px;
      border: 1px solid;
      opacity: 0.5;
      z-index: -1;
    }

    &.train-item {
      &::before {
        border-color: #ff9800;
        box-shadow: inset 0 0 20px rgba(255, 152, 0, 0.3), 0 0 15px rgba(255, 152, 0, 0.4);
      }
      &::after {
        border-color: #ff9800;
      }
    }

    &.queuing-item {
      &::before {
        border-color: #409eff;
        box-shadow: inset 0 0 20px rgba(64, 158, 255, 0.3), 0 0 15px rgba(64, 158, 255, 0.4);
      }
      &::after {
        border-color: #409eff;
      }
    }

    &.success-item {
      &::before {
        border-color: #67c23a;
        box-shadow: inset 0 0 20px rgba(103, 194, 58, 0.3), 0 0 15px rgba(103, 194, 58, 0.4);
      }
      &::after {
        border-color: #67c23a;
      }
    }

    &.failed-item {
      &::before {
        border-color: #f56c6c;
        box-shadow: inset 0 0 20px rgba(245, 108, 108, 0.3), 0 0 15px rgba(245, 108, 108, 0.4);
      }
      &::after {
        border-color: #f56c6c;
      }
    }
  }

  .stat-label {
    position: relative;
    z-index: 1;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.7);
    font-weight: 600;
  }

  .stat-value {
    position: relative;
    z-index: 1;
    font-size: 18px;
    font-weight: 800;
    line-height: 1;
    text-shadow: 0 0 10px currentColor;
  }

  .stat-value.training {
    color: #ff9800;
  }

  .stat-value.queuing {
    color: #409eff;
  }

  .stat-value.success {
    color: #67c23a;
  }

  .stat-value.failed {
    color: #f56c6c;
  }
</style>
