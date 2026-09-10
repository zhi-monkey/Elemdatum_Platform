<template>
  <div style="margin-top: 50px">
    <PageWrapper title="模型保存或下发" style="margin: 0 40px 40px">
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
        <a-tab-pane key="1" tab="模型保存">
          <ModelSave />
        </a-tab-pane>
        <a-tab-pane key="2" tab="模型下发">
          <ModelSchedule />
        </a-tab-pane>
      </a-tabs>
    </PageWrapper>
  </div>
</template>

<script lang="ts" setup>
  import { ref, defineEmits } from 'vue';
  import { Tabs as ATabs, TabPane as ATabPane, Button as AButton } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import ModelSave from '/@/views/mineai/refPage/generationGuide/modelSaveOrSchedule/ModelSave.vue';
  import ModelSchedule from '/@/views/mineai/refPage/generationGuide/modelSaveOrSchedule/ModelSchedule.vue';

  const emit = defineEmits(['next']);
  const activeKey = ref('1');

  import { useGo } from '/@/hooks/web/usePage';

  const go = useGo();

  function handleModelSaveNext() {
    go('/maTrainingCenter/generationGuideList');
  }
</script>
