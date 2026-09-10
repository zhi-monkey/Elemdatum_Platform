import { BasicColumn } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  { title: '子系统名称', dataIndex: 'subsystemName', width: 50 },
  {
    title: '在线状态',
    dataIndex: 'status',
    width: 50,
    customRender: ({ record }) => {
      const status = record.status;
      let text;
      let color;
      if (status === 0) {
        text = '在线';
        color = 'cyan';
      }
      if (status === 1) {
        text = '离线';
        color = 'red';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  { title: '更新时间', dataIndex: 'updateTime', width: 100 },
];
