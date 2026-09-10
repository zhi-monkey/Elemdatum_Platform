<template>
  <div class="main-content">
    <digital-flop />

    <div class="block-top-bottom-content">
      <div class="block-top-content">
        <monitor-scroll-board />
        <alert-scroll-board />
      </div>

      <cards />
    </div>
  </div>
</template>
<script setup lang="ts">
  import DigitalFlop from './monitor/DigitalFlop.vue';
  import Cards from './monitor/Cards.vue';
  import MonitorScrollBoard from './monitor/MonitorScrollBoard.vue';
  import AlertScrollBoard from './monitor/AlertScrollBoard.vue';
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
