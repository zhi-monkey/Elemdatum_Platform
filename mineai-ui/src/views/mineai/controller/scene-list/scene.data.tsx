import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: '场景ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '场景名称',
    dataIndex: 'name',
    width: 100,
  },

  {
    title: '描述',
    dataIndex: 'instruction',
    width: 200,
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '场景名称',
    dataIndex: 'name',
    width: 100,
  },

  {
    title: '描述',
    dataIndex: 'instruction',
    width: 200,
    customRender: ({ record }) => {
      const currentText = record.instruction;
      const currentContent = record.instruction;
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
  const fieldNameList = ['name', 'vagueInfo', 'instruction'];

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
          { label: '场景名称', value: 'name' },
          { label: '描述', value: 'instruction' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'name',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'name',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },

  {
    field: 'instruction',
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
          { label: '场景名称', value: 'name' },
          { label: '描述', value: 'instruction' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'name',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'name',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },

  {
    field: 'instruction',
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
  {
    field: 'id',
    label: 'id',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'name',
    label: '场景名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'instruction',
    label: '描述',
    component: 'Input',
  },
];
