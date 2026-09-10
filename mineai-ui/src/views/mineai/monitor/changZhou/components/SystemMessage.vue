<template>
  <div class="container">
    <ScrollList
      :data="systemInfo"
      :columns="columns"
      :row-config="rowConfig"
      :scroll-speed="1"
      :scroll-interval="30"
      :auto-scroll="true"
      :item-height="56"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, reactive } from 'vue';
  import ScrollList from './ScrollList.vue';
  import { getModelApplicationList, getAllPublicDatasets } from '../data';
  import { dayjs } from 'element-plus';

  // 定义系统信息类型
  interface SystemInfo {
    time: string;
    info: string;
  }

  // 系统信息列表
  const systemInfo = ref<SystemInfo[]>([]);

  // 列配置
  const columns = computed(() => [
    {
      key: 'time',
      title: '时间',
      className: 'time-column',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(140px, 1fr)',
    },
    {
      key: 'info',
      title: '信息详情',
      className: 'info-column',
      align: 'left',
      fontWeight: 'normal',
      width: 'minmax(180px, 2fr)',
    },
  ]);

  // 行配置
  const rowConfig = computed(() => ({
    className: 'info-item',
    oddRowBGC: '#102F5B', // 深蓝色，奇数行
    evenRowBGC: '#1E4069', // 稍浅的深蓝色，偶数行
  }));

  // 获取系统信息
  const fetchSystemInfo = async () => {
    try {
      // 获取公开数据集列表
      const datasetRes = await getAllPublicDatasets({});

      // 获取应用任务列表
      const appRes = await getModelApplicationList({});

      // 合并数据集和应用任务信息
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

      // 合并数据集和应用任务
      const combinedTasks = [...datasetTasks, ...applicationTasks];

      // 按时间排序
      combinedTasks.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());

      // 限制显示的条数
      systemInfo.value = combinedTasks.slice(0, 5);
    } catch (error) {
      console.error('获取数据失败:', error);
    }
  };

  onMounted(() => {
    fetchSystemInfo(); // 组件挂载时获取数据
  });
</script>

<style scoped lang="less">
  .container {
    width: 100%;
    height: 100%;
    position: relative;
  }

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
    margin-bottom: 10px;
  }

  .time-column {
    font-family: 'Courier New', monospace;
    color: rgba(160, 160, 192, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }

  .info-column {
    color: rgba(208, 208, 224, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }
</style>
