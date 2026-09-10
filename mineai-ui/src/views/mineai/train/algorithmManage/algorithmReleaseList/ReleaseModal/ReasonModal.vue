<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="拒绝原因" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { defineComponent } from 'vue';
  import { formSchema2 } from './ReleaseDelpoy.data';
  export default defineComponent({
    name: 'CanCelDeployModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_) {
      const [registerForm, { setFieldsValue }] = useForm({
        labelWidth: 100,
        schemas: formSchema2,
        showActionButtonGroup: false,
      });
      let reason = '';
      const [registerModal, { closeModal }] = useModalInner(async (data) => {
        reason = data.record.rejectReason;
        await setFieldsValue({ rejectReason: reason });
      });

      function handleSubmit() {
        closeModal();
      }
      return { registerModal, registerForm, handleSubmit };
    },
  });
</script>
<style scoped lang="less"></style>
