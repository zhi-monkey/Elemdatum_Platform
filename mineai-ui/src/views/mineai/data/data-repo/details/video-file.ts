import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { formatDateTime } from '/@/utils';

export const columns: BasicColumn[] = [
  {
    title: '视频编号',
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
    width: 150,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 100,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  // {
  //   title: '文件类型',
  //   dataIndex: 'fileType',
  //   ellipsis: true,
  //   width: 150,
  // },
  // {
  //   title: '视频时长',
  //   dataIndex: 'duration',
  //   ellipsis: true,
  //   width: 150,
  // },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
  },
  {
    field: 'createTime',
    component: 'RangePicker',
    componentProps: {
      valueFormat: 'x',
      showTime: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
    },
    label: '创建时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
];
