<template>
  <BasicModal v-bind="$attrs" @register="registerConfigModal" title="自定义配置" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ConfigurationSchemas } from './data';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  const [registerConfigModal, { closeModal }] = useModalInner(() => {});
  const { createMessage } = useMessage();
  const [registerForm, { resetFields, validate }] = useForm({
    labelWidth: 135,
    showActionButtonGroup: false,
    schemas: ConfigurationSchemas,
  });

  async function handleSubmit() {
    let values;
    try {
      values = await validate();
    } catch (e) {
      return;
    }
    try {
      await maHttp.post(
        {
          url: 'hardwareParams/saveHardwareParams',
          data: {
            ...values,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      closeModal();
      await resetFields();
      createMessage.success('新增配置成功');
    } catch (error) {
      createMessage.error('新增配置失败');
    }
  }
</script>
