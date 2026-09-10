import { FormActionType, FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { useMessage } from '/@/hooks/web/useMessage';
import { h } from 'vue';
import { Card, Col, Row, Spin, Statistic, Tag } from 'ant-design-vue';

import {
  getJobAccLossAndImgCountsByGuidedGenerationId,
  getLabelsIByImageUrlApiAndDatasetId,
} from '/@/views/mineai/data/dataset-details2/api';

const { createMessage } = useMessage();

const LABEL_COLORS = ['pink', 'red', 'orange', 'green', 'cyan', 'blue', 'purple'];

export const fetchModelInfo = async (formActionType: FormActionType, generationId?: number) => {
  if (!generationId) return;

  const { setFieldsValue, updateSchema } = formActionType;
  try {
    await setFieldsValue({ modelInfoLoading: true });
    await updateSchema([{ field: 'modelInfo', show: true }]);

    const modelInfo = await getJobAccLossAndImgCountsByGuidedGenerationId(generationId);

    if (modelInfo) {
      await setFieldsValue({ modelInfo: modelInfo });
    }
  } catch (error) {
    console.error('获取模型信息失败:', error);
    createMessage.error('获取模型信息失败');
    await updateSchema([{ field: 'modelInfo', show: false }]); // 出错时隐藏
  } finally {
    await setFieldsValue({ modelInfoLoading: false });
  }
};

export const formSchema: FormSchema[] = [
  {
    field: 'datasetId',
    component: 'Input',
    show: false,
  },
  {
    field: 'applicationName',
    component: 'Input',
    show: false,
  },
  {
    field: 'deviceFirmware',
    component: 'Input',
    show: false,
  },
  {
    field: 'useDefault',
    label: '模型选择',
    component: 'Select',
    defaultValue: false,
    componentProps: ({ formActionType }) => ({
      options: [
        { label: '训练模型', value: false },
        { label: '默认模型', value: true },
      ],
      onChange: async (value) => {
        const { updateSchema, setFieldsValue } = formActionType;
        const isDefault = value === true;

        // 清空所有依赖字段
        await setFieldsValue({
          guidedGenerationId: null,
          image: null,
          labelData: [],
          modelInfo: null,
        });

        await updateSchema([
          // 训练模型路径
          { field: 'isStandardTrain', show: !isDefault, required: !isDefault },
          { field: 'guidedModelTaskSelector', show: !isDefault, required: !isDefault },
          { field: 'guidedGenerationId', required: !isDefault },
          { field: 'modelInfo', show: false },

          // 默认模型路径
          { field: 'image', show: isDefault, required: isDefault },
          { field: 'labelInfo', show: false },
        ]);
      },
    }),
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
  },
  {
    field: 'isStandardTrain',
    label: '模型来源',
    component: 'Input',
    defaultValue: '引导式训练',
    componentProps: {
      disabled: true,
    },
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
  },
  {
    field: 'guidedModelTaskSelector',
    label: '选择训练任务',
    component: 'Input',
    slot: 'guidedModelTaskSelectorSlot',
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
    show: true,
  },
  {
    field: 'image',
    label: '镜像选择',
    component: 'ApiSelect',
    componentProps: ({ formActionType, formModel }) => ({
      api: async () => {
        // 根据数据集的annotateType筛选镜像
        // annotateType: 102=目标检测, 103=语义分割
        // annotationType: Detection, Segmentation
        console.log('annotateType:', formModel.annotateType);
        const annotateType = Number(formModel.annotateType);
        let annotationType: string | undefined = undefined;
        if (annotateType === 102) {
          annotationType = 'Detection';
        } else if (annotateType === 103) {
          annotationType = 'Segmentation';
        }

        const hasApplicationContext = formModel.applicationName && formModel.deviceFirmware;

        return maHttp.get(
          {
            url: hasApplicationContext
              ? '/modelVersion/findMatchedAutoLabelImages'
              : '/modelVersion/findImageSupportAutoLabel',
            params: hasApplicationContext
              ? {
                  applicationName: formModel.applicationName,
                  deviceFirmware: formModel.deviceFirmware,
                  ...(annotationType ? { annotationType } : {}),
                }
              : annotationType
              ? { annotationType }
              : {},
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      immediate: false,
      labelField: 'name',
      valueField: 'url',
      onChange: async (value) => {
        const { updateSchema, setFieldsValue, getFieldsValue } = formActionType;

        // 清空旧的标签数据
        await setFieldsValue({ labelData: [] });
        await updateSchema([{ field: 'labelInfo', show: false }]);

        if (!value) return; // 如果清空了选择，则不执行

        try {
          await setFieldsValue({ isLoadingLabels: true });
          await updateSchema([{ field: 'labelInfo', show: true }]);

          const datasetId = getFieldsValue().datasetId;
          if (!datasetId) {
            createMessage.error('数据集ID丢失，无法获取标签');
            return;
          }

          // 调用 API 获取标签
          const labels = await getLabelsIByImageUrlApiAndDatasetId(value, datasetId);

          if (labels && labels.length > 0) {
            const labelsWithColors = labels.map((label, index) => ({
              ...label,
              color: LABEL_COLORS[index % LABEL_COLORS.length],
            }));
            await setFieldsValue({ labelData: labelsWithColors });
          } else {
            await setFieldsValue({ labelData: [] });
          }
        } catch (error) {
          console.error('获取默认模型标签失败:', error);
          createMessage.error('获取标签失败，请重试');
          await setFieldsValue({ labelData: [] });
        } finally {
          await setFieldsValue({ isLoadingLabels: false });
        }
      },
    }),
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: false,
    show: false, // 默认隐藏
  },
  {
    field: 'modelInfo',
    label: '模型信息',
    component: 'Input',
    show: false,
    render: ({ model }) => {
      const modelInfo = model.modelInfo;
      const loading = model.modelInfoLoading;

      if (loading) {
        return h(
          'div',
          {
            style: 'display: flex; justify-content: center; align-items: center; min-height: 60px;',
          },
          [h(Spin, { size: 'small' }), h('span', { style: 'margin-left: 8px;' }, '加载中...')],
        );
      }
      if (!modelInfo) return null;

      return h(
        Card,
        {
          size: 'small',
          style: 'margin-top: 8px; background-color: #1f1f1f;',
          bodyStyle: { padding: '16px' },
        },
        () =>
          h(Row, { gutter: 16 }, () => [
            h(Col, { span: 8 }, () =>
              h(Statistic, {
                title: 'Accuracy',
                value: modelInfo.accuracy < 0 ? '--' : modelInfo.accuracy,
                precision: 4,
                suffix: '%',
                valueStyle: 'color: #52c41a; font-size: 12px; font-weight: bold;',
              }),
            ),
            h(Col, { span: 8 }, () =>
              h(Statistic, {
                title: 'Loss',
                value: modelInfo.loss < 0 ? '--' : modelInfo.loss,
                precision: 6,
                valueStyle: 'color: #1890ff; font-size: 12px; font-weight: bold;',
              }),
            ),
            h(Col, { span: 8 }, () =>
              h(Statistic, {
                title: '图片数量',
                value: modelInfo.imgCount <= 0 ? '--' : modelInfo.imgCount,
                suffix: '张',
                valueStyle: 'color: #722ed1; font-size: 12px; font-weight: bold;',
              }),
            ),
          ]),
      );
    },
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
  },
  {
    field: 'labelInfo',
    label: '模型支持标签',
    component: 'Input',
    show: false,
    render: ({ model }) => {
      const labels = model.labelData;
      const isLoading = model.isLoadingLabels;

      if (isLoading) {
        return h('div', { style: 'display: flex; align-items: center; min-height: 32px;' }, [
          h(Spin, { size: 'small' }),
          h('span', { style: 'margin-left: 8px; color: #999;' }, '正在加载标签...'),
        ]);
      }
      if (!labels || labels.length === 0) {
        return h(
          'div',
          { style: 'color: #999; min-height: 32px; line-height: 32px;' },
          '暂无匹配标签',
        );
      }

      return h(
        'div',
        { class: 'flex flex-wrap gap-1 items-center', style: 'min-height: 32px;' },
        labels.map((label) =>
          h(Tag, { color: label.color, key: label.id || label.name }, () => label.name),
        ),
      );
    },
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
  },
  {
    field: 'confidence',
    label: '置信度',
    component: 'InputNumber',
    defaultValue: 0.25,
    componentProps: { min: 0, max: 1, step: 0.01 },
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
  },

  {
    field: 'guidedGenerationId',
    component: 'Input',
    show: false,
    rules: [{ required: true, message: '请选择一个训练任务' }],
  },
  {
    field: 'labelData',
    component: 'Input',
    show: false,
    defaultValue: [],
  },
  {
    field: 'isLoadingLabels',
    component: 'Input',
    show: false,
    defaultValue: false,
  },
  {
    field: 'modelInfo',
    component: 'Input',
    show: false,
  },
  {
    field: 'modelInfoLoading',
    component: 'Input',
    show: false,
  },
  {
    field: 'annotateType',
    component: 'Input',
    show: false,
  },
];
