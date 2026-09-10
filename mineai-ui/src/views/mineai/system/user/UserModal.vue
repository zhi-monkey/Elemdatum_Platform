<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm">
      <template #roles="{ model, field }">
        <form-select-item
          v-model:data="model[field]"
          mode="single"
          :options="options"
          placeholder="请选择角色"
          @update-data="(value) => (model[field] = value)"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { FormSelectItem, itemType } from '/@/components/FormSelectItem';
  import { formSchema } from './user.data';
  import { getAll } from '/@/views/mineai/system/api/role';
  import { add, edit } from '/@/views/mineai/system/api/user';

  const emits = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const options = ref<itemType[]>([]);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: true });
    // 在回显表单数据之前加载select组建的options
    options.value = await getRoles();
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
        roles: data.record.roles[0].id,
      });
    }
    setModalProps({ confirmLoading: false });
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增用户' : '编辑用户'));

  async function handleSubmit() {
    try {
      const values = await validate();
      console.log(values);
      setModalProps({ confirmLoading: true });
      !unref(isUpdate)
        ? await add({ ...values, roles: [{ id: values.roles }] })
        : await edit({ ...values, roles: [{ id: values.roles }] });
      closeModal();
      emits('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  async function getRoles() {
    return await getAll().then((v) => {
      let options: itemType[] = [];
      v.forEach((item) => {
        options.push({ value: String(item.id), label: item.name });
      });
      return options;
    });
  }
</script>
