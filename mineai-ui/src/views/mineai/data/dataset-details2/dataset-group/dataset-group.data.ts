import { FormSchema } from '/@/components/Table';

export const datasetGroupFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '数据集组名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入数据集组名称',
      maxlength: 50,
      showCount: true,
    },
    required: true,
    rules: [
      {
        required: true,
        message: '请输入数据集组名称！',
      },
      {
        min: 1,
        max: 40,
        message: '数据集组名称长度在 1 到40 个字符之间！',
      },
      {
        pattern: /^[a-zA-Z0-9\u4e00-\u9fa5_-]+$/,
        message: '数据集组名称只能包含中英文、数字、下划线和短横线！',
      },
    ],
  },
  {
    field: 'description',
    label: '描述',
    component: 'InputTextArea',
    componentProps: {
      placeholder: '请输入数据集组描述（可选）',
      rows: 4,
      maxlength: 200,
      showCount: true,
    },
    rules: [
      {
        max: 200,
        message: '描述长度不能超过 200 个字符！',
      },
    ],
  },
  // {
  //   field: 'isPublic',
  //   label: '是否公开',
  //   component: 'RadioButtonGroup',
  //   defaultValue: false,
  //   componentProps: {
  //     options: [
  //       { label: '私有', value: false },
  //       { label: '公开', value: true },
  //     ],
  //   },
  //   helpMessage: '公开的数据集组其他用户也可以查看',
  // },
];

export const datasetGroupSearchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '组名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入数据集组名称',
    },
    colProps: { span: 8 },
  },
  {
    field: 'isPublic',
    label: '类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择类型',
      options: [
        { label: '全部', value: '' },
        { label: '私有', value: false },
        { label: '公开', value: true },
      ],
    },
    colProps: { span: 8 },
  },
  {
    field: 'createTime',
    label: '创建时间',
    component: 'RangePicker',
    componentProps: {
      format: 'YYYY-MM-DD',
      placeholder: ['开始日期', '结束日期'],
    },
    colProps: { span: 8 },
  },
];
