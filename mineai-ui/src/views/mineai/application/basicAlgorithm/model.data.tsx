import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: '算法ID',
    dataIndex: 'id',
    width: 70,
    sorter: true,
  },
  {
    title: '算法名称',
    dataIndex: 'modelName',
    width: 70,
  },
  {
    title: '算法描述',
    dataIndex: 'description',
    width: 100,
    customRender: ({ record }) => {
      const currentText = record.description;
      const currentContent = record.description;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '算力芯片类型',
    dataIndex: 'chipType',
    width: 80,
    customRender: ({ record }) => {
      return record.chip ? record.chip.chipType : '';
    },
  },
  {
    title: '发布者',
    dataIndex: 'createUser',
    width: 50,
    customRender: ({ record }) => {
      return record.createUser ? record.createUser.username : '无';
    },
  },
  {
    title: '发布时间',
    dataIndex: 'releaseTime',
    width: 70,
    customRender: ({ record }) => {
      return record.source === 1 ? '无' : record.releaseTime;
    },
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '算法名称',
    dataIndex: 'modelName',
    width: 100,
  },
  {
    title: '算法描述',
    dataIndex: 'description',
    width: 200,
    customRender: ({ record }) => {
      const currentText = record.description;
      const currentContent = record.description;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'select',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          // { label: '算法英文名称', value: 'modelEnglishName' },
          { label: '算法名称', value: 'modelName' },
          // { label: '算法类型', value: 'monitorType' },
          { label: '算法描述', value: 'description' },
          { label: '算力芯片类型', value: 'chipType' },
          { label: '模糊查询(不包含ID)', value: 'vagueInfo' },
        ],
        defaultValue: 'modelName',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'modelName',
    label: '',
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ modelName: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
  // {
  //   field: 'modelEnglishName',
  //   label: '',
  //   component: 'Input',
  //   ifShow: false,
  //   colProps: { span: 8 },
  // },
  // {
  //   field: 'monitorType',
  //   label: '',
  //   component: 'Select',
  //   componentProps: {
  //     options: [
  //       { label: '可见光', value: 1 },
  //       { label: '红外', value: 2 },
  //       { label: '三维', value: 3 },
  //     ],
  //   },
  //   ifShow: false,
  //   colProps: { span: 8 },
  // },
  {
    field: 'description',
    label: '',
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ description: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'chipType',
    label: '',
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ chipType: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'vagueInfo',
    label: '',
    ifShow: false,
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          //同步更新表单字段的值
          await setFieldsValue({ vagueInfo: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
];
export const simpleSearchFormSchema: FormSchema[] = [
  {
    field: '',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          { label: '算法名称', value: 'modelName' },
          {
            label: '算法描述',
            value: 'description',
          },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'modelName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'modelName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'description',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'vagueInfo',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'monitorType',
    label: '监控类型',
    required: true,
    component: 'RadioButtonGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '可见光', value: 1 },
        { label: '红外', value: 2, disabled: true },
        { label: '三维', value: 3, disabled: true },
      ],
    },
  },
  {
    field: 'description',
    label: '算法描述',
    required: true,
    component: 'Input',
  },
];

function setCurrentItem(e): any {
  const fieldNameList = [
    'modelName',
    'modelEnglishName',
    'vagueInfo',
    'monitorType',
    'description',
    'chipType',
  ];
  const result: any[] = [];
  for (const item in fieldNameList) {
    if (e === fieldNameList[item]) {
      result.push({ field: fieldNameList[item], ifShow: true });
    } else {
      result.push({ field: fieldNameList[item], ifShow: false });
    }
  }
  return result;
}
