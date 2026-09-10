<template>
  <div style="margin-top: 50px">
    <PageWrapper style="margin: 0 40px 40px">
      <a-tabs
        tab-position="left"
        v-model:activeKey="activeKey"
        size="large"
        :tabBarGutter="70"
        :tabBarStyle="{ fontSize: '132px' }"
      >
        <a-tab-pane key="1" tab="数据审核详情">
          <ReviewBySelect />
        </a-tab-pane>
      </a-tabs>
    </PageWrapper>
  </div>
</template>

<script lang="ts" setup>
  import { ref, defineEmits } from 'vue';
  import { Tabs as ATabs, TabPane as ATabPane, Button as AButton } from 'ant-design-vue';
  import ReviewBySelect from '/@/views/mineai/refPage/selfTrainingCreate/ReviewBySelect.vue';
  import { PageWrapper } from '/@/components/Page';

  const emit = defineEmits(['next']);
  const activeKey = ref('1');
</script>
