import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';

import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

function setCurrentItem(e): any {
  const fieldNameList = [
    'dataType',
    'scene',
    'monitorName',
    'isRecord',
    'protocolMetaData',
    'vagueInfo',
    'controllerIp',
    'instruction',
    'monitorType',
    'modelName',
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

export const columns: BasicColumn[] = [
  {
    title: '监控设备ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 100,
    sorter: true,
  },
  {
    title: '设备名称',
    dataIndex: 'monitorName',
    width: 200,
  },
  {
    title: '序列号',
    dataIndex: 'name',
    width: 120,
    ifShow: false,
  },
  {
    title: '设备场景',
    dataIndex: 'scene.name',
    width: 200,
  },
  {
    title: 'URL',
    dataIndex: 'protocolMetaData',
    width: 300,
    ellipsis: true,
    customRender: ({ record }) => {
      let currentText;
      let currentContent;
      if (record.protocolMetaData) {
        currentText = record.protocolMetaData;
        currentContent = record.protocolMetaData;
      }
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
  // {
  //   title: '控制器IP',
  //   dataIndex: 'controllerIp',
  //   width: 200,
  // },
  // {
  //   title: '监控数据类型 ',
  //   dataIndex: 'dataType',
  //   width: 120,
  //   customRender: ({ record }) => {
  //     const dataType = record.dataType;
  //     let color = 'yellow';
  //     let text = '图像';
  //     if (dataType === '视频') {
  //       color = 'blue';
  //       text = '视频';
  //     } else if (dataType === '音频') {
  //       color = 'purple';
  //       text = '音频';
  //     } else if (dataType === '点云') {
  //       color = 'green';
  //       text = '点云';
  //     }
  //     return h(Tag, { color: color }, () => text);
  //   },
  // },
  {
    title: '设备类型',
    dataIndex: 'monitorType',
    width: 100,
    customRender: ({ record }) => {
      const monitorType = record.monitorType;
      let color;
      let text;
      if (monitorType === 1) {
        color = 'blue';
        text = '可见光';
      } else if (monitorType === 2) {
        color = 'red';
        text = '红外';
      } else if (monitorType === 3) {
        color = 'purple';
        text = '三维';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  // {
  //   title: '描述',
  //   dataIndex: 'instruction',
  //   width: 200,
  // },
  {
    title: '厂商',
    dataIndex: 'manufacturer',
    width: 100,
    ifShow: false,
  },
  {
    title: '状态',
    dataIndex: 'statusUsing',
    width: 120,
    ifShow: true,
    customRender: ({ record }) => {
      const status = record.statusUsing;
      const enable = ~~status === 1;
      const color = enable ? 'green' : 'red';
      const text = enable ? '在线' : '离线';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '录制状态',
    dataIndex: 'isRecord',
    width: 120,
    customRender: ({ record }) => {
      const isRecord = record.isRecord;
      const enable = ~~isRecord === 1;
      const color = enable ? 'green' : 'yellow';
      const text = enable ? '正在录制' : '停止录制';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '推流',
    dataIndex: 'isPushStream',
    width: 120,
    ifShow: false,
    customRender: ({ record }) => {
      const status = record.isPushStream;
      const enable = ~~status === 1;
      const color = enable ? 'green' : 'red';
      const text = enable ? '是' : '否';
      return h(Tag, { color: color }, () => text);
    },
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '设备名称',
    dataIndex: 'monitorName',
    width: 200,
  },
  {
    title: '序列号',
    dataIndex: 'name',
    width: 120,
    ifShow: false,
  },
  {
    title: '设备场景',
    dataIndex: 'scene.name',
    width: 200,
  },
  {
    title: 'URL',
    dataIndex: 'protocolMetaData',
    width: 300,
  },
  {
    title: '控制器IP',
    dataIndex: 'controllerIp',
    width: 200,
  },
  {
    title: '监控数据类型 ',
    dataIndex: 'dataType',
    width: 120,
    customRender: ({ record }) => {
      const dataType = record.dataType;
      let color = 'yellow';
      let text = '图像';
      if (dataType === '视频') {
        color = 'blue';
        text = '视频';
      } else if (dataType === '音频') {
        color = 'purple';
        text = '音频';
      } else if (dataType === '点云') {
        color = 'green';
        text = '点云';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '描述',
    dataIndex: 'instruction',
    width: 200,
  },
  {
    title: '厂商',
    dataIndex: 'manufacturer',
    width: 100,
    ifShow: false,
  },
  {
    title: '状态',
    dataIndex: 'statusUsing',
    width: 120,
    ifShow: true,
    customRender: ({ record }) => {
      const status = record.statusUsing;
      const enable = ~~status === 1;
      const color = enable ? 'green' : 'red';
      const text = enable ? '在线' : '离线';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '录制状态',
    dataIndex: 'isRecord',
    width: 120,
    customRender: ({ record }) => {
      const isRecord = record.isRecord;
      const enable = ~~isRecord === 1;
      const color = enable ? 'green' : 'yellow';
      const text = enable ? '正在录制' : '停止录制';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '推流',
    dataIndex: 'isPushStream',
    width: 120,
    ifShow: false,
    customRender: ({ record }) => {
      const status = record.isPushStream;
      const enable = ~~status === 1;
      const color = enable ? 'green' : 'red';
      const text = enable ? '是' : '否';
      return h(Tag, { color: color }, () => text);
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
          { label: '设备名称', value: 'monitorName' },
          { label: '设备场景', value: 'scene' },
          { label: 'URL', value: 'protocolMetaData' },
          { label: '控制器IP', value: 'controllerIp' },
          { label: '监控数据类型', value: 'dataType' },
          { label: '设备类型', value: 'monitorType' },
          { label: '描述', value: 'instruction' },
          { label: '录制状态', value: 'isRecord' },
          { label: '模糊查询', value: 'vagueInfo' },
          { label: '算法名称', value: 'modelName' },
        ],
        defaultValue: 'monitorName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'protocolMetaData',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'monitorName',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'dataType',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '图像', value: '图像' },
        { label: '视频', value: '视频' },
        { label: '音频', value: '音频' },
        { label: '点云', value: '点云' },
      ],
    },
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
    field: 'statusUsing',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '停用', value: 2 },
        { label: '启用', value: 1 },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'isRecord',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '正在录制', value: 1 },
        { label: '停止录制', value: -1 },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'scene',
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
              url: 'scene/getScene',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'name',
      valueField: 'id',
      immediate: true,
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'controllerIp',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
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
  {
    field: 'modelName',
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
          { label: '设备名称', value: 'monitorName' },
          { label: '设备场景', value: 'scene' },
          { label: 'URL', value: 'protocolMetaData' },
          { label: '控制器IP', value: 'controllerIp' },
          { label: '监控数据类型', value: 'dataType' },
          { label: '描述', value: 'instruction' },
          { label: '录制状态', value: 'isRecord' },
          { label: '模糊查询', value: 'vagueInfo' },
          { label: '算法名称', value: 'modelName' },
        ],
        defaultValue: 'monitorName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'protocolMetaData',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'monitorName',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'dataType',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '图像', value: '图像' },
        { label: '视频', value: '视频' },
        { label: '音频', value: '音频' },
        { label: '点云', value: '点云' },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'statusUsing',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '停用', value: 2 },
        { label: '启用', value: 1 },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'isRecord',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '正在录制', value: 1 },
        { label: '停止录制', value: -1 },
      ],
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'scene',
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
              url: 'scene/getScene',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'name',
      valueField: 'id',
      immediate: false,
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'controllerIp',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
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
  {
    field: 'modelName',
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
    field: 'sceneName',
    label: '设备场景',
    required: true,
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp.get(
          {
            url: 'scene/getScene',
            params: {},
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
        ),
      labelField: 'name',
      valueField: 'id',
      immediate: false,
    },
  },
  {
    field: 'protocolMetaData',
    label: 'URL',
    component: 'Input',
    componentProps: {
      placeholder: '请输入正确的rtsp视频流地址!!! 如：rtsp://',
    },
    rules: [
      {
        required: true,
        pattern: new RegExp(/^rtsp:\/\/.*/, 'g'),
        message: '请输入正确的rtsp视频流地址',
      },
    ],
    required: true,
  },
  {
    field: 'monitorName',
    label: '设备名称',
    component: 'Input',
    required: true,
  },
  // {
  //   field: 'manufacturer',
  //   label: '厂商',
  //   component: 'Input',
  // },
  {
    field: 'dataType',
    label: '监控数据类型',
    required: true,
    component: 'RadioButtonGroup',
    defaultValue: '视频',
    componentProps: {
      options: [
        {
          label: '音频',
          value: '音频',
          disabled: true,
        },
        { label: '视频', value: '视频' },
        { label: '图像', value: '图像', disabled: true },
        { label: '点云', value: '点云', disabled: true },
      ],
    },
  },
  {
    field: 'monitorType',
    label: '设备类型',
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
    field: 'instruction',
    label: '描述',
    component: 'Input',
  },
  {
    field: 'useCustomInfo',
    label: '平台自定义信息',
    component: 'RadioButtonGroup',
    required: true,
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '停用', value: 0 },
        { label: '启用', value: 1, disabled: true },
      ],
    },
  },
  // {
  //   field: 'statusUsing',
  //   label: '状态',
  //   component: 'RadioButtonGroup',
  //   required: false,
  //   componentProps: {
  //     options: [
  //       { label: '启用', value: 1 },
  //       { label: '停用', value: 2 },
  //     ],
  //   },
  // },
  // {
  //   field: 'isPushStream',
  //   label: '推流',
  //   component: 'RadioButtonGroup',
  //   required: true,
  //   componentProps: {
  //     options: [
  //       { label: '是', value: 1 },
  //       { label: '否', value: 0 },
  //     ],
  //   },
  // },
];
