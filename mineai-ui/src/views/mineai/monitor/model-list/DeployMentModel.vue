<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="算法部署" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './DeployMent.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    name: 'Modal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_) {
      const [registerForm, { resetFields, validate }] = useForm({
        labelWidth: 160,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const modelId = ref(0);
      const { createMessage } = useMessage();
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        modelId.value = data.record.id;
        setModalProps({ confirmLoading: false });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          let deploymentValues = {};
          deploymentValues['model'] = { id: modelId.value };
          await maHttp
            .get(
              {
                url: 'controller/findControllerById',
                params: { id: values.controllerId },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
            )
            .then((v) => {
              deploymentValues['controller'] = v;
            });
          deploymentValues['monitor'] = { id: values.monitorId };
          deploymentValues['gpuNum'] = values.gpus;
          deploymentValues['cpuNum'] = values.cpus;
          deploymentValues['memoryNum'] = values.memory;
          deploymentValues['deploymentName'] =
            'deploy' + '-' + modelId.value + '-' + values.controllerId + '-' + values.monitorId;
          setModalProps({ confirmLoading: true });
          console.log(deploymentValues);
          if (values.architecture === 'amd64') {
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
          } else {
            // 根据接口获取算法名字，监控设备名字，原始流地址，推流地址。
            const deploymentData: any[] = await Promise.all([
              maHttp.get(
                {
                  url: 'modelVersion/findModelVersionById',
                  params: { id: values.modelVersionId },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              ),
              maHttp.get(
                {
                  url: 'monitor/findMonitorById',
                  params: { monitorId: values.monitorId },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
              ),
              maHttp.get(
                {
                  url: 'controller/findStreamUrlByModelVersionIdAndMonitorId',
                  params: { modelVersionId: values.modelVersionId, monitorId: values.monitorId },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
              ),
            ]);
            deploymentValues['modelVersionName'] = deploymentData[0]['name'];
            deploymentValues['monitorName'] = deploymentData[1]['monitorName'];
            deploymentValues['monitorUrl'] = deploymentData[1]['protocolMetaData'];
            deploymentValues['streamUrl'] = deploymentData[2];
            // todo: 创建新的deployment,并保存deploymentId
            deploymentValues['deploymentId'] = await maHttp.post(
              {
                url: 'modelDeployment/addArm64ModelDeployment',
                params: deploymentValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );
            // 调用守护进程创建模型部署任务接口
            await maHttp.post(
              {
                url: 'controllerEdge/modelDeploy',
                params: deploymentValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
            );
          }
          closeModal();
          createMessage.success('算法部署成功！');
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
