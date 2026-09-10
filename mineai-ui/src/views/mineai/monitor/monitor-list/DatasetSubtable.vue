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
            icon: 'ant-design:play-circle-outlined',
            tooltip: '开始录制',
            color: 'success',
            disabled: props.isRecord === 1,
            popConfirm: {
              title: '是否开始录制',
              confirm: handleStart.bind(null, record),
            },
          },
          {
            icon: 'ant-design:pause-circle-outlined',
            tooltip: '停止录制',
            color: 'warning',
            disabled: props.isRecord === -1,
            popConfirm: {
              title: '是否停止录制',
              confirm: handleStop.bind(null, record),
            },
          },
        ]"
      />
    </template>
  </Table>
  <MaxRecordModal
    @register="registerMaxRecordModal"
    @success="handleSuccess"
    :monitor-id="props.id"
  />
</template>

<script lang="ts" setup>
  import { Table } from 'ant-design-vue';
  import { TableAction } from '/@/components/Table';
  import { onMounted, ref } from 'vue';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { maHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import MaxRecordModal from '/@/views/mineai/monitor/monitor-list/MaxRecordModal.vue';
  const [registerMaxRecordModal, { openModal: openMaxRecordModal }] = useModal();
  const { createMessage } = useMessage();
  const emit = defineEmits(['success']);
  const props = defineProps<{
    id: Number;
    isRecord: Number;
    columns: Array<Object>;
    url: String;
    urlPrefix: String;
    maxSecond: Number;
  }>();

  let data = ref([] as any[]);
  let loading = ref(true);
  let isRecord = ref();

  function handleStart() {
    console.log('开始录制');
    openMaxRecordModal();
    console.log(props.maxSecond);
  }

  function handleStop() {
    console.log('停止录制');
    isRecord.value = -1;
    maHttp
      .get(
        {
          url: 'monitor/changeMonitorRecording',
          params: { isRecord: isRecord.value, monitorId: props.id, maxSecond: props.maxSecond },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then(() => {
        createMessage.success('停止录制！');
        emit('success');
      });
  }

  onMounted(async () => {
    const datasetId = await maHttp.get(
      {
        url: props.url + props.id.toString(),
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: props.urlPrefix },
    );
    if (datasetId === null) {
      data.value = [];
      loading.value = false;
    } else {
      const datasetInfo = await maHttp.get(
        {
          url: `datasets/${datasetId}`,
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      data.value = [datasetInfo];
      loading.value = false;
    }
  });

  function handleSuccess() {
    emit('success');
  }
</script>

<style scoped></style>
