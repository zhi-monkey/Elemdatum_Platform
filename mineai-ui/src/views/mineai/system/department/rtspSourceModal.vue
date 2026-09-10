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
  import { rtspFormSchema } from './rtspSourceData';
  import { getRtspPublicKey, saveOrUpdateRtspSource } from '/@/views/mineai/system/api/rtspSource';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
    labelWidth: 110,
    schemas: rtspFormSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    await updateSchema({
      field: 'authRequired',
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
        disabled: unref(isUpdate),
      },
    });
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
        authRequired: !!data.record?.username,
        password: '',
      });
    } else {
      await setFieldsValue({
        authRequired: false,
      });
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增RTSP流' : '编辑RTSP流'));

  async function encryptPassword(password: string) {
    const publicKey = await getRtspPublicKey();
    const encryptor = new JSEncrypt();
    encryptor.setPublicKey(publicKey);
    const encrypted = encryptor.encrypt(password);
    if (!encrypted) {
      throw new Error('密码加密失败');
    }
    return encrypted;
  }

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (!values.authRequired) {
        values.username = undefined;
        values.password = undefined;
      }
      if (values.authRequired && values.password && values.password.trim()) {
        values.password = await encryptPassword(values.password.trim());
      }
      await saveOrUpdateRtspSource(values);
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
