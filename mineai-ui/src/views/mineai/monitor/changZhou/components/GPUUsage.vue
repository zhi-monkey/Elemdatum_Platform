<template>
  <BorderBox8 :dur="8">
    <div class="title">GPU利用率</div>
    <div ref="gpuChart" class="gpu-chart"></div>
  </BorderBox8>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue';
  import { BorderBox8 } from '@kjgl77/datav-vue3';
  import { useECharts } from '/@/hooks/web/useECharts';
  import * as echarts from 'echarts';
  import { getRecentGpuUsage } from '/@/views/mineai/monitor/changZhou/data';

  // 数据处理
  const gpuChart = ref<HTMLDivElement | null>(null);

  // 获取GPU使用率数据并更新图表
  const fetchGpuUsageData = async () => {
    try {
      const response = await getRecentGpuUsage(); // 调用后端API获取数据
      const data = response; // 从响应中获取数据
      //console.log(data);

      // 提取时间戳并格式化为 HH:mm
      const times = data[0].recentUsage.map((item) => {
        const date = new Date(parseInt(item.timeStamp) * 1000); // 时间戳转为时间对象
        //console.log(date);
        return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`; // 格式化为 HH:mm
      });
      //console.log(times);

      // 处理每个GPU的使用率数据
      const gpuData = data.map((gpu) => ({
        name: `${gpu.modelName} (${gpu.gpuIndex})`, // GPU的名称及索引
        data: gpu.recentUsage.map((usage) => {
          const usageValue = parseFloat(usage.usage); // 获取使用率
          return usageValue === 0 ? 0 : usageValue; // 如果是0，默认值为0
        }),
      }));
      //console.log(gpuData);

      // 如果GPU图表容器存在，初始化并设置图表
      if (gpuChart.value) {
        const chart = echarts.init(gpuChart.value);

        const option = {
          title: {
            text: 'GPU利用率(近30分钟)',
            left: 'center',
            top: '-5vh',
            fontSize: 16,
            textStyle: {
              color: '#fff', // 标题颜色
            },
          },
          tooltip: {
            trigger: 'axis', // 鼠标悬停时触发的提示框
            axisPointer: { type: 'cross' }, // 十字型指针
            formatter: (params: any) => {
              // 自定义工具提示，显示百分比
              const { seriesName, data } = params[0];
              return `${seriesName}: ${data}%`;
            },
          },
          legend: {
            data: gpuData.map((item) => item.name), // 图例数据
            textStyle: { color: '#fff' }, // 图例文本颜色
            top: '30vh',
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: times, // X轴数据：时间
            axisLabel: {
              color: '#fff', // X轴标签颜色
              interval: 0, // 强制显示所有标签
            },
            axisLine: {
              lineStyle: { color: '#fff' }, // X轴线颜色
            },
          },
          yAxis: {
            type: 'value',
            axisLabel: {
              color: '#fff', // Y轴标签颜色
              formatter: '{value}%', // Y轴标签格式化，显示百分比
            },
            axisLine: {
              lineStyle: { color: '#fff' }, // Y轴线颜色
            },
            splitLine: {
              //show: false,
              lineStyle: { color: '#ccc' }, // Y轴分割线颜色
            },
          },
          series: gpuData.map((item) => ({
            name: item.name, // 每个GPU的名称
            type: 'line', // 折线图
            //stack: '总量', // 堆叠模式
            //areaStyle: {}, // 填充颜色
            data: item.data, // Y轴数据：每个GPU的使用率
          })),
        };

        // 设置图表的配置项
        chart.setOption(option);
      }
    } catch (error) {
      console.error('获取GPU使用率数据失败:', error); // 错误处理
    }
  };

  // 计算下一个更新的时间点
  const calculateNextUpdateTime = () => {
    const now = new Date();
    const nextUpdateTime = new Date(now.getTime() + (3 - (now.getMinutes() % 3)) * 60000); // 下一个 3 分钟时间点
    nextUpdateTime.setSeconds(0); // 设为整秒

    // 计算剩余时间
    const remainingTime = nextUpdateTime.getTime() - now.getTime();
    return remainingTime;
  };

  // 自动刷新GPU数据，每3分钟更新一次
  const startAutoUpdate = () => {
    // 计算到下一个整3分钟的剩余时间
    const remainingTime = calculateNextUpdateTime();

    // 设置初次更新定时器
    setTimeout(() => {
      fetchGpuUsageData(); // 调用函数获取并展示数据

      // 设置定时器，每3分钟更新一次
      setInterval(fetchGpuUsageData, 180000); // 每隔 3 分钟更新一次数据
    }, remainingTime);
  };
  // 组件挂载时调用数据获取函数
  onMounted(() => {
    fetchGpuUsageData(); // 调用函数获取并展示数据
    startAutoUpdate(); // 启动自动更新
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
    background-image: url('../../../../../assets/images/title-bg.png');
    background-repeat: no-repeat;
    background-size: 100% 100%;
  }

  .gpu-chart {
    // background-color: #1f2d3d;
    width: 100%;
    border-radius: 8px;
    padding: 1vh;
    margin-top: -2vh;
    bottom: -3vh;
    height: 26vh;
    //box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }
</style>
