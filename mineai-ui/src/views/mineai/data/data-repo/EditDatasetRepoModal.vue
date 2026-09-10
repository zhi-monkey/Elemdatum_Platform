<template>
  <BasicModal
    v-bind="$attrs"
    title="编辑数据仓库"
    width="500px"
    showFooter
    @ok="handleSubmit"
    @register="registerModal"
  >
    <BasicForm @register="registerForm" class="myForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { defineEmits, ref, unref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { BasicForm, useForm } from '/@/components/Form';
  import { editFormSchema } from '/@/views/mineai/data/data-repo/dataset.repo';
  import { useModalInner } from '/@/components/Modal';

  const isUpdate = ref(true);
  const emits = defineEmits(['success']);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 125,
    schemas: editFormSchema,
    showActionButtonGroup: false,
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await maHttp.put(
        {
          url: `datarepos/${values.id}`,
          data: values,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      closeModal();
      emits('success',true);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

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
</script>

<style scoped></style>
