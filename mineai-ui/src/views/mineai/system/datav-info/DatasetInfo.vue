<template>
  <div class="main-content">
    <digital-flop />

    <div class="block-top-bottom-content">
      <div class="block-top-content">
        <dataset-scroll-board />
        <bind-monitor-scroll-board />
      </div>

      <graphs />
    </div>
  </div>
</template>
<script setup lang="ts">
  import DigitalFlop from './dataset/DigitalFlop.vue';
  import Graphs from './dataset/Graphs.vue';
  import DatasetScrollBoard from './dataset/DatasetScrollBoard.vue';
  import BindMonitorScrollBoard from './dataset/BindMonitorScrollBoard.vue';
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
