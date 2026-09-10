export interface GrowCardItem {
  icon: string;
  title: string;
  value: number;
  total: number;
  color: string;
  action: string;
}

export const growCardList: GrowCardItem[] = [
  {
    title: '设备数',
    icon: 'visit-count|svg',
    value: 2424,
    total: 122578,
    color: 'green',
    action: '月',
  },
  {
    title: '采掘量',
    icon: 'total-sales|svg',
    value: 20132,
    total: 500312,
    color: 'blue',
    action: '月',
  },
  {
    title: '周绩效',
    icon: 'download-count|svg',
    value: 8000,
    total: 120000,
    color: 'orange',
    action: '周',
  },
  {
    title: '净利润',
    icon: 'transaction|svg',
    value: 12345,
    total: 5077321,
    color: 'purple',
    action: '年',
  },
];
