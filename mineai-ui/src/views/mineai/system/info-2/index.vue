<template>
  <div class="p-3">
    <a-row :gutter="18" style="padding-left: 8px; padding-right: 10px">
      <a-col :span="8">
        <MonitorWhetherBindModelPie :loading="loading" />
      </a-col>
      <a-col :span="8">
        <CameraAlarmsNumber :loading="loading" />
      </a-col>
      <a-col :span="8">
        <AlgorithmAlarmsNumber :loading="loading" />
      </a-col>
    </a-row>
    <a-row :gutter="18" style="padding-left: 8px; padding-right: 10px">
      <a-col :span="8">
        <MonitorBar :loading="loading" />
      </a-col>

      <a-col :span="8">
        <GrowCard :loading="loading" />
      </a-col>

      <a-col :span="8">
        <ModelAlertSence :loading="loading" />
      </a-col>
    </a-row>
    <div class="md:flex enter-y">
      <DeviceTable class="md:w-1/2" :loading="loading" />
      <ModelAlertTable class="md:w-1/2" :loading="loading" />
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import MonitorWhetherBindModelPie from './components/pie/MonitorWhetherBindModelPie.vue';
  import GrowCard from './components/cards/GrowCard.vue';
  import ModelAlertTable from './components/table/ModelAlertTable.vue';
  import AlgorithmAlarmsNumber from './components/bar/AlgorithmAlarmsNumber.vue';
  import CameraAlarmsNumber from './components/bar/CameraAlarmsNumber.vue';
  import ModelAlertSence from './components/pie/ModelAlertSence.vue';
  import MonitorBar from './components/bar/DeviceAlertBar.vue';
  import DeviceTable from './components/table/DeviceTable.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { computed, ComputedRef } from 'vue';

  const { hasPermission } = usePermission();

  const colSpan: ComputedRef<number> = computed(() => {
    return hasPermission([RoleEnum.DatasetDetails]) ? 6 : 8;
  });
</script>
