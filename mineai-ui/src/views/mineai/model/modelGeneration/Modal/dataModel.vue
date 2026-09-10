<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="数据集绑定" @ok="handleSubmit">
    <BasicForm :schemas="schema" @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { datasetFormSchema } from '../modelGeneration';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'ModelGenerationModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const schema = ref<FormSchema[]>();

      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 135,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        schema.value = datasetFormSchema;
        await resetFields();
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          id: data.record.id,
          dataset: data.record.dataset?.id,
          trainSize: data.record.trainSize,
        });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });
          await maHttp.post(
            {
              url: 'modelGeneration/bindDataset',
              params: {
                id: values.id,
                dataset: { id: values.dataset },
                trainSize: values.trainSize,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
          closeModal();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return {
        schema,
        registerModal,
        registerForm,
        handleSubmit,
      };
    },
  });
</script>
