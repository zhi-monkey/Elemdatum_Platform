<template>
  <div class="px-2 py-1">
    <div class="flex py-1"
      ><img src="../../../../../assets/icons/titles.svg" /><span style="font-size: medium"
        >算力控制器资源占用率</span
      >
    </div>
    <BasicTable
      id="workerStatusTable"
      :columns="columns"
      :dataSource="data"
      :canResize="canResize"
      :loading="loading"
      :striped="striped"
      :bordered="false"
      :pagination="pagination"
      :scroll="{ y: 150 }"
      @columns-change="handleColumnChange"
      style="padding: 0"
      :class="loading === false && JSON.stringify(data) !== '[]' ? 'myTable' : 'myEmptyTable'"
    >
      <template #cpuProcess="{ record }">
        <a-progress
          :stroke-color="{
            from: '#108ee9',
            to: '#87d068',
          }"
          :percent="record.cpuLoad"
          class="progress"
        />
      </template>
      <template #diskProcess="{ record }">
        <a-progress v-if="record.controller.diskSize>=524288" class="progress" :stroke-color="{
        from: '#108ee9', to: '#87d068', }" style="width: 70%" size="small"
        :percent=record.diskLoad/record.controller.diskSize*100 :format="()=> (record.diskLoad /
        1024/1024).toFixed(2) +'/'+parseFloat(record.controller.diskSize / 1024/1024).toFixed(2)+
        'TB'"/> <a-progress v-else-if="record.controller.diskSize>=512" class="progress"
        :stroke-color="{ from: '#108ee9', to: '#87d068', }" style="width: 70%" size="small"
        :percent=record.diskLoad/record.controller.diskSize*100 :format="()=> (record.diskLoad /
        1024).toFixed(2) +'/'+parseFloat(record.controller.diskSize / 1024).toFixed(2)+ 'GB'"/>
        <a-progress v-else class="progress" :stroke-color="{ from: '#108ee9', to: '#87d068', }"
        style="width: 70%" size="small" :percent=record.diskLoad/record.controller.diskSize*100
        :format="()=> (record.diskLoad ).toFixed(2)
        +'/'+parseFloat(record.controller.diskSize).toFixed(2)+ 'MB'"/>
      </template>
      <template #ramProcess="{ record }">
        <a-progress v-if="record.controller.ramSize>=524288" class="progress" :stroke-color="{ from:
        '#108ee9', to: '#87d068', }" style="width: 70%" size="small"
        :percent=record.ramLoad/record.controller.ramSize*100 :format="()=>
        (record.ramLoad/1024/1024).toFixed(2) +
        '/'+parseFloat(record.controller.ramSize/1024/1024).toFixed(2) +'TB'"/> <a-progress
        v-else-if="record.controller.ramSize>=512" class="progress" :stroke-color="{ from:
        '#108ee9', to: '#87d068', }" style="width: 70%" size="small"
        :percent=record.ramLoad/record.controller.ramSize*100 :format="()=>
        (record.ramLoad/1024).toFixed(2) + '/'+parseFloat(record.controller.ramSize/1024).toFixed(2)
        +'GB'"/> <a-progress v-else class="progress" :stroke-color="{ from: '#108ee9', to:
        '#87d068', }" style="width: 70%" size="small"
        :percent=record.ramLoad/record.controller.ramSize*100 :format="()=>
        (record.ramLoad).toFixed(2) + '/'+parseFloat(record.controller.ramSize).toFixed(2) +'MB'"/>
      </template>
    </BasicTable>
  </div>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { BasicTable, ColumnChangeParam } from '/@/components/Table';
  import { data, getBasicColumns, getData } from './ControllerDetData';
  import { Progress as AProgress } from 'ant-design-vue';

  const canResize = ref(false);
  const loading = ref(true);
  const striped = ref(true);
  const pagination = ref<any>(false);
  const columns = getBasicColumns();
  let tableTop = ref<number>(0);
  let table;
  let height;
  let timer;
  let timer1;
  let timer2;
  let dataTimer;

  onMounted(async () => {
    await getData();
    loading.value = false;
    timer1 = setTimeout(() => {
      table = document.querySelector('#workerStatusTable > div > div > div.ant-table-body');
      height = table.offsetHeight;
      moveToBottom();
    }, 1000);
    dataTimer = setInterval(getData, 10000);
  });

  watch(tableTop, (newTop, _) => {
    if (Math.abs(table.scrollHeight - height - newTop) < 1) {
      table.scrollTop = 0;
      clearInterval(timer);
      timer2 = setTimeout(moveToBottom, 200);
    }
  });

  function moveToBottom() {
    const rate = 15; //多少ms滚动一次
    const speed = 1.5; //一次滚动多少
    timer = setInterval(() => {
      table.scrollBy(0, speed);
      tableTop.value = table.scrollTop;
    }, rate);
  }

  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(dataTimer);
    clearTimeout(timer1);
    clearTimeout(timer2);
  });

  function handleColumnChange(data: ColumnChangeParam[]) {
    console.log('ColumnChanged', data);
  }
</script>

<style scoped>
  .myTable >>> .ant-table-body {
    height: 150px !important;
  }
  .myEmptyTable >>> .ant-table-placeholder {
    height: 150px !important;
  }
</style>
