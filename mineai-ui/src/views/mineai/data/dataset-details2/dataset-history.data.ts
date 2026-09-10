import { BasicColumn } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';

export const columns: BasicColumn[] = [
  {
    title: '数据集ID',
    dataIndex: 'datasetId',
    fixed: 'left',
    width: 75,
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 150,
  },
  {
    title: '标注类型',
    dataIndex: 'annotateType',
    width: 75,
    customRender: ({ record }) => {
      // 102是目标检测，103是语义分割
      const annotateType = record.annotateType;
      let color = 'yellow';
      let text = '目标检测';
      if (annotateType === 103) {
        color = 'blue';
        text = '目标分割';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '导出格式',
    dataIndex: 'format',
    customRender: ({ record }) => {
      // TS是JSON
      const format = record.format;
      let color = 'blue';
      let text = 'JSON';
      if (format === 'COCO') {
        color = 'purple';
        text = 'CreateML';
      } else if (format === 'YOLO') {
        color = 'orange';
        text = 'YOLO';
      } else if (format === 'VOC') {
        color = 'blue';
        text = 'VOC';
      } else if (format === 'Segment-YOLO') {
        color = 'yellow';
        text = 'YOLO';
      }
      return h(Tag, { color: color }, () => text);
    },
    width: 75,
  },
  {
    title: '是否为当前版本',
    dataIndex: 'isCurrent',
    customRender: ({ record }) => {
      // 102是目标检测，103是语义分割
      const isCurrent = record.isCurrent;
      const color = isCurrent === true ? 'green' : 'yellow';
      const text = isCurrent === true ? '是' : '否';
      return h(Tag, { color: color }, () => text);
    },
    width: 75,
  },
  {
    title: '版本号',
    dataIndex: 'versionName',
    width: 75,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 125,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '描述',
    dataIndex: 'versionNote',
    width: 75,
  },
];
