<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerEditMonitorVideoModal"
    title="自定义原始流视频名称"
    @ok="handleSubmit"
    :loading="loading"
  >
    <li style="margin-bottom: 10px">请输入原始流视频名称</li>
    <a-input v-model:value="value" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Input as AInput } from 'ant-design-vue';
  import { ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { createMessage } = useMessage();
  let loading = ref<boolean>(false);
  let monitorId = ref();
  let value = ref<string>('');
  const [registerEditMonitorVideoModal, { closeModal }] = useModalInner(async (data) => {
    console.log(data.record.originalStreamName);
    value.value = data.originalStreamName;
    monitorId.value = data.record.id;
  });
  async function handleSubmit() {
    loading.value = true;
    await maHttp.get(
      {
        url: 'monitor/saveOriginalStreamName',
        params: { monitorId: monitorId.value, originalStreamName: value.value },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    ),
      (loading.value = false);
    createMessage.success('编辑成功！');
    closeModal();
  }
</script>
