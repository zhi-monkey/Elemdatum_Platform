import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum'; // 查询所有芯片类型

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'ip',
    label: '设备ip',
    rules: [
      {
        required: true,
        validator: (_rule, value) => {
          const ipRegex =
            /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
          if (value && ipRegex.test(value)) {
            return Promise.resolve();
          } else {
            return Promise.reject('请输入有效的设备IP');
          }
        },
      },
    ],
    component: 'Input',
  },
  {
    field: 'port',
    label: '设备端口',
    rules: [
      {
        required: true,
        validator: (_rule, value) => {
          const portRegex = /^(?!0)[0-9]{1,5}$/; // 排除以0开头，且范围在1-65535
          if (value && portRegex.test(value) && Number(value) <= 65535) {
            return Promise.resolve();
          } else {
            return Promise.reject('请输入有效的设备端口号（1-65535）');
          }
        },
      },
    ],
    component: 'Input',
  },
  {
    field: 'description',
    label: '设备描述信息',
    rules: [
      {
        required: true,
        validator: (_rule, value) => {
          if (value && value.length <= 30) {
            return Promise.resolve();
          } else {
            return Promise.reject('描述信息不能超过30个字符');
          }
        },
      },
    ],
    component: 'Input',
  },
  {
    field: 'chipType',
    label: '适用算力芯片',
    required: true,
    multiple: true,
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: true,
        },
      },
      mode: 'multiple',
      api: async () => {
        const d = await maHttp.get(
          {
            url: '/chips/findAll',
            method: 'GET',
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
        return d.map((chip) => ({
          label: chip.chipType,
          value: chip.id,
        }));
      },
      placeholder: '请选择芯片类型',
      labelField: 'label',
      valueField: 'value',
      immediate: true,
    },
  },
];

export const deviceConverterColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '设备ip',
    dataIndex: 'ip',
    width: 120,
  },
  {
    title: '设备端口',
    dataIndex: 'port',
    width: 120,
  },
  {
    title: '设备描述信息',
    dataIndex: 'description',
  },
  {
    title: '适用算力芯片',
    dataIndex: 'allChipType',
  },
  {
    title: '设备在线状态',
    dataIndex: 'onlineStatus',
  },
];

function setCurrentItem(e): any {
  const fieldNameList = ['ip', 'description', 'port', 'onlineStatus'];

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
          { label: '设备ip', value: 'ip' },
          { label: '设备描述信息', value: 'description' },
          { label: '端口号', value: 'port' },
          { label: '设备在线状态', value: 'onlineStatus' },
        ],
        defaultValue: 'ip',
      };
    },
    colProps: { span: 8 },
  },

  {
    field: 'ip',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
    componentProps: ({ formActionType }) => ({
      placeholder: 'IPv4地址（例：192.168.1.1）',
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const input = e.target.value;

        // 分步骤处理输入
        const filteredValue = input
          .replace(/[^\d.]/g, '') // 1. 去除非数字和点号
          .replace(/\.\.+/g, '.') // 2. 合并连续点号
          .replace(/^\.+/, '') // 3. 去除开头点号
          .replace(/(\.\d{3})\d+/g, '$1') // 4. 限制每段最多3位数字
          .split('.')
          .slice(0, 4) // 5. 限制最多4段
          .map((segment) => {
            // 6. 处理每段数值
            if (segment === '') return '';
            const num = Math.min(parseInt(segment), 255);
            return num.toString();
          })
          .join('.');

        // 阻止无效输入
        if (input !== filteredValue) {
          e.target.value = filteredValue;
          await setFieldsValue({ ip: filteredValue });
        }

        // 实时验证提示
        const isValid = /^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(?!$)|$)){4}$/.test(
          filteredValue + '.',
        );
        e.target.setCustomValidity(isValid ? '' : '格式示例：192.168.1.1');
      },
    }),
  },
  {
    field: 'description',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'port',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },

    componentProps: ({ formActionType }) => ({
      placeholder: '1-65535之间的数字',
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        let filteredValue = e.target.value.replace(/\D/g, '');
        if (filteredValue.length > 1) {
          filteredValue = filteredValue.replace(/^0+/, '');
        }
        filteredValue = filteredValue.slice(0, 5);
        e.target.value = filteredValue;
        await setFieldsValue({ port: filteredValue });
      },
    }),
  },
  {
    field: 'onlineStatus',
    label: '设备状态', // 可以给出一个标签
    component: 'Select',
    componentProps: {
      options: [
        { label: '在线', value: true },
        { label: '离线', value: false },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
];
