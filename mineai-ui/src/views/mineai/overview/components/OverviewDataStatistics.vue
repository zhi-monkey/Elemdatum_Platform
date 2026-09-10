<template>
  <div class="data-statistics-container">
    <div class="stat-item yellow-theme">
      <img class="stone-bg" src="/resource/overview/yellow_bouncy_plate.png" alt="" />
      <div class="stat-value">{{ surveyDatasetCount }}</div>
      <div class="stat-label">公开数据集</div>
    </div>
    <div class="stat-item blue-theme">
      <img class="stone-bg" src="/resource/overview/blue_bouncy_plate.png" alt="" />
      <div class="stat-value">{{ privateDatasetCount }}</div>
      <div class="stat-label">私有数据集</div>
    </div>
    <div class="stat-item green-theme">
      <img class="stone-bg" src="/resource/overview/green_bouncy_plate.png" alt="" />
      <div class="stat-value">{{ labeledImageCount }}</div>
      <div class="stat-label">标注图片数(万)</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue';
  import { getAllDatasetCount } from '/@/views/mineai/overview/data';
  // TODO: 导入接口函数
  // import { getSurveyDatasetCount, getPrivateDatasetCount, getLabeledImageCount } from '../data';

  // 数据变量定义
  const surveyDatasetCount = ref(0); // 公开数据集数量
  const privateDatasetCount = ref(0); // 私有数据集数量
  const labeledImageCount = ref(0); // 标注图片数(万)

  // 获取所有数据集统计数据
  const fetchAllDatasetCount = async () => {
    try {
      const response = await getAllDatasetCount();
      surveyDatasetCount.value = response?.publicCount ?? 0;
      privateDatasetCount.value = response?.privateCount ?? 0;
      // 标注图片数转换为万
      labeledImageCount.value = Number(((response?.annotatedImageCount ?? 0) / 10000).toFixed(2));
    } catch (error) {
      console.error('获取数据集统计数据失败', error);
      surveyDatasetCount.value = 0;
      privateDatasetCount.value = 0;
      labeledImageCount.value = 0;
    }
  };

  // 刷新所有数据
  const refreshAllData = async () => {
    await fetchAllDatasetCount();
  };

  // 监听刷新事件
  const handleRefresh = () => {
    refreshAllData();
  };

  onMounted(async () => {
    await refreshAllData();
    window.addEventListener('overview-refresh-data-statistics', handleRefresh);
  });

  onUnmounted(() => {
    window.removeEventListener('overview-refresh-data-statistics', handleRefresh);
  });
</script>

<style scoped lang="less">
  .data-statistics-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: row;
    gap: 6px;
    padding: 10px;
    justify-content: space-around;
    align-items: center;
  }

  .stat-item {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 120px;
    overflow: visible;
  }

  .stone-bg {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -60%);
    width: 119px;
    height: 116px;
    object-fit: contain;
    z-index: 1;
  }

  .stat-value {
    position: relative;
    z-index: 2;
    font-size: 36px;
    font-weight: 700;
    text-align: center;
    line-height: 1;
    margin-bottom: auto;
    padding-top: 10px;
    transform: translateY(-50%);
  }

  .stat-label {
    position: relative;
    z-index: 2;
    font-size: 18px;
    font-weight: 500;
    text-align: center;
    margin-top: auto;
    transform: translateY(130%);
  }

  // 黄色主题 - 公开数据集
  .yellow-theme {
    .stat-value {
      color: #ffd700;
      text-shadow: 0 0 10px rgba(255, 215, 0, 0.9), 0 0 20px rgba(255, 215, 0, 0.7),
        0 0 30px rgba(255, 215, 0, 0.5), 0 0 40px rgba(255, 180, 0, 0.4);
    }
    .stat-label {
      color: rgba(255, 215, 0, 0.9);
      text-shadow: 0 0 6px rgba(255, 215, 0, 0.6), 0 0 12px rgba(255, 215, 0, 0.4);
    }
  }

  // 蓝色主题 - 私有数据集
  .blue-theme {
    .stat-value {
      color: #00d4ff;
      text-shadow: 0 0 10px rgba(0, 212, 255, 0.9), 0 0 20px rgba(0, 212, 255, 0.7),
        0 0 30px rgba(0, 212, 255, 0.5), 0 0 40px rgba(0, 150, 255, 0.4);
    }
    .stat-label {
      color: rgba(0, 212, 255, 0.9);
      text-shadow: 0 0 6px rgba(0, 212, 255, 0.6), 0 0 12px rgba(0, 212, 255, 0.4);
    }
  }

  // 绿色主题 - 标注图片数
  .green-theme {
    .stat-value {
      color: #00ff88;
      text-shadow: 0 0 10px rgba(0, 255, 136, 0.9), 0 0 20px rgba(0, 255, 136, 0.7),
        0 0 30px rgba(0, 255, 136, 0.5), 0 0 40px rgba(0, 200, 100, 0.4);
    }
    .stat-label {
      color: rgba(0, 255, 136, 0.9);
      text-shadow: 0 0 6px rgba(0, 255, 136, 0.6), 0 0 12px rgba(0, 255, 136, 0.4);
    }
  }
</style>
