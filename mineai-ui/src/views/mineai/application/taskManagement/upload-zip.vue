<template>
  <el-dialog
    :key="state.uploadKey"
    :closeOnClickModal="false"
    append-to-body
    width="640px"
    v-model="dialogVisible"
    :title="state.title"
    :show-close="false"
    :z-index="10"
  >
    <!--选择上传的文件-->
    <div v-show="state.uploadStep === 0">
      <upload-inline
        :key="state.row.id"
        ref="fileUploadForm"
        action="fakeApi"
        :accept="state.accept"
        :accept-size="MAX_FILE_SIZE"
        :limit="MAX_FILE_COUNT"
        v-bind="optionCreateProps"
        @upload-success="uploadSuccess"
        @upload-error="uploadError"
      />
    </div>
    <!--上传文件进度展示-->
    <div v-show="state.uploadStep === 1">
      <el-progress
        :percentage="state.percentage"
        :status="state.uploadStatus"
        :stroke-width="24"
        :format="format"
      />
    </div>
    <!--结果-->
    <div v-show="state.uploadStep === 2">
      <el-progress
        :percentage="state.uploadStatus === 'exception' ? 0 : 100"
        :status="state.uploadStatus"
        :stroke-width="24"
      />
      <div v-if="state.uploadStatus === 'exception'" class="app-result-subtitle mt-10">
        {{ state.error.message }}
      </div>
    </div>
    <template #footer>
      <div v-show="state.uploadStep === 0">
        <el-button @click="handleClose(false)">取消</el-button>
        <el-button type="primary" :loading="state.loading" @click="uploadSubmit(fileUploadForm)"
          >开始上传
        </el-button>
      </div>
      <div v-show="state.uploadStep === 1" class="tc">
        <el-button @click="hideUploadZipFile">隐藏</el-button>
      </div>
      <div v-show="state.uploadStep === 2" class="tc">
        <el-button type="primary" @click="handleClose(true)"
          >{{ state.uploadStatus === 'success' ? '完成' : '关闭' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
  import { last } from 'lodash-es';
  import { computed, nextTick, reactive, ref, watch } from 'vue';
  import { toFixed } from '/@/utils/dubhe';
  import UploadInline from '../taskManagement/UploadForm/UploadInline.vue';
  import { getFileFromMinIO } from './util';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { createMessage } = useMessage();

  // 每次最多上传的文件数量
  const MAX_FILE_COUNT = 1;

  // 单个压缩包最大size
  const MAX_FILE_SIZE = 1024;

  const emit = defineEmits(['uploadAllDone']);

  // props
  type Props = {
    row?: Object;
    visible?: boolean;
    loading?: boolean;
    closeUploadZipFile?: Function;
    hideUploadZipFile?: Function;
  };

  const props = withDefaults(defineProps<Props>(), {
    row: () => {
      return {};
    },
    visible: false,
    loading: false,
    closeUploadZipFile: () => {},
    hideUploadZipFile: () => {},
  });

  // refs
  const fileUploadForm = ref();

  const defaultFrameInterval = 5;
  const dialogVisible = computed(() => {
    return props.visible;
  });
  const state = reactive({
    uploadKey: 1,
    row: {},
    uploadStep: 0,
    loading: false,
    accept: '.zip,.tar',
    title: '导入压缩包',
    percentage: 0,
    uploadStatus: undefined,
    form: {
      frameInterval: defaultFrameInterval,
    },
    error: null, // 上传错误
    // 上传步数
    steps: [],
  });

  // 文件信息存库，常州这里的逻辑是存储压缩包的url到数据库中。
  const uploader = async (modelApplicationId, appZipPath) => {
    return await maHttp.post(
      {
        url: `modelApplication/saveZipUrl`,
        headers: {},
        data: {
          modelApplicationId: modelApplicationId,
          appZipPath: appZipPath,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  };

  // 上传实时进度格式
  const format = (percentage) => {
    return percentage <= 100 ? `${percentage}%` : ``;
  };

  // 上传到minio失败，给出提示
  const uploadError = (err) => {
    state.loading = false;
    createMessage.error(err.message || '上传失败', 5);
  };

  // 上传到minio成功后操作，信息存库
  const uploadSuccess = (res) => {
    const files = getFileFromMinIO(res);
    const successMessage = '上传文件成功';
    if (files.length > 0) {
      // 上传文件到minio成功后，将文件信息存库
      uploader(state.row.id, files[0].url)
        .then(() => {
          Object.assign(state, {
            loading: false,
            uploadStatus: 'success',
            uploadStep: 2,
            title: '上传成功',
          });
          createMessage.success(successMessage, 5);
          emit('uploadAllDone');
        })
        .catch((err) => {
          Object.assign(state, {
            loading: false,
            error: err,
            uploadStatus: 'exception',
            uploadStep: 2,
            title: '上传失败',
          });
        });
    }
  };

  // 点击确定上传按钮，针对上传组件进行操作(ref="fileUploadForm")
  const uploadSubmit = (ref) => {
    if (/[\u4e00-\u9fa5\s()[\]{}<>!@#$%^&*]/.test(ref.formRef.fileList[0].name)) {
      createMessage.error('文件名不能包含中文、空格或特殊符号（如括号）', 5);
      return;
    }
    // 判断选中文件数量再去调接口
    if (ref.formRef.lenOfFileList === 0) {
      createMessage.error('文件不能为空', 5);
      return;
    }
    state.loading = true;
    // 上传组件的上传接口,也叫uploadSubmit
    ref.uploadSubmit((resolved, fileList) => {
      // 用于进度条显示，上传文件到minIO时触发
      const mod = fileList.length % MAX_FILE_COUNT;
      const intSteps = (fileList.length - mod) / MAX_FILE_COUNT;
      const steps = Array.from({ length: intSteps }, (_, i) => (i + 1) * MAX_FILE_COUNT);
      if (mod) {
        steps.push((last(steps) || 0) + mod);
      }
      Object.assign(state, {
        uploadStep: 1,
        title: '上传中',
        steps,
      });

      nextTick(() => {
        state.percentage = state.percentage > 100 ? 100 : toFixed(resolved / fileList.length, 2, 0);
      });
    });
  };

  const handleClose = (flag) => {
    props.closeUploadZipFile(flag);
    Object.assign(state, {
      uploadStep: 0,
      uploadKey: state.uploadKey + 1,
      percentage: 0,
      uploadStatus: undefined,
      error: null,
    });
  };

  // 根据选中行的数据集信息，创建传递给子组件的参数
  const optionCreateProps = computed(() => {
    if (!state.row) return {};
    return {
      params: {
        datasetId: state.row.id,
        // 压缩包文件存放路径为根目录
        objectPath: '',
        hash: true,
      },
    };
  });
  watch(
    () => props.row,
    (newV) => {
      Object.assign(state, {
        row: newV,
      });
    },
  );
</script>
<style scoped>
  .upload-form :deep(.el-upload-list) {
    display: none;
  }
</style>
