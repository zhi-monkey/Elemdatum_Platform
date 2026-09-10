<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script setup lang="ts">
  import { computed, ref, unref } from 'vue';
  import JSEncrypt from 'jsencrypt/bin/jsencrypt';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { httpServerFormSchema } from './httpServerData';
  import {
    createHttpCameraServer,
    getHttpCameraServerPublicKey,
    updateHttpCameraServer,
  } from '../api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 120,
    schemas: httpServerFormSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
        authToken: '',
      });
    }
  });

  const getTitle = computed(() =>
    !unref(isUpdate) ? '新增数据收集服务器' : '编辑数据收集服务器',
  );

  async function encryptToken(token: string) {
    const publicKey = await getHttpCameraServerPublicKey();
    const encryptor = new JSEncrypt();
    encryptor.setPublicKey(publicKey);
    const encrypted = encryptor.encrypt(token);
    if (!encrypted) throw new Error('Token 加密失败');
    return encrypted;
  }

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (values.authToken && values.authToken.trim()) {
        values.authToken = await encryptToken(values.authToken.trim());
      } else {
        values.authToken = undefined;
      }
      if (unref(isUpdate) && values.id) {
        await updateHttpCameraServer(values.id, values);
      } else {
        await createHttpCameraServer(values);
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
