<template>
  <div class="main-content">
    <a-tabs v-model:activeKey="activeKey" animated type="card">
      <a-tab-pane key="1" tab="CPU服务器信息"><DigitalFlopCPU /></a-tab-pane>
      <a-tab-pane key="2" tab="GPU服务器信息" force-render><DigitalFlopGPU /></a-tab-pane>
    </a-tabs>
    <!--    <DigitalFlopCPU />-->
    <!--    <DigitalFlopGPU />-->

    <div class="block-top-bottom-content">
      <div class="block-top-content">
        <resource-scroll-board />
      </div>

      <!--      <line-charts-and-pie-charts />-->
    </div>
  </div>
</template>
<script setup lang="ts">
  import DigitalFlopGPU from './controller/DigitalFlopGPU.vue';
  import DigitalFlopCPU from './controller/DigitalFlopCPU.vue';
  // import LineChartsAndPieCharts from './controller/LineChartsAndPieCharts.vue';
  import ResourceScrollBoard from './controller/ResourceScrollBoard.vue';
  import { Button as AButton } from '/@/components/Button';
  import { Carousel } from 'ant-design-vue';
  import { Tabs as ATabs, TabPane as ATabPane } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';

  const activeKey = ref('1');
  let timer: NodeJS.Timer;
  onMounted(async () => {
    timer = setInterval(async () => {
      if (activeKey.value == '1') {
        activeKey.value = '2';
      } else if (activeKey.value == '2') {
        activeKey.value = '1';
      }
    }, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style scoped lang="less">
  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .block-left-right-content {
    flex: 1;
    display: flex;
    margin-top: 20px;
  }

  .block-top-bottom-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    padding-top: 20px;
  }

  .block-top-content {
    height: 50%;
    display: flex;
    flex-grow: 0;
    box-sizing: border-box;
    padding-bottom: 20px;
  }
</style>
