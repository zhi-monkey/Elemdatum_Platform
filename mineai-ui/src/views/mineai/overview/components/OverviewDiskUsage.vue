<template>
  <div class="disk-usage-container">
    <div class="component-title">磁盘用量</div>
    <div ref="gaugeChart" class="chart-container"></div>
    <div class="stats-info">
      <div class="stat-row">
        <span class="stat-label">总量</span>
        <span class="stat-value">{{ formatBytes(totalDisk) }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-label">可用</span>
        <span class="stat-value available">{{ formatBytes(availableDisk) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import * as echarts from 'echarts';
  import { getClusterDiskSummary } from '../data';

  const gaugeChart = ref<HTMLDivElement | null>(null);
  let chartInstance: echarts.ECharts | null = null;

  // 使用后端真实数据：集群磁盘总量 / 已用 / 使用率
  const totalDisk = ref(0); // 磁盘总量（bytes）
  const usedDisk = ref(0); // 已用磁盘（bytes）
  const availableDisk = computed(() => totalDisk.value - usedDisk.value);
  const usagePercentage = ref(0); // 磁盘使用率

  const formatBytes = (bytes: number) => {
    const tb = bytes / (1024 * 1024 * 1024 * 1024);
    if (tb >= 1) {
      return `${tb.toFixed(2)} TB`;
    }
    const gb = bytes / (1024 * 1024 * 1024);
    return `${gb.toFixed(1)} GB`;
  };

  const initChart = () => {
    if (!gaugeChart.value) return;

    if (!chartInstance) {
      chartInstance = echarts.init(gaugeChart.value);
    }

    const option = {
      series: [
        {
          name: '磁盘占用率',
          type: 'gauge',
          radius: '95%',
          startAngle: 200,
          endAngle: -20,
          center: ['50%', '55%'],
          min: 0,
          max: 100,
          splitNumber: 10,
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [usagePercentage.value / 100, '#ff9800'],
                [1, 'rgba(255, 255, 255, 0.1)'],
              ],
            },
          },
          pointer: {
            itemStyle: {
              color: '#ff9800',
            },
          },
          axisTick: {
            show: false,
          },
          splitLine: {
            show: false,
          },
          axisLabel: {
            show: false,
          },
          detail: {
            valueAnimation: true,
            fontSize: 18,
            fontWeight: 'bold',
            color: '#fff',
            formatter: '{value}%',
            offsetCenter: [0, '20%'],
          },
          data: [
            {
              value: Number(usagePercentage.value.toFixed(1)),
            },
          ],
        },
      ],
    };

    chartInstance.setOption(option);
  };

  const handleResize = () => {
    if (chartInstance) {
      chartInstance.resize();
    }
  };

  const fetchDiskSummary = async () => {
    try {
      const res = await getClusterDiskSummary();
      if (res) {
        const total = res.totalDisk ?? 0;
        const used = res.usedDisk ?? 0;
        const percent = res.diskPercent ?? 0;
        totalDisk.value = total;
        usedDisk.value = used;
        usagePercentage.value = typeof percent === 'number' ? percent : 0;
      } else {
        totalDisk.value = 0;
        usedDisk.value = 0;
        usagePercentage.value = 0;
      }
    } catch (e) {
      console.error('获取集群磁盘汇总失败', e);
      totalDisk.value = 0;
      usedDisk.value = 0;
      usagePercentage.value = 0;
    }
  };

  // 刷新数据并更新图表
  const refreshData = async () => {
    await fetchDiskSummary();
    if (chartInstance) {
      const option = {
        series: [
          {
            axisLine: {
              lineStyle: {
                color: [
                  [usagePercentage.value / 100, '#ff9800'],
                  [1, 'rgba(255, 255, 255, 0.1)'],
                ],
              },
            },
            data: [
              {
                value: Number(usagePercentage.value.toFixed(1)),
              },
            ],
          },
        ],
      };
      chartInstance.setOption(option);
    }
  };

  // 监听刷新事件
  const handleRefresh = () => {
    refreshData();
  };

  onMounted(async () => {
    await fetchDiskSummary();
    initChart();
    window.addEventListener('resize', handleResize);
    window.addEventListener('overview-refresh-disk-usage', handleRefresh);
  });

  onUnmounted(() => {
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    window.removeEventListener('resize', handleResize);
    window.removeEventListener('overview-refresh-disk-usage', handleRefresh);
  });
</script>

<style scoped lang="less">
  .disk-usage-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 5px;
    position: relative;
  }

  .component-title {
    font-size: 14px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.9);
    text-align: center;
    margin-bottom: 8px;
    text-shadow: 0 0 6px rgba(255, 152, 0, 0.5);
    white-space: nowrap;
    flex-shrink: 0;
    position: absolute;
    transform: translateY(13px);
  }

  .chart-container {
    width: 100%;
    flex: 1;
    max-height: 100%;
    margin-top: 5px;
  }

  .stats-info {
    display: flex;
    flex-direction: row;
    gap: 16px;
    width: 100%;
    padding: 0 10px;
    justify-content: center;
    position: absolute;
    bottom: 18%;
    left: 0;
    right: 0;
    transform: translateY(0);
  }

  .stat-row {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 6px;
    font-size: 12px;
  }

  .stat-label {
    color: rgba(255, 255, 255, 0.7);
    font-size: 11px;
    font-weight: 500;
    white-space: nowrap;
  }

  .stat-value {
    color: #fff;
    font-weight: 600;
    font-size: 11px;
    white-space: nowrap;

    &.used {
      color: #ff9800;
    }

    &.available {
      color: #67c23a;
    }
  }
</style>
