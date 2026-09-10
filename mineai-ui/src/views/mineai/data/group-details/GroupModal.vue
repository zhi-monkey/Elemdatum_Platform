<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { computed, ref, unref } from 'vue';
  import { formSchema } from './data';
  import { addNewTeam, updateTeam } from '/@/views/mineai/data/dataset-details2/api';

  const emits = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增团队' : '编辑团队'));

  async function handleSubmit() {
    try {
      const values = await validate();
      const data = {
        id: !unref(isUpdate) ? undefined : values.id,
        name: values.name,
        remark: values.remark,
        memberNum: values.userIds.length,
        userIds: values.userIds,
        // 无用字段
        type: 0,
      };
      setModalProps({ confirmLoading: true });
      !unref(isUpdate) ? await addNewTeam(data) : await updateTeam(data);
      closeModal();
      emits('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped lang="less"></style>
