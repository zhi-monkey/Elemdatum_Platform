<template>
  <div style="margin-top: 50px">
    <PageWrapper title="模型测试" style="margin: 0 40px 40px">
      <template #extra>
        <a-button type="primary" @click="handleModelTestNext">下一步</a-button>
      </template>
      <a-tabs
        tab-position="left"
        v-model:activeKey="activeKey"
        size="large"
        :tabBarGutter="70"
        :tabBarStyle="{ fontSize: '132px' }"
      >
        <a-tab-pane key="1" tab="通过测试集测试">
          <TestByTestDataset />
        </a-tab-pane>
        <a-tab-pane key="2" tab="本地上传图片测试">
          <TestByLocalUpload />
        </a-tab-pane>
      </a-tabs>
    </PageWrapper>
  </div>
</template>

<script lang="ts" setup>
  import { ref, defineEmits } from 'vue';
  import { Tabs as ATabs, TabPane as ATabPane, Button as AButton } from 'ant-design-vue';
  import TestByTestDataset from '/@/views/mineai/refPage/generationGuide/modelTest/TestByTestDataset.vue';
  import TestByLocalUpload from '/@/views/mineai/refPage/generationGuide/modelTest/TestByLocalUpload.vue';
  import { PageWrapper } from '/@/components/Page';

  const emit = defineEmits(['next']);
  const activeKey = ref('1');

  function handleModelTestNext() {
    emit('next');
  }
</script>
