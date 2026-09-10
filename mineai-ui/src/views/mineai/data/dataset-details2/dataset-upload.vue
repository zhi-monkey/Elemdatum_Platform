<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="导入数据" @ok="handleSubmit">
    <BasicForm
      :labelWidth="100"
      :schemas="schemas"
      :actionColOptions="{ span: 24 }"
      :showResetButton="false"
      :showSubmitButton="false"
      @register="registerForm"
    />
    <a-button type="link" @click="handleDownload" v-show="showButton" class="ml-21">
      下载示例压缩包
    </a-button>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref } from 'vue';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import { downloadByUrl } from '/@/utils/file/download';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const dataType = ref(0);
  const record = ref();
  // 是否是点云(5) / 独立视频(6) 数据集：此类数据集仅支持“本地上传 + 无标注信息”直传，不提供数据仓库导入/有标注压缩包
  const isSpecialDataType = ref(false);
  const emits = defineEmits(['localUpload', 'localUploadZip', 'cloudUpload']);
  const condition = ref('CreateML');
  const showButton = ref(false);

  const schemas: FormSchema[] = [
    {
      field: 'type',
      label: '上传方式',
      component: 'Select',
      defaultValue: 'local',
      componentProps: {
        options: [
          { label: '本地上传', value: 'local' },
          { label: '从数据仓库导入', value: 'cloud' },
        ],
        onChange: (e) => {
          if (e === 'cloud') {
            showButton.value = false;
          }
        },
      },
      required: true,
    },
    {
      field: 'annotationStatus',
      label: '数据标注状态',
      component: 'RadioButtonGroup',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '无标注信息', value: 1 },
          { label: '有标注信息', value: 2 },
        ],
        onChange: (e) => {
          showButton.value = e === 2;
        },
      },
      ifShow: (renderCallbackParams) => {
        return renderCallbackParams.values?.type === 'local';
      },
    },
    {
      field: 'labelType',
      label: '标注格式',
      component: 'Select',
      required: true,
      componentProps: {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        options: [
          { label: 'CreateML', value: 'CreateML' },
          { label: 'VOC', value: 'VOC' },
          { label: 'YOLO', value: 'YOLO' },
        ],
        onChange: (e) => {
          condition.value = e;
        },
      },
      ifShow: (renderCallbackParams) => {
        return (
          renderCallbackParams.values?.annotationStatus === 2 &&
          renderCallbackParams.values?.type === 'local'
        );
      },
    },
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
      ifShow: (renderCallbackParams) => {
        return renderCallbackParams.values?.type === 'cloud';
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
      ifShow: (renderCallbackParams) => {
        return renderCallbackParams.values?.type === 'cloud';
      },
    },
  ];

  const [registerForm, { validate, resetFields, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: schemas,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    record.value = data.record;
    // 记录是否为点云(5)/独立视频(6) 数据集：此类数据集仅支持本地上传直传
    isSpecialDataType.value = data.dataType === 5 || data.dataType === 6;
    if (data.record.annotateType === 103) {
      await updateSchema({
        field: 'labelType',
        label: '标注格式',
        component: 'Select',
        required: true,
        componentProps: {
          dropdownAlign: {
            overflow: {
              adjustY: false, // 关闭下拉框垂直位置自适应
            },
          },
          options: [
            { label: 'YOLO', value: 'Segment-YOLO' },
            { label: 'COCO', value: 'Segment-COCO' },
          ],
          onChange: (e) => {
            condition.value = e;
          },
        },
        ifShow: (renderCallbackParams) => {
          return (
            renderCallbackParams.values?.annotationStatus === 2 &&
            renderCallbackParams.values?.type === 'local'
          );
        },
      });
      condition.value = 'Segment-YOLO';
    } else {
      await updateSchema({
        field: 'labelType',
        label: '标注格式',
        component: 'Select',
        required: true,
        componentProps: {
          dropdownAlign: {
            overflow: {
              adjustY: false, // 关闭下拉框垂直位置自适应
            },
          },
          options: [
            { label: 'COCO', value: 'COCO' },
            { label: 'VOC', value: 'VOC' },
            { label: 'YOLO', value: 'YOLO' },
          ],
          onChange: (e) => {
            condition.value = e;
          },
        },
        ifShow: (renderCallbackParams) => {
          return (
            renderCallbackParams.values?.annotationStatus === 2 &&
            renderCallbackParams.values?.type === 'local'
          );
        },
      });
    }
    // 是引导式就不显示有标注信息
    if (record.value?.isGuided === true) {
      // 只显示“无标注信息”选项
      await updateSchema({
        field: 'annotationStatus',
        label: '数据标注状态',
        component: 'RadioButtonGroup',
        defaultValue: 1,
        componentProps: {
          options: [
            { label: '无标注信息', value: 1 },
            // 不显示“有标注信息”选项
            // { label: '有标注信息', value: 2 },
          ],
          onChange: (e) => {
            showButton.value = e === 2;
          },
        },
        ifShow: (renderCallbackParams) => {
          return renderCallbackParams.values?.type === 'local';
        },
      });
    } else {
      // 恢复默认的 annotationStatus 配置（包含两个选项）
      await updateSchema({
        field: 'annotationStatus',
        label: '数据标注状态',
        component: 'RadioButtonGroup',
        defaultValue: 1,
        componentProps: {
          options: [
            { label: '无标注信息', value: 1 },
            { label: '有标注信息', value: 2 },
          ],
          onChange: (e) => {
            showButton.value = e === 2;
          },
        },
        ifShow: (renderCallbackParams) => {
          return renderCallbackParams.values?.type === 'local';
        },
      });
    }
    // 点云(5)/独立视频(6) 数据集：仅支持“本地上传 + 无标注信息”，强制切回本地上传、隐藏数据仓库与有标注相关选项
    if (isSpecialDataType.value) {
      await updateSchema({
        field: 'type',
        label: '上传方式',
        component: 'Select',
        defaultValue: 'local',
        componentProps: {
          options: [
            { label: '本地上传', value: 'local' },
            // 点云/独立视频暂不支持数据仓库导入
            // { label: '从数据仓库导入', value: 'cloud' },
          ],
          onChange: (e) => {
            if (e === 'cloud') {
              showButton.value = false;
            }
          },
        },
        required: true,
      });
      await updateSchema({
        field: 'annotationStatus',
        label: '数据标注状态',
        component: 'RadioButtonGroup',
        defaultValue: 1,
        componentProps: {
          options: [
            { label: '无标注信息', value: 1 },
            // 点云/独立视频暂不支持带标注上传
            // { label: '有标注信息', value: 2 },
          ],
          onChange: (e) => {
            showButton.value = e === 2;
          },
        },
        ifShow: (renderCallbackParams) => {
          return renderCallbackParams.values?.type === 'local';
        },
      });
    }
    console.log(showButton.value);
    showButton.value = false;
    console.log(showButton.value);
    await resetFields();
    dataType.value = data.dataType;
    record.value = data.record;
  });

  function handleDownload() {
    let url;
    if (condition.value === 'COCO') {
      url = 'zip/sample-obj-dct-annotated-coco.zip';
    } else if (condition.value === 'VOC') {
      url = 'zip/sample-obj-dct-annotated-voc.zip';
    } else if (condition.value === 'YOLO') {
      url = 'zip/sample-YOLO.zip';
    } else if (condition.value === 'Segment-COCO') {
      url = 'zip/sample_segmentation_COCO.zip';
    } else if (condition.value === 'Segment-YOLO') {
      url = 'zip/sample_segmentation_YOLO.zip';
    }

    downloadByUrl({
      url: url,
      target: '_self',
    });
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      if (values.type === 'local' && values.annotationStatus === 1) {
        emits('localUpload', record.value, dataType.value);
      }
      if (values.type === 'local' && values.annotationStatus === 2) {
        emits('localUploadZip', record.value, values.labelType);
      }
      if (values.type === 'cloud') {
        emits('cloudUpload', record.value, values.dataType, values.dataRepo);
      }
      closeModal();
    } catch (e) {
      console.error(e);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
