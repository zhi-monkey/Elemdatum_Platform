import { BasicColumn, FormSchema } from '/@/components/Table';

export const httpServerColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '服务器名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '服务器地址',
    dataIndex: 'serverUrl',
    width: 360,
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 240,
    customRender: ({ record }) => record.description || '-',
  },
  // {
  //   title: 'Token',
  //   dataIndex: 'authToken',
  //   width: 120,
  //   customRender: () => '******',
  // },
];

export const httpServerSearchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '服务器名称',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'serverUrl',
    label: '服务器地址',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const httpServerFormSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', show: false },
  {
    field: 'name',
    label: '服务器名称',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入服务器名称' },
  },
  {
    field: 'serverUrl',
    label: '服务器地址',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '如 http://192.168.1.100:8080' },
  },
  // {
  //   field: 'authToken',
  //   label: '访问 Token',
  //   component: 'InputPassword',
  //   componentProps: { placeholder: '访问令牌，编辑时留空则保持原值' },
  //   helpMessage: '新增时建议填写，编辑时留空则保持原 Token',
  // },
  {
    field: 'description',
    label: '描述信息',
    component: 'InputTextArea',
    componentProps: { rows: 3, placeholder: '请输入描述' },
  },
];
