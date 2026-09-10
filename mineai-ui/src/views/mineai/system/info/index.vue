<template>
  <div class="p-2">
    <a-row>
      <a-col :span="6">
        <GrowCard4 />
      </a-col>
      <a-col :span="6">
        <GrowCard2 />
      </a-col>
      <a-col :span="6">
        <GrowCard3 />
      </a-col>
      <a-col :span="6">
        <GrowCard1 />
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12">
        <DeviceTable />
      </a-col>
      <a-col :span="12">
        <ServerTable />
      </a-col>
    </a-row>
    <div class="px-2 py-1">
      <a-row :gutter="18" style="padding: 5px 0">
        <a-col :span="8">
          <ControllerPie />
        </a-col>
        <a-col :span="8">
          <StoragePie />
        </a-col>
        <a-col :span="8">
          <MonitorPie />
        </a-col>
      </a-row>
    </div>
    <div class="px-2 py-1">
      <a-row :gutter="18" style="padding: 0 0">
        <a-col :span="colSpan">
          <DatasetPie />
        </a-col>

        <a-col :span="colSpan" v-if="hasPermission([RoleEnum.DatasetDetails])">
          <DataTypePie />
        </a-col>

        <a-col :span="colSpan">
          <ServerCapacity />
        </a-col>

        <a-col :span="colSpan">
          <MonitorBar />
        </a-col>
      </a-row>
    </div>
  </div>
</template>
<script lang="ts" setup name="SystemInfo">
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import GrowCard1 from './components/cards/GrowCard1.vue';
  import GrowCard2 from './components/cards/GrowCard2.vue';
  import GrowCard3 from './components/cards/GrowCard3.vue';
  import GrowCard4 from './components/cards/GrowCard4.vue';
  import DataTypePie from './components/DataTypePie.vue';
  import DatasetPie from './components/DeviceServicePie.vue';
  import ControllerPie from './components/ControllerPie.vue';
  import StoragePie from './components/StoragePie.vue';
  import MonitorPie from './components/MonitorPie.vue';
  import ServerCapacity from './components/ServerCapacity.vue';
  import MonitorBar from './components/DeviceAlertBar.vue';
  import DeviceTable from './components/DeviceTable.vue';
  import ServerTable from './components/ServerTable.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { computed, ComputedRef } from 'vue';

  const { hasPermission } = usePermission();

  const colSpan: ComputedRef<number> = computed(() => {
    return hasPermission([RoleEnum.DatasetDetails]) ? 6 : 8;
  });
</script>
