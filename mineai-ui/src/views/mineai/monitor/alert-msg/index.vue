<template>
  <div>
    <div class="flex flex-row justify-around w-full px-5 gap-x-3">
      <div class="w-1/3"><DeviceAlertBar /></div>
      <div class="w-1/3"><CameraAlarmsNumber /></div>
      <div class="w-1/3"><TopAlertMonitor /></div>
    </div>
    <Table :columns="columns" />
  </div>
</template>

<script lang="ts" setup name="WarningHistory">
  import CameraAlarmsNumber from '/@/views/mineai/system/info-2/components/bar/CameraAlarmsNumber.vue';
  import Table from '/@/views/mineai/monitor/alert-msg/Table.vue';
  import { columns } from '/@/views/mineai/monitor/alert-msg/alert.data';
  import DeviceAlertBar from '/@/views/mineai/system/info-2/components/bar/DeviceAlertBar.vue';
  import TopAlertMonitor from '/@/views/mineai/monitor/alert-msg/cameraTable/TopAlertMonitor.vue';
</script>
