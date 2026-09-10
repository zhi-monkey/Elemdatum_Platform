<template>
  <el-dialog
    :key="state.uploadKey"
    :closeOnClickModal="false"
    append-to-body
    width="640px"
    v-model="dialogVisible"
    destroy-on-close
    :title="state.title"
    :show-close="false"
    :z-index="10"
  >
    <!--选择上传的文件-->
    <div v-show="state.uploadStep === 0">
      <upload-inline
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
        <el-button @click="handleClose('cancel')">取消</el-button>
        <el-button type="primary" :loading="state.loading" @click="uploadSubmit(fileUploadForm)"
          >开始上传
        </el-button>
      </div>
      <div v-show="state.uploadStep === 1" class="tc">
        <el-button @click="handleClose('hide')">隐藏</el-button>
      </div>
      <div v-show="state.uploadStep === 2" class="tc">
        <el-button
          type="primary"
          @click="handleClose(state.uploadStatus === 'success' ? 'success' : 'cancel')"
          >{{ state.uploadStatus === 'success' ? '开始验证' : '关闭' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
  import { last } from 'lodash-es';
  import { computed, nextTick, reactive, ref } from 'vue';
  import { toFixed } from '/@/utils/dubhe';
  import UploadInline from '/@/views/mineai/data/dataset-details2/components/UploadForm/inline.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileFromMinIO } from '/@/views/mineai/data/dataset-details2/util';

  const { createMessage } = useMessage();

  // 每次最多上传的文件数量
  const MAX_FILE_COUNT = 100;

  // 单个文件最大size
  const MAX_FILE_SIZE = 10;

  // props
  type Props = {
    filePath: string;
    visible?: boolean;
  };
  const emits = defineEmits(['closeUploader', 'uploadSuccess', 'startUpload']);
  const props = withDefaults(defineProps<Props>(), {
    visible: false,
  });

  // refs
  const fileUploadForm = ref();

  const imgUrlList = ref(['']);
  const isHide = ref(false);
  const defaultFrameInterval = 5;
  const dialogVisible = computed(() => {
    return props.visible;
  });
  const state = reactive({
    uploadKey: 1,
    uploadStep: 0,
    loading: false,
    accept: '.jpg,.png,.bmp,.jpeg',
    title: '导入图片',
    percentage: 0,
    uploadStatus: undefined,
    form: {
      frameInterval: defaultFrameInterval,
    },
    error: null, // 上传错误
    // 上传步数
    steps: [],
  });

  // 上传实时进度格式
  const format = (percentage) => {
    return percentage <= 100 ? `${percentage}%` : ``;
  };

  // 上传到minio失败，给出提示
  const uploadError = (err) => {
    state.loading = false;
    createMessage.error(err.message || '上传失败', 5);
    Object.assign(state, {
      loading: false,
      error: err,
      uploadStatus: 'exception',
      uploadStep: 2,
      title: '上传失败',
    });
  };

  // 上传到minio成功后，获去图片链接列表
  const uploadSuccess = (res) => {
    const files = getFileFromMinIO(res);
    createMessage.success('上传成功', 5);
    Object.assign(state, {
      loading: false,
      uploadStatus: 'success',
      uploadStep: 2,
      title: '上传成功',
    });
    if (files && files.length > 0) {
      imgUrlList.value = files;
      if (isHide.value) {
        emits('uploadSuccess', imgUrlList.value);
      }
    } else {
      createMessage.error('获取图片异常', 5);
    }
  };

  // 点击确定上传按钮，针对上传组件进行操作(ref="fileUploadForm")
  const uploadSubmit = (ref) => {
    // 判断选中文件数量再去调接口
    if (ref.formRef.lenOfFileList === 0) {
      createMessage.error('文件不能为空', 5);
      return;
    }
    state.loading = true;
    emits('startUpload');
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

  const handleClose = (flag: string) => {
    if (flag === 'hide') isHide.value = true;
    emits('closeUploader', flag, imgUrlList.value);
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
    return {
      params: {
        // 图片存放路径
        objectPath: props.filePath + '/images',
        hash: true,
      },
    };
  });
</script>
<style scoped>
  .upload-form :deep(.el-upload-list) {
    display: none;
  }
</style>
