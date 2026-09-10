<template>
  <div style="margin-top: 50px">
    <a-tabs
      tab-position="left"
      v-model:activeKey="activeKey"
      size="large"
      :tabBarGutter="70"
      :tabBarStyle="{ fontSize: '132px' }"
    >
      <a-tab-pane key="1" tab="本地上传图片测试">
        <TestByLocalUpload />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { TabPane as ATabPane, Tabs as ATabs } from 'ant-design-vue';
  import TestByLocalUpload from './TestByLocalUpload.vue';

  const activeKey = ref('1');
</script>
