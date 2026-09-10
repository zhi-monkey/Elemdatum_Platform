import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
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
    title: '状态',
    dataIndex: 'status',
    width: 75,
    customRender: ({ record }) => {
      //视频 101未抽帧 102抽帧中 103已抽帧
      const status = record.status;
      let color = 'gray';
      let text = '未抽帧';
      if (status === 103) {
        color = 'green';
        text = '已抽帧';
      } else if (status === 102) {
        color = 'yellow';
        text = '抽帧中';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 100,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '文件类型',
    dataIndex: 'fileType',
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
    title: '视频时长',
    dataIndex: 'duration',
    ellipsis: true,
    width: 150,
  },
  // {
  //   title: '更新时间',
  //   dataIndex: 'updateTime',
  //   width: 100,
  //   customRender: ({ record }) => {
  //     return formatDateTime(record.updateTime);
  //   },
  // },
  // {
  //   title: '抽帧帧数',
  //   dataIndex: 'frameInterval',
  //   width: 50,
  // },
  // {
  //   title: '视频预览',
  //   dataIndex: 'url',
  //   width: 75,
  //   slots: { customRender: 'img' },
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
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '未抽帧', value: 101 },
        { label: '已抽帧', value: 103 },
      ],
    },
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
