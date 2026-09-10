<template>
  <div class="algorithm-stats-container">
    <!-- 算力芯片 - 紫色UFO -->
    <div class="stat-item purple-theme">
      <div
        class="stat-value"
        :style="{ transform: `translate(var(--text1-x, 0%), var(--text1-y, 0%))` }"
      >
        {{ chipCount }}
      </div>
      <img
        class="stat-icon"
        src="/resource/overview/purple_ufo.png"
        alt=""
        :style="{
          transform: `translate(var(--icon1-x, 0%), var(--icon1-y, 0%)) scaleY(var(--icon1-scale-y, 1))`,
        }"
      />
      <div
        class="stat-label"
        :style="{ transform: `translate(var(--label1-x, 0%), var(--label1-y, 0%))` }"
      >
        算力芯片
      </div>
    </div>
    <!-- 基础算法库 - 蓝色UFO -->
    <div class="stat-item blue-theme">
      <div
        class="stat-value"
        :style="{ transform: `translate(var(--text2-x, 0%), var(--text2-y, 0%))` }"
      >
        {{ algorithmCount }}
      </div>
      <img
        class="stat-icon"
        src="/resource/overview/blue_ufo.png"
        alt=""
        :style="{
          transform: `translate(var(--icon2-x, 0%), var(--icon2-y, 0%)) scaleY(var(--icon2-scale-y, 1))`,
        }"
      />
      <div
        class="stat-label"
        :style="{ transform: `translate(var(--label2-x, 0%), var(--label2-y, 0%))` }"
      >
        基础算法库
      </div>
    </div>
    <!-- 算法应用 - 黄色UFO -->
    <div class="stat-item yellow-theme">
      <div
        class="stat-value"
        :style="{ transform: `translate(var(--text3-x, 0%), var(--text3-y, 0%))` }"
      >
        {{ appCount }}
      </div>
      <img
        class="stat-icon icon-stretch"
        src="/resource/overview/yellow_ufo.png"
        alt=""
        :style="{
          transform: `translate(var(--icon3-x, 0%), var(--icon3-y, 0%)) scaleY(var(--icon3-scale-y, 1.3))`,
        }"
      />
      <div
        class="stat-label"
        :style="{ transform: `translate(var(--label3-x, 0%), var(--label3-y, 0%))` }"
      >
        算法应用
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import {
    getBaseModelCount,
    getChipCount,
    getModelApplicationCount,
  } from '/@/views/mineai/overview/data';

  // 数据变量定义
  const chipCount = ref(0); // 算力芯片数量
  const algorithmCount = ref(0); // 基础算法库数量
  const appCount = ref(0); // 算法应用数量

  const fetchChipCount = async () => {
    try {
      const response = await getChipCount();
      chipCount.value = response ?? 0;
    } catch (error) {
      console.error('获取算力芯片数量失败', error);
      chipCount.value = 0;
    }
  };

  const fetchAlgorithmCount = async () => {
    try {
      const response = await getBaseModelCount();
      algorithmCount.value = response ?? 0;
    } catch (error) {
      console.error('获取基础算法库数量失败', error);
      algorithmCount.value = 0;
    }
  };

  const fetchModelApplicationCount = async () => {
    try {
      const response = await getModelApplicationCount();
      appCount.value = (response?.releasedCount ?? 0) + (response?.unreleasedCount ?? 0);
    } catch (error) {
      console.error('获取算法应用数量失败', error);
      appCount.value = 0;
    }
  };

  // 刷新所有数据
  const refreshAllData = async () => {
    await Promise.all([fetchChipCount(), fetchAlgorithmCount(), fetchModelApplicationCount()]);
  };

  // 监听刷新事件
  const handleRefresh = () => {
    refreshAllData();
  };

  onMounted(async () => {
    await refreshAllData();
    window.addEventListener('overview-refresh-algorithm-stats', handleRefresh);
  });

  onUnmounted(() => {
    window.removeEventListener('overview-refresh-algorithm-stats', handleRefresh);
  });
