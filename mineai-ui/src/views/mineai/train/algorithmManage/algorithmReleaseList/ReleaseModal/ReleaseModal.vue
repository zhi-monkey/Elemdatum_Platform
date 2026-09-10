<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="拒绝发布" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { defineComponent } from 'vue';
  import { formSchema } from './ReleaseDelpoy.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  export default defineComponent({
    name: 'CanCelDeployModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_) {
      const [registerForm, { validate }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      let generationId = 0;
      let exploreId = 0;
      const { createMessage } = useMessage();
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        generationId = data.record.modelGenerationDTO.id;
        exploreId = data.record.id;
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          await maHttp.get(
            {
              url: 'modelExplore/rejectRelease',
              params: {
                id: exploreId,
                reason: values.rejectReason,
                generationId: generationId,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
          createMessage.success('申请发布已拒绝！');
          closeModal();
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerModal, registerForm, handleSubmit };
    },
  });
</script>
<style scoped lang="less"></style>
