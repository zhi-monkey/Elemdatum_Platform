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
  import { BasicForm, useForm } from '/@/components/Form';
  import { Divider } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { DeploySchemas } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted } from 'vue';

  const { createMessage } = useMessage();

  const emit = defineEmits(['next']);
  const props = defineProps<{
    monitorId: number;
    monitorName: string;
    modelVersionId: number;
    modelVersionUrl: string;
    modelVersionShowName: string;
  }>();

  const [register, { setFieldsValue, appendSchemaByField, validate }] = useForm({
    labelWidth: 200,
    schemas: DeploySchemas,
    actionColOptions: {
      span: 14,
    },
    showResetButton: false,
    submitButtonOptions: {
      text: '下一步',
    },
    submitFunc: customSubmitFunc,
  });

  onMounted(async () => {
    await setFieldsValue({
      monitorId: props.monitorId,
      monitorName: props.monitorName,
      modelVersionId: props.modelVersionId,
      image: props.modelVersionUrl,
      showName: props.modelVersionShowName,
    });
    await appendSchemaByField(
      {
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
                  params: { id: props.modelVersionId },
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
      },
      'controllerId',
    );
  });

  async function customSubmitFunc() {
    try {
      const values = await validate();
      let deploymentValues = {};
      deploymentValues['modelVersion'] = { id: values.modelVersionId };
      deploymentValues['controller'] = { id: values.controllerId };
      deploymentValues['monitor'] = { id: values.monitorId };
      deploymentValues['image'] = values.image;
      deploymentValues['namespace'] = 'ai-platform-s2';
      deploymentValues['weightPath'] = values.weightPath;
      deploymentValues['weightRootPath'] = 'weight';
      deploymentValues['gpuNum'] = values.gpus;
      deploymentValues['cpuNum'] = values.cpus;
      deploymentValues['memoryNum'] = values.memory;
      deploymentValues['label'] = 'm1';
      deploymentValues['deploymentName'] =
        'deploy' + '-' + values.modelVersionId + '-' + values.controllerId + '-' + values.monitorId;
      if (values.architecture === 'amd64') {
        await maHttp
          .post(
            {
              url: 'modelDeployment/addModelDeployment',
              params: deploymentValues,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(() => {
            createMessage.success('部署成功');
            emit('next', {
              namespace: deploymentValues['namespace'],
              deployName: deploymentValues['deploymentName'],
            });
          });
      } else {
        createMessage.error('控制器架构非amd64');
      }
    } catch (error) {
      console.log(error);
    }
  }
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
