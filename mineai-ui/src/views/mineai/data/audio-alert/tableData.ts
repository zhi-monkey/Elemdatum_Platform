import { BasicColumn } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    sorter: true,
    width: 50,
  },
  {
    title: '拾音器编号',
    dataIndex: 'pickUpId',
    sorter: true,
    fixed: 'left',
    width: 50,
  },
  {
    title: '报警内容',
    dataIndex: 'content',
    width: 50,
    customRender: ({ record }) => {
      const alertContent = record.content;
      let color = 'yellow';
      const text = '状态更新为:' + alertContent;
      if (alertContent === '异常') {
        color = 'red';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '上报时间',
    sorter: true,
    dataIndex: 'createTime',
    width: 50,
  },
];
