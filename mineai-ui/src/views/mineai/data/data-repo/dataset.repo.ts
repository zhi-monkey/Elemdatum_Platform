import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Input, Popover } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';
import { useDataSetFormDataStore } from '/@/store/modules/formData';

const datasetFormDataStore = useDataSetFormDataStore();

// 格式化时间
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

// 展示表格信息
export const columns: BasicColumn[] = [
  {
    title: '数据仓库编号',
    dataIndex: 'id',
    fixed: 'left',
    width: 75,
    sorter: true,
  },
  {
    title: '数据仓库名称',
    dataIndex: 'name',
    width: 150,
    customRender: ({ record }) => {
      const currentText = record.name;
      const currentContent = record.name;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '描述',
    dataIndex: 'remark',
    width: 75,
    customRender: ({ record }) => {
      const currentText = record.remark;
      const currentContent = record.remark;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 125,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

// 搜索框内容
export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '数据仓库名称',
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
    field: 'createTime',
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
    label: '创建时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'name',
    label: '数据仓库名称',
    span: 20,
    required: true,
    component: 'Input',
    rules: [
      {
        required: true,
        pattern: new RegExp(/^[^\\\/]*$/, 'g'),
        message: '不允许带文件分隔符',
      },
      {
        max: 20,
        message: '数据仓库名称不能超过20个字符',
      },
    ],
  },
  {
    field: 'remark',
    label: '数据仓库描述',
    span: 12,
    component: 'Input',
    rules: [
      {
        max: 30,
        message: '数据仓库描述不能超过30个字符',
      },
    ],
  },
];

export const editFormSchema: FormSchema[] = [
  {
    field: 'id',
    label: '数据仓库id',
    span: 12,
    component: 'Input',
    dynamicDisabled: true,
  },
  {
    field: 'name',
    label: '数据仓库名称',
    span: 12,
    required: true,
    component: 'Input',
    rules: [
      {
        required: true,
        pattern: new RegExp(/^[^\\\/]*$/, 'g'),
        message: '不允许带文件分隔符',
      },
      {
        max: 20,
        message: '数据仓库名称不能超过20个字符',
      },
    ],
  },
  {
    field: 'remark',
    label: '数据仓库描述',
    span: 12,
    component: 'Input',
    rules: [
      {
        max: 30,
        message: '数据仓库描述不能超过30个字符',
      },
    ],
  },
];
