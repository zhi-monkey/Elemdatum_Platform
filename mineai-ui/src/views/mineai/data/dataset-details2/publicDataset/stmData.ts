import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Input, Tag } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';
import { useDataSetFormDataStore } from '/@/store/modules/formData';

const datasetFormDataStore = useDataSetFormDataStore();

export const columns: BasicColumn[] = [
  {
    title: '名称',
    dataIndex: 'name',
    width: 100,
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
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 125,
    customRender: ({ record }) => {
      return formatDateTime(record.updateTime);
    },
  },
  {
    title: '描述',
    dataIndex: 'remark',
    width: 75,
  },
  {
    title: '发布者名称',
    dataIndex: 'creatorName',
    width: 75,
    ifShow: false,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
    render: ({ model, field }) => {
      return h(Input, {
        placeholder: '请输入',
        value: model[field],
        onChange: (e: ChangeEvent) => {
          const filteredValue = e.target.value.replace(/\s+/g, '');
          model[field] = filteredValue;
          datasetFormDataStore.setCurrentName(filteredValue);
        },
      });
    },
  },
  {
    field: 'annotateType',
    label: '标注类型',
    component: 'Select',
    componentProps: {
      options: [
        { label: '目标检测', value: 102 },
        { label: '目标分割', value: 103 },
      ],
      onChange: (e) => {
        datasetFormDataStore.setCurrentAnnotateType(e);
      },
    },
    colProps: { xl: 12, xxl: 4 },
  },
  {
    field: 'updateTime',
    component: 'RangePicker',
    componentProps: {
      valueFormat: 'x',
      showTime: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      onChange: (e) => {
        const formattedDateTimes = convertTimestampsToDateTimeStrings(e);
        datasetFormDataStore.setCurrentCreateTime(formattedDateTimes);
      },
    },
    label: '更新时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
];

function convertTimestampsToDateTimeStrings(timestamps) {
  return timestamps.map((timestamp) => {
    const date = new Date(parseInt(timestamp, 10));
    const year = date.getFullYear();
    const month = `0${date.getMonth() + 1}`.slice(-2);
    const day = `0${date.getDate()}`.slice(-2);
    const hours = `0${date.getHours()}`.slice(-2);
    const minutes = `0${date.getMinutes()}`.slice(-2);
    const seconds = `0${date.getSeconds()}`.slice(-2);
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  });
}

export const subTableColumns: BasicColumn[] = [
  {
    title: '版本号',
    dataIndex: 'versionName',
    width: 75,
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
    title: '创建时间',
    dataIndex: 'createTime',
    width: 75,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 75,
    slots: { customRender: 'action' },
  },
];
