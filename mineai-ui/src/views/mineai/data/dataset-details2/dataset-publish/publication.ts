import { FormSchema } from '/@/components/Table';

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '编号',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'name',
    label: '名称',
    required: true,
    dynamicDisabled: true,
    component: 'Input',
    componentProps: { readOnly: true, allowClear: false },
    show: false,
  },
  {
    field: 'currentVersionName',
    label: '当前版本',
    dynamicDisabled: true,
    component: 'Input',
    componentProps: { readOnly: true, allowClear: false },
    show: false,
  },
  {
    field: 'nextVersionName',
    label: '下一版本',
    dynamicDisabled: true,
    component: 'Input',
    componentProps: { readOnly: true, allowClear: false },
  },
  {
    field: 'format',
    label: '导出格式',
    component: 'Select',
    componentProps: {
      options: [{ label: 'YOLO', value: 'YOLO' }],
    },
    required: true,
  },
  {
    field: 'versionNote',
    label: '版本描述',
    component: 'Input',
    defaultValue: '',
  },
  {
    field: 'needArchive',
    label: '是否保留映射前状态',
    component: 'Checkbox',
    defaultValue: true,
    itemProps: { colProps: { span: 16 }, labelCol: { span: 8 } },
    helpMessage:
      '保留映射前状态会创建一个未进行标签映射的版本作为备份。如果不存档，则只创建一个带标签映射的公开版本。',
  },
];
