import { DescItem } from '/@/components/Description';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

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
    label: '数据集ID：',
  },
  {
    field: 'name',
    label: '数据集名称：',
  },
  {
    field: 'annotationType',
    label: '标注类型：',
    render: (annotationType) => {
      //标注类型 102目标检测 103语义分割
      let color = 'yellow';
      let text = '目标检测';
      if (annotationType === 103) {
        color = 'blue';
        text = '目标分割';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    field: 'currentVersionName',
    label: '当前版本名称：',
    render: (currentVersionName) => {
      //标注类型 102目标检测 103语义分割
      if (currentVersionName == null || currentVersionName == undefined) {
        return '暂无版本';
      }
      return currentVersionName;
    },
  },
  {
    field: 'status',
    label: '标注状态：',
    render: (status) => {
      //数据集 101未标注 102标注中 104标注完成
      let color = 'gray';
      let text = '未标注';
      if (status === 102) {
        color = 'yellow';
        text = '标注中';
      } else if (status === 105) {
        color = 'green';
        text = '已标注';
      } else if (status === 302) {
        color = 'blue';
        text = '抽帧中';
      } else if (status === 401) {
        color = 'purple';
        text = '数据增强中';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  // {
  //   field: 'isImport',
  //   label: '是否是用户导入：',
  // },
  {
    field: 'createTime',
    label: '创建时间：',
    render: (createTime) => {
      return formatDateTime(createTime);
    },
  },
  {
    field: 'updateTime',
    label: '更新时间：',
    render: (updateTime) => {
      return formatDateTime(updateTime);
    },
  },
];
export const picSchema: DescItem[] = [
  {
    field: 'totalImages',
    label: '图片总数：',
  },
  {
    field: 'unlabeledImages',
    label: '未标注：',
  },
  {
    field: 'labeledImages',
    label: '已标注：',
  },
  {
    field: 'totalVideos',
    label: '视频总数',
  },
  {
    field: 'totalLabels',
    label: '标签总数',
  },
  {
    field: 'totalSamples',
    label: '样本数总数',
  },
];

export const videoSchema: DescItem[] = [
  {
    field: 'totalVideos',
    label: '视频总数：',
  },
  {
    field: 'unextractedVideos',
    label: '未抽帧：',
  },
  {
    field: 'extractedVideos',
    label: '已抽帧：',
  },
];

// 独立视频数据集（dataType=6）信息总览-数据集信息 schema
export const videoDatasetSchema: DescItem[] = [
  {
    field: 'id',
    label: '数据集ID：',
  },
  {
    field: 'name',
    label: '数据集名称：',
  },
  {
    field: 'status',
    label: '数据集状态：',
    render: (status) => {
      // 视频数据集 2001待上传 2002上传中 2003已上传 2006上传失败
      const videoStatusMap = {
        2001: { color: 'gray', text: '待上传' },
        2002: { color: 'blue', text: '上传中' },
        2003: { color: 'green', text: '已上传' },
        2006: { color: 'red', text: '上传失败' },
      };
      const item = videoStatusMap[status] || { color: 'gray', text: '未知' };
      return h(Tag, { color: item.color }, () => item.text);
    },
  },
  {
    field: 'uploadStatus',
    label: '上传状态：',
    render: (uploadStatus) => {
      if (uploadStatus == null || uploadStatus == undefined || uploadStatus === '') {
        return '-';
      }
      return uploadStatus;
    },
  },
  {
    field: 'createTime',
    label: '创建时间：',
    render: (createTime) => {
      return formatDateTime(createTime);
    },
  },
  {
    field: 'updateTime',
    label: '更新时间：',
    render: (updateTime) => {
      return formatDateTime(updateTime);
    },
  },
];

export const labelColumn = [
  {
    title: '标签ID',
    dataIndex: 'labelId',
    key: 'labelId',
  },
  {
    title: '标签名称',
    dataIndex: 'labelName',
    key: 'labelName',
  },
  {
    title: '标签所对应的样本数量',
    dataIndex: 'annotationCount',
    key: 'annotationCount',
  },
];
