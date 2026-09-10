<template>
  <div class="root">
    <div class="container">
      <div style="margin-top: 50px; width: 100%; min-height: 600px; min-width: 1200px">
        <Card title="模型测试" style="width: 100%; min-height: 600px; min-width: 1200px">
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
        </Card>
      </div>
    </div>
    <!-- 下一步按钮居中显示 -->
    <div class="button-container">
      <a-button type="primary" @click="handleModelTestNext">下一步</a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { defineEmits, ref } from 'vue';
  import { Button as AButton, TabPane as ATabPane, Tabs as ATabs } from 'ant-design-vue';
  import TestByTestDataset from '/@/views/mineai/refPage/generationGuide/modelTest/TestByTestDataset.vue';
  import TestByLocalUpload from '/@/views/mineai/refPage/generationGuide/modelTest/TestByLocalUpload.vue';
  import { Card } from 'ant-design-vue';

  const emit = defineEmits(['next']);
  const activeKey = ref('1');

  function handleModelTestNext() {
    emit('next');
  }
</script>

<style scoped>
  .root {
    height: 100%;
  }

  .container {
    display: flex;
    flex-direction: column;
    align-items: center;
    height: 70vh;
    width: 100%;
  }

  .button-container {
    text-align: center;
    margin-top: 20px;
    margin-bottom: 20px;
  }
</style>
