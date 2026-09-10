<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px">
    <template #title>数据源管理</template>
    <div>
      <component :is="content" />
    </div>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <!-- 现在暂时不用摄像头视频流，隐藏该选项及对应界面。 -->
        <TabPane key="2" tab="数据收集服务器" />
        <TabPane key="3" tab="分析服务器" />
      </Tabs>
    </template>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, shallowRef } from 'vue';
  import { useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { Tabs, TabPane } from 'ant-design-vue';
  import HttpCameraServerPage from './http-camera-server/index.vue';
  import GpuUrlTargetPage from './gpu-url-target/index.vue';

  const router = useRouter();
  // 现在暂时不用摄像头视频流，默认展示数据收集服务器管理界面。
  const activeKey = ref('2');
  const content = shallowRef(HttpCameraServerPage);

  const goBack = () => {
    router.go(-1);
  };

  const handleTabChange = (key: string) => {
    activeKey.value = key;
    switch (key) {
      case '2':
        content.value = HttpCameraServerPage;
        break;
      case '3':
        content.value = GpuUrlTargetPage;
        break;
    }
  };
</script>
