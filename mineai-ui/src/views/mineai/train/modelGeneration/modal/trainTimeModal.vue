<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="训练时间设置" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { trainJobTimeSchema } from '../modelGeneration';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'TrainTimeModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const modelGenerationId = ref(0);
      const { createMessage } = useMessage();
      const [registerForm, { resetFields, validate }] = useForm({
        labelWidth: 200,
        schemas: trainJobTimeSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: false });
        modelGenerationId.value = data.modelGenerationId;
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          await maHttp
            .get(
              {
                url: 'modelJob/trainJob',
                params: {
                  modelGenerationId: modelGenerationId.value,
                  trainTime: values.trainTime,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then(async () => {
              await maHttp.get(
                {
                  url: 'modelJob/stopTrainJob',
                  params: {
                    modelGenerationId: modelGenerationId.value,
                    trainTime: values.trainTime,
                    stopTime: values.stopTime,
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
              createMessage.success('延时训练作业创建成功');
            });
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
