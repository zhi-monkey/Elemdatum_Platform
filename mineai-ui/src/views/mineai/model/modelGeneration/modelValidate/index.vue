<template>
  <PageWrapper
    @back="goBack"
    :contentFullHeight="true"
    :fixedHeight="true"
    style="margin: 0 16px 0 16px"
  >
    <template #title>
      模型
      <span style="font-weight: 700; color: #f5b956; font-size: 18px"> {{ modelInfo.image }} </span>
    </template>
    <div class="h-full w-full flex justify-center">
      <a-card
        :loading="false"
        title="发起验证"
        style="height: 100%; width: 99%"
        :bodyStyle="{ height: '100%' }"
      >
        <template #extra>
          <div class="flex flex-row justify-end gap-x-2">
            <div>
              <a-button
                type="primary"
                block
                :disabled="(validatingPath && validatingPath !== '') || uploading"
                @click="validateModel"
              >
                验证模型
              </a-button>
            </div>
            <a-select
              style="width: 230px"
              placeholder="历史结果"
              v-model:value="curValidationPath"
              :options="oldValidation"
              @change="handleChange"
            />
          </div>
        </template>
        <div class="w-full h-full flex flex-col items-center">
          <div
            class="mb-1 w-2/5"
            v-if="
              (validatingPath && validatingPath !== '' && validatingPath !== 'uncertainty') ||
              uploading
            "
          >
            <a-alert
              :message="
                uploading
                  ? '上传中'
                  : validatingPath && validatingPath !== '' && validatingPath !== 'uncertainty'
                  ? '验证中'
                  : ''
              "
              type="info"
              show-icon
            >
              <template #icon>
                <sync-outlined spin />
              </template>
            </a-alert>
          </div>
          <div class="w-full h-8/9">
            <div class="w-full h-full" v-show="ifShow">
              <swiper
                :style="{
                  '--swiper-navigation-color': '#fff',
                  '--swiper-pagination-color': '#fff',
                }"
                :loop="true"
                :prevent-clicks="true"
                :spaceBetween="10"
                :navigation="true"
                :thumbs="{ swiper: thumbsSwiper }"
                :modules="[FreeMode, Navigation, Thumbs]"
                class="mySwiper2"
              >
                <swiper-slide v-for="item in imgList" :key="item">
                  <img :src="item" alt="item" />
                </swiper-slide>
              </swiper>
              <swiper
                @swiper="setThumbsSwiper"
                :loop="true"
                :spaceBetween="10"
                :slidesPerView="5"
                :freeMode="true"
                :watchSlidesProgress="true"
                :modules="modules"
                class="mySwiper w-3/4"
              >
                <swiper-slide v-for="item in imgList" :key="item">
                  <img :src="item" alt="item" />
                </swiper-slide>
              </swiper>
            </div>
            <div v-if="!ifShow && loading" class="w-full h-full flex justify-center">
              <a-spin size="large" class="self-center" />
            </div>
          </div>
        </div>
      </a-card>
    </div>
    <a-modal
      v-model:visible="open"
      title="请输入验证任务置信度"
      ok-text="确认"
      cancel-text="取消"
      @ok="hideModal"
    >
      <div class="w-full h-full p-8 flex justify-center">
        <a-input-number
          v-model:value="confidence"
          :precision="0"
          :min="0"
          :max="1"
          :step="0.01"
          :status="inputStatus"
          :formatter="(value) => `${value}%`"
          style="width: 50px"
        />
      </div>
    </a-modal>
    <UploadImages
      :file-path="filePath"
      :visible="isVisible"
      @close-uploader="handleClose"
      @upload-success="handleUploadSuccess"
      @start-upload="handleStartUpload"
    />
  </PageWrapper>
