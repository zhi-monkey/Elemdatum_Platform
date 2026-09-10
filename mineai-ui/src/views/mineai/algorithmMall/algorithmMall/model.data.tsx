import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export const fatherColumns: BasicColumn[] = [
  {
    title: '应用名称',
    dataIndex: 'applicationName',
    width: 80,
  },
  {
    title: '算法应用数量',
    dataIndex: 'modelCount',
    width: 80,
  },
  {
    title: '设备型号',
    dataIndex: 'deviceName',
    width: 80,
  },
];

export const sonColumns: BasicColumn[] = [
  {
    title: '算法应用ID',
    dataIndex: 'id',
    width: 40,
    sorter: false,
    customRender: ({ record }) => {
      const formattedId = `A${record.id.toString().padStart(8, '0')}`;
      return formattedId;
    },
  },
  {
    title: '应用任务名称',
    dataIndex: 'applicationTaskName',
    width: 40,
  },
  {
    title: '设备型号',
    dataIndex: 'device.deviceName',
    width: 40,
  },
  {
    title: '固件版本',
    dataIndex: 'device.firmwareVersion',
    width: 40,
  },
  {
    title: '适用场景',
    dataIndex: 'allApplicableSceneName',
    width: 40,
  },
  {
    title: '发布者',
    dataIndex: 'publisherId',
    width: 40,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 40,
    customRender: ({ text }) => {
      // 1. 将数据库时间转换为 Date 对象（视为 UTC）
      const utcDate = new Date(text + 'Z'); // 添加 Z 表示 UTC

      // 2. 转换为北京时间 (UTC+8)
      const beijingTime = new Date(utcDate.getTime() + 8 * 60 * 60 * 1000);

      // 3. 格式化为 YYYY-MM-DD HH:mm:ss
      return beijingTime.toISOString().replace('T', ' ').replace(/\..+/, '').substring(0, 19);
    },
  },
  {
    title: '描述信息',
    dataIndex: 'description',
    width: 40,
  },
];

export const columns: BasicColumn[] = [
  {
    title: '应用任务ID',
    dataIndex: 'id',
    width: 40,
    sorter: true,
  },
  {
    title: '应用任务名称',
    dataIndex: 'applicationTaskName',
    width: 80,
  },
  {
    title: '应用名称',
    dataIndex: 'applicationName.applicationName',
    width: 80,
  },
  {
    title: '适用场景',
    dataIndex: 'allApplicableSceneName',
    width: 80,
  },
  {
    title: '设备固件',
    dataIndex: 'device.deviceAndFirmwareName',
    width: 80,
  },
  {
    title: '绑定算法',
    dataIndex: 'model.modelName',
    width: 80,
  },
  {
    title: '算法应用包名称',
    dataIndex: 'appZipName',
    width: 80,
  },
  {
    title: '发布状态',
    dataIndex: 'isReleased',
    width: 80,
  },
  {
    title: '应用描述',
    dataIndex: 'description',
    width: 80,
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
        options: [{ label: '应用任务名称', value: 'applicationTaskName' }],
        defaultValue: 'applicationTaskName',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'applicationTaskName',
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

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'applicationTaskName',
    label: '任务名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'applicationNameId',
    label: '应用名称',
    required: true,
    component: 'ApiSelect',
    componentProps: () => ({
      dropdownAlign: {
        overflow: {
          adjustY: true, // 关闭下拉框垂直位置自适应
        },
      },
      mode: 'single',
      api: async () => {
        const url = `applicationName/findAll`;
        return await maHttp.get(
          {
            url,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'applicationName',
      // modelApplication的id
      valueField: 'id',
      immediate: true,
    }),
  },
  {
    field: 'description',
    label: '应用描述',
    required: true,
    component: 'Input',
  },
];
