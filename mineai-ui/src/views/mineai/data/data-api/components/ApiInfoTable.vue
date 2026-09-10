<template>
  <div>
    <div class="flex flex-row" style="margin-bottom: 10px"
      ><img src="../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >参数信息</span
      >
    </div>
    <BasicTable
      @register="registerTable"
      :striped="true"
      :columns="getParamColumns()"
      :dataSource="getApiColData(props.type1, props.type2)"
      :canResize="false"
      :showTableSetting="false"
      :pagination="false"
      :bordered="true"
      :showIndexColumn="false"
    />
  </div>
</template>
<script lang="ts" setup>
  import { defineProps } from 'vue';
  import { getParamColumns, getApiColData } from './apiModalData';
  import { BasicTable, useTable } from '/@/components/Table';

  const props = defineProps({ type1: { type: Number }, type2: { type: Number } });

  const [registerTable] = useTable();
</script>
