<template>
  <div>
    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <template #title>
        <div style="display: flex; justify-content: space-between">
          <div>图片上传</div>
          <div>
            <a-button type="primary" @click="openLogModal" :disabled="!jobName.length">
              查看日志
            </a-button>
          </div>
        </div>
      </template>
      <div class="clearfix">
        <a-upload
          list-type="picture-card"
          v-model:file-list="fileList"
          @preview="handlePreview"
          :max-count="200"
          :multiple="true"
          @change="handleUploadChange"
          :before-upload="beforeUpload"
        >
          <div v-if="fileList.length < 200">
            <plus-outlined />
            <div class="ant-upload-text">Upload</div>
          </div>
        </a-upload>
        <a-modal :visible="previewVisible" :footer="null" @cancel="handleCancel">
          <img alt="example" style="width: 100%" :src="previewImage" />
        </a-modal>
      </div>
    </a-card>

    <a-card style="max-width: 1000px; margin-bottom: 16px">
      <template #title>图片信息</template>
      <template #extra>
        <a-input-number
          v-model:value="confidence"
          placeholder="置信度"
          style="margin-right: 8px"
          :min="0"
          :max="1"
          :step="0.01"
          @change="handleConfidenceChange"
        />
        <a-button
          type="primary"
          :loading="uploading"
          @click="startTest"
          :disabled="uploading || fileList.length === 0 || !confidence"
          >开始测试
        </a-button>
      </template>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-statistic title="图片数量" :value="fileList.length" />
        </a-col>
        <a-col :span="12">
          <a-statistic title="测试进度" :value="`${currentIndex}/${fileList.length}`" />
        </a-col>
      </a-row>
    </a-card>

    <ImageViewCard v-if="showImageViewCard" :img-list="imgList" />
    <a-modal
      :visible="logModalVisible"
      title="验证日志信息"
      :footer="null"
      :width="1100"
      @cancel="logModalVisible = false"
    >
      <div class="terminal-wrapper">
        <div class="terminal-content" id="terminal">
          <a-spin :spinning="terminalLoading">
            <p v-for="(log, index) in consoleLogs" :key="index">{{ log }}</p>
          </a-spin>
        </div>
        <div class="toolkit">
          <span class="icon icon-clickable icon-changeable" @click="refreshContent"
            ><i class="iconfont icon-shuaxin"></i>
          </span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { defineProps, onMounted, onUnmounted, ref, Ref } from 'vue';
  import { PlusOutlined } from '@ant-design/icons-vue';
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    InputNumber as AInputNumber,
    Modal as AModal,
    Row as ARow,
    Spin as ASpin,
    Statistic as AStatistic,
    Upload as AUpload,
  } from 'ant-design-vue';
  import ImageViewCard from './ImageViewCard.vue';
  import { getMinIOAuth } from '/@/views/mineai/data/dataset-details2/api';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { minIOConfig } from '/@/utils/dubhe';
  import { Minio } from 'minio-js';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { getFileUrlList } from '/@/utils/dubhe/download';
  import { minIOUpload, renameFile } from './utils.js';

  const props = defineProps({
    modelInfo: {
      type: Object,
      required: true,
    },
  });

  const previewVisible = ref<boolean>(false);
  const previewImage = ref<string | undefined>('');
  const currentIndex = ref<number>(0);
  let confidence = ref<number | null>(0.35); // 置信度值
  const uploading = ref<boolean>(false);
  const logLoading = ref<boolean>(false);
  const validated = ref<boolean>(false);
  const showImageViewCard = ref<boolean>(false);
  const { createMessage } = useMessage();
  const terminalLoading: Ref<boolean> = ref(false);
  // 历史验证结果
  const oldValidation = ref<{ value: string; label: string }[]>([]);
  // 目前选择的验证记录
  const curValidationPath = ref<string>();
  // 正在进行验证的job
  const validatingPath = ref('');
  let logModalVisible = ref<boolean>(false);
  let minioClient;

  interface FileItem {
    uid: string;
    name?: string;
    status?: string;
    response?: string;
    percent?: number;
    url?: string;
    preview?: string;
    originFileObj?: any;
  }

  const beforeUpload = (file: FileItem) => {
    fileList.value = [...fileList.value, file];
    // console.log(fileList.value);
    return false;
  };

  // 将图片数量减少到21张
  const fileList = ref<FileItem[]>([]);
  let jobName = ref('');

  const startTest = async () => {
    // 检查一下置信度值，保险起见
    if (confidence.value === null || confidence.value < 0 || confidence.value > 1) {
      createMessage.warning('置信度不能为空');
      return;
    }
    // console.log('fileList', fileList.value);
    // 上传图片到minio
    const renameFileList = fileList.value.map((file) => ({
      ...file,
      name: renameFile(file.name, { hash: true, encode: true }),
    }));
    // 非空判断
    if (!fileList.value || fileList.value.length === 0) {
      throw new Error('文件不能为空');
    }
    filePath.value = 'tmp/validate-' + props.modelInfo.id + '-' + new Date().getTime().toString();
    const objectPath = filePath.value + '/images';
    // 不知道有啥用，抄前面学长的 2333
    const transformFile = undefined;
    console.log(filePath.value);
    // 开始调用上传接口
    await minIOUpload({ objectPath, fileList: renameFileList, transformFile });
    jobName.value = filePath.value.split('/')[1];
    uploading.value = true;
    try {
      await maHttp.post(
        {
          url: 'job/validation/create',
          params: {
            id: props.modelInfo?.id,
            jobName: jobName.value,
            image: props.modelInfo?.image,
            path: filePath.value,
            weightPath: props.modelInfo?.weightPath,
            gpuNum: props.modelInfo?.gpuNum,
            params: {
              MODE_WORKING_MODE: props.modelInfo?.params.MODE_WORKING_MODE,
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
      uploading.value = false;
      createMessage.error('创建验证任务失败', 5);
      console.error(e);
      return;
    }
    // 更新下拉框验证记录，加载最新验证信息
    await getValidatingState();
    // let index = 0;
    // const interval = setInterval(() => {
    //   currentIndex.value = ++index;
    //   if (index === fileList.value.length) {
    //     clearInterval(interval);
    //     uploading.value = false;
    //     showImageViewCard.value = true;
    //   }
    // }, 1);
  };
  let validateLog = ref([]);
  const getValidateLog = async () => {
    if (!jobName.value) {
      return;
    }
    logLoading.value = true;
    await maHttp
      .get(
        {
          url: 'log/getJobLog',
          params: {
            jobName: jobName.value,
            direction: 'BACKWARD',
            namespace: 'ai-platform',
            limit: 1000,
            //start: 1,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((res) => {
        if (!res) {
          throw new Error('暂未生成日志');
        }
        validateLog.value = res;
        extractProgress(res);
      })
      .catch((e) => {
        console.error(e);
      })
      .finally(() => {
        logLoading.value = false;
      });
  };

  const logInfo = ref([]);
  const consoleLogs: Ref<string[]> = ref([]);
  let logTimer = null;

  const openLogModal = async () => {
    // const terminal = document.querySelector('#terminal');
    // terminal.scrollTop = terminal?.scrollHeight;
    consoleLogs.value = [];
    logTimer = setInterval(refreshContent, 3000);
    logInfo.value = [];
    validateLog.value.forEach((log) => {
      logInfo.value.push(Object.values(log)[0]);
    });
    logModalVisible.value = true;
  };

  async function refreshContent() {
    terminalLoading.value = false;
    consoleLogs.value = [];
    validateLog.value.forEach((value) => {
      consoleLogs.value.push(value[Object.keys(value)[0]]);
    });
    terminalLoading.value = false;
  }

  interface LogEntry {
    [key: string]: string; // 动态属性名，值为字符串
  }

  function extractProgress(logs: LogEntry[]): { current: number; total: number } | null {
    const progressPattern = /image (\d+)\/(\d+)/;
    let lastProgress: { current: number; total: number } | null = null;

    for (const log of logs) {
      const logMessage = Object.values(log)[0];
      const match = logMessage.match(progressPattern);
      if (match) {
        const current = parseInt(match[1], 10); // 当前图像索引
        const total = parseInt(match[2], 10); // 总图像数量
        currentIndex.value = current;
        // console.log('currentIndex: ', currentIndex.value);
        // console.log('current: ', current);
        lastProgress = { current, total };
      }
    }
    return lastProgress; // 如果没有匹配到，返回 null
  }

  const handleCancel = () => {
    previewVisible.value = false;
  };

  const handlePreview = async (file: FileItem) => {
    if (!file.url && !file.preview) {
      file.preview = (await getBase64(file.originFileObj)) as string;
    }
    previewImage.value = file.url || file.preview;
    previewVisible.value = true;
  };

  function getBase64(file: File) {
    return new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
    });
  }

  // 不知道啥玩意
  const filePath = ref('');

  const handleConfidenceChange = (newValue) => {
    confidence.value = newValue;
  };
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

  // 判断是否有验证任务正在进行，以及是否有旧的验证结果
  async function getValidatingState() {
    const result = await maHttp.get(
      {
        url: `modelJob/${props.modelInfo?.id}/cache/query`,
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
        // if (curValidationPath.value !== validatingPath.value) {
        //   await render(curValidationPath.value);
        // }
      }
    }
  }

  let timer;

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
      await getValidateLog();
      if (!result) {
        createMessage.success('验证任务执行失败', 5);
        validatingPath.value = '';
        curValidationPath.value = undefined;
        uploading.value = false;
        validated.value = true;
        window.clearInterval(timer);
      }
      // 任务完成
      if (result.status.succeeded === 1) {
        createMessage.success('验证任务完成', 5);
        if (curValidationPath.value === validatingPath.value) {
          console.log('curValidationPath.value', curValidationPath.value);
          await render(curValidationPath.value);
        }
        validatingPath.value = '';
        uploading.value = false;
        validated.value = true;
        window.clearInterval(timer);
      }
      // 任务失败
      if (result.status.failed === 1) {
        createMessage.error('验证任务执行失败', 5);
        if (curValidationPath.value === validatingPath.value) {
          uploading.value = false;
        }
        validatingPath.value = '';
        uploading.value = false;
        validated.value = true;
        window.clearInterval(timer);
      }
    }, 3000);
  }

  let imgList = ref<string[]>([]);
  const render = async (path: string) => {
    // 获取验证输出图片列表
    imgList.value = [];
    imgList.value = await getFileUrlList(minioClient, `${path}/output/`, [
      'jpg',
      'png',
      'bmp',
      'jpeg',
      'gfif',
    ]);
    showImageViewCard.value = true;
  };

  const handleUploadChange = () => {
    // console.log(fileList);
  };
  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(logTimer);
  });
