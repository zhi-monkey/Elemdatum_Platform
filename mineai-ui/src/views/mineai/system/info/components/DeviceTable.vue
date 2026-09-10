<template>
  <div class="px-2 py-1">
    <div class="flex py-1">
      <img src="../../../../../assets/icons/titles.svg" alt="title" />
      <span style="font-size: medium">监控设备</span>
    </div>
    <BasicTable
      id="deviceTable"
      :dataSource="data"
      :columns="columns"
      :loading="loading"
      :canResize="false"
      :showTableSetting="false"
      :pagination="false"
      :scroll="{ x: true, y: 150 }"
      :showIndexColumn="false"
      style="padding: 0"
      :class="loading === false && JSON.stringify(data) !== '[]' ? 'myTable' : 'myEmptyTable'"
    >
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'detail|svg',
              onClick: handleDetail.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
  </div>
</template>
<script lang="ts">
  import { defineComponent, onMounted, onUnmounted, ref, watch } from 'vue';
  import { getBasicColumns, getData, data } from './deviceData';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { PageEnum } from '/@/enums/pageEnum';
  import { useGo } from '/@/hooks/web/usePage';

  export default defineComponent({
    name: 'Device',
    components: { BasicTable, TableAction },
    setup() {
      const go = useGo();
      const loading = ref(true);
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
          table = document.querySelector('#deviceTable > div > div > div.ant-table-body');
          height = table.offsetHeight;
          moveToBottom();
        }, 1000);
        dataTimer = setInterval(getData, 10000);
      });

      watch(tableTop, (newTop) => {
        if (Math.abs(table.scrollHeight - height - newTop) < 1) {
          table.scrollTop = 0;
          clearInterval(timer);
          timer2 = setTimeout(moveToBottom, 200); //第一条展示等待时间
        }
      });

      function handleDetail() {
        go(PageEnum.MONITOR_LIST);
      }

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

      return {
        data,
        columns,
        loading,
        handleDetail,
      };
    },
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
