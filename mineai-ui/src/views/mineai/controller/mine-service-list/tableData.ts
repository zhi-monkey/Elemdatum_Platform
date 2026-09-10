import { BasicColumn, FormSchema } from '/@/components/Table';
import {h} from "vue";
import {Popover} from "ant-design-vue";

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 100,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 120,
    customRender: ({ record }) => {
      let currentText = record.name;
      let currentContent = record.name;
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
    title: '备注',
    dataIndex: 'mineServiceName',
    width: 120,
    customRender: ({ record }) => {
      let currentText = record.mineServiceName;
      let currentContent = record.mineServiceName;
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
    title: '端口',
    dataIndex: 'port',
    width: 200,
    customRender: ({ record }) => {
      let currentText = record.port;
      let currentContent = record.port;
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

export const simpleColumns: BasicColumn[] = [
  {
    title: '名称',
    dataIndex: 'name',
    width: 120,
  },
  {
    title: '备注',
    dataIndex: 'mineServiceName',
    width: 120,
  },
  {
    title: '端口',
    dataIndex: 'port',
    width: 200,
  },
];

function setCurrentItem(e): any {
  const fieldNameList = ['name', 'mineServiceName', 'vagueInfo', 'port'];

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
          { label: '名称', value: 'name' },
          { label: '备注', value: 'mineServiceName' },
          { label: '端口', value: 'port' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
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
    field: 'mineServiceName',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'port',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
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
          { label: '名称', value: 'name' },
          { label: '备注', value: 'mineServiceName' },
          { label: '端口', value: 'port' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
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
    field: 'mineServiceName',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'port',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
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
    label: '名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'mineServiceName',
    label: '备注',
    component: 'Input',
  },
  {
    field: 'port',
    component: 'Input',
    label: '访问端口',
    componentProps: {
      placeholder: '请输入访问端口: ',
    },
    rules: [
      {
        required: true,
        pattern: new RegExp(
          /^([1-9](\d{0,3}))$|^([1-5]\d{4})$|^(6[0-4]\d{3})$|^(65[0-4]\d{2})$|^(655[0-2]\d)$|^(6553[0-5])$/,
          'g',
        ),
        message: '请输入规范的端口号1-65535',
      },
    ],
  },
];
