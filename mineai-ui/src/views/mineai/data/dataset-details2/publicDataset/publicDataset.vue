<template>
  <BasicTable @register="registerTable" row-key="id">
    <template #expandedRowRender="{ record }">
      <SubTable :record="record" @reload="reload" />
    </template>
  </BasicTable>
</template>

<script lang="ts" setup>
  import { BasicTable, useTable } from '/src/components/Table';
  import { columns, searchFormSchema } from './stmData';
  import { maHttp } from '/src/utils/http/axios';
  import { DubheBackendUrlEnum } from '/src/enums/mineaiEnum';
  import { onMounted, onUnmounted, Ref, ref } from 'vue';
  import SubTable from './subTable.vue';

  const timer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const uploadTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const [registerTable, { reload }] = useTable({
    title: '数据集列表',
    api: async (params) => {
      //判断时间数据是否符合要求，并完成格式转换
      if (params.createTime && Array.isArray(params.createTime)) {
        params.createTime = params.createTime.map((time) => {
          const timestamp = Date.parse(time);
          return isNaN(timestamp) ? time : timestamp.toString();
        });
      }
      const v = await maHttp.get(
        {
          url: 'datasets/getAllPublicDatasets',
          params,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      // 如果currentVersionName为空就删除这整条数据
      v.result = v.result.filter((item) => item.currentVersionName);
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      submitOnReset: true,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    clickToRowSelect: false,
    handleSearchInfoFn(info) {
      //请求前检查日期参数是否为时间戳格式，若不是则尝试转换。
      if (info.createTime && Array.isArray(info.createTime)) {
        const timestampArray = info.createTime.map((time) => {
          const timestamp = Date.parse(time);
          return isNaN(timestamp) ? time : timestamp.toString();
        });
        info.createTime = timestampArray;
      }
      return info;
    },
  });

  onUnmounted(() => {
    // 退出时关闭定时器
    clearInterval(timer.value);
    clearInterval(uploadTimer.value);
  });
</script>
<style scoped lang="less"></style>
