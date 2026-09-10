<template>
  <BasicModal
    v-bind="$attrs"
    title="配置下发"
    width="400px"
    ok-text="确认"
    :centered="true"
    :body-style="{ 'max-height': '100px' }"
    @ok="handleConfig"
    @cancel="handleCancel"
    @register="registerMonitorConfigModal"
    :loading="loading"
  >
    <div style="text-align: left; margin-top: 0; font-size: 17px">
      配置信息已修改，请确认是否下发。点击确认将进行自动配置下发，取消将还原修改。
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { createMessage } = useMessage();
  const emit = defineEmits(['success']);
  let loading = ref<Boolean>(false);
  let monitorId = ref();
  let oriUrl = ref();
  // const props = defineProps<{
  //   changeModel: Function;
  // }>();
  const [registerMonitorConfigModal, { closeModal }] = useModalInner(async (data) => {
    monitorId.value = data.monitorId;
    oriUrl.value = data.url;
  });

  function handleConfig() {
    loading.value = true;
    maHttp
      .get(
        {
          url: 'controller/createAllJsonConfigByMonitor',
          params: { monitorId: monitorId.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
      )
      .then(() => {
        createMessage.success('配置下发成功！');
        loading.value = false;
        closeModal();
      })
      .catch(() => {
        createMessage.error('配置下发失败！');
        handleCancel();
        loading.value = false;
        closeModal();
      });
  }

  function handleCancel() {
    loading.value = true;
    maHttp
      .get(
        {
          url: 'monitor/changeMonitorUrlByMonitorId',
          params: { monitorId: monitorId.value, url: oriUrl.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then(() => {
        loading.value = false;
        emit('success');
        closeModal();
        createMessage.success('取消成功！修改已回退。');
      })
      .catch(location.reload);
  }
</script>
