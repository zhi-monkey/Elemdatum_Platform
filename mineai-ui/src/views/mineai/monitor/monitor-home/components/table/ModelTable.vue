<template>
  <BasicTable
    id="modelTable"
    :columns="columns"
    :dataSource="data"
    :canResize="canResize"
    :loading="loading"
    :striped="striped"
    :bordered="false"
    :pagination="pagination"
    :scroll="{ y: 240 }"
    @columns-change="handleColumnChange"
    style="padding: 0; margin-left: 4px"
    :class="loading === false && JSON.stringify(data) !== '[]' ? 'myTable' : 'myEmptyTable'"
  >
    <template #powerProcess="{ record }">
      <a-progress
        :stroke-color="{ from: '#108ee9', to: '#87d068' }"
        :percent="record.power"
        :format="(percent) => percent + 'W'"
      />
    </template>
    <template #energyConsumptionProcess="{ record }">
      <a-progress
        :stroke-color="{ from: '#108ee9', to: '#87d068' }"
        :percent="record.energyConsumption"
        :format="(percent) => percent + 'kWh'"
      />
    </template>
  </BasicTable>
</template>
<script lang="ts" setup>
  import { onUnmounted, ref, watch } from 'vue';
  import { BasicTable, ColumnChangeParam } from '/@/components/Table';
  import { getBasicColumns, data, getData } from './modelData';
  import { Progress as AProgress } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

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
  maHttp
    .get(
      {
        url: 'model/findModelListByDataNumAndDesc',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      if (v.length != 0) data.value = v;
      else data.value = [];
      loading.value = false;
      timer1 = setTimeout(() => {
        table = document.querySelector('#modelTable > div > div > div.ant-table-body');
        height = table.offsetHeight;
        moveToBottom();
      }, 1000);
      dataTimer = setInterval(getData, 10000);
    });

  watch(tableTop, (newTop, _) => {
    if (Math.abs(table.scrollHeight - height - newTop) < 1) {
      table.scrollTop = 0;
      clearInterval(timer);
      timer2 = setTimeout(moveToBottom, 200); //第一条展示等待时间
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
    height: 250px !important;
  }
  .myEmptyTable >>> .ant-table-placeholder {
    height: 250px !important;
  }
</style>
