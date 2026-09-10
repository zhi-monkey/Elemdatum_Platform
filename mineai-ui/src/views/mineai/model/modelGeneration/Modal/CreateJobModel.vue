<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { computed, defineComponent, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import {
    datasetId,
    gpuRemained,
    hyperParamPrefix,
    jobSchemas,
  } from '/@/views/mineai/model/modelGeneration/Modal/CreateJob.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'Model',
    components: { BasicForm, BasicModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isTrain = ref(true);
      const jobType = ref();
      const { createMessage } = useMessage();
      const [
        registerForm,
        {
          updateSchema,
          setFieldsValue,
          resetFields,
          appendSchemaByField,
          removeSchemaByFiled,
          validate,
        },
      ] = useForm({
        labelWidth: 170,
        schemas: jobSchemas,
        showActionButtonGroup: false,
      });
      const modelGenerationId = ref(0);
      const getTitle = computed(() => (unref(isTrain) ? '模型训练' : '模型质检'));
      let hyperParamList: string[] = [];
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        jobType.value = data.jobType;
        hyperParamList.forEach(async (value) => {
          await removeSchemaByFiled(hyperParamPrefix + value);
        });
        datasetId.value = data.datasetId;
        await resetFields();
        modelGenerationId.value = data.modelGenerationId;
        setModalProps({ confirmLoading: false });
        isTrain.value = data.jobType === 1;
        await setFieldsValue({
          modelVersionId: data.modelVersionId,
          modelVersionShowName: data.modelVersionShowName,
        });
        await maHttp
          .get(
            {
              url: 'modelDataset/findDatasetById',
              params: { datasetId: datasetId.value },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            setFieldsValue({ dataset: v.name });
          });
        await maHttp
          .get(
            {
              url: 'deploy/getGpuMax',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            gpuRemained.value = Number(v.gpuRemained);
            setFieldsValue({ gpu: v.gpuRemained + '(GiB)' + ' / ' + v.gpuNum + '(GiB)' });
          });

        await maHttp
          .get(
            {
              url: 'modelVersion/findModelVersionById',
              params: { id: data.modelVersionId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(async (v) => {
            if (isTrain.value) {
              await updateSchema({ field: 'weightPath', ifShow: false });
              await setFieldsValue({ jobType: 1 });
            } else {
              await maHttp
                .get(
                  {
                    url: 'modelJob/getJobByJobId',
                    params: { id: data.trainJobId },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then((v) => {
                  setFieldsValue({ weightPath: v.name });
                });
              await setFieldsValue({ jobType: 2 });
            }
            //超参输入框
            hyperParamList = [];
            if (v.modelConfigList != null) {
              v.modelConfigList.forEach((e) => {
                if (e.field != null && e.field != '') hyperParamList.push(e.field);
              });
            }
            hyperParamList.forEach((value) => {
              appendSchemaByField(
                {
                  field: hyperParamPrefix + value,
                  label: value,
                  component: 'Input',
                  required: true,
                },
                undefined,
                undefined,
              );
              if (value === 'HP_BATCH_SIZE') {
                setFieldsValue({ hyperParamPrefixHP_BATCH_SIZE: '20' });
              } else if (value === 'HP_EPOCHES') {
                setFieldsValue({ hyperParamPrefixHP_EPOCHES: '20' });
              } else if (value === 'HP_LEARNING_RATE') {
                setFieldsValue({ hyperParamPrefixHP_LEARNING_RATE: '0.001' });
              }
            });
          });
      });

      async function handleSubmit() {
        const values = await validate();
        let modelValues = { params: {} };
        Object.keys(values).forEach((key) => {
          if (key.length <= hyperParamPrefix.length) {
            modelValues[key] = values[key];
          } else {
            const prefix = key.slice(0, hyperParamPrefix.length);
            const hyperParam = key.slice(hyperParamPrefix.length);
            if (prefix === hyperParamPrefix) {
              modelValues['params'][hyperParam] = values[key];
            } else {
              modelValues[key] = values[key];
            }
          }
        });
        const model_version_id = values.modelVersionId;
        await maHttp
          .get(
            {
              url: 'modelVersion/findModelVersionById',
              params: { id: model_version_id },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            modelValues['modelVersion'] = v;
          });

        modelValues['dataset'] = { id: datasetId.value };
        modelValues['modelGenerationId'] = modelGenerationId.value;
        await maHttp
          .post(
            {
              url: 'modelJob/addModelJob',
              params: {
                modelValues,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(() => {
            createMessage.success('新建作业成功！');
          });

        closeModal();
        emit('success');
      }

      return {
        registerModal,
        registerForm,
        handleSubmit,
        getTitle,
      };
    },
  });
</script>
