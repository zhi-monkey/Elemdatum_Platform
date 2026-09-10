<template>
  <div class="step">
    <div class="step-form">
      <BasicForm @register="register" />
    </div>
    <Divider />
    <h3>说明</h3>
    <p>将算法模型绑定到对应服务和监控设备。</p>
  </div>
</template>
<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { Divider } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { bindSchemas, selectedMonitor } from './data';
  import { onMounted } from 'vue';

  const emit = defineEmits(['next']);
  const props = defineProps<{ modelVersionId: number; modelVersionShowName: string }>();

  const { createMessage } = useMessage();

  const [register, { validate, setFieldsValue }] = useForm({
    labelWidth: 100,
    schemas: bindSchemas,
    actionColOptions: {
      span: 14,
    },
    showResetButton: false,
    submitButtonOptions: {
      text: '绑定',
    },
    submitFunc: customSubmitFunc,
  });

  async function customSubmitFunc() {
    try {
      const values = await validate();
      await maHttp
        .post(
          {
            url: 'mineServiceModel/bindModel',
            params: {
              mineServiceId: values.mineServiceId,
              modelVersionIds: [Number(values.modelVersionIds)],
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
        )
        .then(async () => {
          await maHttp.post(
            {
              url: 'modelVersion/bindModelVersion',
              params: {
                monitorId: values.monitorId,
                modelVersionIds: [Number(values.modelVersionIds)],
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
        })
        .then(() => {
          createMessage.success('绑定成功');
          emit('next', selectedMonitor.value);
        });
    } catch (error) {
      console.log(error);
    }
  }

  onMounted(() => {
    setFieldsValue({
      modelVersionIds: props.modelVersionId,
      modelVersionShowName: props.modelVersionShowName,
    });
  });
</script>
<style scoped lang="less">
  .step {
    &-form {
      width: 450px;
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
