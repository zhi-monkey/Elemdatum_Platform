import { DescItem } from '/@/components/Description';

function formatDateTime(dateTimeStr) {
  // 创建一个新的 Date 对象
  const date = new Date(dateTimeStr);

  // 获取日期和时间的各个部分
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  // 返回格式化的日期和时间
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

export const datasetSchema: DescItem[] = [
  {
    field: 'id',
    label: '数据仓库ID：',
  },
  {
    field: 'name',
    label: '数据仓库名称：',
  },
  {
    field: 'remark',
    label: '数据仓库描述：',
  },
  {
    field: 'createTime',
    label: '创建时间：',
    render: (createTime) => {
      return formatDateTime(createTime);
    },
  },
];

export const picSchema: DescItem[] = [
  {
    field: 'imageCount',
    label: '图片文件总数：',
  },
  {
    field: 'videoCount',
    label: '视频文件总数：',
  },
  {
    field: 'otherCount',
    label: '其他类型文件总数:',
  },
];
