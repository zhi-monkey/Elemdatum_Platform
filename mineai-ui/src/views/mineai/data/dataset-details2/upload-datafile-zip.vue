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
        <el-button type="primary" :loading="state.loading" @click="handleUploadClick"
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
  import UploadInline from './components/UploadForm/inline.vue';
  import { getFileFromMinIO } from './util';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import JSZip from 'jszip';

  const { createMessage } = useMessage();

  // 每次最多上传的文件数量
  const MAX_FILE_COUNT = 1;

  // 单个压缩包最大size
  const MAX_FILE_SIZE = 1024 * 4;

  // props
  type Props = {
    row?: Object;
    visible?: boolean;
    loading?: boolean;
    closeUploadZipFile?: Function;
    hideUploadZipFile?: Function;
    startZipImportPolling?: Function;
  };

  const props = withDefaults(defineProps<Props>(), {
    row: () => {
      return {};
    },
    visible: false,
    loading: false,
    closeUploadZipFile: () => {},
    hideUploadZipFile: () => {},
    startZipImportPolling: () => {},
  });

  // refs
  const fileUploadForm = ref();

  const defaultFrameInterval = 5;
  const dialogVisible = computed(() => {
    return props.visible;
  });
  const state = reactive<{
    uploadKey: number;
    row: any;
    uploadStep: number;
    loading: boolean;
    accept: string;
    title: string;
    percentage: number;
    uploadStatus: any;
    form: { frameInterval: number };
    error: any;
    steps: number[];
  }>({
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

  // 文件信息存库
  const uploader = async (datasetId, currentVersionName, files) => {
    return await maHttp.post(
      {
        url: `datasets/datasetImport`,
        headers: {},
        data: {
          datasetId: datasetId,
          archiveUrl: files[0].url,
          datasetType: state.row.labelType,
          currentVersionName: currentVersionName,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
  };

  // 上传实时进度格式
  const format = (percentage) => {
    return percentage <= 100 ? `${percentage}%` : ``;
  };

  // 检测字符串是否包含中文
  const containsChinese = (str: string): boolean => {
    return /[\u4e00-\u9fa5]/.test(str);
  };

  // 验证压缩包中的标签是否包含中文
  const validateLabelsInZip = async (file: File): Promise<{ valid: boolean; message?: string }> => {
    try {
      // 只验证 .zip 格式，.tar 格式跳过验证
      if (!file.name.toLowerCase().endsWith('.zip')) {
        console.log('非 .zip 格式，跳过标签验证');
        return { valid: true };
      }

      // 读取文件为 ArrayBuffer，避免影响原始 File 对象
      const arrayBuffer = await file.arrayBuffer();
      const zip = await JSZip.loadAsync(arrayBuffer);

      const labelType = state.row.labelType;
      const labels: string[] = [];

      if (labelType === 'YOLO' || labelType === 'Segment-YOLO') {
        // YOLO 格式：查找 classes.txt 或 data.yaml
        const classesFiles = zip.file(/classes\.txt$/i);
        const yamlFiles = zip.file(/data\.yaml$/i);
        const targetFile =
          classesFiles.length > 0 ? classesFiles[0] : yamlFiles.length > 0 ? yamlFiles[0] : null;

        if (targetFile) {
          const content = await targetFile.async('string');
          // 解析标签名称（每行一个标签）
          const parsedLabels = content
            .split('\n')
            .map((line) => line.trim())
            .filter((line) => line && !line.startsWith('#'));
          labels.push(...parsedLabels);
        }
      } else if (labelType === 'VOC') {
        // VOC 格式：解析 XML 文件中的 <name> 标签
        const xmlFiles = zip.file(/\.xml$/i);
        for (const xmlFile of xmlFiles) {
          const content = await xmlFile.async('string');
          const nameMatches = content.match(/<name>(.*?)<\/name>/g);
          if (nameMatches) {
            nameMatches.forEach((match) => {
              const label = match.replace(/<\/?name>/g, '').trim();
              if (label && !labels.includes(label)) {
                labels.push(label);
              }
            });
          }
        }
      } else if (labelType === 'COCO' || labelType === 'Segment-COCO') {
        // COCO 格式：解析 JSON 文件中的 categories
        const jsonFiles = zip.file(/\.json$/i);
        for (const jsonFile of jsonFiles) {
          const content = await jsonFile.async('string');
          try {
            const data = JSON.parse(content);
            if (data.categories && Array.isArray(data.categories)) {
              data.categories.forEach((cat: any) => {
                if (cat.name && !labels.includes(cat.name)) {
                  labels.push(cat.name);
                }
              });
            }
          } catch (e) {
            // JSON 解析失败，跳过
          }
        }
      }

      // 如果没有找到标签文件，跳过验证
      if (labels.length === 0) {
        return { valid: true };
      }

      // 检查标签是否包含中文
      const chineseLabels = labels.filter((label) => containsChinese(label));
      if (chineseLabels.length > 0) {
        return {
          valid: false,
          message: `检测到文件中存在中文标签`,
        };
      }
      return { valid: true };
    } catch (error) {
      // 如果验证过程出错，允许继续上传（避免阻塞正常流程）
      return { valid: true };
    }
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
      uploader(state.row.id, state.row.currentVersionName, files)
        .then(() => {
          Object.assign(state, {
            loading: false,
            uploadStatus: 'success',
            uploadStep: 2,
            title: '上传成功',
          });
          props.startZipImportPolling(state.row.id);
          createMessage.success(successMessage, 5);
          // 手动触发数据集状态更新
          handleClose(true);
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

  // 点击"开始上传"按钮的处理函数
  const handleUploadClick = async () => {
    // 判断选中文件数量
    if (fileUploadForm.value.formRef.lenOfFileList === 0) {
      createMessage.error('文件不能为空', 5);
      return;
    }

    // 获取文件并验证标签（仅在有中文时阻止）
    try {
      const fileList = fileUploadForm.value.formRef.fileList;
      if (fileList && fileList.length > 0) {
        const fileItem = fileList[0];
        const file = fileItem.file || fileItem.raw || fileItem;

        if (file && file instanceof File) {
          // 验证标签是否包含中文
          state.loading = true;
          const validationResult = await validateLabelsInZip(file);
          state.loading = false;

          // 如果检测到中文标签，阻止上传
          if (!validationResult.valid) {
            createMessage.error(validationResult.message || '标签验证失败', 10);
            return;
          }
        }
      }
    } catch (error) {
      state.loading = false;
    }

    // 验证通过或跳过验证，执行原有的上传逻辑
    uploadSubmit(fileUploadForm.value);
  };

  // 原有的上传逻辑（不包含验证）
  const uploadSubmit = (ref: any) => {
    state.loading = true;
    // 上传组件的上传接口,也叫uploadSubmit
    ref.uploadSubmit((resolved, fileList) => {
      // 防御性检查：确保 fileList 存在
      if (!fileList || !Array.isArray(fileList) || fileList.length === 0) {
        console.error('fileList 无效:', fileList);
        return;
      }

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
        objectPath: 'dataset/' + state.row.id,
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
