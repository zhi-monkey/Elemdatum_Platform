<template>
  <div class="h-full w-full flex justify-center">
    <a-card :loading="false" class="test-card">
      <div>
        <TestByLocalUpload :modelInfo="modelInfo" />
      </div>
    </a-card>
  </div>
</template>
<script setup lang="ts">
  import { Card as ACard } from 'ant-design-vue';
  import 'swiper/css';
  import 'swiper/css/free-mode';
  import 'swiper/css/navigation';
  import 'swiper/css/thumbs';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRouter } from 'vue-router';
  import { defineProps, onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { getMinIOAuth } from '/@/views/mineai/data/dataset-details2/api';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { minIOConfig } from '/@/utils/dubhe/minIO';
  import { Minio } from 'minio-js';
  import { getFileUrlList } from '/@/utils/dubhe/download';
  import TestByLocalUpload from '/@/views/mineai/train/generationGuideList/detail/validate/TestByLocalUpload.vue';

  let minioClient;

  let timer;

  const { createMessage } = useMessage();

  const router = useRouter();
  const props = defineProps({
    jobId: Number,
    image: String,
    weightPath: String,
    gpuNum: Number,
  });

  // 模型信息
  // const modelInfo = history.state;
  const modelInfo = ref({
    id: props.jobId,
    image: props.image,
    weightPath: props.weightPath,
    gpuNum: props.gpuNum,
    params: {
      MODE_WORKING_MODE: 4,
    },
  });
  const ifShow = ref(false);
  const loading = ref(false);

  // 历史验证结果
  const oldValidation = ref<{ value: string; label: string }[]>([]);
  // 目前选择的验证记录
  const curValidationPath = ref<string>();
  // 正在进行验证的job
  const validatingPath = ref('');

  const imgList = ref(['']);

  onMounted(async () => {
    // 在不确定是否有验证任务正在进行之前禁止创建新验证任务
    validatingPath.value = 'uncertainty';
    // 初始化Minio;
    const authInfo = await getMinIOAuth();
    const { accessKey, privateKey, secretKey } = authInfo || {};
    const rawAccessKey = decrypt(accessKey, privateKey);
    const rawSecretKey = decrypt(secretKey, privateKey);
    const config = { ...minIOConfig.config, accessKey: rawAccessKey, secretKey: rawSecretKey };
    minioClient = new Minio.Client(config);
    await getValidatingState();
    if (validatingPath.value === 'uncertainty') validatingPath.value = '';
  });

  onUnmounted(() => {
    window.clearInterval(timer);
  });

  // 判断是否有验证任务正在进行，以及是否有旧的验证结果
  async function getValidatingState() {
    const result = await maHttp.get(
      {
        url: `modelJob/${modelInfo.value.id}/cache/query`,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    if (result) {
      if (result.length > 0) {
        if (result[0].finishTime === -1) {
          validatingPath.value = result[0].path;
          // 定时轮询状态
          polling(result[0].name);
        }
        oldValidation.value = result.map((item): { value: string; label: string } => ({
          value: item.path,
          label: item.name,
        }));
        curValidationPath.value = result[0].path;
        // 渲染imgViewer
        if (curValidationPath.value !== validatingPath.value) {
          await render(curValidationPath.value);
        }
      }
    }
  }

  //轮询job状态
  function polling(jobName: string) {
    window.clearInterval(timer);
    timer = setInterval(async () => {
      const result = await maHttp.get(
        {
          url: 'job/getV1JobByDefaultNamespace',
          params: { jobName: jobName },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      if (!result) {
        createMessage.success('验证任务执行失败', 5);
        validatingPath.value = '';
        curValidationPath.value = undefined;
        window.clearInterval(timer);
      }
      // 任务完成
      if (result.status.succeeded === 1) {
        createMessage.success('验证任务完成', 5);
        if (curValidationPath.value === validatingPath.value) {
          await render(curValidationPath.value);
        }
        validatingPath.value = '';
        window.clearInterval(timer);
      }
      // 任务失败
      if (result.status.failed === 1) {
        createMessage.error('验证任务执行失败', 5);
        if (curValidationPath.value === validatingPath.value) {
          loading.value = false;
        }
        validatingPath.value = '';
        window.clearInterval(timer);
      }
    }, 5000);
  }

  function goBack() {
    router.go(-1);
  }

  // 渲染imgViewer
  const render = async (path: string | undefined) => {
    ifShow.value = false;
    loading.value = true;
    // 获取验证输出图片列表
    imgList.value = [];
    imgList.value = await getFileUrlList(minioClient, `${path}/output/`, [
      'jpg',
      'png',
      'bmp',
      'jpeg',
    ]);
    loading.value = false;
    ifShow.value = true;
  };
</script>
<style scoped>
  .test-card {
    width: 100%;
  }
</style>
