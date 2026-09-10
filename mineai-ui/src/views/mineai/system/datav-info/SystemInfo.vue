<template>
  <div class="main-content">
    <line-charts-and-bar-charts />

    <div class="block-top-bottom-content">
      <div class="block-top-content">
        <controller-scroll-board />

        <monitor-scroll-board />
      </div>
      <div></div>
      <border-box3 class="block-bottom-content" :reverse="true">
        <pie-charts />
      </border-box3>
      <!--      <pie-charts />-->
    </div>
  </div>
</template>
<script setup lang="ts">
  import MonitorScrollBoard from './system/MonitorScrollBoard.vue';
  import LineChartsAndBarCharts from './system/LineChartsAndBarCharts.vue';
  import PieCharts from '/@/views/mineai/system/datav-info/system/PieCharts.vue';
  import ControllerScrollBoard from '/@/views/mineai/system/datav-info/controller/ControllerScrollBoard.vue';
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

  .block-bottom-content {
    height: 50%;
  }
</style>
