<template>
  <BasicModal
    v-bind="$attrs"
    title="开始录制"
    width="400px"
    ok-text="是"
    cancel-text="否"
    :centered="true"
    :body-style="{ 'max-height': '100px' }"
    @ok="handleStart"
    @cancel="handleCancel"
    @register="registerRecordModal"
    :loading="loading"
  >
    <div style="text-align: center; margin-top: 10px; font-size: 20px">
      数据集已绑定，是否开始录制？
    </div>
  </BasicModal>
  <MaxRecordModal @register="registerMaxRecordModal" :monitor-id="monitorId" />
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import MaxRecordModal from '/@/views/mineai/monitor/monitor-list/MaxRecordModal.vue';

  let loading = ref<Boolean>(false);
  let monitorId = ref();
  // const props = defineProps<{
  //   changeModel: Function;
  // }>();
  const [registerRecordModal, { closeModal }] = useModalInner(async (data) => {
    monitorId.value = data.monitorId;
  });

  const [registerMaxRecordModal, { openModal: openMaxRecordModal }] = useModal();

  function handleStart() {
    // loading.value = true;
    // maHttp
    //   .get(
    //     {
    //       url: 'monitor/changeMonitorRecording',
    //       params: { isRecord: 1, monitorId: monitorId.value },
    //       headers: {
    //         // @ts-ignore
    //         ignoreCancelToken: true,
    //       },
    //     },
    //     { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    //   )
    //   .then(() => {
    //     createMessage.success('开始录制！');
    //     loading.value = false;
    //     closeModal();
    //   })
    //   .catch(() => {
    //     createMessage.error('录制失败！');
    //     loading.value = false;
    //     closeModal();
    //   });
    openMaxRecordModal();
    closeModal();
  }

  function handleCancel() {
    closeModal();
  }
</script>
