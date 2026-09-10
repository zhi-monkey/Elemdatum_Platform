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
  import { Ref, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  // 字符串数组
  // const props = defineProps(['foo']);

  // 对象方式
  // defineProps({
  //   id: Number,
  // });

  // 泛型
  const props = defineProps<{
    id: Number;
    columns: Array<Object>;
    url: String;
    urlPrefix: String;
  }>();

  let data: Ref<any[]> = ref([]);

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

<style scoped></style>
