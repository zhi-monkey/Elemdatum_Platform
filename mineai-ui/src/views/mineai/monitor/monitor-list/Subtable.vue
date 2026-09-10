<template>
  <Table
    size="small"
    :row-key="() => props.id"
    :columns="props.columns"
    :pagination="false"
    :data-source="data"
    :loading="loading"
  >
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'ant-design:edit-outlined',
            tooltip: '自定义视频名称',
            onClick: editVideoName.bind(null, record),
          },
        ]"
      />
    </template>
  </Table>
  <VideoNameModal
    @register="registerModal"
    @success="handleSuccess"
    :modal-id="props.id"
    :data-id="currentDataId"
  />
</template>

<script lang="ts" setup>
  import { Table } from 'ant-design-vue';
  import { TableAction } from '/@/components/Table';
  import { ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import VideoNameModal from './VideoNameModal.vue';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let currentDataId = ref();
  // 泛型
  const props = defineProps<{
    id: Number;
    columns: Array<Object>;
    url: String;
    urlPrefix: String;
  }>();

  const [registerModal, { openModal }] = useModal();
  let data = ref([]);
  let loading = ref(true);
  let videoName = ref();
  function editVideoName(record: Recordable) {
    loading.value = true;
    maHttp
      .get(
        {
          url: 'model/findMonitorModelConfigByMonitorIdAndModelId',
          params: { modelId: record.id, monitorId: props.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        videoName.value = v.videoName;
        currentDataId.value = record.id;
        openModal(true, { videoName });
        loading.value = false;
      });
  }

  function handleSuccess() {
    alert('提交成功！');
  }

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
