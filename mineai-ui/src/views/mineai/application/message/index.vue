<template>
  <PageWrapper dense style="margin: 0 16px 0 16px">
    <template #title>应用信息管理</template>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <TabPane key="1" tab="基础算法库" />
        <TabPane key="2" tab="算力芯片管理" />
        <TabPane key="3" tab="设备固件管理" />
        <TabPane key="4" tab="场景名称管理" />
        <TabPane key="5" tab="应用名称管理" />
        <!-- 模型接收地址管理已废弃，暂时保留代码但隐藏入口。 -->
        <TabPane v-if="false" key="6" tab="模型接收地址管理" />
      </Tabs>
    </template>

    <template v-if="activeKey === '1'">
      <BasicAlgorithmManagement :active="activeKey === '1'" />
    </template>
    <template v-if="activeKey === '2'">
      <ComputingManagement />
    </template>
    <template v-if="activeKey === '3'">
      <DeviceFirmwareManagement />
    </template>
    <template v-if="activeKey === '4'">
      <SceneManagement />
    </template>
    <template v-if="activeKey === '5'">
      <ApplicationManagement />
    </template>
    <template v-if="false && activeKey === '6'">
      <InferenceDeviceManagement />
    </template>
  </PageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import ComputingManagement from '/@/views/mineai/application/message/chipDetail/chipManagement.vue';
  import DeviceFirmwareManagement from './deviceDetail/deviceFirmwareManagement.vue';
  import SceneManagement from './sceneDetail/SceneManagement.vue';
  import ApplicationManagement from './applicationDetail/ApplicationManagement.vue';
  import BasicAlgorithmManagement from './basicAlgorithmDetail/BasicAlgorithmManagement.vue';
  import InferenceDeviceManagement from './inference-device/index.vue';
  import { PageWrapper } from '/@/components/Page';
  import { TabPane, Tabs } from 'ant-design-vue';

  const activeKey = ref('1');

  function handleTabChange(key) {
    activeKey.value = key;
  }
</script>
