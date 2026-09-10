<template>
  <div class="gpu-usage-container">
    <!-- 切换按钮 -->
    <div class="chart-header">
      <div class="metric-switch">
        <el-radio-group v-model="currentMetric" @change="handleMetricChange">
          <el-radio-button label="memory">显存使用情况</el-radio-button>
          <el-radio-button label="core">算力使用情况</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div ref="lineChart" class="chart-container"></div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue';
  import * as echarts from 'echarts';
  import { getRecentGpuMetrics } from '../data';
  import { ElRadioGroup, ElRadioButton } from 'element-plus';

  const lineChart = ref<HTMLDivElement | null>(null);
  let chartInstance: echarts.ECharts | null = null;

  // 当前显示的指标类型
  const currentMetric = ref<'memory' | 'core'>('memory');
  const timePoints = ref<string[]>([]);
  const seriesData = ref<any[]>([]);

  const COLOR_PALETTE = [
    '#5470c6', // 蓝色
    '#91cc75', // 绿色
    '#fac858', // 黄色
    '#ee6666', // 红色
    '#73c0de', // 浅蓝
    '#3ba272', // 深绿
    '#fc8452', // 橙色
    '#9a60b4', // 紫色
    '#ea7ccc', // 粉色
    '#60c0dd', // 青蓝
  ];
  // 指标配置
  const metricConfig = {
    memory: {
      key: 'gpuMemoryPercent',
      name: '显存利用率',
      unit: '%',
      color: '#5470c6', // 显存使用蓝色
    },
    core: {
      key: 'gpuCorePercent',
      name: '核心利用率',
      unit: '%',
      color: '#91cc75', // 核心使用绿色
    },
  } as const;

  const fetchGpuMetrics = async () => {
    try {
      const response = await getRecentGpuMetrics({ minutes: 30 });
      let list = Array.isArray(response) ? [...response] : [];

      if (!Array.isArray(list) || list.length === 0) {
        timePoints.value = [];
        seriesData.value = [];
        return;
      }

      // 统一时间轴
      const allTimestamps = Array.from(
        new Set(list.map((m: any) => m.timestamp).filter((ts: string) => !!ts)),
      ).sort();

      const formatTime = (ts: string) => {
        const d = new Date(ts);
        if (Number.isNaN(d.getTime())) return ts;
        return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      };

      timePoints.value = allTimestamps.map((ts) => formatTime(ts as string));

      // 按 GPU 维度分组：nodeName + gpuIndex 作为 key
      const gpuMap: Record<
        string,
        {
          name: string;
          memoryData: Record<string, number>;
          coreData: Record<string, number>;
        }
      > = {};

      list.forEach((m: any) => {
        const key = `${m.nodeName}-GPU${m.gpuIndex}`;
        const displayName = m.gpuName ? `${m.nodeName}-GPU${m.gpuIndex}-${m.gpuName}` : key;

        if (!gpuMap[key]) {
          gpuMap[key] = {
            name: displayName,
            memoryData: {},
            coreData: {},
          };
        }

        // 存储两种指标数据
        const memoryPercent = typeof m.gpuMemoryPercent === 'number' ? m.gpuMemoryPercent : 0;
        const corePercent = typeof m.gpuCorePercent === 'number' ? m.gpuCorePercent : 0;

        gpuMap[key].memoryData[m.timestamp] = Number(
          memoryPercent.toFixed ? memoryPercent.toFixed(1) : memoryPercent,
        );
        gpuMap[key].coreData[m.timestamp] = Number(
          corePercent.toFixed ? corePercent.toFixed(1) : corePercent,
        );
      });

      // 根据当前指标更新系列数据
      updateSeriesData(gpuMap, allTimestamps);
    } catch (error) {
      console.error('获取 GPU 利用率数据失败', error);
      timePoints.value = [];
      seriesData.value = [];
    }
  };

  // 更新系列数据
  const updateSeriesData = (
    gpuMap: Record<
      string,
      {
        name: string;
        memoryData: Record<string, number>;
        coreData: Record<string, number>;
      }
    >,
    timestamps: string[],
  ) => {
    const gpuKeys = Object.keys(gpuMap);

    seriesData.value = gpuKeys.map((key, index) => {
      const gpu = gpuMap[key];
      const dataSource = currentMetric.value === 'memory' ? gpu.memoryData : gpu.coreData;
      const points = timestamps.map((ts) => dataSource[ts] ?? 0);

      // 使用调色板颜色，循环使用
      const colorIndex = index % COLOR_PALETTE.length;
      const color = COLOR_PALETTE[colorIndex];

      return {
        name: gpu.name,
        type: 'line',
        smooth: true,
        data: points,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: {
          width: 2,
          color: color,
        },
        itemStyle: {
          color: color,
        },
        // 添加区域阴影（可选）
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: color + '40' },
            { offset: 1, color: color + '05' },
          ]),
        },
      };
    });
  };

  const initChart = () => {
    if (!lineChart.value) return;

    if (!chartInstance) {
      chartInstance = echarts.init(lineChart.value);
    }

    const currentConfig = metricConfig[currentMetric.value];

    const option = {
      tooltip: {
        trigger: 'axis',
        appendToBody: true,
        axisPointer: {
          type: 'cross',
        },
        formatter: (params: any) => {
          let tooltip = `<div style="font-size: 12px; color: #666;">${params[0].axisValue}</div>`;
          params.forEach((item: any) => {
            tooltip += `<div style="display: flex; align-items: center; margin-top: 4px;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${item.color}; margin-right: 8px; border-radius: 2px;"></span>
            <span>${item.seriesName}: ${item.value}%</span>
          </div>`;
          });
          return tooltip;
        },
      },
      legend: {
        type: 'scroll',
        data: seriesData.value.map((s) => s.name),
        textStyle: {
          color: '#fff',
          fontSize: 12,
        },
        top: 8,
        left: 'center',
        width: '90%',
        itemGap: 10,
        itemWidth: 14,
        itemHeight: 8,
        pageIconSize: 10,
        pageButtonItemGap: 3,
        pageTextStyle: {
          color: '#fff',
        },
      },
      grid: {
        left: '8%',
        right: '10%',
        top: '24%',
        bottom: '5%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: timePoints.value,
        axisLine: {
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.3)',
          },
        },
        axisLabel: {
          color: '#fff',
          fontSize: 10,
          rotate: 45,
          interval: 'auto',
        },
        axisTick: {
          show: false,
        },
      },
      yAxis: {
        type: 'value',
        name: `${currentConfig.name} (%)`,
        nameTextStyle: {
          color: '#fff',
          fontSize: 12,
        },
        min: 0,
        max: 100,
        axisLine: {
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.3)',
          },
        },
        axisLabel: {
          color: '#fff',
          fontSize: 11,
          formatter: '{value}%',
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.1)',
          },
        },
      },
      series: seriesData.value,
    };

    chartInstance.setOption(option);
  };

  // 指标切换处理
  const handleMetricChange = () => {
    // 重新获取数据或重新处理数据
    // 这里我们假设已经获取了所有数据，只需要更新显示
    refreshData();
  };

  // 刷新数据并更新图表
  const refreshData = async () => {
    await fetchGpuMetrics();
    if (chartInstance) {
      const currentConfig = metricConfig[currentMetric.value];

      const option = {
        legend: {
          data: seriesData.value.map((s) => s.name),
        },
        xAxis: {
          data: timePoints.value,
        },
        yAxis: {
          name: `${currentConfig.name} (%)`,
        },
        series: seriesData.value,
        color: [currentConfig.color],
      };
      chartInstance.setOption(option);
    }
  };

  const handleResize = () => {
    if (chartInstance) {
      chartInstance.resize();
    }
  };

  // 监听刷新事件
  const handleRefresh = () => {
    refreshData();
  };

  onMounted(async () => {
    await fetchGpuMetrics();
    initChart();
    window.addEventListener('resize', handleResize);
    window.addEventListener('overview-refresh-gpu-usage', handleRefresh);
  });

  onUnmounted(() => {
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    window.removeEventListener('resize', handleResize);
    window.removeEventListener('overview-refresh-gpu-usage', handleRefresh);
  });
</script>

<style scoped lang="less">
  .gpu-usage-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  .chart-header {
    padding: 4px 0;
    margin-bottom: 4px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .metric-switch {
      display: flex;
      justify-content: center;
      margin-bottom: 8px;

      :deep(.el-radio-group) {
        .el-radio-button {
          &__inner {
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.2);
            color: rgba(255, 255, 255, 0.7);
            transition: all 0.3s;
            padding: 4px 12px;
            font-size: 12px;
            line-height: 1.4;

            &:hover {
              background: rgba(255, 255, 255, 0.15);
              color: #fff;
            }
          }

          &.is-active {
            .el-radio-button__inner {
              background: #409eff;
              border-color: #409eff;
              color: #fff;
              box-shadow: none;
            }
          }
        }
      }
    }
  }

  .chart-container {
    width: 100%;
    height: 100%;
    flex: 1;
    min-height: 0;
    position: relative;
    overflow: hidden;
  }
</style>
