<template>
  <BorderBox8 :dur="8">
    <div class="title">算力资源统计</div>
    <div class="dashboard-container">
      <!-- 左侧 CPU 使用仪表盘 -->
      <div class="dashboard" ref="cpuChart"></div>
      <!-- 右侧 Memory 使用仪表盘 -->
      <div class="dashboard" ref="memoryChart"></div>
    </div>
  </BorderBox8>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue';
  import * as echarts from 'echarts'; // 确保引入 ECharts
  import { BorderBox8 } from '@kjgl77/datav-vue3';
  import { getResourceData } from '../data';

  const resourceStats = ref({
    cpuUsage: 0,
    memoryUsage: 0,
  });

  const timer = ref<NodeJS.Timeout | null>(null);

  // ECharts 引用
  const cpuChart = ref<HTMLDivElement | null>(null);
  const memoryChart = ref<HTMLDivElement | null>(null);

  // 计算 CPU 和内存使用率的平均值
  function calculateAverageUsage(data: any[]): { cpuUsage: number; memoryUsage: number } {
    let totalCpuUsage = 0;
    let totalCpuTotal = 0;
    let totalMemoryUsed = 0;
    let totalMemoryTotal = 0;

    // 遍历每个节点的数据，计算总和
    data.forEach((item) => {
      totalCpuUsage += parseFloat(item.cpuUsage);
      totalCpuTotal += parseFloat(item.cpuTotal);
      totalMemoryUsed += parseFloat(item.memoryUsed);
      totalMemoryTotal += parseFloat(item.memoryTotal);
    });

    // 计算每个指标的平均值
    const avgCpuUsage = totalCpuUsage / data.length;
    const avgCpuTotal = totalCpuTotal / data.length;
    const avgMemoryUsed = totalMemoryUsed / data.length;
    const avgMemoryTotal = totalMemoryTotal / data.length;

    // 计算 CPU 使用率和内存使用率
    const cpuUsagePercentage = Math.round((avgCpuUsage / avgCpuTotal) * 100);
    const memoryUsagePercentage = Math.round((avgMemoryUsed / avgMemoryTotal) * 100);

    return {
      cpuUsage: cpuUsagePercentage,
      memoryUsage: memoryUsagePercentage,
    };
  }

  // 获取算力资源信息并更新图表
  async function fetchAndUpdateResourceData() {
    // 获取算力资源信息
    const response = await getResourceData();
    if (response) {
      // 获取 m1, m2, m3 三个节点的数据
      const data = response;
      //console.log(data);

      // 计算 CPU 和内存使用率的平均值
      const { cpuUsage, memoryUsage } = calculateAverageUsage(data);

      // 更新资源统计数据
      resourceStats.value.cpuUsage = cpuUsage;
      resourceStats.value.memoryUsage = memoryUsage;

      // ECharts 配置
      const cpuOptions = {
        tooltip: {
          formatter: '{a} <br/>{b} : {c}%',
        },
        series: [
          {
            name: 'CPU 使用率',
            type: 'gauge',
            radius: '80%',
            center: ['50%', '50%'],
            startAngle: 225,
            endAngle: -45,
            title: {
              show: true,
              offsetCenter: [0, '100%'],
              textStyle: {
                color: 'rgb(35, 245, 251)',
                fontSize: 20,
                fontWeight: 'bold',
              },
              formatter: 'CPU 使用率',
            },
            detail: {
              formatter: '{value}%',
              textStyle: {
                color: 'rgb(35, 245, 251)',
                fontSize: 20,
                fontWeight: 'bold',
              },
              offsetCenter: [0, '65%'],
            },
            data: [{ value: resourceStats.value.cpuUsage, name: 'CPU 使用率' }],
            axisLine: {
              lineStyle: {
                color: [
                  [0.2, '#0000ff'],
                  [0.5, '#00FF00'],
                  [0.8, '#FFFF00'],
                  [1, '#FF0000'],
                ],
                width: 10,
              },
            },
            axisLabel: {
              color: '#fff',
              fontSize: 10,
              fontWeight: 'bold',
            },
            splitLine: {
              length: 15,
              lineStyle: {
                color: '#fff',
                width: 3,
              },
            },
          },
        ],
      };

      const memoryOptions = {
        tooltip: {
          formatter: '{a} <br/>{b} : {c}%',
        },
        series: [
          {
            name: '内存使用率',
            type: 'gauge',
            radius: '80%',
            center: ['50%', '50%'],
            startAngle: 225,
            endAngle: -45,
            title: {
              show: true,
              offsetCenter: [0, '100%'],
              textStyle: {
                color: 'rgb(35, 245, 251)',
                fontSize: 20,
                fontWeight: 'bold',
              },
              formatter: '内存使用率',
            },
            detail: {
              formatter: '{value}%',
              textStyle: {
                color: 'rgb(35, 245, 251)',
                fontSize: 20,
                fontWeight: 'bold',
              },
              offsetCenter: [0, '65%'],
            },
            data: [{ value: resourceStats.value.memoryUsage, name: '内存使用率' }],
            axisLine: {
              lineStyle: {
                color: [
                  [0.2, '#0000ff'],
                  [0.5, '#00FF00'],
                  [0.8, '#FFFF00'],
                  [1, '#FF0000'],
                ],
                width: 10,
              },
            },
            axisLabel: {
              color: '#fff',
              fontSize: 10,
              fontWeight: 'bold',
            },
            splitLine: {
              length: 15,
              lineStyle: {
                color: '#fff',
                width: 3,
              },
            },
          },
        ],
      };

      // 初始化 ECharts 实例
      const cpuChartInstance = echarts.init(cpuChart.value);
      const memoryChartInstance = echarts.init(memoryChart.value);

      // 设置图表配置
      cpuChartInstance.setOption(cpuOptions);
      memoryChartInstance.setOption(memoryOptions);
    }
  }
  onMounted(() => {
    // 调用获取并更新资源数据的函数
    fetchAndUpdateResourceData();
    timer.value = setInterval(fetchAndUpdateResourceData, 10000);
  });
  onUnmounted(() => {
    if (timer.value) {
      clearInterval(timer.value);
      timer.value = null;
    }
  });
</script>

<style scoped lang="less">
  .title {
    left: 0.3%;
    top: 1%;
    position: relative;
    padding-left: 3vh;
    height: 3vh;
    line-height: 3vh;
    font-size: 2vh;
    font-weight: normal;
    color: rgba(35, 245, 251, 0.92);
    background-image: url('../../../../../assets/images/title-bg-long.png');
    background-repeat: no-repeat;
    background-size: 100% 100%;
  }

  .dashboard-container {
    display: flex;
    justify-content: space-between;
    margin-top: -3vh;
  }

  .dashboard {
    width: 60%;
    height: 30vh;
    margin-top: -0.5vh;
    border-radius: 1vh;
    padding: 2vh;
  }
</style>
