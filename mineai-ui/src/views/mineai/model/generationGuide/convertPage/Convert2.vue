<template>
  <div class="step">
    <div class="step-form">
      <BasicForm @register="register" v-if="showForm" />
      <a-spin style="margin-left: 45%; margin-top: 30%" :tip="tip" v-if="!showForm" />
    </div>
    <Divider />
    <h3>说明</h3>
    <p> 选择已有算法商城中的算法，进行训练，并自动比较。 </p>
  </div>
</template>
<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { Spin as ASpin } from 'ant-design-vue';
  import { convertSchems } from '.././data';
  import { Divider } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  const modelGeneration = ref();
  const image = ref('');
  const weightPath = ref('');
  const generationId = ref(-1);
  const gpuNum = ref(0);
  const showForm = ref(true);
  const tip = ref('模型转换中');

  const props = defineProps<{ generationId: number; generationName: string }>();
  const { createMessage } = useMessage();

  const [register, { validate }] = useForm({
    labelWidth: 200,
    schemas: convertSchems,
    actionColOptions: {
      span: 14,
    },
    showResetButton: false,
    submitButtonOptions: {
      text: '进行转换',
    },
    submitFunc: customSubmitFunc,
  });

  onMounted(async () => {
    await getModelGenerationData();
  });
  async function customSubmitFunc() {
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
      await maHttp.post(
        {
          url: `modelJob/createConvertJob?modelGenerationId=${generationId.value}`,
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
      createMessage.success('算法转换执行开始');
      showForm.value = false;
    } finally {
    }
  }
  function getModelGenerationData() {
    maHttp
      .get(
        {
          url: 'modelGeneration/findModelGenerationById',
          params: {
            id: props.generationId,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
          timeout: 102400000,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(async (thisModelGeneration) => {
        generationId.value = thisModelGeneration.id;
        modelGeneration.value = thisModelGeneration;
        let latestJobId = thisModelGeneration.latestJobId;
        image.value = thisModelGeneration.modelStore.convertModelVersion.url;
        await maHttp
          .get(
            {
              url: 'modelJob/getJobByJobId',
              params: {
                id: latestJobId,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
              timeout: 102400000,
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((job) => {
            weightPath.value = job.weightPath;
            gpuNum.value = job.gpuNum;
          });
      });
  }

  /**
   * 根据ModelGenerationId获取ModelGeneration
   */
</script>

<style scoped lang="less">
  .step {
    &-form {
      width: 600px;
      height: 500px;
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
