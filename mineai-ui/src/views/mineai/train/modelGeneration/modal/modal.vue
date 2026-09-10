<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm :schemas="schema" @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { computed, defineComponent, ref, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { addFormSchema, reuseFormSchema } from '../modelGeneration';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useUserStore } from '/@/store/modules/user';

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;

  export default defineComponent({
    name: 'ModelGenerationModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const schema = ref<FormSchema[]>();
      const isReuse = ref(false);
      const isUpdate = ref();

      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 150,
        showActionButtonGroup: false,
      });

      const getTitle = computed(() => (!unref(isReuse) ? '新增训练任务' : '新建引导任务'));

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        if (data.isReuse) {
          schema.value = reuseFormSchema;
          await resetFields();
          setModalProps({ confirmLoading: false });
          await setFieldsValue({
            isAddModel: 0,
          });
        } else {
          schema.value = addFormSchema;
        }
        isUpdate.value = false;
        isReuse.value = data.isReuse;
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });
          if (!isUpdate.value) {
            let modelId;
            if (values.isAddModel === 1) {
              await maHttp
                .post(
                  {
                    url: 'model/addModel',
                    params: {
                      modelName: values.modelName,
                      modelEnglishName: values.modelEnglishName,
                      monitorType: 1,
                      datasetType: '图像',
                      description: values.description,
                    },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then((v) => {
                  modelId = v;
                });
              isReuse.value = false;
            } else {
              modelId = values.model;
              isReuse.value = true;
            }
            await maHttp.post(
              {
                url: 'modelGeneration/addModelGeneration',
                params: {
                  userId: userData.id,
                  name: values.name,
                  model: { id: modelId },
                  // trainImageDescription: values.trainImageDescription,
                  // trainImageUrl: values.trainImageUrl,
                  // testImageDescription: values.testImageDescription,
                  // testTrainImageUrl: values.testTrainImageUrl,
                  // deployImageDescription: values.deployImageDescription,
                  // deployImageUrl: values.deployImageUrl,
                  // trainDataset: { id: values.trainDataset },
                  // trainSize: values.trainSize,
                  // testDataset: { id: values.testDataset },
                  // resource: { id: values.resource },
                  isReuse: isReuse.value,
                  isGuided: false,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );
          } else {
            await maHttp.post(
              {
                url: 'modelGeneration/updateModelGeneration',
                params: {
                  name: values.name,
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
          }
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
        getTitle,
      };
    },
  });
</script>
