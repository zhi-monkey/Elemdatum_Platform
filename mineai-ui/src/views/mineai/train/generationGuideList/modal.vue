<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="新建引导任务" @ok="handleSubmit">
    <BasicForm :schemas="schema" @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { reuseFormSchema } from './generationGuideList.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import {useUserStore} from "/@/store/modules/user";

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
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

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
        schema.value = reuseFormSchema;
        await resetFields();
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          isAddModel: 0,
        });
      });

      async function handleSubmit() {
        try {
          let modelId;
          const values = await validate();
          setModalProps({ confirmLoading: true });
          modelId = values.model;
          await maHttp.post(
            {
              url: 'modelGeneration/addModelGeneration',
              params: {
                userId: userData.id,
                name: values.name,
                model: { id: modelId },
                isReuse: true,
                isGuided: 1,
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
