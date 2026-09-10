<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="作业质检" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import {
    gpuRemained,
    hyperParamPrefix,
    jobSchemas,
  } from '/@/views/mineai/model/jobDetails/createJob/createInspectJob.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'Model',
    components: { BasicForm, BasicModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
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
      let hyperParamList: string[] = [];
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        let modelId = data.modelVersion.model.id;
        hyperParamList.forEach(async (value) => {
          await removeSchemaByFiled(hyperParamPrefix + value);
        });
        await resetFields();
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          modelName: data.modelVersion.model.modelName,
          modelVersionName: data.modelVersion.showName,
          modelVersionId: data.modelVersion.id,
        });
        await updateSchema({
          field: 'dataset',
          label: '数据集',
          component: 'ApiSelect',
          componentProps: {
            dropdownAlign: {
              overflow: {
                adjustY: false, // 关闭下拉框垂直位置自适应
              },
            },
            api: () =>
              maHttp
                .get(
                  {
                    url: 'modelDataset/findDatasetByDataType',
                    params: { modelId: modelId },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then((v) => {
                  return v;
                }),
            labelField: 'name',
            valueField: 'id',
            immediate: false,
            placeholder: '请选择作业需要的数据集',
          },
          ifShow: true,
          colProps: { span: 120 },
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
            console.log(v);
            gpuRemained.value = v.gpuRemained;
            setFieldsValue({ gpu: v.gpuRemained + '(GiB)' + ' / ' + v.gpuNum + '(GiB)' });
          });

        await maHttp
          .get(
            {
              url: 'modelVersion/findModelVersionById',
              params: { id: data.modelVersion.id },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            updateSchema({
              field: 'weightPath',
              label: '选择权重文件',
              component: 'ApiSelect',
              componentProps: {
                placeholder: '请选择权重文件',
                dropdownAlign: {
                  overflow: {
                    adjustY: false, // 关闭下拉框垂直位置自适应
                  },
                },
                api: () =>
                  maHttp
                    .get(
                      {
                        url: 'modelJob/getJobByModelVersionId',
                        params: { id: data.modelVersion.id },
                        headers: {
                          // @ts-ignore
                          ignoreCancelToken: true,
                        },
                      },
                      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                    )
                    .then((v) => {
                      return v;
                    }),
                labelField: 'description',
                valueField: 'name',
                immediate: false,
              },
              show: true,
            });
            setFieldsValue({ jobType: 2 });

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
        modelValues['modelVersion'] = values['modelVersion'];
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
            values.modelVersion = v;
            modelValues['modelVersion'] = v;
            // modelValues[]
          });

        modelValues['dataset'] = { id: values.dataset };
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
      };
    },
  });
</script>
