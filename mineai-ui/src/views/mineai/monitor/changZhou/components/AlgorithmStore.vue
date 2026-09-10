<template>
  <div class="container">
    <ScrollList
      :data="datasets"
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
  import { ref, computed, onMounted } from 'vue';
  import ScrollList from './ScrollList.vue';
  import { getModelApplicationListForReal } from '../data';
  import { dayjs } from 'element-plus';

  // 定义数据类型
  interface Application {
    id: number;
    applicationName: string;
    deviceName: string;
    taskCount: number;
    updateTime: string;
  }

  // 数据列表
  const datasets = ref<Application[]>([]);

  // 列配置
  const columns = computed(() => [
    {
      key: 'applicationName',
      title: '算法应用名称',
      className: 'application-name',
      align: 'left',
      fontWeight: 'bold',
      width: 'minmax(120px, 1fr)',
    },
    {
      key: 'deviceName',
      title: '设备名称',
      className: 'device-name',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(100px, 0.8fr)',
    },
    {
      key: 'taskCount',
      title: '已发布任务数',
      className: 'task-count',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(110px, 0.8fr)',
    },
    {
      key: 'updateTime',
      title: '更新时间',
      className: 'update-time',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(150px, 1.2fr)',
    },
  ]);

  // 行配置
  const rowConfig = computed(() => ({
    className: 'application-item',
    oddRowBGC: '#102F5B', // 深蓝色，奇数行
    evenRowBGC: '#1E4069', // 稍浅的深蓝色，偶数行
  }));

  // 获取数据
  const fetchData = async () => {
    try {
      const res = await getModelApplicationListForReal({});
      const publishedTasks = res.content.filter((item) => item.isReleased === '已发布');
      const aggregatedData = publishedTasks.reduce((acc, item) => {
        const applicationName = item.applicationName.applicationName;
        const deviceName = item.device.deviceName;
        const updateTime = item.updateTime;

        if (!acc[applicationName]) {
          acc[applicationName] = {
            applicationName,
            deviceName,
            taskCount: 0,
            latestUpdateTime: updateTime,
          };
        }
        acc[applicationName].taskCount += 1;
        if (new Date(updateTime) > new Date(acc[applicationName].latestUpdateTime)) {
          acc[applicationName].latestUpdateTime = updateTime;
        }

        return acc;
      }, {});

      datasets.value = Object.values(aggregatedData).map((item, index) => ({
        id: index + 1,
        applicationName: item.applicationName,
        deviceName: item.deviceName,
        taskCount: item.taskCount || 0,
        updateTime: dayjs(item.latestUpdateTime).format('YYYY-MM-DD HH:mm:ss'),
      }));
    } catch (error) {
      console.error('获取算法应用数据失败:', error);
    }
  };

  onMounted(() => {
    fetchData();
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
    background-image: url('../../../../../assets/images/title-bg-long.png');
    background-repeat: no-repeat;
    background-size: 100% 100%;
    margin-bottom: 10px;
  }

  .application-name {
    color: rgba(255, 255, 255, 0.9) !important;
    font-weight: 500;
  }

  .device-name {
    color: rgba(208, 208, 224, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }

  .task-count {
    color: rgba(160, 160, 192, 0.9) !important;
    font-weight: 500;
  }

  .update-time {
    font-family: 'Courier New', monospace;
    color: rgba(160, 160, 192, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }
</style>
