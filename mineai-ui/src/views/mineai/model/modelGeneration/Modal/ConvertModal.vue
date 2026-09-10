<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="模型转换" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formSchema } from './ConvertModel.data';
  import { ref } from 'vue';

  const emit = defineEmits(['success', 'register']);

  const modelGenerationId = ref(-1);
  const image = ref('');
  const weightPath = ref('');
  const gpuNum = ref(0);

  const [registerForm, { resetFields, validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
  });
  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    modelGenerationId.value = data.modelGenerationId;
    image.value = data.image;
    weightPath.value = data.weightPath;
    gpuNum.value = data.gpuNum;
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      const curTime = new Date();
      const jobName =
        weightPath.value +
        '-convert-' +
        curTime.getFullYear() +
        '-' +
        (curTime.getMonth() + 1).toString() +
        '-' +
        curTime.getDate() +
        '-' +
        curTime.getHours() +
        '-' +
        curTime.getMinutes() +
        '-' +
        curTime.getSeconds();
      setModalProps({ confirmLoading: true });
      await maHttp.post(
        {
          url: `modelJob/createConvertJob?modelGenerationId=${modelGenerationId.value}`,
          params: {
            jobName: jobName,
            image: image.value,
            datasetPath: values.datasetPath + '/origin',
            weightPath: weightPath.value,
            outputWeightPath: jobName,
            gpuNum: gpuNum.value,
            params: {
              TARGET_PLATFORM: values.type,
              INPUT_MODEL_NAME: 'best.onnx',
              OUTPUT_MODEL_NAME: 'best.rknn',
              RKNN_QUANTIZED_DTYPE: 'asymmetric_quantized-u8',
              RKNN_QUANTIZED_IMG_NUM: 100,
              REORDER_CHANNEL: 'RGB',
              RKNN_MEAN_VALUES: '0_0_0',
              RKNN_STD_VALUES: '255_255_255',
            },
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      closeModal();
      createMessage.success('算法转换执行成功');
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped lang="less"></style>
