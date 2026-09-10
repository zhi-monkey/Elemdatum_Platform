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
        v-bind="optionCreateProps"
        @upload-success="uploadSuccess"
        @upload-error="uploadError"
      />
      <!--上传视频时显示帧间隔设置-->
      <el-form
        v-if="!(state.isImage || state.isText || state.isAudio)"
        ref="formStep"
        :model="state.form"
        label-width="100px"
        style="margin-top: 10px"
      >
        <el-form-item
          label="视频帧间隔"
          prop="frameInterval"
          :rules="[{ required: true, message: '请输入有效的帧间隔', trigger: 'blur' }]"
          v-show="false"
        >
          <el-input-number v-model="state.form.frameInterval" :min="1" step-strictly :step="1" />
        </el-form-item>
      </el-form>
    </div>
    <!--上传文件进度展示-->
    <div v-show="state.uploadStep === 1">
      <el-progress
        v-if="state.isImage || state.isText || state.isAudio || state.isVideo"
        :percentage="state.percentage"
        :status="state.uploadStatus"
        :stroke-width="24"
        :format="format"
      />
      <div v-else class="circleProgressWrapper">
        <div class="circleText">正在上传</div>
        <div class="wrapper right">
          <div class="circleProgress rightCircle"></div>
        </div>
        <div class="wrapper left">
          <div class="circleProgress leftCircle"></div>
        </div>
      </div>
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
        <el-button @click="hideUploadDataFile">隐藏</el-button>
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
  import { last, isNil } from 'lodash-es';
  import { reactive, watch, computed, nextTick, ref } from 'vue';
  import { toFixed } from '/@/utils/dubhe';
  import UploadInline from './components/UploadForm/inline.vue';
  import { getFileFromMinIO, withDimensionFile, trackUploadProps, dataTypeCodeMap } from './util';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { createMessage } = useMessage();

  // 上传文件接口
  const submit = async (id, files) => {
    return await maHttp.post(
      {
        url: `datasets/${id}/files`,
        headers: {},
        data: {
          files,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
  };

  // 上传视频接口
  const submitVideo = async (id, data) => {
    return await maHttp.post(
      {
        url: `datasets/${id}/video`,
        headers: {},
        data,
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
  };

  // 每次最多上传的文件数量
  const MAX_FILE_COUNT = 200;

  // props
  type Props = {
    row?: Object;
    visible?: boolean;
    loading?: boolean;
    closeUploadDataFile?: Function;
    hideUploadDataFile?: Function;
  };

  const props = withDefaults(defineProps<Props>(), {
    row: () => {
      return {};
    },
    visible: false,
    loading: false,
    closeUploadDataFile: () => {},
    hideUploadDataFile: () => {},
  });

  // refs
  const fileUploadForm = ref();
  const formStep = ref();

  const defaultFrameInterval = 5;
  const dialogVisible = computed(() => {
    return props.visible;
  });
  const state = reactive({
    uploadKey: 1,
    row: {} as any,
    uploadStep: 0,
    loading: false,
    isImage: undefined,
    isText: undefined,
    isAudio: undefined,
    isVideo: undefined,
    accept: '',
    title: '',
    percentage: 0,
    uploadStatus: undefined,
    form: {
      frameInterval: defaultFrameInterval,
    },
    error: null, // 上传错误
    // 上传步数
    steps: [],
  });

  // 图片和视频信息存库
  const uploader = async (datasetId, files) => {
    // 文件上传
    if (state.isImage || state.isText || state.isAudio) {
      return submit(datasetId, files);
    }
    return submitVideo(datasetId, {
      files: files.map((file) => ({
        url: file.url,
        frameInterval: state.form.frameInterval,
      })),
    });
  };

  // 上传视频时不显示实时进度
  const format = (percentage) => {
    return percentage <= 100 ? `${percentage}%` : ``;
  };

  // 上传到minio失败，给出提示
  const uploadError = (err) => {
    state.loading = false;
    createMessage.error(err.message || '上传失败', 5);
  };

  // 上传到minio成功后操作，图片信息存库存库
  const uploadSuccess = (res) => {
    // 文本已经同步过，不需要再同步
    if (state.isText) return;
    // 视频上传完毕
    if (state.isVideo) {
      state.percentage = 100;
    }
    const files = getFileFromMinIO(res);
    // 自动标注完成时 导入 提示信息不同
    const successMessage = '上传文件成功';
    if (files.length > 0) {
      // 上传文件到minio成功后，将图片信息存库
      uploader(state.row.id, files)
        .then(() => {
          Object.assign(state, {
            loading: false,
            uploadStatus: 'success',
            uploadStep: 2,
            title: '上传成功',
          });
          createMessage.success(successMessage, 5);
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
    // 判断选中文件数量再去调接口
    if (ref.formRef.lenOfFileList === 0) {
      createMessage.error('文件不能为空', 5);
      return;
    }
    if (state.isVideo && isNil(state.form.frameInterval)) {
      createMessage.error('视频帧间隔不能为空', 5);
      return;
    }
    state.loading = true;
    // 上传组件的上传接口,也叫uploadSubmit
    ref.uploadSubmit((resolved, fileList, resolveFiles) => {
      // 用于进度条显示，上传文件到minIO时触发
      // 按阶段最大值取模（针对文本分批同步）
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

      // eslint-disable-next-line func-names
      nextTick(function () {
        // 只针对文本做分批上传
        if (state.isText) {
          state.steps.forEach((step, i) => {
            if (step === resolved) {
              const prevStep = i === 0 ? 0 : state.steps[i - 1];
              // 分批同步文件
              const stepFiles = getFileFromMinIO(resolveFiles.slice(prevStep, step));
              uploader(state.row.id, stepFiles).then(() => {
                // 最后一步同步
                if (i === state.steps.length - 1) {
                  Object.assign(state, {
                    loading: false,
                    uploadStatus: 'success',
                    uploadStep: 2,
                    title: '上传成功',
                  });
                  createMessage.success('上传文件成功', 5);
                }
              });
            }
          });
        }

        state.percentage = state.percentage > 100 ? 100 : toFixed(resolved / fileList.length, 2, 0);
      });
    });
  };

  const handleClose = (flag) => {
    props.closeUploadDataFile(flag);
    Object.assign(state, {
      uploadStep: 0,
      uploadKey: state.uploadKey + 1,
      percentage: 0,
      uploadStatus: undefined,
      error: null,
    });
  };

  // 切换每一行时，更新参数
  const buildDataParams = (type) => {
    const dataParamMap = {
      [dataTypeCodeMap.IMAGE]: {
        isImage: true,
        isText: false,
        isAudio: false,
        isVideo: false,
        title: '导入图片',
        accept: '.jpg,.png,.bmp,.jpeg',
      },
      [dataTypeCodeMap.TEXT]: {
        isImage: false,
        isText: true,
        isAudio: false,
        isVideo: false,
        title: '导入文本',
        accept: '.txt',
      },
      [dataTypeCodeMap.AUDIO]: {
        isImage: false,
        isText: false,
        isAudio: true,
        isVideo: false,
        title: '导入音频',
        accept: '.mp3,.wav,.wma,.aac',
      },
      [dataTypeCodeMap.VIDEO]: {
        isImage: false,
        isText: false,
        isAudio: false,
        isVideo: true,
        title: '导入视频',
        accept: '.mp4,.avi,.mkv,.mov,.webm,.wmv',
      },
    };
    return dataParamMap[type] || {};
  };

  // 根据选中行的数据集信息，创建传递给子组件的参数
  const optionCreateProps = computed(() => {
    if (!state.row) return {};
    const props = {
      params: {
        datasetId: state.row.id,
        // dataset/8/origin
        objectPath: `dataset/${state.row.id}/${state.isVideo ? 'video' : 'origin'}`, // 图片/视频对象存储路径
      },
    };
    // 在上传图片前做自定义转换 withDimensionFile
    const baseImgProps = {
      transformFile: withDimensionFile,
      hash: true,
    };
    if (state.isText || state.isAudio) {
      Object.assign(props, {
        dataType: 'text',
        hash: true, // 支持同名文件上传
      });
    }
    if (state.isText) {
      Object.assign(props, {
        acceptSize: 0.1, // Mb 为单位
        acceptSizeFormat: (size) => `${size * 1000} kb`,
      });
    }
    if (state.isImage) {
      Object.assign(props, baseImgProps);
    }
    if (state.isVideo) {
      Object.assign(props, baseImgProps, trackUploadProps);
    }
    return props;
  });

  // 监测选中导入的列数据变化
  watch(
    () => props.row,
    (next) => {
      Object.assign(state, {
        row: { ...state.row, ...next },
      });
      const nextParams = buildDataParams(state.row.dataType);
      Object.assign(state, nextParams);
    },
  );
</script>
<style scoped>
  .upload-form :deep(.el-upload-list) {
    display: none;
  }
</style>
