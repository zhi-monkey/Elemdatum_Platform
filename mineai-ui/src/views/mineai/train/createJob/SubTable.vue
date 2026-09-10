<template>
  <Table
    size="small"
    :row-key="() => props.jobId"
    :columns="modelColumns"
    :pagination="false"
    :data-source="data"
    :canResize="false"
    :loading="loading"
  />
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Table } from 'ant-design-vue';

  // 泛型

  const props = defineProps<{
    jobId: Number;
  }>();

  const modelColumns = [
    { title: '超参名称', dataIndex: 'paramName', width: 20 },
    { title: '超参值', dataIndex: 'paramValue', width: 20 },
  ];

  let data = ref([{ paramName: '', paramValue: '' }]);
  let loading = ref(true);

  //根据作业id获取超参内容
  maHttp
    .get(
      {
        url: 'modelJob/getParamByJobId',
        params: { id: props.jobId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((resp) => {
      Object.keys(resp).forEach((key) => {
        data.value.push({ paramName: key, paramValue: resp[key] });
      });
      loading.value = false;
    });
</script>

<style scoped></style>
