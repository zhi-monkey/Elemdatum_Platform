<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="作业编辑" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './jobData';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'JobModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 90,
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          ...data.record,
        });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });
          await maHttp.post(
            {
              url: 'modelJob/updateJobDescription',
              params: values,
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
        registerModal,
        registerForm,
        handleSubmit,
      };
    },
  });
</script>
