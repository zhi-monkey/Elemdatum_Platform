<template>
  <BasicModal
    v-bind="$attrs"
    title="新增数据仓库"
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
  import { formSchema } from '/@/views/mineai/data/data-repo/dataset.repo';
  import { useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();

  const isUpdate = ref(true);
  const emits = defineEmits(['success']);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 125,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      const nameExistsResponse = await maHttp.get(
        {
          url: `datarepos/isreponameexist/${values.name}`, // 假设 name 从表单中获取
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );

      if (nameExistsResponse) {
        createMessage.error('该数据集仓库名称已存在，请使用其他名称');
        return; // 退出函数，不继续执行后续逻辑
      }
      setModalProps({ confirmLoading: true });
      await maHttp.post(
        {
          url: 'datarepos',
          data: values,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      closeModal();
      emits('success', false);
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
