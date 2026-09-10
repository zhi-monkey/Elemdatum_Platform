<template>
  <div>
    <div class="px-2 py-1 myForm">
      <div class="flex py-1">
        <img src="../../../../../assets/icons/titles.svg" alt="title" />
        <span style="font-size: medium">监控设备报警数TOP5</span>
      </div>
      <BasicTable
        :dataSource="monitorAlert"
        :columns="columns"
        :loading="loading"
        :canResize="false"
        :striped="false"
        :showTableSetting="false"
        :pagination="false"
        :showIndexColumn="true"
        ellipsis="true"
        style="padding: 0"
        :class="
          loading === false && JSON.stringify(monitorAlert) !== '[]' ? 'myTable' : 'myEmptyTable'
        "
      />
    </div>
  </div>
</template>
<script lang="ts">
  import { defineComponent, onMounted, onUnmounted, ref } from 'vue';
  import { monitorAlert, getData, getBasicColumns } from './monitor_data';
  import { BasicTable } from '/@/components/Table';

  export default defineComponent({
    components: { BasicTable },
    setup() {
      let timer;
      const loading = ref(true);
      const columns = getBasicColumns();

      onMounted(async () => {
        await getData();
        loading.value = false;
        setTimeout(() => {
          loading.value = false;
        }, 100);
        timer = setInterval(getData, 10000);
      });
      onUnmounted(() => clearInterval(timer));

      return {
        monitorAlert,
        columns,
        timer,
        loading,
      };
    },
  });
</script>
<style scoped>
  .myTable >>> .ant-table-body {
    height: 250px !important;
    background-color: transparent;
  }

  .myEmptyTable >>> .ant-table-placeholder {
    height: 100px !important;
    background-color: transparent;
  }

  .myForm :deep(.ant-table-wrapper),
  :deep(.ant-table-thead > tr > th),
  :deep(.vben-basic-table-form-container .ant-form) {
    background-color: transparent;
  }

  .myForm :deep(.ant-table),
  :deep(.ant-table-header),
  :deep(.ant-table-fixed-header > .ant-table-content > .ant-table-scroll > .ant-table-body),
  :deep(.ant-table-cell-fix-left),
  :deep(.ant-table-cell-fix-right) {
    background: transparent;
  }
</style>