</script>

<style scoped>
  .ant-upload-select-picture-card i {
    font-size: 32px;
    color: #999;
  }

  .ant-upload-select-picture-card .ant-upload-text {
    margin-top: 8px;
    color: #666;
  }

  .custom-modal {
    width: 1100px; /* 设置最大宽度 */
    padding: 20px; /* 内边距 */
  }

  .modal-body {
    width: 1050px;
    font-size: 14px; /* 字体大小 */
    font-weight: bold; /* 字体加粗 */
    line-height: 1.5; /* 行高 */
  }

  .log-item {
    margin-bottom: 15px; /* 每个日志项之间的间距 */
    padding: 10px; /* 日志项内边距 */
    border-radius: 4px; /* 圆角 */
  }

  .log-item p {
    margin: 0; /* 去掉段落的默认外边距 */
  }

  .terminal-wrapper {
    height: 95%;
    padding: 20px;
    background-color: #212639;
  }

  .terminal-content {
    height: 100%;
    background-color: #242e42;
    font-family: PT Mono, Monaco, Menlo, Consolas, Courier New, monospace;
    padding: 20px;
    border-radius: 4px;
    overflow: auto;
  }

  .terminal-content p {
    color: #b7c4d1;
    font-weight: 600;
    line-height: 20px;
    white-space: pre-wrap;
  }

  .toolkit {
    position: absolute;
    display: -ms-flexbox;
    display: flex;
    -ms-flex-align: center;
    align-items: center;
    top: 60px;
    right: 40px;
    z-index: 2;
    height: 40px;
    padding: 8px 8px;
    color: #fff;
    border-radius: 12px;
    background-color: #36435c;
  }

  @font-face {
    font-family: 'iconfont'; /* Project id 4098445 */
    src: url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.woff2?t=1685585739405') format('woff2'),
      url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.woff?t=1685585739405') format('woff'),
      url('//at.alicdn.com/t/c/font_4098445_f6haxdxtttk.ttf?t=1685585739405') format('truetype');
  }

  .iconfont {
    font-family: 'iconfont' !important;
    font-size: 25px;
    font-style: normal;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  .icon-shuaxin:before {
    content: '\ec08';
  }

  .icon {
    display: -ms-inline-flexbox;
    display: inline-flex;
    -ms-flex-item-align: center;
    align-self: center;
    background-position: 50%;
    background-size: contain;
    background-repeat: no-repeat;
    vertical-align: middle;
  }

  .icon-clickable {
    cursor: pointer;
    border-radius: 4px;
    pointer-events: auto;
  }
</style>
