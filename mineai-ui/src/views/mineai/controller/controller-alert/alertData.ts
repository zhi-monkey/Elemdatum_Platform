import { BasicColumn } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
  },
  {
    title: '控制器',
    dataIndex: 'controller.ip',
    fixed: 'left',
    width: 50,
  },
  {
    title: '所属服务',
    dataIndex: 'mineService.name',
    fixed: 'left',
    width: 50,
  },
  {
    title: '描述',
    dataIndex: 'description',
    fixed: 'left',
    width: 50,
  },
  {
    title: '更新时间',
    dataIndex: 'createTime',
    fixed: 'left',
    width: 50,
  },
];
