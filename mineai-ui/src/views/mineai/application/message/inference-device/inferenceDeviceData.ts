import { BasicColumn, FormSchema } from '/@/components/Table';

const fallback = (value?: string) => {
  return value && value.trim() ? value : '--';
};

const ipv4Reg = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/;

const ipv6Reg =
  /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$/;

export const inferenceDeviceColumns: BasicColumn[] = [
  {
    title: '地址名称',
    dataIndex: 'deviceName',
    width: 220,
  },
  {
    title: '地址状态',
    dataIndex: 'deviceStatus',
    width: 140,
    customRender: ({ record }) => fallback(record.deviceStatus),
  },
  {
    title: '加载算法应用',
    dataIndex: 'loadedAlgorithmApp',
    width: 320,
    customRender: ({ record }) => fallback(record.loadedAlgorithmApp),
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 220,
    customRender: ({ record }) => fallback(record.createdAt),
  },
  {
    title: '创建人',
    dataIndex: 'createdBy',
    width: 180,
    customRender: ({ record }) => fallback(record.createdBy),
  },
];

export const inferenceDeviceSearchFormSchema: FormSchema[] = [
  {
    field: 'deviceName',
    label: '地址名称',
    component: 'Input',
    colProps: { span: 8 },
  },
];

export const inferenceDeviceFormSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', show: false },
  {
    field: 'deviceName',
    label: '地址名称',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入地址名称' },
  },
  {
    field: 'inferenceIp',
    label: '接收地址IP',
    component: 'Input',
    dynamicRules: () => {
      return [
        {
          validator: async (_rule, value) => {
            const ip = value ? String(value).trim() : '';
            if (!ip) {
              return Promise.reject('请输入接收地址IP');
            }
            if (!ipv4Reg.test(ip) && !ipv6Reg.test(ip)) {
              return Promise.reject('请输入正确的IP地址');
            }
            return Promise.resolve();
          },
          trigger: 'blur',
          required: true,
        },
      ];
    },
    required: true,
    componentProps: { placeholder: '请输入接收地址IP（IPv4/IPv6）' },
  },
  {
    field: 'inferencePort',
    label: '端口号',
    component: 'InputNumber',
    required: true,
    componentProps: {
      min: 1,
      max: 65535,
      style: { width: '100%' },
      placeholder: '请输入端口号',
    },
  },
  {
    field: 'authCode',
    label: '授权码',
    component: 'InputPassword',
    componentProps: {
      placeholder: '新增必填，编辑留空保持原值',
      autocomplete: 'new-password',
      name: 'inference-auth-code',
    },
    helpMessage: '编辑时留空则保持原授权码',
  },
  {
    field: 'remark',
    label: '备注信息',
    component: 'InputTextArea',
    componentProps: { rows: 3, placeholder: '请输入备注信息' },
  },
];