</template>
<script setup lang="ts">
  import { PageWrapper } from '/@/components/Page';
  import {
    Alert as AAlert,
    Button as AButton,
    Card as ACard,
    Select as ASelect,
    Spin as ASpin,
    Modal as AModal,
    InputNumber as AInputNumber,
  } from 'ant-design-vue';
  import { Swiper, SwiperSlide } from 'swiper/vue';
  import 'swiper/css';
  import 'swiper/css/free-mode';
  import 'swiper/css/navigation';
  import 'swiper/css/thumbs';
  import { FreeMode, Navigation, Thumbs } from 'swiper/modules';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { SyncOutlined } from '@ant-design/icons-vue';
  import { useRouter } from 'vue-router';
  import { onMounted, onUnmounted, ref } from 'vue';
  import UploadImages from './upload-images.vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { getMinIOAuth } from '/@/views/mineai/data/dataset-details2/api';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { minIOConfig } from '/@/utils/dubhe/minIO';
  import { Minio } from 'minio-js';
  import { getFileUrlList } from '/@/utils/dubhe/download';

  let minioClient;

  let timer;

  const { createMessage } = useMessage();

  const router = useRouter();

  // 模型信息
  const modelInfo = history.state;
  const filePath = ref('');

  const modules = [FreeMode, Navigation, Thumbs];
  const thumbsSwiper = ref(null);
  const setThumbsSwiper = (swiper) => {
    thumbsSwiper.value = swiper;
  };

  const open = ref(false);
  const confidence = ref(0.85);
  const inputStatus = ref('');

  const isVisible = ref(false);

  const uploading = ref(false);

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
        url: `modelJob/${modelInfo.id}/cache/query`,
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

  function validateModel() {
    open.value = true;
  }

  function hideModal() {
    if (!confidence.value) {
      confidence.value = modelInfo.params.HP_CONFIDENCE;
      inputStatus.value = 'error';
      return;
    }
    inputStatus.value = '';
    open.value = false;
    filePath.value = 'tmp/validate-' + modelInfo.id + '-' + new Date().getTime().toString();
    isVisible.value = true;
  }

  const handleStartUpload = () => {
    ifShow.value = false;
    loading.value = true;
    uploading.value = true;
  };

  const handleClose = (state: string): void => {
    isVisible.value = false;
    if (state === 'success') {
      // 上传完成，进行下一步
      handleUploadSuccess();
    } else if (state === 'hide') {
      // 后台上传
    } else if (state === 'cancel') {
      // 取消上传
      uploading.value = false;
      loading.value = false;
    }
  };

  // 上传成功后执行图片展示及后续验证操作
  const handleUploadSuccess = async () => {
    uploading.value = false;
    validatingPath.value = filePath.value;
    // 发起验证job
    const jobName = filePath.value.split('/')[1];
    try {
      await maHttp.post(
        {
          url: 'job/validation/create',
          params: {
            id: modelInfo.id,
            jobName: jobName,
            image: modelInfo.image,
            path: filePath.value,
            weightPath: modelInfo.weightPath,
            gpuNum: modelInfo.gpuNum,
            params: {
              MODE_WORKING_MODE: modelInfo.params.MODEL_WORKING_MODE,
              HP_CONFIDENCE: confidence.value,
            },
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    } catch (e) {
      validatingPath.value = '';
      loading.value = false;
      createMessage.error('创建验证任务失败', 5);
      console.error(e);
      return;
    }
    // 更新下拉框验证记录，加载最新验证信息
    await getValidatingState();
  };

  // 根据当前选中的验证记录进行页面加载渲染
  const handleChange = async (value: string | undefined) => {
    if (!value) {
      ifShow.value = false;
      loading.value = false;
    }
    // 渲染imgViewer
    if (value !== validatingPath.value) {
      await render(value);
    }
  };

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
  .swiper {
    height: 85%;
  }

  .swiper-thumbs {
    height: 20% !important;

    .swiper-slide-thumb-active {
      opacity: 1 !important;
    }

    .swiper-slide {
      opacity: 0.4;
    }
  }

  .swiper-slide {
    display: flex;
    justify-content: center;
  }

  img {
    height: 97%;
  }

  .swiper-thumbs :deep(.swiper-slide img) {
    width: 100%;
  }

  .mySwiper2 :deep(.swiper-button-prev:after) {
    font-size: 32px;
  }

  .mySwiper2 :deep(.swiper-button-next:after) {
    font-size: 32px;
  }
</style>
