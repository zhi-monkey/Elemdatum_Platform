<template>
  <Table
    size="small"
    :row-key="() => props.id"
    :columns="props.columns"
    :pagination="false"
    :data-source="data"
    :loading="loading"
  />
</template>

<script lang="ts" setup>
  import { Table } from 'ant-design-vue';
  import { ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';

  const props = defineProps<{
    id: Number;
    columns: Array<Object>;
    url: String;
    urlPrefix: String;
  }>();

  let data = ref([]);

  let loading = ref(true);

  maHttp
    .get(
      {
        url: props.url + props.id.toString(),
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: props.urlPrefix },
    )
    .then((resp) => {
      data.value = resp;
      loading.value = false;
    });
</script>
