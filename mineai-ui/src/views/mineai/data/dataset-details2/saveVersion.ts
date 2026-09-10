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
      options: [
        { label: 'CreateML', value: 'COCO' },
        { label: 'YOLO', value: 'YOLO' },
        { label: 'VOC', value: 'VOC' },
      ],
    },
    required: true,
  },
  {
    field: 'versionNote',
    label: '版本描述',
    component: 'Input',
    defaultValue: '',
  },
];
