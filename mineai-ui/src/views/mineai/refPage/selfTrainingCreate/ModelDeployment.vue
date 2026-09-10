<template>
  <div style="margin-top: 50px" class="container">
    <Card title="模型保存或下发" style="margin: 0 40px 40px; width: 75%; height: 55vh">
      <template #extra>
        <a-button type="primary" @click="handleModelSaveNext">完成</a-button>
      </template>
      <a-tabs
        tab-position="left"
        v-model:activeKey="activeKey"
        size="large"
        :tabBarGutter="70"
        :tabBarStyle="{ fontSize: '132px' }"
      >
        <a-tab-pane key="1" tab="模型下发">
          <ModelSchedule />
        </a-tab-pane>
      </a-tabs>
    </Card>
  </div>
</template>

<script setup>
  import { ref, defineEmits } from 'vue';
  import { Tabs as ATabs, TabPane as ATabPane, Button as AButton, Card } from 'ant-design-vue';
  import ModelSchedule from '/@/views/mineai/refPage/generationGuide/modelSaveOrSchedule/ModelSchedule.vue';

  const activeKey = ref('1');

  import { useGo } from '/@/hooks/web/usePage';

  const go = useGo();

  function handleModelSaveNext() {
    go('/maTrainingCenter/generationGuideList');
  }
</script>

<style scoped>
  .container {
    display: flex;
    flex-direction: column;
    align-items: center;
    height: 85vh; /* 可以根据需要调整高度 */
    width: 100%; /* 宽度为 100% */
  }

  :deep(.ant-card-head-title) {
    height: 120px;
    line-height: 100px;
    font-size: 24px;
  }

  :deep(.ant-tabs) {
    font-size: 20px;
  }

  :deep(.ant-form-vertical) {
    font-size: 18px;
  }
</style>
