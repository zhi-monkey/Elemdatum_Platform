import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { fromByteArray } from 'base64-js';
import { h } from 'vue';
import {Popover, Tag} from 'ant-design-vue';

const encoder = new TextEncoder();

export function strToColor(str: string): {
  str: string;
  rgb: number[];
} {
  const encoded = fromByteArray(encoder.encode(str));

  const rgb = [0, 0, 0];
  Array.from(encoded).forEach((char, idx) => {
    rgb[idx % 3] += char.charCodeAt(0) * 9;
  });
  [0, 1, 2].forEach((i) => {
    rgb[i] %= 256;
  });
  const colorStr = rgb.map((v) => v.toString(16)).join('');
  return {
    str: `#${colorStr}`,
    rgb,
  };
}

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    sorter: true,
    fixed: 'left',
    width: 80,
  },
  {
    title: '算力控制器IP',
    dataIndex: 'ip',
    width: 120,
    customRender: ({ record }) => {
      let currentText = record.ip;
      let currentContent = record.ip;
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
    sorter: true,
    width: 80,
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
  {
    title: '算力控制器名称',
    dataIndex: 'name',
    sorter: true,
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
    title: '架构',
    dataIndex: 'architecture',
    width: 60,
    align: 'center',
    customRender: ({ record }) => {
      const architecture = record.architecture;
      let color = 'yellow';
      let text = 'amd64';
      if (architecture === 'arm64') {
        color = 'blue';
        text = 'arm64';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: 'CPU型号',
    dataIndex: 'cpuInfo',
    sorter: true,
    width: 200,
    customRender: ({ record }) => {
      let currentText = record.cpuInfo;
      let currentContent = record.cpuInfo;
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
    title: '内存大小',
    dataIndex: 'ramSize',
    sorter: true,
    width: 120,
    slots: { customRender: 'RamSizeprocess' },
  },
  {
    title: '硬盘大小',
    dataIndex: 'diskSize',
    sorter: true,
    width: 120,
    slots: { customRender: 'diskSizeprocess' },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 50,
    customRender: ({ record }) => {
      const status = record.status;
      let color = 'green';
      let text = '运行';
      if (status === -1) {
        color = 'red';
        text = '故障';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '算力控制器IP',
    dataIndex: 'ip',
    width: 120,
  },
  {
    title: '端口',
    dataIndex: 'port',
    sorter: true,
    width: 80,
  },
  {
    title: '算力控制器名称',
    dataIndex: 'name',
    sorter: true,
    width: 120,
  },
  {
    title: 'CPU型号',
    dataIndex: 'cpuInfo',
    sorter: true,
    width: 200,
  },
  {
    title: '内存大小',
    dataIndex: 'ramSize',
    sorter: true,
    width: 120,
    slots: { customRender: 'RamSizeprocess' },
  },
  {
    title: '硬盘大小',
    dataIndex: 'diskSize',
    sorter: true,
    width: 120,
    slots: { customRender: 'diskSizeprocess' },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 50,
    customRender: ({ record }) => {
      const status = record.status;
      let color = 'green';
      let text = '运行';
      if (status === -1) {
        color = 'red';
        text = '故障';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
];

function setCurrentItem(e): any {
  const fieldNameList = ['ip', 'name', 'port', 'cpuInfo', 'vagueInfo', 'status'];

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
          { label: 'IP', value: 'ip' },
          { label: '端口', value: 'port' },
          { label: '算力控制器名称', value: 'name' },
          { label: 'CPU型号', value: 'cpuInfo' },
          { label: '状态', value: 'status' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'ip',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'port',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'name',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'cpuInfo',
    label: '',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'controller/findAllCpuInfo',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then((v) => {
            const list: any[] = [];
            v.forEach((item) => {
              list.push({ name: item });
            });
            return list;
          }),
      labelField: 'name',
      valueField: 'name',
      immediate: false,
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'status',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '故障', value: '-1' },
        { label: '运行', value: '1' },
      ],
    },
    ifShow: false,
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
          { label: 'IP', value: 'ip' },
          { label: '端口', value: 'port' },
          { label: '算力控制器名称', value: 'name' },
          { label: 'CPU型号', value: 'cpuInfo' },
          { label: '状态', value: 'status' },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'ip',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'port',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'name',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'cpuInfo',
    label: '',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'controller/findAllCpuInfo',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then((v) => {
            const list: any[] = [];
            v.forEach((item) => {
              list.push({ name: item });
            });
            return list;
          }),
      labelField: 'name',
      valueField: 'name',
      immediate: false,
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'status',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '故障', value: '-1' },
        { label: '运行', value: '1' },
      ],
    },
    ifShow: false,
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
    field: 'ip',
    component: 'Input',
    label: '新增控制器IP',
    componentProps: {
      placeholder: '255.255.255.255',
    },
    rules: [
      {
        required: true,
        pattern: new RegExp(
          /^((25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))$/,
          'g',
        ),
        message: '请输入规范的ip地址',
      },
    ],
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
  {
    field: 'architecture',
    label: '控制器架构',
    component: 'RadioButtonGroup',
    defaultValue: 'amd64',
    componentProps: {
      options: [
        { label: 'amd64', value: 'amd64' },
        { label: 'arm64', value: 'arm64' },
      ],
    },
  },
  {
    field: 'name',
    label: '控制器名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入控制器名称: ',
    },
  },
];
export const formSchema1: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'ip',
    component: 'Input',
    label: '控制器IP',
    componentProps: {
      placeholder: '255.255.255.255',
    },
    rules: [
      {
        required: true,
        pattern: new RegExp(
          /^((25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))$/,
          'g',
        ),
        message: '请输入规范的ip地址',
      },
    ],
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
  {
    field: 'architecture',
    label: '控制器架构',
    component: 'RadioButtonGroup',
    required: true,
    defaultValue: 'amd64',
    componentProps: {
      options: [
        { label: 'amd64', value: 'amd64' },
        { label: 'arm64', value: 'arm64' },
      ],
    },
  },
  {
    field: 'name',
    label: '控制器名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入控制器名称: ',
    },
  },
];
