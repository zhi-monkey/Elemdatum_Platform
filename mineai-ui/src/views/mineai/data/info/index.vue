<template>
  <div class="p-2">
    <a-row>
      <a-col :span="4">
        <GrowCard1 />
      </a-col>
      <a-col :span="5">
        <GrowCard2 />
      </a-col>
      <a-col :span="5">
        <GrowCard3 />
      </a-col>
      <a-col :span="5">
        <GrowCard4 />
      </a-col>
      <a-col :span="5">
        <GrowCard5 />
      </a-col>
    </a-row>
    <a-row :gutter="10" style="padding: 20px 0 0 0">
      <a-col :span="12">
        <DatasetTable />
      </a-col>
      <a-col :span="12">
        <DeviceTable />
      </a-col>
    </a-row>
    <a-row :gutter="10" style="padding: 20px 0">
      <a-col :span="8">
        <DatasetInputBar />
      </a-col>
      <a-col :span="8">
        <DatasetLine />
      </a-col>
      <a-col :span="8">
        <DataSortPie />
      </a-col>
      <a-col :span="8">
        <DeviceStatePie />
      </a-col>
      <a-col :span="8">
        <DeviceTypePie />
      </a-col>
      <a-col :span="8">
        <DataPublishPie />
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup name="DataDatasetInfo">
  import { ref } from 'vue';
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import DatasetLine from './components/line/DatasetLine.vue';
  import DatasetTable from './components/table/DatasetTable.vue';
  import DeviceTable from './components/table/DeviceTable.vue';
  import DataSortPie from './components/pie/DataSortPie.vue';
  import DataPublishPie from './components/pie/DataPublishPie.vue';
  import DeviceTypePie from './components/pie/DeviceTypePie.vue';
  import DatasetInputBar from './components/line/DatasetInputBar.vue';
  import DeviceStatePie from './components/pie/DeviceStatePie.vue';
  import GrowCard1 from './components/card/GrowCard1.vue';
  import GrowCard2 from './components/card/GrowCard2.vue';
  import GrowCard3 from './components/card/GrowCard3.vue';
  import GrowCard4 from './components/card/GrowCard4.vue';
  import GrowCard5 from './components/card/GrowCard5.vue';

  const loading = ref(true);

  setTimeout(() => {
    loading.value = false;
  }, 1500);
</script>
