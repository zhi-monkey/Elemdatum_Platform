<template>
  <div class="p-2">
    <a-row>
      <a-col :span="8">
        <GrowCardFirst />
      </a-col>

      <a-col :span="8">
        <GrowCardThird />
      </a-col>
      <a-col :span="8">
        <GrowCardFourth />
      </a-col>
    </a-row>

    <div class="md:flex enter-y">
      <ControllerDetTable class="md:w-1/2" :loading="loading" />
      <ControllerInfoTable class="md:w-1/2" :loading="loading" />
    </div>
    <div class="px-2" style="margin-top: 5px">
      <a-row>
        <a-col :span="8">
          <div>
            <Icon icon="titles|svg" />
            <span style="font-size: medium">设备服务占比</span>
          </div>
        </a-col>
        <a-col :span="8">
          <div>
            <Icon icon="titles|svg" />
            <span style="font-size: medium">算力控制器CPU总负载</span>
          </div>
        </a-col>
        <a-col :span="8">
          <div>
            <Icon icon="titles|svg" />
            <span style="font-size: medium">报警情况</span>
          </div>
        </a-col>
      </a-row>
    </div>

    <a-row :gutter="10" style="padding-left: 8px; padding-right: 10px">
      <a-col :span="8">
        <ScenePie :loading="loading" />
      </a-col>

      <a-col :span="8">
        <CpuLoadAnalysis :loading="loading" />
      </a-col>

      <a-col :span="8">
        <UploadAnalysis :loading="loading" />
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup name="ControllerInfo">
  import { ref } from 'vue';
  import GrowCardFirst from './components/GrowCardFirst.vue';
  import GrowCardThird from './components/GrowCardThird.vue';
  import GrowCardFourth from './components/GrowCardFourth.vue';
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import ControllerDetTable from './components/ControllerDetTable.vue';
  import ControllerInfoTable from './components/ControllerInfoTable.vue';
  import UploadAnalysis from './components/UploadAnalysis.vue';
  import CpuLoadAnalysis from './components/CpuLoadAnalysis.vue';
  import ScenePie from './components/ServicePie.vue';
  import { Icon } from '/@/components/Icon';

  const loading = ref(true);

  setTimeout(() => {
    loading.value = false;
  }, 1500);
</script>
