import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: '应用ID',
    dataIndex: 'id',
    width: 40,
    sorter: true,
  },
  {
    title: '应用名称',
    dataIndex: 'name',
    width: 80,
  },
  {
    title: '应用场景',
    dataIndex: 'scene',
    width: 80,
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

function setCurrentItem(e): any {
  const fieldNameList = [
    'modelName',
    'modelEnglishName',
    'vagueInfo',
    'monitorType',
    'description',
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
          { label: '应用名称', value: 'name' },
          { label: '应用场景', value: 'scene' },
        ],
        defaultValue: 'name',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'modelName',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'modelEnglishName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'monitorType',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '可见光', value: 1 },
        { label: '红外', value: 2 },
        { label: '三维', value: 3 },
      ],
    },
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
    field: 'name',
    label: '应用名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'scene',
    label: '应用场景',
    required: true,
    component: 'Input',
  },
];
