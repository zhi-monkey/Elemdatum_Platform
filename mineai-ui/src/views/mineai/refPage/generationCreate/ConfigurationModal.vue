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
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 135,
        showActionButtonGroup: false,
      });
      schema.value = ConfigurationSchemas;
      const { closeCurrent } = useTabs();
      async function handleSubmit() {
        try {
          const values = await validate();
          console.log(values.cpus);
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
              emit('success');
            });
        } catch (error) {
          console.log(error);
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
