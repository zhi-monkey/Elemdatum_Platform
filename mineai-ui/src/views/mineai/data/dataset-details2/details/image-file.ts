import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

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
    title: '状态',
    dataIndex: 'status',
    width: 75,
    customRender: ({ record }) => {
      //图片 101未标注 104已标注
      const status = record.status;
      let color = 'gray';
      let text = '未标注';
      if (status === 104) {
        color = 'green';
        text = '已标注';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '文件类型',
    dataIndex: 'fileType',
    ellipsis: true,
    width: 50,
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    ellipsis: true,
    width: 100,
  },
  // {
  //   title: '创建时间',
  //   dataIndex: 'createTime',
  //   width: 100,
  //   customRender: ({ record }) => {
  //     return formatDateTime(record.createTime);
  //   },
  // },
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
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '未标注', value: 101 },
        { label: '已标注', value: 104 },
      ],
    },
    colProps: { xl: 12, xxl: 4 },
  },
  // {
  //   field: 'createTime',
  //   component: 'RangePicker',
  //   componentProps: {
  //     valueFormat: 'x',
  //     showTime: {
  //       format: 'YYYY-MM-DD HH:mm:ss',
  //       showTime: true,
  //     },
  //   },
  //   label: '创建时间',
  //   colProps: {
  //     xl: 12,
  //     xxl: 8,
  //   },
  // },
];
