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
    <!-- 图片/视频上传区域（使用 inline2 + uploadStore） -->
    <div v-if="activeTabName !== 'zip'" class="form-upload">
      <upload-inline
        :key="state.row.id"
        ref="fileUploadForm"
        action="fakeApi"
        :accept="state.accept"
        v-bind="optionCreateProps"
        @upload-success="uploadSuccess"
        @upload-error="uploadError"
        @file-change="handleFileChange"
        @upload-progress="handleUploadProgress"
      />

      <!--上传视频时显示帧间隔设置-->
      <!-- 移除视频帧间隔设置 -->

      <!-- 填入元信息（可选，供数据筛选使用） -->
      <div v-if="activeTabName === 'image'" style="margin-top: 8px">
        <el-collapse v-model="metadataCollapseActive">
          <el-collapse-item title="填入元信息（可选）" name="metadata">
            <el-form label-width="96px" size="small">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="业务场景">
                    <el-select
                      v-model="metadata.scenario"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in scenarioOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="采集地点">
                    <el-select
                      v-model="metadata.location"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in locationOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="车辆/设备">
                    <el-select
                      v-model="metadata.device"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in deviceOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="数据来源">
                    <el-select
                      v-model="metadata.sourceType"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in sourceTypeOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="设备/相机编号">
                    <el-select
                      v-model="metadata.deviceSn"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in deviceSnOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="光照/环境">
                    <el-select
                      v-model="metadata.lighting"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in lightingOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="数据质量">
                    <el-select
                      v-model="metadata.quality"
                      clearable
                      filterable
                      allow-create
                      placeholder="选择或输入"
                      style="width: 100%"
                    >
                      <el-option v-for="s in qualityOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="采集时间">
                    <el-date-picker
                      v-model="metadata.captureTime"
                      type="datetime"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      placeholder="选择时间"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <!-- 压缩包导入区域（参考 upload-datafile-zip.vue，使用 inline.vue 自己管理进度） -->
    <div v-else>
      <!-- 选择上传的文件 -->
      <div v-show="state.uploadStep === 0">
        <upload-inline-zip
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
      <!-- 上传文件进度展示 -->
      <div v-show="state.uploadStep === 1">
        <el-progress
          :percentage="state.percentage"
          :status="state.uploadStatus"
          :stroke-width="24"
        />
      </div>
      <!-- 结果展示 -->
      <div v-show="state.uploadStep === 2">
        <el-progress
          :percentage="state.uploadStatus === 'exception' ? 0 : 100"
          :status="state.uploadStatus"
          :stroke-width="24"
        />
        <div v-if="state.uploadStatus === 'exception'" class="app-result-subtitle mt-10">
          {{ state.error && state.error.message }}
        </div>
      </div>
    </div>

    <!-- 弹窗头部 -->
    <template #header>
      <el-tabs v-model="activeTabName" class="demo-tabs" @tab-click="handleTabClick">
        <el-tab-pane label="上传图片" name="image" :disabled="imageTabStat" />
        <el-tab-pane label="上传视频" name="video" :disabled="videoTabStat" />
        <el-tab-pane label="从压缩包导入图片" name="zip" />
      </el-tabs>
    </template>

    <!-- 弹窗底部按钮区域 -->
    <template #footer>
      <!-- 图片/视频：统一按钮 -->
      <div v-if="activeTabName !== 'zip'">
        <el-button size="small" @click="handleClose(false)">取消</el-button>
        <el-button size="small" :loading="state.loading" @click="uploadSubmit(fileUploadForm)">
          开始上传
        </el-button>
      </div>

      <!-- 压缩包导入：三步按钮逻辑 -->
      <div v-else>
        <div v-show="state.uploadStep === 0">
          <el-button size="small" @click="handleClose(false)">取消</el-button>
          <el-button
            size="small"
            type="primary"
            :loading="state.loading"
            @click="uploadSubmit(fileUploadForm)"
          >
            开始上传
          </el-button>
        </div>
        <div v-show="state.uploadStep === 1" class="tc">
          <el-button size="small" @click="handleClose(false)">隐藏</el-button>
        </div>
        <div v-show="state.uploadStep === 2" class="tc">
          <el-button size="small" type="primary" @click="handleClose(true)">
            {{ state.uploadStatus === 'success' ? '完成' : '关闭' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

</template>

<script setup lang="ts">
  import { isNil } from 'lodash-es';
  import { reactive, watch, computed, ref } from 'vue';
  import UploadInline from './components/UploadForm/inline2.vue';
  import UploadInlineZip from './components/UploadForm/inline.vue';
  import { getFileFromMinIO, withDimensionFile, trackUploadProps, dataTypeCodeMap } from './util';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { getUploadTaskStatus } from './api';
  import { useUploadStore } from '/@/store/modules/upload';
  import {
    createImportTransferTask,
    updateImportTransferProgress,
    completeImportTransferTask,
    failImportTransferTask,
  } from '/@/api/mineai/importTransferTask';

  const { createMessage } = useMessage();
  const uploadStore = useUploadStore();

  // 压缩包上传配置（参考 upload-datafile-zip.vue）
  const MAX_FILE_COUNT = 1;
  // 单个压缩包最大 size（MB），2G
  const MAX_FILE_SIZE = 1024 * 2;

  // 上传文件接口（异步，返回taskId）
  const submit = async (id, files, metadata): Promise<{ taskId: string }> => {
    return await maHttp.post(
      {
        url: `datasets/${id}/files`,
        headers: {},
        data: { files, metadata },
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

  // 压缩包导入接口（将压缩包中的数据导入数据集）
  const submitZip = async (
    id,
    archiveUrl: string,
    datasetType?: string,
    currentVersionName?: string,
  ) => {
    return await maHttp.post(
      {
        url: `datasets/${id}/files/zip`,
        headers: {},
        data: {
          // 为满足 DatasetImportDTO.@NotNull 对 datasetId 的校验，这里也在 body 中显式传入
          datasetId: id,
          archiveUrl,
          datasetType,
          currentVersionName,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
  };

  // props
  type Props = {
    row?: Object;
    visible?: boolean;
    loading?: boolean;
    closeUploadDataFile?: Function;
    hideUploadDataFile?: Function;
    startZipImportPolling?: Function;
  };

  const props = withDefaults(defineProps<Props>(), {
    row: () => ({}),
    visible: false,
    loading: false,
    closeUploadDataFile: () => {},
    hideUploadDataFile: () => {},
    startZipImportPolling: () => {},
  });

  // refs
  const fileUploadForm = ref();
  const formStep = ref();
  // 控制Tab启用 停用
  const imageTabStat = ref(false);
  const videoTabStat = ref(false);
  const dialogVisible = computed(() => props.visible);

  // 组件内部状态
  const state = reactive({
    uploadKey: 1,
    row: {} as any,
    uploadStep: 0,
    loading: false,
    isImage: undefined as boolean | undefined,
    isText: undefined as boolean | undefined,
    isAudio: undefined as boolean | undefined,
    isVideo: undefined as boolean | undefined,
    isZip: false as boolean,
    accept: '',
    title: '',
    percentage: 0,
    uploadStatus: undefined as 'success' | 'exception' | 'warning' | undefined,
    error: null as any,
    steps: [],
    transferTaskId: null as number | null, // 传输任务ID
  });

  // ========== 元信息（可选，供数据筛选使用） ==========
  const metadataCollapseActive = ref([]);
  const formatNow = () => {
    const d = new Date();
    const pad = (n: number) => (n < 10 ? '0' + n : '' + n);
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };
  const metadata = reactive({
    sourceType: '其他',
    captureTime: formatNow(),
    device: '其他',
    deviceSn: '其他',
    location: '其他',
    scenario: '其他',
    lighting: '其他',
    quality: '其他',
  });
  const scenarioOptions = ['皮带跑偏', '异物检测', '人员行为', '车辆检测', '明火烟雾', '设备状态', '其他'];
  const locationOptions = ['采煤工作面', '皮带巷', '运输大巷', '井口', '洗煤厂', '其他'];
  const deviceOptions = ['采煤机', '掘进机', '皮带输送机', '矿车', '刮板机', '其他'];
  const sourceTypeOptions = ['系统登记', '数据回流(RTSP)', 'HTTP相机', '数据仓库', '其他'];
  const deviceSnOptions = [] as string[];
  const lightingOptions = ['正常光照', '低照度', '逆光', '夜间', '井下照明', '强光', '其他'];
  const qualityOptions = ['清晰', '轻微模糊', '严重模糊', '过曝', '欠曝', '噪声', '其他'];

  // 组装提交给后端的元信息（空字符串转为 null，来源默认"系统登记"）
  const buildMetadata = () => {
    const trim = (v: any) => (typeof v === 'string' && v.trim() === '' ? null : v);
    return {
      sourceType: trim(metadata.sourceType) ?? '系统登记',
      captureTime: trim(metadata.captureTime),
      device: trim(metadata.device),
      deviceSn: trim(metadata.deviceSn),
      location: trim(metadata.location),
      scenario: trim(metadata.scenario),
      lighting: trim(metadata.lighting),
      quality: trim(metadata.quality),
    };
  };

  // ========== 关键新增：通过 store 或组件事件驱动遮罩里的进度 ==========
  // 如果 UploadInline 有 @upload-progress 事件，会直接走 handleUploadProgress。
  // 否则回退为监听 uploadStore 内的上传进度（如有）。
  const storeMinioPercent = computed(() => {
    const info: any = uploadStore.queryUploadStatus(state.row.id);
    return info?.percentage ?? 0;
  });

  watch(storeMinioPercent, async (val) => {
    state.percentage = val;
    // 更新传输任务进度
    if (state.transferTaskId && val > 0 && val < 100) {
      try {
        await updateImportTransferProgress(state.transferTaskId, {
          progress: val,
          stage: 'UPLOADING',
        });
      } catch (err) {
        console.error('更新传输任务进度失败:', err);
      }
    }
  });

  const handleUploadProgress = async (percent: number) => {
    state.percentage = Math.min(100, Math.max(0, Math.floor(percent)));
    // 更新传输任务进度
    if (state.transferTaskId && state.percentage > 0 && state.percentage < 100) {
      try {
        await updateImportTransferProgress(state.transferTaskId, {
          progress: state.percentage,
          stage: 'UPLOADING',
        });
      } catch (err) {
        console.error('更新传输任务进度失败:', err);
      }
    }
  };

  // 图片、视频以及压缩包信息存库
  const uploader = async (datasetId, files) => {
    // 压缩包导入：files 只取第一个文件的 url 作为压缩包地址
    if (state.isZip) {
      const firstFile = files && files.length > 0 ? files[0] : null;
      if (!firstFile || !firstFile.url) {
        throw new Error('未获取到压缩包地址');
      }

      const archiveUrl = firstFile.url;
      const row: any = state.row || {};

      const datasetType = row.datasetType || row.labelType || 'IMAGES_ONLY';
      const currentVersionName = row.currentVersionName;

      return submitZip(datasetId, archiveUrl, datasetType, currentVersionName);
    }

    if (state.isImage) {
      return submit(datasetId, files, buildMetadata());
    }
    if (state.isText || state.isAudio) {
      return submit(datasetId, files, null);
    }
    return submitVideo(datasetId, {
      files: files.map((file) => ({ url: file.url })),
    });
  };

  const uploadError = async (err) => {
    // 压缩包导入：只在当前对话框里展示错误，不操作 uploadStore
    if (state.isZip) {
      Object.assign(state, {
        loading: false,
        error: err,
        uploadStatus: 'exception',
        uploadStep: 2,
        title: '上传失败',
      });
      createMessage.error(err?.message || '上传失败', 5);
      return;
    }

    state.loading = false;

    // 更新传输任务为失败状态
    if (state.transferTaskId) {
      try {
        await failImportTransferTask(state.transferTaskId, err?.message || '上传失败');
      } catch (e) {
        console.error('更新传输任务失败状态时出错:', e);
      }
    }

    if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
    createMessage.error(err.message || '上传失败', 5);
    handleClose(false, true);
  };

  // 轮询上传任务状态（后台进行，不显示对话框，不显示遮罩）
  const pollUploadTaskStatus = async (datasetId: number, taskId: string) => {
    const maxPollCount = 120; // 10分钟
    const pollInterval = 5000;
    let pollCount = 0;

    const poll = async (): Promise<void> => {
      try {
        const result = await getUploadTaskStatus(datasetId, taskId);
        if (result.status === 'success') {
          if (uploadStore.queryUploadStatus(datasetId)) uploadStore.resetState(datasetId);
          createMessage.success('上传文件成功', 5);
          props.closeUploadDataFile(true);
          return;
        }
        if (result.status === 'failed') {
          if (uploadStore.queryUploadStatus(datasetId)) uploadStore.resetState(datasetId);
          createMessage.error(result.errorMessage || '上传失败', 5);
          props.closeUploadDataFile(true);
          return;
        }
        // processing
        pollCount++;
        if (pollCount >= maxPollCount) {
          if (uploadStore.queryUploadStatus(datasetId)) uploadStore.resetState(datasetId);
          createMessage.warning('处理超时，请刷新页面查看结果', 5);
          props.closeUploadDataFile(true);
          return;
        }
        setTimeout(poll, pollInterval);
      } catch (err: any) {
        if (err?.message?.includes('任务不存在')) {
          if (uploadStore.queryUploadStatus(datasetId)) uploadStore.resetState(datasetId);
          createMessage.warning('上传任务已完成，请刷新页面查看结果', 5);
          props.closeUploadDataFile(true);
          return;
        }
        pollCount++;
        if (pollCount >= maxPollCount) {
          if (uploadStore.queryUploadStatus(datasetId)) uploadStore.resetState(datasetId);
          createMessage.error('查询状态失败，请刷新页面查看结果', 5);
          props.closeUploadDataFile(true);
          return;
        }
        console.error('轮询任务状态时出错:', err);
        const errorMessage = err instanceof Error ? err.message : '未知错误';
        createMessage.error('轮询任务状态时出错: ' + errorMessage, 5);
        setTimeout(poll, pollInterval);
      }
    };

    await poll();
  };

  // 上传到minio成功后操作
  const uploadSuccess = async (res) => {
    // 文本已同步：无需任何进度
    if (state.isText) {
      if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
      if (state.transferTaskId) {
        try {
          await completeImportTransferTask(state.transferTaskId);
        } catch (e) {
          console.error('完成传输任务时出错:', e);
        }
      }
      handleClose(true, true);
      return;
    }

    const files = getFileFromMinIO(res);
    const successMessage = '上传文件成功';

    // 压缩包导入：参考 upload-datafile-zip.vue，不走图片/视频那套任务轮询和 uploadStore
    if (state.isZip) {
      if (!files.length) {
        handleClose(true, true);
        return;
      }
      try {
        await uploader(state.row.id, files);
        Object.assign(state, {
          loading: false,
          uploadStatus: 'success',
          uploadStep: 2,
          title: '上传成功',
        });
        props.startZipImportPolling(state.row.id);
        createMessage.success(successMessage, 5);
      } catch (err: any) {
        Object.assign(state, {
          loading: false,
          error: err,
          uploadStatus: 'exception',
          uploadStep: 2,
          title: '上传失败',
        });
      } finally {
        // 这里直接关闭当前对话框，由外层刷新数据集状态
        handleClose(true, true);
      }
      return;
    }

    // 非 zip：保持原有图片/音频/视频逻辑
    state.percentage = 0;

    // MinIO上传完成，更新传输任务进度到100%
    if (state.transferTaskId) {
      try {
        await updateImportTransferProgress(state.transferTaskId, {
          progress: 100,
          stage: 'PROCESSING',
        });
      } catch (err) {
        console.error('更新传输任务进度失败:', err);
      }
    }

    if (files.length > 0) {
      // 视频：原同步方式（此处不再展示遮罩，列表仅显示标注进度）
      if (state.isVideo) {
        uploader(state.row.id, files)
          .then(async () => {
            Object.assign(state, {
              loading: false,
              uploadStatus: 'success',
              uploadStep: 2,
              title: '上传成功',
            });
            if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
            if (state.transferTaskId) {
              try {
                await completeImportTransferTask(state.transferTaskId);
              } catch (e) {
                console.error('完成传输任务时出错:', e);
              }
            }
            createMessage.success(successMessage, 5);
            props.closeUploadDataFile(true);
          })
          .catch(async (err: any) => {
            Object.assign(state, {
              loading: false,
              error: err,
              uploadStatus: 'exception',
              uploadStep: 2,
              title: '上传失败',
            });
            if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
            if (state.transferTaskId) {
              try {
                await failImportTransferTask(state.transferTaskId, err?.message || '上传失败');
              } catch (e) {
                console.error('更新传输任务失败状态时出错:', e);
              }
            }
            createMessage.error(err?.message || '上传失败', 5);
          })
          .finally(() => {
            handleClose(true, true);
          });
      } else {
        // 图片/音频：MinIO 完成后立即开始后端任务轮询（列表仅显示标注进度）
        handleClose(true, false);
        try {
          const response = await uploader(state.row.id, files);
          const taskId = response?.data?.taskId || response?.taskId;
          if (!taskId) throw new Error('未能获取任务ID，请检查后端响应格式');
          // 后续仅轮询标注/解析进度（不再显示遮罩）
          // 注意：这里后端会自动关联并更新传输任务，所以前端不需要再手动更新
          pollUploadTaskStatus(state.row.id, taskId).catch(async (err: any) => {
            console.error('轮询任务状态失败:', err);
            if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
            if (state.transferTaskId) {
              try {
                await failImportTransferTask(state.transferTaskId, err?.message || '处理失败');
              } catch (e) {
                console.error('更新传输任务失败状态时出错:', e);
              }
            }
            createMessage.error(err?.message || '查询任务状态失败', 5);
            props.closeUploadDataFile(true);
          });
        } catch (err: any) {
          if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
          if (state.transferTaskId) {
            try {
              await failImportTransferTask(state.transferTaskId, err?.message || '上传失败');
            } catch (e) {
              console.error('更新传输任务失败状态时出错:', e);
            }
          }
          createMessage.error(err?.message || '上传失败', 5);
          props.closeUploadDataFile(true);
        }
      }
    } else {
      if (uploadStore.queryUploadStatus(state.row.id)) uploadStore.resetState(state.row.id);
      if (state.transferTaskId) {
        try {
          await completeImportTransferTask(state.transferTaskId);
        } catch (e) {
          console.error('完成传输任务时出错:', e);
        }
      }
      handleClose(true, true);
    }
  };

  // 点击确定上传按钮
  const uploadSubmit = async (ref) => {
    if (ref.formRef.lenOfFileList === 0) {
      createMessage.error('文件不能为空', 5);
      return;
    }

    // 压缩包导入：参考 upload-datafile-zip.vue，不使用 MinIO 进度遮罩和任务轮询
    if (state.isZip) {
      state.loading = true;
      // 使用 inline.vue 的回调更新进度和步骤
      ref.uploadSubmit((resolved, fileList) => {
        const mod = fileList.length % MAX_FILE_COUNT;
        const intSteps = (fileList.length - mod) / MAX_FILE_COUNT;
        const steps = Array.from({ length: intSteps }, (_, i) => (i + 1) * MAX_FILE_COUNT);
        if (mod) {
          steps.push((steps[steps.length - 1] || 0) + mod);
        }
        Object.assign(state, {
          uploadStep: 1,
          title: '上传中',
          steps,
        });

        // 进度条百分比
        state.percentage = state.percentage > 100 ? 100 : Math.round(resolved / fileList.length);
      });
      return;
    }

    // 图片/视频：创建传输任务
    state.percentage = 0;
    state.uploadStatus = undefined;

    try {
      // 创建传输任务
      const taskName = `${state.row.name || '数据集'} - ${state.isImage ? '图片' : state.isVideo ? '视频' : '音频'}导入`;
      const datasetType = state.isImage ? 'IMAGE' : state.isVideo ? 'VIDEO' : 'AUDIO';
      const totalFiles = ref.formRef.lenOfFileList || 0;

      const response = await createImportTransferTask({
        taskName,
        datasetType,
        datasetId: state.row.id,
        sourceType: 'LOCAL_UPLOAD',
        totalFiles,
        status: 'PROCESSING',
        stage: 'UPLOADING',
        progress: 0,
      });

      state.transferTaskId = response?.data?.id || response?.id || response;

      // 关闭对话框（避免用户继续操作），由传输任务监控进度
      handleClose(true, false);
      ref.uploadSubmit();
    } catch (err: any) {
      createMessage.error(err?.message || '创建传输任务失败', 5);
      console.error('创建传输任务失败:', err);
    }
  };

  // 文件选中后，另一个Tab禁用
  const handleFileChange = (_file, _fileList) => {
    if (activeTabName.value === 'image') {
      videoTabStat.value = true;
    } else {
      imageTabStat.value = true;
    }
  };

  const handleClose = (flag, shouldCloseDialog = true) => {
    props.closeUploadDataFile(flag);
    if (shouldCloseDialog) {
      imageTabStat.value = false;
      videoTabStat.value = false;
      Object.assign(state, {
        uploadStep: 0,
        uploadKey: state.uploadKey + 1,
        percentage: 0,
        uploadStatus: undefined,
        error: null,
        isZip: false,
        transferTaskId: null,
      });
      activeTabName.value = 'image';
    }
  };

  // 构建数据参数
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
    } as any;
    return dataParamMap[type] || {};
  };

  // 传递给子组件的参数
  const optionCreateProps = computed(() => {
    if (!state.row) return {};
    const storageRoot = state.row.uri || `dataset/${state.row.id}`;
    const props = {
      params: {
        datasetId: state.row.id,
        // The dataset URI is the storage root and prevents cross-type ID collisions.
        objectPath: state.isZip
          ? storageRoot
          : `${storageRoot}/${state.isVideo ? 'video' : 'origin'}`,
        type: state.isVideo ? 1 : 0,
      },
    } as any;
    const baseImgProps = { transformFile: withDimensionFile, hash: true } as any;
    if (state.isText || state.isAudio) {
      Object.assign(props, { dataType: 'text', hash: true });
    }
    if (state.isText)
      Object.assign(props, { acceptSize: 0.1, acceptSizeFormat: (size) => `${size * 1000} kb` });
    if (state.isImage) Object.assign(props, baseImgProps);
    if (state.isVideo) Object.assign(props, baseImgProps, trackUploadProps);
    if (state.isZip) {
      Object.assign(props, {
        acceptSize: 1024 * 2, // 2G 上限（单位：MB）
        url: `/datasets/${state.row.id}/files/zip`,
        method: 'POST',
        headers: {
          'Content-Type': 'application/zip',
        },
      });
    }
    return props;
  });

  const activeTabName = ref('image');

  const updateStateByTab = () => {
    if (activeTabName.value === 'zip') {
      Object.assign(state, {
        isImage: false,
        isText: false,
        isAudio: false,
        isVideo: false,
        isZip: true,
        title: '导入压缩包',
        accept: '.zip,.tar',
      });
      return;
    }

    const dataType =
      activeTabName.value === 'image' ? dataTypeCodeMap.IMAGE : dataTypeCodeMap.VIDEO;
    const nextParams = buildDataParams(dataType);
    Object.assign(state, { ...nextParams, isZip: false });
  };

  const handleTabClick = (pane) => {
    activeTabName.value = pane.paneName;
    updateStateByTab();
  };

  // 监测选中导入的行数据变化
  watch(
    () => props.row,
    (next) => {
      Object.assign(state, { row: { ...state.row, ...next } });
      if (state.row.dataType !== undefined) {
        const nextParams = buildDataParams(state.row.dataType);
        Object.assign(state, nextParams);
        activeTabName.value = state.row.dataType === dataTypeCodeMap.VIDEO ? 'video' : 'image';
      } else {
        updateStateByTab();
      }
    },
    { immediate: true },
  );
</script>

<style scoped>
  .form-upload :deep(.el-upload-list) {
    display: none;
  }

  /* 上传进度弹窗样式 */
  .upload-progress-dialog :deep(.el-dialog__body) {
    padding: 20px;
    display: flex;
    justify-content: center;
  }

  .upload-progress-dialog :deep(.el-dialog) {
    border-radius: 8px;
    margin: 0 auto !important;
    top: 50%;
    transform: translateY(-50%);
  }

  .import-loading-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30px 20px;
  }

  .import-loading-text {
    margin-top: 20px;
    font-size: 16px;
  }
</style>
