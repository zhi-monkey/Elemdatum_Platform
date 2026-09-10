<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="自定义推理流视频名称"
    @ok="handleSubmit"
    :loading="loading"
  >
    <li style="margin-bottom: 10px">请输入推理流视频名称</li>
    <a-input v-model:value="value" />
  </BasicModal>
  <ConfigModal @register="registerConfigModal" @success="handleSuccess" />
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { Input as AInput } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ConfigModal from '/@/views/mineai/controller/scene-list/ConfigModal.vue';
  let value = ref<string>('');

  const { createMessage } = useMessage();
  const props = defineProps<{
    modalId: Number;
    dataId: Number;
  }>();
  let modelId = ref();
  const [registerConfigModal, { openModal: openConfigModal }] = useModal();
  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    value.value = data.videoName;
    console.log(value.value);
  });

  let loading = ref<boolean>(false);
  async function handleSubmit() {
    loading.value = true;
    console.log(value.value);
    if (value.value === null) {
      value.value = '';
    }
    await maHttp
      .get(
        {
          url: 'monitorModelConfig/saveVideoName',
          params: { modelId: props.dataId, monitorId: props.modalId, videoName: value.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then((v) => {
        loading.value = false;
        createMessage.success('编辑成功！');
        closeModal();
        modelId.value = props.dataId;
        console.log(v);
        if (v === true) {
          openConfigModal(true, {});
        }
      });
  }
</script>
