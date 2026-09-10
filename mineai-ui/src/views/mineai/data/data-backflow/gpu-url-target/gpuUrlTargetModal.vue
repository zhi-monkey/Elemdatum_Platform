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
  import { createGpuUrlTarget, updateGpuUrlTarget } from '../api';
  import { createGpuUrlTargetFormSchema } from './gpuUrlTargetData';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 180,
    schemas: createGpuUrlTargetFormSchema(),
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false, width: 860 });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
        uploadUrl: buildUploadUrl(data.record),
      });
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增分析服务器' : '编辑分析服务器'));

  function parseUrl(value: string) {
    try {
      const parsed = new URL((value || '').trim());
      if (!parsed.hostname || !parsed.port) {
        return null;
      }
      const port = Number(parsed.port);
      if (!Number.isFinite(port) || port <= 0 || port > 65535) {
        return null;
      }
      return { ip: parsed.hostname, port };
    } catch (error) {
      return null;
    }
  }

  function buildUploadUrl(record?: Recordable) {
    if (!record?.platformIp || !record?.platformPort) return '';
    return `http://${record.platformIp}:${record.platformPort}/api/sys-ai-dev/uploadAppZip`;
  }

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      const uploadTarget = parseUrl(values.uploadUrl);
      if (!uploadTarget) {
        createMessage.warning('视频平台接口地址格式不正确，且必须显式包含端口');
        return;
      }
      if (!values.ip || !values.port) {
        createMessage.warning('请填写分析服务器IP和分析服务器端口');
        return;
      }
      values.platformIp = uploadTarget.ip;
      values.platformPort = uploadTarget.port;
      values.gpuUrl = `http://${values.ip}:${values.port}`;
      values.port = Number(values.port);
      values.platformPort = Number(values.platformPort);
      delete values.uploadUrl;
      if (unref(isUpdate) && values.id) {
        await updateGpuUrlTarget(values);
      } else {
        await createGpuUrlTarget(values);
      }
      createMessage.success(unref(isUpdate) ? '更新成功！' : '新增成功！');
      emit('success');
      closeModal();
    } catch (error) {
      createMessage.error('保存失败，请检查填写内容');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
