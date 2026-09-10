<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="'新建数据处理任务'"
    :width="800"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { FormSchema } from '/@/components/Form';

  const emit = defineEmits(['success', 'register']);

  // 模拟数据集列表
  const mockDatasets = [
    { label: '猫狗分类数据集', value: 1 },
    { label: '监控视频数据集', value: 2 },
    { label: '文本数据集A', value: 3 },
    { label: '车辆检测数据集', value: 4 },
    { label: '人脸识别数据集', value: 5 },
  ];

  // 模拟任务编排模板
  const mockTemplates = [
    { label: '图像预处理流程', value: 1 },
    { label: '视频处理流程', value: 2 },
    { label: '数据清洗标准流程', value: 3 },
    { label: '数据增强流程', value: 4 },
  ];

  // 模拟平台内置算子
  const mockBuiltinOperators = [
    { label: '数据增强', value: 1 },
    { label: '自动标注', value: 2 },
    { label: '图像裁剪', value: 3 },
    { label: '格式转换', value: 4 },
    { label: '数据去重', value: 5 },
  ];

  // 模拟用户自定义算子
  const mockCustomOperators = [
    { label: '数据清洗脚本v2', value: 1 },
    { label: '自定义滤镜', value: 2 },
    { label: '特征提取脚本', value: 3 },
  ];

  const taskType = ref('template');

  const formSchema: FormSchema[] = [
    {
      field: 'taskName',
      label: '任务名称',
      component: 'Input',
      required: true,
      componentProps: {
        placeholder: '请输入任务名称',
      },
    },
    {
      field: 'taskType',
      label: '任务类型',
      component: 'RadioGroup',
      required: true,
      defaultValue: 'template',
      componentProps: {
        options: [
          { label: '任务编排模板', value: 'template' },
          { label: '平台内置算子', value: 'builtin' },
          { label: '用户自定义算子', value: 'custom' },
        ],
        onChange: (e) => {
          taskType.value = e.target.value;
          updateSchema([
            {
              field: 'templateId',
              ifShow: e.target.value === 'template',
            },
            {
              field: 'builtinOperatorId',
              ifShow: e.target.value === 'builtin',
            },
            {
              field: 'customOperatorId',
              ifShow: e.target.value === 'custom',
            },
          ]);
        },
      },
    },
    {
      field: 'templateId',
      label: '选择模板',
      component: 'Select',
      required: true,
      ifShow: ({ values }) => values.taskType === 'template',
      componentProps: {
        placeholder: '请选择任务编排模板',
        options: mockTemplates,
      },
    },
    {
      field: 'builtinOperatorId',
      label: '选择算子',
      component: 'Select',
      required: true,
      ifShow: ({ values }) => values.taskType === 'builtin',
      componentProps: {
        placeholder: '请选择平台内置算子',
        options: mockBuiltinOperators,
      },
    },
    {
      field: 'customOperatorId',
      label: '选择算子',
      component: 'Select',
      required: true,
      ifShow: ({ values }) => values.taskType === 'custom',
      componentProps: {
        placeholder: '请选择用户自定义算子',
        options: mockCustomOperators,
      },
    },
    {
      field: 'datasetId',
      label: '绑定数据集',
      component: 'Select',
      required: true,
      componentProps: {
        placeholder: '请选择要处理的数据集',
        options: mockDatasets,
      },
    },
    {
      field: 'params',
      label: '配置参数',
      component: 'InputTextArea',
      componentProps: {
        placeholder: '请输入配置参数（JSON格式）',
        rows: 6,
      },
      helpMessage: '示例: {"batch_size": 32, "augmentation": true}',
    },
    {
      field: 'description',
      label: '任务描述',
      component: 'InputTextArea',
      componentProps: {
        placeholder: '请输入任务描述',
        rows: 3,
      },
    },
  ];

  const [registerForm, { validate, resetFields, updateSchema }] = useForm({
    labelWidth: 120,
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    resetFields();
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });

      // 模拟提交
      console.log('提交数据:', values);

      setTimeout(() => {
        setModalProps({ confirmLoading: false });
        closeModal();
        emit('success');
      }, 500);
    } catch (error) {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
