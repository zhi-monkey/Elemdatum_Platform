import { BasicColumn, FormSchema } from '/@/components/Table';
import { formatToDateTime } from '/@/utils/dateUtil';

const UPLOAD_APP_ZIP_PATH = '/api/sys-ai-dev/uploadAppZip';

function buildUploadUrl(record: Recordable) {
  if (!record.platformIp || !record.platformPort) return '-';
  return `http://${record.platformIp}:${record.platformPort}${UPLOAD_APP_ZIP_PATH}`;
}

export const gpuUrlTargetColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 180,
  },
  {
    title: '视频平台接口地址',
    dataIndex: 'platformIp',
    width: 360,
    customRender: ({ record }) => buildUploadUrl(record),
  },
  {
    title: '分析服务器IP',
    dataIndex: 'ip',
    width: 180,
    customRender: ({ record }) => record.ip || '-',
  },
  {
    title: '分析服务器端口',
    dataIndex: 'port',
    width: 180,
    customRender: ({ record }) => record.port || '-',
  },
  {
    title: '授权码',
    dataIndex: 'authCode',
    width: 100,
    customRender: ({ record }) => (record.authCode ? '******' : '-'),
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 220,
    customRender: ({ record }) => record.description || '-',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
    customRender: ({ text }) => (text ? formatToDateTime(text) : '-'),
  },
];

export const gpuUrlTargetSearchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'ip',
    label: '分析服务器IP',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export function createGpuUrlTargetFormSchema(): FormSchema[] {
  return [
    { field: 'id', label: 'id', component: 'Input', show: false },
    {
      field: 'name',
      label: '名称',
      component: 'Input',
      required: true,
      componentProps: { placeholder: '请输入名称' },
    },
    {
      field: 'uploadUrl',
      label: '视频平台接口地址',
      component: 'Input',
      required: true,
      componentProps: {
        placeholder: '如 http://10.50.31.239:8080/api/sys-ai-dev/uploadAppZip',
      },
    },
    {
      field: 'gpuUrl',
      component: 'Input',
      show: false,
    },
    {
      field: 'platformIp',
      component: 'Input',
      show: false,
    },
    {
      field: 'platformPort',
      component: 'InputNumber',
      show: false,
    },
    {
      field: 'ip',
      label: '分析服务器IP',
      component: 'Input',
      required: true,
      componentProps: { placeholder: '如 10.50.31.223' },
    },
    {
      field: 'port',
      label: '分析服务器端口',
      component: 'InputNumber',
      required: true,
      componentProps: { min: 1, max: 65535, style: 'width: 100%', autocomplete: 'off' },
    },
    {
      field: 'authCode',
      label: '授权码',
      component: 'InputPassword',
      componentProps: { placeholder: '请输入授权码', autocomplete: 'new-password' },
    },
    {
      field: 'description',
      label: '描述信息',
      component: 'InputTextArea',
      componentProps: { rows: 3, placeholder: '请输入描述' },
    },
  ];
}
