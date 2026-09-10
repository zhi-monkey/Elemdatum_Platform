<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="修改密码" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { adminFormSchema } from './pwd.data';
  import { updatePassByAdmin } from '/@/views/mineai/system/api/user';
  import { encrypt } from '/@/utils/dubhe/rsaEncrypt';

  const emits = defineEmits(['success', 'register']);

  const userId = ref(-1);

  const [registerForm, { resetFields, validate }] = useForm({
    labelWidth: 100,
    schemas: adminFormSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: true });
    await resetFields();
    userId.value = data.record.id;
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await updatePassByAdmin(userId.value, { newPass: encrypt(values.newPass) });
      closeModal();
      emits('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
