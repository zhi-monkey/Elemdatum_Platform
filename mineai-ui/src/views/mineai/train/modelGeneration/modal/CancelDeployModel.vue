<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="取消部署" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { defineComponent } from 'vue';
  import { formSchema, modelVersionId } from './CancelDelpoy.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  export default defineComponent({
    name: 'CanCelDeployModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_) {
      const [registerForm, { resetFields, validate }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const { createMessage } = useMessage();
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();

        modelVersionId.value = data.record.deployModelVersion.id;

        setModalProps({ confirmLoading: false });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          const deploymentInfo = JSON.parse(values.deploy);
          let id = deploymentInfo.id;
          console.log(typeof id);
          setModalProps({ confirmLoading: true });
          await maHttp.get(
            {
              url: 'deploy/deleteDeploy',
              params: {
                deployName: deploymentInfo.deploymentName,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
          await maHttp.post(
            {
              url: 'modelDeployment/deleteModelDeploymentById',
              data: {
                id: id,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
          closeModal();
          createMessage.success('算法取消部署成功！');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerModal, registerForm, handleSubmit };
    },
  });
</script>
<style scoped lang="less"></style>
