export interface GrowCardItem {
  title: string;
  total: number;
  text1: string;
  text2: string;
  text3: string;
  text4: string;
  value1: number;
  value2: number;
  value3: number;
  value4: number;
}

export const growCardList: GrowCardItem[] = [
  {
    title: '数据集总数',
    total: 1050,
    text1: '本月新增',
    text2: '本周新增',
    text3: '本日新增',
    value1: 110,
    value2: 50,
    value3: 11,
    text4: '',
    value4: NaN,
  },
  {
    title: '占用存储空间大小（T）',
    total: 3.8,
    text1: '图像',
    text2: '音频',
    text3: '视频',
    value1: 0.6,
    value2: 0.9,
    value3: 1.4,
    text4: '点云',
    value4: 0.8,
  },
  {
    title: '数据源统计',
    total: 5031,
    text1: '手动上传',
    text2: '设备绑定',
    text3: '',
    value1: 674,
    value2: 4128,
    value3: NaN,
    text4: '',
    value4: NaN,
  },
  {
    title: '数据标注',
    total: 5031,
    text1: '已标注',
    text2: '未标注',
    text3: '',
    value1: 1593,
    value2: 3438,
    value3: NaN,
    text4: '',
    value4: NaN,
  },
  {
    title: '关联设备总数',
    total: 523,
    text1: '启动',
    text2: '关闭',
    text3: '故障',
    value1: 323,
    value2: 98,
    value3: 102,
    text4: '',
    value4: NaN,
  },
];
