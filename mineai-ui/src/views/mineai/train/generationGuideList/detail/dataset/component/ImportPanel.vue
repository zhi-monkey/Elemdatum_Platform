<!-- ImportPanel.vue -->
<template>
  <div>
    <ACard
      title="导入方式"
      style="
        padding: 20px;
        min-height: 600px;
        margin-bottom: 30px;
        display: flex;
        flex-direction: column;
        background: transparent !important;
      "
      :tab-list="tabItems"
      :active-tab-key="activeTab"
      @tabChange="handleTabChange"
    >
      <!-- 本地上传 -->
      <div v-if="activeTab === 'local'" class="import-content local-upload-content">
        <div class="content-section">
          <div class="description-section">
            <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 16px">本地文件上传</h3>
            <p style="margin-bottom: 16px; color: #9d9d9d; line-height: 1.6">
              支持从本地上传图片或视频文件至平台进行标注处理
            </p>
          </div>
          <div class="format-info">
            <h4 style="margin-bottom: 12px; color: #989898">支持格式：</h4>
            <ul style="margin: 0; padding-left: 20px; color: #666; line-height: 1.8">
              <li><strong>图片格式：</strong>.jpg, .png, .bmp, .jpeg</li>
              <li><strong>图片限制：</strong>单个文件不大于 5 MB</li>
              <li><strong>视频格式：</strong>.mp4</li>
              <li><strong>视频限制：</strong>单个文件不大于 1024 MB</li>
            </ul>
          </div>
          <div class="upload-actions">
            <div class="action-buttons">
              <AButton
                type="primary"
                size="large"
                @click="handleLocalUploadImage"
                :disabled="isImportDisabled"
                class="upload-btn image-upload"
              >
                <template #icon>
                  <FileImageOutlined />
                </template>
                上传图片
              </AButton>
              <AButton
                type="primary"
                size="large"
                @click="handleLocalUploadVideo"
                :disabled="isImportDisabled"
                class="upload-btn video-upload"
              >
                <template #icon>
                  <VideoCameraOutlined />
                </template>
                上传视频
              </AButton>
            </div>
            <div class="upload-tips">
              <p style="margin: 16px 0 0 0; color: #9d9d9d; font-size: 12px">
                提示：上传后系统将自动进行格式验证和预处理；
                上传本地视频后需进入右侧【查看详情】的【视频列表】中进行视频抽帧,
                抽帧后图片会进入数据集
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 数据仓库导入 -->
      <div v-if="activeTab === 'datarepo'" class="import-content datarepo-content">
        <div class="content-section">
          <div class="description-section">
            <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 16px">数据仓库导入</h3>
            <p style="margin-bottom: 24px; color: #9d9d9d; line-height: 1.6">
              从已配置的数据仓库中选择数据进行导入，支持批量处理
            </p>
          </div>

          <div class="form-section">
            <BasicForm
              :labelWidth="100"
              :schemas="dataRepoImportSchemas"
              :actionColOptions="{ span: 24 }"
              :showResetButton="false"
              :showSubmitButton="false"
              @register="registerForm"
            />
          </div>

          <div class="import-actions">
            <AButton
              type="primary"
              size="large"
              @click="handleDataRepoImportSubmit"
              :disabled="isImportDisabled"
              class="import-btn"
            >
              <template #icon>
                <CloudDownloadOutlined />
              </template>
              确认导入
            </AButton>
          </div>

          <div class="import-tips">
            <p style="margin: 16px 0 0 0; color: #9d9d9d; font-size: 12px">
              提示：导入过程中请勿关闭页面，系统会自动同步数据仓库中的最新数据
            </p>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'dataset'" class="import-content dataset-content">
        <div class="content-section">
          <div class="description-section">
            <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 16px">已发布数据集导入</h3>
            <p style="margin-bottom: 24px; color: #9d9d9d; line-height: 1.6">
              从已发布的数据集版本中选择合适的数据进行导入
            </p>
          </div>

          <div class="form-section">
            <AForm
              layout="vertical"
              :model="publishedDatasetImportFormModel"
              ref="datasetVersionImportRef"
            >
              <AFormItem
                label="选择数据集"
                required
                :rules="[{ required: true, message: '请选择数据集' }]"
              >
                <ASelect
                  placeholder="请选择"
                  v-model:value="publishedDatasetImportFormModel.dataset"
                  :options="datasetList"
                  @dropdown-visible-change="handleDatasetDropdownOpen"
                  :loading="isDatasetLoading"
                  @change="reFetchVersionList"
                />
              </AFormItem>
              <AFormItem
                label="版本选择"
                required
                :rules="[{ required: true, message: '请选择版本' }]"
              >
                <ASelect
                  placeholder="请选择版本"
                  v-model:value="publishedDatasetImportFormModel.availableVersion"
                  :options="availableVersionList"
                  @change="showVersionInfo"
                />
              </AFormItem>
              <div v-if="versionInfo" class="version-info-panel">
                <!-- 图片数量信息 -->
                <p class="info-line">
                  该数据集版本包含
                  <span class="info-highlight">{{ versionInfo.imgCount }}</span>
                  张图片
                </p>

                <!-- 重合标签信息 -->
                <div
                  v-if="versionInfo.mutualLabels && versionInfo.mutualLabels.length > 0"
                  class="info-line"
                >
                  <span class="label-title">重合的标签:</span>
                  <div class="tags-container">
                    <ATag
                      v-for="label in versionInfo.mutualLabels"
                      :key="label.id"
                      :color="label.color"
                    >
                      {{ label.name }}
                    </ATag>
                  </div>
                </div>
                <!-- 如果没有重合标签 -->
                <div v-else class="info-line">
                  <span class="label-title">与当前数据集无重合标签</span>
                </div>
              </div>
            </AForm>
          </div>

          <div class="import-actions">
            <AButton
              type="primary"
              size="large"
              @click="handleDataVersionImportSubmit"
              :disabled="
                isImportDisabled ||
                !publishedDatasetImportFormModel.dataset ||
                !publishedDatasetImportFormModel.availableVersion
              "
              class="import-btn"
            >
              <template #icon>
                <DatabaseOutlined />
              </template>
              确认导入
            </AButton>
          </div>
        </div>
      </div>

      <!-- 公开数据集组导入 -->
      <div v-if="activeTab === 'public-dataset'" class="import-content public-dataset-content">
        <div class="content-section">
          <div class="description-section">
            <h3 style="margin-bottom: 16px; color: #1890ff; font-size: 16px">绑定公开数据集</h3>
            <p style="margin-bottom: 24px; color: #9b9b9b; line-height: 1.6">
              从公开数据集组中选择合适的数据集版本进行绑定使用
            </p>
          </div>
          <PublicDatasetCascade
            :model-generation-id="modelGenerationId"
            :bound-versions="boundVersions"
            :is-import-disabled="isImportDisabled"
            :annotate-type="annotateType"
            :annotation-format="annotationFormat"
            @bind-success="handlePublicDatasetBindSuccess"
          />
        </div>
      </div>
    </ACard>

    <UploadDataFile
      :row="uploadDataFileParam"
      :visible="uploadDataFileVisible"
      :closeUploadDataFile="closeUploadDataFile"
      :hideUploadDataFile="hideUploadDataFile"
    />
    <div id="resume-button" v-if="resumeButtonVisible">
      <Button type="primary" shape="circle" size="large" @click="hideUploadDataFile">
        <template #icon>
          <RollbackOutlined />
        </template>
      </Button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { defineProps, onUnmounted, Ref, ref } from 'vue';
  import type { SelectProps } from 'ant-design-vue';
  import {
    Button,
    Button as AButton,
    Card as ACard,
    Form as AForm,
    FormItem as AFormItem,
    Modal,
    Select as ASelect,
    Tag as ATag,
  } from 'ant-design-vue';
  import {
    FileImageOutlined,
    VideoCameraOutlined,
    CloudDownloadOutlined,
    DatabaseOutlined, // 新增
    RollbackOutlined,
  } from '@ant-design/icons-vue';
  import UploadDataFile from '/@/views/mineai/data/dataset-details2/upload-datafile-inline.vue';
  import PublicDatasetCascade from './PublicDatasetCascade.vue'; // 导入新创建的级联选择组件
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { importDatasetFromDataRepo } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    getAvailableVersionList,
    getDatasetListApi,
    getImportingVersionInfo,
  } from '/@/views/mineai/train/generationGuideList/detail/dataset/component/api';
  import { importDatasetFromDatasetVersion } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { guidedTrainEventEmitter } from '/@/views/mineai/train/generationGuideList/detail/train/intervalBus';

  const { createMessage } = useMessage();

  const props = defineProps<{
    createdDatasetId: number;
    modelGenerationId?: number;
    boundVersions?: any[];
    annotateType?: number; // 标注类型：102=目标检测，103=语义分割
    annotationFormat?: string; // 标注格式：YOLO, Segment-YOLO, COCO等
  }>();

  const emits = defineEmits<{
    (e: 'bind-success'): void;
  }>();

  const activeTab = ref<'local' | 'datarepo' | 'dataset' | 'public-dataset'>('local');
  const datasetVersionImportRef = ref();
  const tabItems = [
    { key: 'local', tab: '本地上传' },
    { key: 'datarepo', tab: '数据仓库导入' },
    // { key: 'dataset', tab: '已发布数据集导入' },
    { key: 'public-dataset', tab: '绑定公开数据集' },
  ];

  // 原始版本的数据集导入相关状态
  const isDatasetLoaded = ref(false);
  const isDatasetLoading = ref(false);
  const datasetList = ref<SelectProps['options']>([]);
  const availableVersionList = ref<SelectProps['options']>([]);
  const versionInfo = ref(null);
  const publishedDatasetImportFormModel = ref({
    dataset: null,
    availableVersion: null,
  });

  //禁用导入
  const isImportDisabled = ref(false);

  // 监听状态变化
  guidedTrainEventEmitter.on('datasetStatusChanged', ({ status, dataConversion }) => {
    // 定义需要禁用导入操作的状态
    const disabledStatuses = [302, 403, 103, 1]; // 抽帧中、导入中、自动标注中、上传中
    const isProcessing = disabledStatuses.includes(status) || dataConversion === 4;

    isImportDisabled.value = isProcessing;
  });

  // 在组件卸载时取消监听
  onUnmounted(() => {
    guidedTrainEventEmitter.off('datasetStatusChanged');
  });

  const resumeButtonVisible: Ref<boolean> = ref(false);
  const uploadDataFileParam: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);

  // 原始版本的数据集相关方法
  const handleDatasetDropdownOpen = async (visible) => {
    if (visible && !isDatasetLoaded.value) {
      try {
        datasetList.value = await getDatasetList();
        isDatasetLoaded.value = true;
      } catch (error) {
        console.error('数据集列表获取失败', error);
        createMessage.error('数据集列表获取失败');
      }
    }
  };

  const showVersionInfo = async () => {
    const targetDatasetId = props.createdDatasetId;
    const fromVersionId = publishedDatasetImportFormModel.value.availableVersion;
    if (!fromVersionId) {
      versionInfo.value = null;
      return;
    }
    try {
      // 将接口返回的数据存入 ref
      versionInfo.value = await getImportingVersionInfo(fromVersionId, targetDatasetId);
    } catch (error) {
      console.error('获取版本导入信息失败', error);
      createMessage.error('获取版本导入信息失败');
      versionInfo.value = null; // 出错时也清空信息
    }
  };

  const useDebounce = (fn, delay = 1000) => {
    let timer;
    return function (...args) {
      clearTimeout(timer); // 每次调用时清除旧定时器
      timer = setTimeout(() => {
        fn.apply(this, args); // 仅在停止触发后执行一次
      }, delay);
    };
  };

  // 使用示例
  const reFetchVersionList = useDebounce(async () => {
    publishedDatasetImportFormModel.value.availableVersion = null;
    versionInfo.value = null;

    let res = await getAvailableVersionList(
      publishedDatasetImportFormModel.value.dataset,
      props.createdDatasetId,
    );
    if (res.length > 0) {
      availableVersionList.value = res;
    } else {
      availableVersionList.value = [
        {
          label: '当前数据集没有可选的版本',
          value: null,
          disabled: true,
        },
      ];
    }
  });

  const getDatasetList = async () => {
    isDatasetLoading.value = true;
    let res = await getDatasetListApi();
    isDatasetLoading.value = false;
    return res;
  };

  function resetDatasetImportForm() {
    // 1. 清空表单模型中的值
    publishedDatasetImportFormModel.value.dataset = null;
    publishedDatasetImportFormModel.value.availableVersion = null;

    // 2. 清空版本下拉列表的选项
    availableVersionList.value = [];

    // 3. 清空（隐藏）版本信息展示面板
    versionInfo.value = null;
  }

  async function handleDataVersionImportSubmit() {
    try {
      Modal.confirm({
        title: '确认导入操作',
        content: '确认要从该数据集版本导入吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          await importDatasetFromDatasetVersion(
            props.createdDatasetId,
            publishedDatasetImportFormModel.value.availableVersion, // 使用选择的版本ID作为第二个参数
          );
          createMessage.success('导入任务下发成功');
          guidedTrainEventEmitter.emit('datasetCardStatusPoll');
          resetDatasetImportForm();
        },
        onCancel: () => {},
      });
    } catch (e) {
      console.error(e);
    }
  }

  // 处理上传文件模态框
  function handleLocalUploadImage() {
    uploadDataFileParam.value = { id: props.createdDatasetId, dataType: 0 };
    uploadDataFileVisible.value = true;
  }

  // 处理上传视频模态框
  function handleLocalUploadVideo() {
    uploadDataFileParam.value = { id: props.createdDatasetId, dataType: 1 };
    uploadDataFileVisible.value = true;
  }

  const handleTabChange = (key: string) => {
    activeTab.value = key as typeof activeTab.value;
  };

  const closeUploadDataFile = (flag): void => {
    uploadDataFileVisible.value = false;
    // 关闭恢复按钮
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }
    // 判断是否是取消还是完成
    // 完成的话刷新dataset状态的卡片
    if (flag) {
      guidedTrainEventEmitter.emit('datasetCardStatusRefresh');
    }
  };

  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
  };

  // 处理公开数据集绑定成功
  const handlePublicDatasetBindSuccess = () => {
    guidedTrainEventEmitter.emit('datasetCardStatusPoll');
    emits('bind-success');
  };

  const dataRepoImportSchemas: FormSchema[] = [
    {
      field: 'dataRepo',
      label: '数据仓库',
      component: 'ApiSelect',
      required: true,
      componentProps: {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        api: async () => {
          return await maHttp.get(
            {
              url: '/datarepos/queryAllDataReposNotNull',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
        },
        labelField: 'name',
        valueField: 'id',
        immediate: false,
      },
    },
    {
      field: 'dataType',
      label: '数据类型',
      component: 'RadioButtonGroup',
      defaultValue: 0,
      componentProps: {
        options: [
          { label: '图片', value: 0 },
          { label: '视频', value: 1 },
        ],
      },
    },
  ];

  const [registerForm, { validate, resetFields, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: dataRepoImportSchemas,
    showActionButtonGroup: false,
  });

  async function handleDataRepoImportSubmit() {
    try {
      const values = await validate();
      Modal.confirm({
        title: '确认导入操作',
        content: '确认要从该数据仓库导入吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          await importDatasetFromDataRepo(values.dataRepo, props.createdDatasetId, values.dataType);
          createMessage.success('导入任务下发成功');
          guidedTrainEventEmitter.emit('datasetCardStatusPoll');
        },
        onCancel: () => {},
      });
    } catch (e) {
      console.error(e);
    } finally {
    }
  }
</script>

<style scoped lang="scss">
  .import-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 500px;
    padding: 20px 0;

    .content-section {
      display: flex;
      flex-direction: column;
      height: 100%;
      gap: 24px;
    }

    .description-section {
      background: linear-gradient(135deg, rgba(24, 29, 49, 0.6) 0%, rgba(24, 29, 49, 0.4) 100%);
      padding: 24px;
      border-radius: 8px;
      border: 1px solid rgba(64, 150, 255, 0.2);
      backdrop-filter: blur(10px);

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #4096ff;

        &::before {
          content: '📋';
          font-size: 18px;
        }
      }

      p {
        color: rgba(255, 255, 255, 0.85);
      }
    }
    .format-info {
      padding: 16px;
      background: rgba(24, 29, 49, 0.3);
      border-radius: 6px;
      border: 1px solid rgba(64, 150, 255, 0.15);
      backdrop-filter: blur(5px);

      h4 {
        color: #4096ff;
        font-size: 14px;
        margin-bottom: 12px;
      }

      ul {
        li {
          margin-bottom: 4px;
          color: rgba(255, 255, 255, 0.75);

          strong {
            color: #4096ff;
          }
        }
      }
    }
  }

  .local-upload-content {
    .upload-actions {
      margin-top: auto;

      .action-buttons {
        display: flex;
        gap: 20px;
        justify-content: center;
        margin-bottom: 20px;

        .upload-btn {
          min-width: 140px;
          height: 48px;
          font-size: 16px;
          font-weight: 500;
          border-radius: 8px;
          backdrop-filter: blur(10px);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 16px rgba(64, 150, 255, 0.4);
          }

          &.image-upload {
            background: linear-gradient(135deg, #0077ff 0%, #0d90a2 100%) !important;
            color: #ffffff !important;
            box-shadow: 0 4px 12px rgba(0, 175, 255, 0.3) !important;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 6px 20px rgba(0, 175, 255, 0.4) !important;
            }
          }

          // 视频上传按钮：薄荷绿 -> 成功绿
          &.video-upload {
            background: linear-gradient(135deg, #57b47e 0%, #0b733b 100%) !important;
            color: #ffffff !important;
            box-shadow: 0 4px 12px rgba(40, 199, 111, 0.3) !important;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 6px 20px rgba(40, 199, 111, 0.4) !important;
            }
          }
        }
      }

      .upload-tips {
        text-align: center;
        padding: 16px;
        background: rgba(24, 29, 49, 0.2);
        border-radius: 6px;
        border: 1px dashed rgba(64, 150, 255, 0.2);
        backdrop-filter: blur(5px);

        p {
          color: rgba(255, 255, 255, 0.6);
        }
      }
    }
  }

  .datarepo-content {
    .form-section {
      background: rgba(24, 29, 49, 0.3);
      padding: 24px;
      border-radius: 8px;
      border: 1px solid rgba(64, 150, 255, 0.15);
      flex: 1;
      backdrop-filter: blur(5px);
    }

    .import-actions {
      text-align: center;

      .import-btn {
        min-width: 160px;
        height: 48px;
        font-size: 16px;
        font-weight: 500;
        border-radius: 8px;
        background: linear-gradient(
          135deg,
          rgba(114, 46, 209, 0.8) 0%,
          rgba(83, 29, 171, 0.9) 100%
        );
        border: 1px solid rgba(114, 46, 209, 0.3);
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(114, 46, 209, 0.3);
        backdrop-filter: blur(10px);
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(114, 46, 209, 0.4);
        }
      }
    }

    .import-tips {
      text-align: center;
      padding: 16px;
      background: transparent;
      border-radius: 6px;
      border: 1px dashed rgba(114, 46, 209, 0.3);

      p {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }
  .datarepo-content .import-btn {
    border: none !important;
    background: linear-gradient(135deg, #4f56ff 0%, #7242f6 100%) !important;
    color: #ffffff !important;
    box-shadow: 0 4px 12px rgba(98, 77, 248, 0.35) !important;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(98, 77, 248, 0.45) !important;
    }
  }

  // (可选) 如果您使用了“已发布数据集导入”，也为它应用相同样式
  .dataset-content .import-btn {
    border: none !important;
    background: linear-gradient(135deg, #4f56ff 0%, #7242f6 100%) !important;
    color: #ffffff !important;
    box-shadow: 0 4px 12px rgba(98, 77, 248, 0.35) !important;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(98, 77, 248, 0.45) !important;
    }
  }

  .dataset-content {
    .form-section {
      background: rgba(24, 29, 49, 0.3);
      padding: 24px;
      border-radius: 8px;
      border: 1px solid rgba(64, 150, 255, 0.15);
      flex: 1;
      backdrop-filter: blur(5px);
    }

    .import-actions {
      text-align: center;

      .import-btn {
        min-width: 160px;
        height: 48px;
        font-size: 16px;
        font-weight: 500;
        border-radius: 8px;
        background: linear-gradient(
          135deg,
          rgba(250, 140, 22, 0.8) 0%,
          rgba(212, 107, 8, 0.9) 100%
        );
        border: 1px solid rgba(250, 140, 22, 0.3);
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(250, 140, 22, 0.3);
        backdrop-filter: blur(10px);
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(250, 140, 22, 0.4);
        }
      }
    }

    .import-tips {
      text-align: center;
      padding: 16px;
      background: rgba(250, 140, 22, 0.1);
      border-radius: 6px;
      border: 1px dashed rgba(250, 140, 22, 0.3);
      backdrop-filter: blur(5px);

      p {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }

  .public-dataset-content {
    .description-section {
      background: transparent;
      border: 1px solid rgba(82, 196, 26, 0.2);
    }
  }

  // 版本信息面板也要适配深色主题
  .version-info-panel {
    padding: 16px;
    border: 1px solid rgba(64, 150, 255, 0.3);
    border-radius: 4px;
    margin-bottom: 24px;
    background: rgba(24, 29, 49, 0.4);
    backdrop-filter: blur(10px);
  }

  .info-line {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.75);
    display: flex;
    align-items: flex-start;
    margin-bottom: 12px;
  }
  .info-line:last-child {
    margin-bottom: 0;
  }

  .info-highlight {
    font-weight: bold;
    color: #4096ff;
    margin: 0 4px;
  }

  .label-title {
    margin-right: 8px;
    flex-shrink: 0;
    color: rgba(255, 255, 255, 0.85);
  }

  .tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  // 深色主题下的表单样式调整
  :deep(.ant-form-item-label > label) {
    color: rgba(255, 255, 255, 0.85);
  }

  :deep(.ant-select-selector) {
    background: rgba(24, 29, 49, 0.4) !important;
    border: 1px solid rgba(64, 150, 255, 0.2) !important;
    backdrop-filter: blur(5px);
  }

  :deep(.ant-select-selection-placeholder) {
    color: rgba(255, 255, 255, 0.4);
  }

  :deep(.ant-radio-button-wrapper) {
    background: rgba(24, 29, 49, 0.3);
    border: 1px solid rgba(64, 150, 255, 0.2);
    color: rgba(255, 255, 255, 0.75);

    &:hover {
      border-color: rgba(64, 150, 255, 0.4);
    }

    &.ant-radio-button-wrapper-checked {
      background: rgba(64, 150, 255, 0.2);
      border-color: #4096ff;
      color: #4096ff;
    }
  }

  // 响应式处理
  @media (max-width: 768px) {
    .local-upload-content .upload-actions .action-buttons {
      flex-direction: column;
      align-items: center;

      .upload-btn {
        width: 200px;
      }
    }
  }
</style>
