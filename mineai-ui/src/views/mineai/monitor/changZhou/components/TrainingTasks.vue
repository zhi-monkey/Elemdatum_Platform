<template>
  <div class="container">
    <ScrollList
      :data="taskData"
      :columns="columns"
      :row-config="rowConfig"
      :scroll-speed="1"
      :scroll-interval="30"
      :auto-scroll="true"
      :item-height="56"
    >
      <!-- 自定义状态列样式 -->
      <template #cell-status="{ value }">
        <span :class="statusClass(value.status)" :style="{ color: statusColor(value.status) }">
          {{ value.text }}
        </span>
      </template>
    </ScrollList>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue';
  import ScrollList from './ScrollList.vue';
  import { getModelJobWithUserList } from '../data';

  // 定义任务类型
  interface Task {
    id: number;
    name: string;
    status: { text: string; status: number };
    createTime: string;
    creator: string;
  }

  // 任务列表
  const taskData = ref<Task[]>([]);

  // 列配置
  const columns = computed(() => [
    {
      key: 'name',
      title: '任务名称',
      className: 'task-name',
      align: 'left',
      fontWeight: 'bold',
      width: 'minmax(140px, 1fr)',
    },
    {
      key: 'status',
      title: '任务状态',
      className: 'task-status',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(100px, 0.8fr)',
    },
    {
      key: 'createTime',
      title: '创建时间',
      className: 'create-time',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(140px, 1fr)',
    },
    {
      key: 'creator',
      title: '创建者',
      className: 'creator',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(100px, 0.8fr)',
    },
  ]);

  // 行配置
  const rowConfig = computed(() => ({
    className: 'task-item',
    oddRowBGC: '#102F5B', // 深蓝色，奇数行
    evenRowBGC: '#1E4069', // 稍浅的深蓝色，偶数行
  }));

  // 定义状态和对应的文本
  const statusMap: Record<string, string> = {
    '0': '创建成功',
    '1': '训练中',
    '-1': '训练暂停',
    '-10': '作业取消',
    '-2': '训练失败',
    '2': '训练成功',
    '3': '质检中',
    '-3': '质检失败',
    '4': '质检成功',
    '5': '转换中',
    '-5': '转换失败',
    '6': '转换成功',
    '7': '已发布',
  };

  // 根据状态设置颜色
  const statusColor = (status: number) => {
    switch (status) {
      case 0:
        return '#28A745';
      case 1:
        return '#FF9800';
      case -1:
        return '#B0BEC5';
      case -10:
        return '#757575';
      case -2:
        return '#F43404';
      case 2:
        return '#4CAF50';
      case 3:
        return '#FFEB3B';
      case -3:
        return '#D32F2F';
      case 4:
        return '#8BC34A';
      case 5:
        return '#FF5722';
      case -5:
        return '#C2185B';
      case 6:
        return '#673AB7';
      case 7:
        return '#2196F3';
      default:
        return '#607D8B';
    }
  };

  // 根据状态设置样式类
  const statusClass = (status: number) => {
    return 'status-badge';
  };

  // 格式化时间
  const formatDate = (date: string) => {
    const d = new Date(date);
    return d.toLocaleString();
  };

  // 获取任务数据
  const fetchJobList = async () => {
    const response = await getModelJobWithUserList({});
    if (response) {
      const filteredResponse = response.filter((job: any) => job.isDelete === false);
      taskData.value = filteredResponse.map((job: any, index) => ({
        id: index + 1,
        name: job.modelJob.name,
        status: {
          text: statusMap[job.modelJob.status],
          status: job.modelJob.status,
        },
        createTime: formatDate(job.modelJob.createTime),
        creator: job.userName || '无',
      }));
    }
  };

  onMounted(() => {
    fetchJobList();
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

  .task-name {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  .status-badge {
    font-weight: 500;
  }

  .create-time {
    font-family: 'Courier New', monospace;
    color: rgba(160, 160, 192, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }

  .creator {
    color: rgba(208, 208, 224, 0.9) !important;
    background: rgba(255, 255, 255, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    backdrop-filter: blur(2px);
    -webkit-backdrop-filter: blur(2px);
  }
</style>
