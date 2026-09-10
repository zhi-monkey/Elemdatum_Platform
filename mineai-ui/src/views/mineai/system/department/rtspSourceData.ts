import { BasicColumn, FormSchema } from '/@/components/Table';

export const rtspColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '源名称',
    dataIndex: 'name',
    width: 180,
  },
  {
    title: 'RTSP地址',
    dataIndex: 'rtspUrl',
    width: 420,
  },
  {
    title: '用户名',
    dataIndex: 'username',
    width: 140,
    customRender: ({ record }) => record.username || '-',
  },
  {
    title: '密码',
    dataIndex: 'password',
    width: 120,
    customRender: () => '******',
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 220,
    customRender: ({ record }) => record.description || '-',
  },
];

export const rtspSearchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '源名称',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'rtspUrl',
    label: 'RTSP地址',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'username',
    label: '用户名',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const rtspFormSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', show: false },
  {
    field: 'name',
    label: '源名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'rtspUrl',
    label: 'RTSP地址',
    component: 'Input',
    required: true,
  },
  {
    field: 'authRequired',
    label: '需要认证',
    component: 'Switch',
    defaultValue: false,
    componentProps: {
      checkedChildren: '是',
      unCheckedChildren: '否',
    },
  },
  {
    field: 'username',
    label: '用户名',
    component: 'Input',
    ifShow: ({ values }) => !!values.authRequired,
    dynamicRules: ({ values }) => {
      if (values.authRequired) {
        return [{ required: true, message: '请输入用户名' }];
      }
      return [];
    },
  },
  {
    field: 'password',
    label: '密码',
    component: 'InputPassword',
    ifShow: ({ values }) => !!values.authRequired,
    dynamicRules: ({ values }) => {
      if (values.authRequired && !values.id) {
        return [{ required: true, message: '请输入密码' }];
      }
      return [];
    },
    helpMessage: '新增且开启认证时必填，编辑留空则保持原密码',
  },
  {
    field: 'description',
    label: '描述信息',
    component: 'InputTextArea',
    componentProps: {
      rows: 3,
    },
  },
];
