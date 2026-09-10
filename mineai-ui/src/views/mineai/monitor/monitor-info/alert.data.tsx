import { BasicColumn, FormSchema } from '/@/components/Table';
export const columns: BasicColumn[] = [
  {
    title: '报警内容',
    dataIndex: 'description',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'image',
    width: 30,
    slots: { customRender: 'image' },
  },
  {
    title: '视频',
    dataIndex: 'video',
    width: 30,
    slots: { customRender: 'video' },
  },
  {
    title: '音频',
    dataIndex: 'audio',
    width: 30,
    slots: { customRender: 'audio' },
  },
  {
    title: '上报时间',
    dataIndex: 'createTime',
    width: 50,
    customRender: ({ text }) => {
      return text.split(' ').pop();
    },
  },
];

export const formSchema: FormSchema[] = [];
