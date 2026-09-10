import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h } from 'vue';
import { Spin, Tag } from 'ant-design-vue';
import { useMessage } from '/@/hooks/web/useMessage';
import { getLabelsByImageUrlApi } from '/@/views/mineai/data/dataset-details2/api';

const { createMessage } = useMessage();
const LABEL_COLORS = ['pink', 'red', 'orange', 'green', 'cyan', 'blue', 'purple'];
// @ts-ignore
export const formSchema: FormSchema[] = [
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
      onChange: (value) => {
        const { updateSchema, setFieldsValue } = formActionType;
        // 清空所有依赖字段
        setFieldsValue({
          isStandardTrain: true,
          standardGenerationId: null,
          standardJobName: null,
          guidedGenerationId: null,
          image: null,
          labelData: [],
        });

        const isDefault = value === true;
        updateSchema([
          { field: 'isStandardTrain', show: !isDefault, required: !isDefault },
          { field: 'standardGenerationId', show: !isDefault, required: !isDefault },
          { field: 'modelTaskSelector', show: !isDefault },
          { field: 'guidedModelTaskSelector', show: false },
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
    component: 'RadioGroup',
    defaultValue: true,
    componentProps: ({ formActionType }) => ({
      options: [
        { label: '标准化训练', value: true },
        { label: '引导式训练', value: false },
      ],
      onChange: (e) => {
        const { updateSchema, setFieldsValue } = formActionType;
        const isStandard = e.target.value;
        // 清空选择
        setFieldsValue({
          standardGenerationId: null,
          standardJobName: null,
          guidedGenerationId: null,
        });
        updateSchema([
          {
            field: 'standardGenerationId',
            show: isStandard,
            required: isStandard,
          },
          {
            field: 'modelTaskSelector',
            show: isStandard,
            required: isStandard,
          },
          {
            field: 'guidedGenerationId',
            required: !isStandard,
          },
          {
            field: 'standardJobName',
            required: isStandard,
          },
          {
            field: 'guidedModelTaskSelector',
            show: !isStandard,
          },
        ]);
      },
    }),
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
    show: true,
  },
  {
    // 标准化训练名称下拉框，用于驱动卡片选择器
    field: 'standardGenerationId',
    label: '标准化训练名称',
    component: 'ApiSelect',
    componentProps: ({ formActionType, formModel }) => ({
      api: async () => {
        // 从父组件的 datasetId 获取数据集ID
        const datasetId = formModel.datasetId;
        console.log(datasetId, 'datasetId', formModel);
        return maHttp.get(
          {
            url: '/modelGeneration/findModelGenerationSupportAutoLabel',
            params: datasetId ? { datasetId } : {},
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'name',
      valueField: 'id',
      immediate: false,
      onChange: () => {
        // 清空下级的任务选择
        formActionType.setFieldsValue({ standardJobName: null });
      },
    }),
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: true,
  },
  {
    // 卡片列表 - 用于标准化训练
    field: 'modelTaskSelector', // 占位字段
    label: '选择训练任务',
    component: 'Input',
    slot: 'modelTaskSelectorSlot', // 使用插槽
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    show: true,
  },
  {
    // 卡片列表 - 用于引导式训练
    field: 'guidedModelTaskSelector', // 占位字段
    label: '选择训练任务',
    component: 'Input',
    slot: 'guidedModelTaskSelectorSlot', // 使用插槽
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: false,
    show: false, // 默认隐藏
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
        const annotateType = Number(formModel.annotateType);
        let annotationType: string | undefined = undefined;
        if (annotateType === 102) {
          annotationType = 'Detection';
        } else if (annotateType === 103) {
          annotationType = 'Segmentation';
        }

        return maHttp.get(
          {
            url: '/modelVersion/findImageSupportAutoLabel',
            params: annotationType ? { annotationType } : {},
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'name',
      valueField: 'url',
      immediate: false,
      onChange: async (value) => {
        const { updateSchema, setFieldsValue } = formActionType;
        setFieldsValue({ labelData: [] });
        updateSchema([{ field: 'labelInfo', show: false }]);

        if (!value) {
          return;
        }

        try {
          await setFieldsValue({ isLoadingLabels: true });
          await updateSchema([{ field: 'labelInfo', show: true }]);

          const labels = await getLabelsByImageUrlApi(value);

          if (labels && labels.length > 0) {
            const labelsWithColors = labels.map((label, index) => ({
              ...label,
              color: LABEL_COLORS[index % LABEL_COLORS.length],
            }));
            await setFieldsValue({ labelData: labelsWithColors });
          } else {
            await setFieldsValue({ labelData: [] }); // API返回空数组
          }
        } catch (error) {
          console.error('获取默认模型标签失败:', error);
          createMessage.error('获取标签失败，请重试');
          await setFieldsValue({ labelData: [] }); // 出错时也清空
        } finally {
          await setFieldsValue({ isLoadingLabels: false });
        }
      },
    }),
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
    required: false,
    show: false,
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
        return h('div', { style: { display: 'flex', alignItems: 'center', minHeight: '32px' } }, [
          h(Spin, { size: 'small' }),
          h('span', { style: { marginLeft: '8px', color: '#999' } }, '正在加载标签...'),
        ]);
      }

      if (!labels || labels.length === 0) {
        return h(
          'div',
          { style: { color: '#999', minHeight: '32px', lineHeight: '32px' } },
          '暂无支持的标签',
        );
      }

      return h(
        'div',
        {
          class: 'flex flex-wrap gap-1 items-center',
          style: { minHeight: '32px' },
        },
        labels.map((label) =>
          h(Tag, { color: label.color, key: label.id || label.name }, () => label.name),
        ),
      );
    },
    itemProps: {
      colProps: { span: 20 },
      labelCol: { span: 4 },
    },
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
  // 隐藏的字段，用于存储最终选择的任务ID或名称
  {
    field: 'standardJobName',
    component: 'Input',
    show: false,
    rules: [{ required: true, message: '请选择一个训练任务' }],
  },
  {
    field: 'clearExistingLabels',
    label: '清除已有标签',
    component: 'Switch',
    defaultValue: false,
    componentProps: {
      checkedChildren: '是',
      unCheckedChildren: '否',
    },
    helpMessage: '开启后将清除图片上已有的标注，仅保留自动标注结果',
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
  },
  {
    field: 'requireManualConfirmation',
    label: '需要人工确认标注',
    component: 'Switch',
    defaultValue: true,
    componentProps: {
      checkedChildren: '是',
      unCheckedChildren: '否',
    },
    helpMessage: '开启后自动标注的结果需要人工确认（未标注状态），关闭后直接设置为已标注',
    itemProps: { colProps: { span: 20 }, labelCol: { span: 4 } },
  },
  {
    field: 'guidedGenerationId',
    component: 'Input',
    show: false,
    rules: [{ required: true, message: '请选择一个训练任务' }],
  },
  { field: 'labelData', component: 'Input', show: false, defaultValue: [] },
  { field: 'isLoadingLabels', component: 'Input', show: false, defaultValue: false },
  { field: 'datasetId', component: 'Input', show: false }, // 用于存储当前数据集ID
  { field: 'annotateType', component: 'Input', show: false }, // 用于存储数据集标注类型(102/103)
];
