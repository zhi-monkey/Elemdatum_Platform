<template>
  <div class="step">
    <div class="step-form">
      <BasicForm @register="register" />
    </div>
    <Divider />
    <h3>...</h3>
    <p>......</p>
  </div>
</template>

<script setup lang="ts">
  import { Divider } from 'ant-design-vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { jobSchemas, hyperParamPrefix, gpuRemained } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted } from 'vue';

  const { createMessage } = useMessage();

  const emit = defineEmits(['next']);
  const props = defineProps<{
    modelVersion: any;
    jobType: number;
  }>();

  const [register, { updateSchema, setFieldsValue, appendSchemaByField, validate }] = useForm({
    labelWidth: 200,
    schemas: jobSchemas,
    actionColOptions: {
      span: 14,
    },
    showResetButton: false,
    submitButtonOptions: {
      text: '下一步',
    },
    submitFunc: customSubmitFunc,
  });

  async function customSubmitFunc() {
    try {
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
      modelValues['modelVersion'] = props.modelVersion;
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
        .then((jobId) => {
          createMessage.success('新建作业成功！');
          emit('next', jobId);
        });
    } catch (error) {
      console.log(error);
    }
  }

  onMounted(async () => {
    await setFieldsValue({
      modelName: props.modelVersion.model.modelName,
      modelVersionId: props.modelVersion.id,
      modelVersionShowName: props.modelVersion.showName,
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
                params: { modelId: props.modelVersion.model.id },
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
        gpuRemained.value = Number(v.gpuRemained);
        setFieldsValue({ gpu: v.gpuRemained + '(GiB)' + ' / ' + v.gpuNum + '(GiB)' });
      });

    await maHttp
      .get(
        {
          url: 'modelVersion/findModelVersionById',
          params: { id: props.modelVersion.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        if (props.jobType === 1) {
          updateSchema({ field: 'weightPath', show: false });
          setFieldsValue({ jobType: 1 });
        } else {
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
                      params: { id: props.modelVersion.id },
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
        }

        //超参输入框
        if (v.modelConfigList != null) {
          v.modelConfigList.forEach((e) => {
            if (e.field != null && e.field != '') {
              const field = e.field;
              appendSchemaByField(
                {
                  field: hyperParamPrefix + field,
                  label: field,
                  component: 'Input',
                  required: true,
                },
                undefined,
                undefined,
              );
              if (field === 'HP_BATCH_SIZE') {
                setFieldsValue({ hyperParamPrefixHP_BATCH_SIZE: '20' });
              } else if (field === 'HP_EPOCHES') {
                setFieldsValue({ hyperParamPrefixHP_EPOCHES: '20' });
              } else if (field === 'HP_LEARNING_RATE') {
                setFieldsValue({ hyperParamPrefixHP_LEARNING_RATE: '0.001' });
              }
            }
          });
        }
      });
  });
</script>
<style scoped lang="less">
  .step {
    &-form {
      width: 580px;
      margin: 0 auto;
    }

    h3 {
      margin: 0 0 12px 10px;
      font-size: 16px;
      line-height: 32px;
    }

    h4 {
      margin: 0 0 4px 10px;
      font-size: 14px;
      line-height: 22px;
    }

    p {
      margin-left: 10px;
    }
  }
</style>
