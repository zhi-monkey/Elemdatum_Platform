<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <template #title> 平台信息管理</template>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <TabPane key="1" tab="软硬件平台管理" />
        <TabPane key="2" tab="设备固件信息管理" />
      </Tabs>
    </template>

    <template v-if="activeKey === '1'">
      <SHPlatformInfoTable />
    </template>
    <template v-if="activeKey === '2'">
      <DFPlatformInfoTable />
    </template>
  </PageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import DFPlatformInfoTable from './DFPlatformInfoTable.vue';
  import SHPlatformInfoTable from './SHPlatformInfoTable.vue';
  import { PageWrapper } from '/@/components/Page';
  import { TabPane, Tabs } from 'ant-design-vue';

  const activeKey = ref('1');

  function handleTabChange(key) {
    activeKey.value = key;
  }
</script>
