<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script setup lang="ts">
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createInferenceDevice, updateInferenceDevice } from './api';
  import { inferenceDeviceFormSchema } from './inferenceDeviceData';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate, updateSchema, clearValidate }] =
    useForm({
      labelWidth: 120,
      schemas: inferenceDeviceFormSchema,
      showActionButtonGroup: false,
    });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    await updateSchema({
      field: 'authCode',
      dynamicRules: () => {
        if (unref(isUpdate)) {
          return [];
        }
        return [{ required: true, message: '请输入授权码', trigger: 'blur' }];
      },
    });
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
        authCode: '',
      });
    }
    await clearValidate('authCode');
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增模型接收地址' : '编辑模型接收地址'));

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (!values.authCode || !values.authCode.trim()) {
        values.authCode = undefined;
      }
      if (unref(isUpdate) && values.id) {
        await updateInferenceDevice(values.id, values);
      } else {
        await createInferenceDevice(values);
      }
      createMessage.success(unref(isUpdate) ? '更新成功！' : '新增成功！');
      emit('success');
      closeModal();
    } catch (error) {
      createMessage.error('保存失败，请重试');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style scoped>
  :deep(.ant-input:-webkit-autofill),
  :deep(.ant-input-affix-wrapper input:-webkit-autofill),
  :deep(.ant-input-affix-wrapper input:-webkit-autofill:hover),
  :deep(.ant-input-affix-wrapper input:-webkit-autofill:focus) {
    -webkit-text-fill-color: inherit !important;
    transition: background-color 5000s ease-in-out 0s !important;
  }

  [data-theme='dark'] :deep(.ant-input:-webkit-autofill),
  [data-theme='dark'] :deep(.ant-input-affix-wrapper input:-webkit-autofill),
  [data-theme='dark'] :deep(.ant-input-affix-wrapper input:-webkit-autofill:hover),
  [data-theme='dark'] :deep(.ant-input-affix-wrapper input:-webkit-autofill:focus) {
    -webkit-box-shadow: 0 0 0 1000px #1f2c4a inset !important;
  }
</style>
