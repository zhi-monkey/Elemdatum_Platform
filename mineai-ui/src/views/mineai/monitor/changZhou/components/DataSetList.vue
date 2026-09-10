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
    >
      <!-- 自定义状态列样式 -->
      <template #cell-status="{ value }">
        <span :class="statusClass(value)">{{ value }}</span>
      </template>
    </ScrollList>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue';
  import ScrollList from './ScrollList.vue';
  import { getAllPublicDatasets } from '../data';
  import { dayjs } from 'element-plus';

  // 定义数据集类型
  interface Dataset {
    id: number;
    name: string;
    creatorName: string;
    createTime: string;
    status: string;
  }

  // 数据集列表
  const datasets = ref<Dataset[]>([]);

  // 列配置
  const columns = computed(() => [
    {
      key: 'id',
      title: '序号',
      className: 'dataset-id',
      align: 'center',
      fontWeight: 'bold',
      width: 'minmax(60px, 1fr)',
    },
    {
      key: 'name',
      title: '数据集名称',
      className: 'dataset-name',
      align: 'left',
      fontWeight: 'bold',
      width: 'minmax(150px, 1fr)',
    },
    {
      key: 'creatorName',
      title: '发布者',
      className: 'creator',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(100px, 1fr)',
    },
    {
      key: 'createTime',
      title: '创建时间',
      className: 'create-time',
      align: 'center',
      fontWeight: 'normal',
      width: 'minmax(200px, 1fr)',
    },
  ]);

  // 行配置
  const rowConfig = computed(() => ({
    className: 'dataset-item',
    oddRowBGC: '#102F5B', // 深蓝色，奇数行
    evenRowBGC: '#1E4069', // 稍浅的深蓝色，偶数行
  }));

  // 获取数据集列表
  const fetchDatasetList = async () => {
    try {
      const response = await getAllPublicDatasets({});
      if (response) {
        datasets.value = response.result.map((dataset, index) => ({
          id: index + 1,
          name: dataset.name,
          creatorName: dataset.creatorName || '无',
          createTime: dayjs(dataset.createTime).format('YYYY-MM-DD HH:mm:ss'),
          status: '已发布', // 假设所有公共数据集都是已发布状态
        }));
      }
    } catch (error) {
      console.error('获取数据集失败:', error);
    }
  };

  // 根据状态设置样式类
  const statusClass = (status: string) => {
    switch (status) {
      case '已发布':
        return 'published';
      default:
        return '';
    }
  };

  onMounted(() => {
    fetchDatasetList(); // 组件挂载时获取数据集
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

  .dataset-id {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  .dataset-name {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  // 状态样式 - 透明毛玻璃效果
  .published {
    font-weight: 500;
    padding: 4px 8px;
    border-radius: 6px;
    backdrop-filter: blur(4px);
    -webkit-backdrop-filter: blur(4px);
    color: #2196f3 !important;
    background: rgba(33, 150, 243, 0.2);
    border: 1px solid rgba(33, 150, 243, 0.3);
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
