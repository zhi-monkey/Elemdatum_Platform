<template>
  <div class="px-2 py-1">
    <div class="flex py-1 px-1"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="font-size: medium"
        >数据集列表</span
      >
    </div>
    <BasicTable
      id="datasetTable"
      :columns="columns"
      :dataSource="data"
      :loading="loading"
      :bordered="false"
      :pagination="false"
      :scroll="{ y: 150 }"
      style="padding: 0"
      :class="loading === false && JSON.stringify(data) !== '[]' ? 'myTable' : 'myEmptyTable'"
    />
  </div>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { getBasicColumns, data, getData } from './datasetTableData';

  const loading = ref(true);
  const columns = getBasicColumns();
  let tableTop = ref<number>(0);
  let table;
  let height;
  let timer;
  let timer1;
  let timer2;
  let dataTimer;

  watch(tableTop, (newTop, _) => {
    if (Math.abs(table.scrollHeight - height - newTop) < 1) {
      table.scrollTop = 0;
      clearInterval(timer);
      timer2 = setTimeout(moveToBottom, 200);
    }
  });

  function moveToBottom() {
    const rate = 15;
    const speed = 1.5;
    timer = setInterval(() => {
      table.scrollBy(0, speed);
      tableTop.value = table.scrollTop;
    }, rate);
  }

  onMounted(async () => {
    await getData();
    loading.value = false;
    timer1 = setTimeout(() => {
      table = document.querySelector('#datasetTable > div > div > div.ant-table-body');
      height = table.offsetHeight;
      moveToBottom();
    }, 1000);
    dataTimer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(dataTimer);
    clearTimeout(timer1);
    clearTimeout(timer2);
  });
</script>
<style scoped>
  .myTable >>> .ant-table-body {
    height: 150px !important;
  }
  .myEmptyTable >>> .ant-table-placeholder {
    height: 150px !important;
  }
</style>
