<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="申请发布" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { publishFormSchema } from '/@/views/mineai/refPage/modelGeneration/Modal/ReleaseModel.data';

  const emit = defineEmits(['success', 'register']);
  const [registerForm, { resetFields, validate }] = useForm({
    labelWidth: 100,
    schemas: publishFormSchema,
    showActionButtonGroup: false,
  });
  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped lang="less"></style>
