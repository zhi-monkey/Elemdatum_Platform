<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from '/@/views/mineai/application/message/applicationDetail/model.data';

  export default defineComponent({
    name: 'ApplicationModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 90,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: true });
        isUpdate.value = !!data?.isUpdate;
        if (unref(isUpdate)) {
          await setFieldsValue({
            ...data.record,
          });
        }
        setModalProps({ confirmLoading: false });
      });
      const getTitle = computed(() => (!unref(isUpdate) ? '新增应用' : '编辑应用'));

      // 提交表单
      const handleSubmit = async () => {
        try {
          const valid = await validate();
          if (valid) {
            emit('success', valid, unref(isUpdate));
            closeModal();
          }
        } catch (error) {
          console.error('表单验证失败', error);
        }
      };

      return {
        registerModal,
        registerForm,
        getTitle,
        handleSubmit,
      };
    },
  });
</script>

<style scoped lang="less"></style>
