<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="算法部署" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import {
    deployImage,
    deployImageId,
    formSchema,
  } from '/@/views/mineai/model/jobDetails/createDeploy/createDeploy.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'Modal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const [registerForm, { updateSchema, resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 160,
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        console.log('data', data);
        await resetFields();
        let currentModelVersionId = data.modelVersion.id;
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          namespace: 'ai-platform-s2',
          modelVersionId: data.modelVersion.id,
          image: data.modelVersion.name,
          showName: data.modelVersion.showName,
        });
        await updateSchema({
          field: 'monitorId',
          label: '选择摄像头',
          component: 'ApiSelect',
          required: true,
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
                    url: 'modelVersion/findMonitorsInfoByModelVersionShowName',
                    params: { modelVersionShowName: data.modelVersion.showName },
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

            labelField: 'monitorName',
            valueField: 'id',
            immediate: true,
          },
          ifShow: true,
          colProps: { span: 120 },
        });
        await updateSchema({
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
                    params: { id: currentModelVersionId },
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

        await maHttp
          .get(
            {
              url: 'modelVersion/findModelVersionByModelVersionShowName',
              params: { modelVersionShowName: data.modelVersion.showName },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            deployImageId.value = v.id;
            deployImage.value = v.name;
            console.log('ddddd:', deployImageId.value);
            console.log('dddd', v);
            return v;
          });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          let deploymentValues = {};
          deploymentValues['modelVersion'] = { id: deployImageId.value };
          deploymentValues['controller'] = { id: values.controllerId };
          deploymentValues['monitor'] = { id: values.monitorId };
          deploymentValues['image'] = deployImage.value;
          deploymentValues['namespace'] = 'ai-platform-s2';
          deploymentValues['weightPath'] = values.weightPath;
          deploymentValues['weightRootPath'] = 'weight';
          deploymentValues['gpuNum'] = values.gpus;
          deploymentValues['cpuNum'] = values.cpus;
          deploymentValues['memoryNum'] = values.memory;
          deploymentValues['label'] = 'm1';
          deploymentValues['deploymentName'] =
            'deploy' +
            '-' +
            values.modelVersionId +
            '-' +
            values.controllerId +
            '-' +
            values.monitorId;
          setModalProps({ confirmLoading: true });
          await maHttp.post(
            {
              url: 'modelDeployment/addModelDeployment',
              params: deploymentValues,
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
        registerModal,
        registerForm,
        handleSubmit,
      };
    },
  });
</script>