</script>

<style scoped lang="less">
  .algorithm-stats-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: row;
    padding: 10px 5px;
    justify-content: space-between;
    align-items: center;

    /* ========== 位置调整变量（使用百分比，自适应分辨率） ========== */
    /* 图标位置: --icon{n}-x 横向, --icon{n}-y 纵向 */
    /* 数字位置: --text{n}-x 横向, --text{n}-y 纵向 */
    /* 标签位置: --label{n}-x 横向, --label{n}-y 纵向 */
    /* 正值向右/下移动，负值向左/上移动 */
    /* 例如: --icon1-x: 5%; --icon1-y: -10%; */

    /* 第1个(紫色UFO - 算力芯片) */
    --icon1-x: 0%;
    --icon1-y: -20%;
    --icon1-scale-y: 1.6; /* 纵向拉伸比例，1为原始，>1拉长 */
    --text1-x: 0%;
    --text1-y: 120%;
    --label1-x: 0%;
    --label1-y: 140%;

    /* 第2个(蓝色UFO - 基础算法库) */
    --icon2-x: 0%;
    --icon2-y: -20%;
    --icon2-scale-y: 1.6; /* 纵向拉伸比例，1为原始，>1拉长 */
    --text2-x: 0%;
    --text2-y: 120%;
    --label2-x: 0%;
    --label2-y: 140%;

    /* 第3个(黄色UFO - 算法应用) */
    --icon3-x: 0%;
    --icon3-y: -20%;
    --icon3-scale-y: 1.6; /* 纵向拉伸比例，1为原始，>1拉长 */
    --text3-x: 0%;
    --text3-y: 120%;
    --label3-x: 0%;
    --label3-y: 140%;
  }

  .stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 8px 4px;
    min-height: 120px;
    position: relative;
  }

  .stat-icon {
    width: 80%;
    height: 200%;
    object-fit: contain;
    margin-top: -20px;
    margin-bottom: -15px;
    z-index: 1;
  }

  .stat-value {
    font-size: 36px;
    font-weight: 700;
    line-height: 1;
    z-index: 2;
    position: relative;
  }

  .stat-label {
    font-size: 18px;
    font-weight: 500;
    white-space: nowrap;
    z-index: 2;
    position: relative;
  }

  // 紫色主题 - 算力芯片
  .purple-theme {
    .stat-value {
      color: #b388ff;
      text-shadow: 0 0 10px rgba(179, 136, 255, 0.9), 0 0 20px rgba(179, 136, 255, 0.7),
        0 0 30px rgba(179, 136, 255, 0.5);
    }
    .stat-label {
      color: rgba(179, 136, 255, 0.9);
      text-shadow: 0 0 6px rgba(179, 136, 255, 0.4);
    }
  }

  // 蓝色主题 - 基础算法库
  .blue-theme {
    .stat-value {
      color: #00d4ff;
      text-shadow: 0 0 10px rgba(0, 212, 255, 0.9), 0 0 20px rgba(0, 212, 255, 0.7),
        0 0 30px rgba(0, 212, 255, 0.5);
    }
    .stat-label {
      color: rgba(0, 212, 255, 0.9);
      text-shadow: 0 0 6px rgba(0, 212, 255, 0.4);
    }
  }

  // 黄色主题 - 算法应用
  .yellow-theme {
    .stat-value {
      color: #ffd700;
      text-shadow: 0 0 10px rgba(255, 215, 0, 0.9), 0 0 20px rgba(255, 215, 0, 0.7),
        0 0 30px rgba(255, 215, 0, 0.5);
    }
    .stat-label {
      color: rgba(255, 215, 0, 0.9);
      text-shadow: 0 0 6px rgba(255, 215, 0, 0.4);
    }
  }
</style>
