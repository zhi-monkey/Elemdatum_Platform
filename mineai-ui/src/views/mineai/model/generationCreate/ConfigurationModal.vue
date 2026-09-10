<template>
  <BasicModal v-bind="$attrs" @register="registerConfigModal" title="自定义配置" @ok="handleSubmit">
    <BasicForm :schemas="schema" @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { ConfigurationSchemas } from './data';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { useGo } from '/@/hooks/web/usePage';
  import { maHttp } from '/@/utils/http/axios';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    components: { BasicModal, BasicForm, ConfigurationSchemas },
    props: {
      data: { type: Object },
    },
    emits: ['success'],
    setup(_, {}) {
      const [registerConfigModal, { closeModal }] = useModalInner(() => {});
      const go = useGo();
      const schema = ref<FormSchema[]>();
      const { createMessage } = useMessage();
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 135,
        showActionButtonGroup: false,
      });
      schema.value = ConfigurationSchemas;
      const { closeCurrent } = useTabs();

      async function handleSubmit() {
        try {
          const values = await validate();
          await maHttp
            .get(
              {
                url: 'hardwareParams/saveHardwareParams',
                params: {
                  cpu: values.cpus,
                  memory: values.memory,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then(() => {
              closeModal();
              createMessage.success('新增配置成功');
            });
        } catch (error) {
          createMessage.error('新增配置失败');
        }
      }

      return {
        ConfigurationSchemas,
        registerConfigModal,
        handleSubmit,
        closeCurrent,
        go,
        registerForm,
        resetFields,
        setFieldsValue,
        validate,
        schema,
      };
    },
  });
</script>
