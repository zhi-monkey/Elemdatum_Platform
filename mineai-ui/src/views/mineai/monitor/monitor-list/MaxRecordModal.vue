<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerMaxRecordModal"
    title="自定义每段视频录制时长"
    @ok="handleSubmit"
    :loading="loading"
  >
    <li style="margin-bottom: 10px">请设置每段视频录制时长（单位为分钟）</li>
    <a-input v-model:value="value" @input="validateInput" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Input as AInput } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  let value = ref<number>(1);
  const { createMessage } = useMessage();
  const props = defineProps<{
    monitorId: Number;
  }>();
  const [registerMaxRecordModal, { closeModal }] = useModalInner(async (data) => {});
  const emit = defineEmits(['success']);
  let loading = ref<boolean>(false);
  async function handleSubmit() {
    let regex = /^[1-9]\d*(\.\d+)?$/;
    if (value.value.toString() === '') {
      createMessage.error('输入不能为空！');
    } else if (regex.test(value.value.toString())) {
      loading.value = true;
      console.log(value.value);
      let maxSecond = value.value * 60;
      await maHttp
        .get(
          {
            url: 'monitor/changeMonitorRecording',
            params: { isRecord: 1, monitorId: props.monitorId, maxSecond: maxSecond },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
        )
        .then(() => {
          loading.value = false;
          createMessage.success('开始录制！');
          closeModal();
          emit('success');
        });
    } else {
      createMessage.error('请输入正数！');
    }
  }
  function validateInput() {}
</script>
