<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from './device.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { addAndUpdateDeviceManager } from '/@/views/mineai/application/message/deviceDetail/deviceDetailApi.tsx';

  export default defineComponent({
    name: 'DeviceModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const { createMessage } = useMessage();
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 100,
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

            chipType: data.record.chip.chipType,
          });
        }
        setModalProps({ confirmLoading: false });
      });
      const getTitle = computed(() => (!unref(isUpdate) ? '新增固件' : '编辑固件'));

      async function handleSubmit() {
        let values = null;
        try {
          values = await validate();
        } catch (e) {}

        try {
          if (!values) return;
          closeModal();
          await addAndUpdateDeviceManager(values);
          await setFieldsValue(values);
          createMessage.success(isUpdate.value ? '更新成功！' : '新增成功！');
          emit('success', values);
        } catch (error) {
          console.error('操作失败:', error);
          createMessage.error('操作失败，请重试！');
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
