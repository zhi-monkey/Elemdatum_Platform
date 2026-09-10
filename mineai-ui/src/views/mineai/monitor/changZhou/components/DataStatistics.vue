<template>
  <div ref="barChart" class="chart-container"></div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue';
  import { BorderBox8 } from '@kjgl77/datav-vue3';
  import * as echarts from 'echarts';
  import {
    getAllPublicDatasets,
    getLabelGroupList,
    getModelApplicationList,
    getModelJobList,
  } from '../data';

  // 数据处理
  const ModelJobCount = ref(0);
  const ModelApplicationCount = ref(0);
  const PublicDatasetCount = ref(0);
  const labelGroupCount = ref(0);

  const barChart = ref(null);

  // 获取标签组总数并更新
  const fetchLabelGroupCount = async () => {
    try {
      const response = await getLabelGroupList({});
      labelGroupCount.value = response.length; // 获取标签组的数量
    } catch (error) {
      console.error('获取标签组失败', error);
      labelGroupCount.value = 0; // 如果请求失败，保持为 0
    }
  };
  //获取训练任务总数
  const fetchGetModelJobList = async () => {
    try {
      const response = await getModelJobList({});
      ModelJobCount.value = response.length;
    } catch (error) {
      console.error('获取训练任务总数失败', error);
      ModelJobCount.value = 0;
    }
  };

  //获取数据集总数
  const fetchGetAllPublicDatasets = async () => {
    try {
      const response = await getAllPublicDatasets({});
      PublicDatasetCount.value = response.result.length;
    } catch (error) {
      console.error('获取数据集总数失败', error);
      PublicDatasetCount.value = 0;
    }
  };
  //获取应用任务总数
  const fetchGetModelApplicationList = async () => {
    try {
      const response = await getModelApplicationList({});
      //console.log('应用任务数据', response);
      ModelApplicationCount.value = response.totalElements;
    } catch (error) {
      console.error('获取应用任务总数失败', error);
      ModelApplicationCount.value = 0;
    }
  };
  // 初始化图表
  const initChart = () => {
    const myChart = echarts.init(barChart.value);

    // 深蓝色到浅蓝色的渐变颜色数组
    const colors = [
      /*'#2AaFCE', // 深蓝色
      '#A3D8FF', // 浅蓝色
      '#7EC8FF', // 温和蓝色
      '#4F9AF1', // 中蓝色*/
      '#FFB6C1', // 浅粉色
      '#FFEB8A', // 浅黄色
      '#90EE90', // 浅绿色
      '#ADD8E6', // 浅蓝色
    ];
    const option = {
      title: {
        text: '数据统计',
        left: 'center',
        top: '10%',
        textStyle: {
          fontSize: 16,
          color: '#fff',
        },
      },
      tooltip: {
        trigger: 'item',
      },
      xAxis: {
        type: 'category',
        data: ['训练任务总数', '应用任务总数', '数据集总数', '标签组总数'],
        axisLine: {
          show: false, // 去掉纵轴的横线
          lineStyle: {
            color: '#fff',
          },
        },
        axisLabel: {
          color: '#fff',
        },
        axisTick: {
          show: false, // 去掉纵轴的刻度线
        },
        /* splitLine: {
          show: false, // 去掉网格线（竖线）
        },*/
      },
      yAxis: {
        type: 'value',
        axisLine: {
          show: false, // 去掉纵轴的横线
          lineStyle: {
            color: '#fff',
          },
        },
        axisLabel: {
          color: '#fff',
        },
        axisTick: {
          show: false, // 去掉纵轴的刻度线
        },
        // splitLine: {
        //   show: false, // 去掉网格线（竖线）
        // },
      },
      series: [
        {
          data: [
            ModelJobCount.value,
            ModelApplicationCount.value,
            PublicDatasetCount.value,
            labelGroupCount.value,
          ], // 使用实际的标签组总数
          type: 'bar',
          barWidth: '50%',
          itemStyle: {
            // 为每个柱状条设置不同的颜色
            color: (params) => {
              // 返回颜色数组中的不同颜色
              return colors[params.dataIndex % colors.length];
            },
          },
          label: {
            show: true, // 显示数值
            position: 'top', // 数值显示在柱子顶部
            color: '#fff', // 设置数值颜色为白色
            fontSize: 16, // 设置数值字体大小
          },
        },
      ],
    };

    myChart.setOption(option);
  };

  onMounted(async () => {
    // 等待所有异步请求完成
    await Promise.all([
      fetchGetModelApplicationList(),
      fetchLabelGroupCount(),
      fetchGetModelJobList(),
      fetchGetAllPublicDatasets(),
    ]);

    // 数据加载完成后，初始化图表
    initChart();
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

  /* 添加ECharts容器样式 */
  .chart-container {
    width: 100%;
    height: 110%; /* 可以根据需要调整高度 */
    margin-top: -2vh; /* 设置标题与图表之间的间隔 */
    //background-color: #34495e;
    border-radius: 10px;
  }

  //@media (max-width: 22560px) {
  //  .title {
  //    font-size: 16px;
  //  }
  //}
</style>
