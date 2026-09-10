<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from './chip.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { addAndUpdateChipManager, checkChipTypeUnique } from './chipDetailApi';

  export default defineComponent({
    name: 'ChipModal',
    components: { BasicModal, BasicForm },
    emits: ['success'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const { createMessage } = useMessage();
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

      const getTitle = computed(() => (!unref(isUpdate) ? '新增算力芯片' : '编辑算力芯片'));

      async function handleSubmit() {
        let values: any;
        try {
          values = await validate();
        } catch (e) {
          return;
        }

        try {
          if (!values) return;
          if (await checkChipTypeUnique(values.chipType)) {
            throw new Error('算力芯片类型已存在');
          }
          await addAndUpdateChipManager(values);

          createMessage.success(isUpdate.value ? '更新成功！' : '新增成功！');
          emit('success', values, unref(isUpdate));
          closeModal();
        } catch (error) {
          console.error('操作失败:', error);
          createMessage.error(error.message ? error.message : '操作失败，请重试！');
        }
      }

      return {
        handleSubmit,
        registerModal,
        registerForm,
        getTitle,
      };
    },
  });
</script>

<style scoped lang="less"></style>
