<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <template #title> 算法管理 </template>
    <div>
      <component :is="content" ref="tabContent" />
    </div>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <tab-pane key="1" tab="算法列表" />
<!--        <tab-pane key="2" tab="发布算法列表" />-->
      </Tabs>
    </template>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { onActivated, ref, shallowRef } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { Tabs, TabPane } from 'ant-design-vue';
  import algorithmList from './algorithmList/index.vue';
  import algorithmReleaseList from './algorithmReleaseList/index.vue';

  const activeKey = ref('1');
  const content = shallowRef(algorithmList);
  const tabContent = ref(null);
  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (activeKey.value) {
      case '1':
        content.value = algorithmList;
        break;
      case '2':
        content.value = algorithmReleaseList;
        break;
      default:
        // 可以在这里处理其他情况，或者什么都不做
        break;
    }
  };

  onActivated(() => {
    if (activeKey.value === '2') tabContent.value?.reload();
  });
</script>
