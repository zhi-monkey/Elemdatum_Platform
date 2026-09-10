<template>
  <div class="data-search">
    <BasicTable @register="registerTable" rowKey="id" />
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, useTable, BasicColumn, FormSchema } from '/@/components/Table';
  import { searchPcFiles, getPcDatasets } from './api';
  import { h } from 'vue';

  const formatFileSize = (bytes: number) => {
    if (bytes == null) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`;
  };

  const columns: BasicColumn[] = [
    {
      title: '编号',
      dataIndex: 'id',
      fixed: 'left',
      width: 80,
      sorter: true,
    },
    {
      title: '文件名',
      dataIndex: 'name',
      ellipsis: true,
      width: 200,
    },
    {
      title: '所属数据集',
      dataIndex: 'datasetName',
      ellipsis: true,
      width: 180,
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      width: 120,
      customRender: ({ text }) => h('span', formatFileSize(text)),
    },
    {
      title: '点数',
      dataIndex: 'pointCount',
      width: 110,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 160,
    },
  ];

  const searchFormSchema: FormSchema[] = [
    {
      field: 'datasetIds',
      label: '数据集',
      component: 'ApiSelect',
      componentProps: {
        mode: 'multiple',
        api: () => getPcDatasets(),
        labelField: 'name',
        valueField: 'id',
        immediate: false,
      },
      colProps: { xl: 8, xxl: 6 },
    },
    {
      field: 'name',
      label: '文件名',
      component: 'Input',
      colProps: { xl: 8, xxl: 6 },
    },
    {
      field: 'updateTime',
      label: '更新时间',
      component: 'RangePicker',
      componentProps: {
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      colProps: { xl: 12, xxl: 8 },
    },
  ];

  const [registerTable] = useTable({
    api: async (params) => {
      const v: any = await searchPcFiles(buildSearchBody(params));
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    columns,
    formConfig: {
      labelWidth: 100,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
  });

  function buildSearchBody(params: any) {
    const body: any = {
      datasetIds: params.datasetIds,
      name: params.name,
      current: params.page,
      size: params.pageSize,
      sortField: params.field || 'id',
      sortOrder: params.order === 'ascend' ? 'asc' : 'desc',
    };
    if (Array.isArray(params.updateTime) && params.updateTime.length === 2) {
      body.updateTimeStart = params.updateTime[0];
      body.updateTimeEnd = params.updateTime[1];
    }
    return body;
  }
</script>
