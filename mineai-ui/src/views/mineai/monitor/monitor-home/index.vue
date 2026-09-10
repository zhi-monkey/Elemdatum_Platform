<template>
  <div class="p-2" style="width: 100%">
    <a-row>
      <a-col :span="8">
        <Suspense><GrowCard1 /></Suspense>
      </a-col>
      <a-col :span="8">
        <Suspense><GrowCard3 /></Suspense>
      </a-col>
      <a-col :span="8">
        <Suspense> <GrowCard4 /></Suspense>
      </a-col>
    </a-row>
    <div class="px-2" style="margin-top: 5px">
      <a-row>
        <a-col :span="12">
          <div>
            <Icon icon="titles|svg" />
            <span style="font-size: medium">设备列表</span>
          </div>
        </a-col>
        <a-col :span="12">
          <div>
            <Icon icon="titles|svg" />
            <span style="font-size: medium">算法列表</span>
          </div>
        </a-col>
      </a-row>
    </div>
    <div class="md:flex enter-y px-2">
      <MonitorTable class="md:w-1/2" />
      <ModelTable class="md:w-1/2" />
    </div>
    <div class="px-2" style="margin-top: 5px">
      <div>
        <Icon icon="titles|svg" />
        <span style="font-size: medium">警报信息</span>
      </div>
    </div>
    <div class="md:flex enter-y px-2">
      <ModelAlertTable class="md:w-1/1" />
    </div>
    <!--    <div class="px-2" style="margin-top: 5px">-->
    <!--      <a-row>-->
    <!--        <a-col :span="8">-->
    <!--          <div>-->
    <!--            <Icon icon="titles|svg" />-->
    <!--            <span style="font-size: medium">运行状况占比</span>-->
    <!--          </div>-->
    <!--        </a-col>-->
    <!--        <a-col :span="8">-->
    <!--          <div>-->
    <!--            <Icon icon="titles|svg" />-->
    <!--            <span style="font-size: medium">推流占比</span>-->
    <!--          </div>-->
    <!--        </a-col>-->
    <!--        <a-col :span="8">-->
    <!--          <div>-->
    <!--            <Icon icon="titles|svg" />-->
    <!--            <span style="font-size: medium">报警信息类型占比</span>-->
    <!--          </div>-->
    <!--        </a-col>-->
    <!--      </a-row>-->
    <!--    </div>-->
    <!--    <div class="md:flex enter-y px-2">-->
    <!--      <MonitorPie class="md:w-1/3" />-->
    <!--      <PushStreamPie class="md:w-1/3" />-->
    <!--      <ModelAlertDataTypePie class="md:w-1/3" />-->
    <!--    </div>-->
  </div>
</template>
<script lang="ts" setup name="MonitorMonitorHome">
  import GrowCard1 from './components/card/GrowCard1.vue';
  import GrowCard3 from './components/card/GrowCard3.vue';
  import GrowCard4 from './components/card/GrowCard4.vue';
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import MonitorTable from './components/table/MonitorTable.vue';
  import ModelTable from './components/table/ModelTable.vue';
  import { Icon } from '/@/components/Icon';
  import ModelAlertTable from './components/table/ModelAlertTable.vue';
</script>
