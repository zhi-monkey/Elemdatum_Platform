import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { formatDateTime } from '/@/utils';

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    sorter: true,
    width: 200,
  },
  {
    title: '名称',
    dataIndex: 'authCode',
    width: 200,
  },
  {
    title: '备注信息',
    dataIndex: 'description',
    width: 200,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
    customRender: ({ record }) => {
      return formatDateTime(record.updateTime);
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'keyword',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
  },
];
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'authCode',
    label: '权限组名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'description',
    label: '备注信息',
    component: 'InputTextArea',
  },
  {
    label: '',
    field: 'menu',
    slot: 'menu',
    component: 'Input',
  },
];
