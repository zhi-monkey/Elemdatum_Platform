import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { formatDateTime } from '/@/utils';

export const columns: BasicColumn[] = [
  {
    title: '图片编号',
    dataIndex: 'id',
    fixed: 'left',
    width: 75,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    ellipsis: true,
    width: 150,
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    ellipsis: true,
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    ellipsis: true,
    width: 100,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '图片预览',
    dataIndex: 'url',
    width: 75,
    slots: { customRender: 'img' },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
  },
];
