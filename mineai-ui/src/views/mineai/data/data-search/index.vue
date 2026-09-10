<template>
  <div class="data-search">
    <Tabs :active-key="activeKey" @change="handleTabChange">
      <tab-pane key="image" tab="图像数据" />
      <tab-pane key="video" tab="视频数据" />
      <tab-pane key="pointcloud" tab="点云数据" />
      <tab-pane key="export" tab="导出任务" />
    </Tabs>
    <component :is="content" @switch-tab="handleTabChange" />
  </div>
</template>

<script setup lang="ts">
  import { ref, shallowRef } from 'vue';
  import { Tabs, TabPane } from 'ant-design-vue';
  import ImageSearch from './image-search.vue';
  import VideoSearch from './video-search.vue';
  import PointcloudSearch from './pointcloud-search.vue';
  import ExportTask from './export-task.vue';

  const content = shallowRef(ImageSearch);
  const activeKey = ref('image');

  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (key) {
      case 'image':
        content.value = ImageSearch;
        break;
      case 'video':
        content.value = VideoSearch;
        break;
      case 'pointcloud':
        content.value = PointcloudSearch;
        break;
      case 'export':
        content.value = ExportTask;
        break;
      default:
        break;
    }
  };
</script>
