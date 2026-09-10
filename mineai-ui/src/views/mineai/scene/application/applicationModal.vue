<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from './model.data';

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

      return {
        registerModal,
        registerForm,
        getTitle,
      };
    },
  });
</script>

<style scoped lang="less"></style>
