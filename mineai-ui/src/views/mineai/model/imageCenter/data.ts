import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';

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

export const columns: BasicColumn[] = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 50,
    sorter: true,
  },
  {
    title: '镜像名称',
    dataIndex: 'showName',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      const currentText = record.showName;
      const currentContent = record.showName;
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
    title: '镜像版本号',
    dataIndex: 'level',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      const currentText = record.level;
      const currentContent = record.level;
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
    title: '状态',
    dataIndex: 'status',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.status === 0) {
        text = '上传中';
        color = 'gray';
      } else if (record.status === 1) {
        text = '已上传';
        color = 'green';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '镜像描述',
    dataIndex: 'description',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      const currentText = record.description;
      const currentContent = record.description;
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
    title: '上传时间',
    dataIndex: 'createTime',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '创建人',
    dataIndex: 'roleName',
    sorter: true,
    width: 60,
    customRender: ({ record }) => {
      return record.user.username;
    },
  },
];

export const schema: FormSchema[] = [
  {
    field: 'showName',
    label: '镜像名称',
    component: 'Input',
    required: true,
    colProps: { span: 20 },
  },
  {
    field: 'level',
    label: '镜像版本号',
    component: 'Input',
    required: true,
    colProps: { span: 20 },
  },
  {
    label: '镜像用途',
    field: 'use',
    required: true,
    component: 'RadioButtonGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '训练', value: 1 },
        { label: '质检', value: 2 },
        { label: '推理', value: 3 },
        { label: '自动标注', value: 4 },
      ],
    },
  },
  {
    label: '镜像描述',
    field: 'description',
    required: true,
    component: 'Input',
    colProps: { span: 20 },
  },
  {
    field: 'isUrl',
    component: 'RadioButtonGroup',
    label: '选择上传方式',
    required: true,
    show: true,
    colProps: { span: 18 },
    componentProps: () => {
      return {
        options: [
          {
            label: 'Url上传',
            value: 1,
          },
          {
            label: '本地文件上传',
            value: 0,
          },
        ],
      };
    },
  },
  {
    label: 'Url上传',
    field: 'url',
    required: true,
    component: 'Input',
    colProps: { span: 20 },
    ifShow: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
];

function setCurrentItem(e): any {
  const fieldNameList = ['showName', 'vagueInfo', 'description'];

  const result: any[] = [];

  for (const item in fieldNameList) {
    if (e === fieldNameList[item]) {
      result.push({ field: fieldNameList[item], ifShow: true });
    } else {
      result.push({ field: fieldNameList[item], ifShow: false });
    }
  }

  return result;
}

export const searchSchema: FormSchema[] = [
  {
    field: '',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          { label: '镜像名称', value: 'showName' },
          {
            label: '镜像描述',
            value: 'description',
          },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'showName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'showName',
    label: '',
    component: 'Input',
    colProps: { span: 5 },
  },
  {
    label: '',
    field: 'description',
    component: 'Input',
    ifShow: false,
    colProps: { span: 5 },
  },
  {
    label: '',
    field: 'vagueInfo',
    component: 'Input',
    ifShow: false,
    colProps: { span: 5 },
  },
];
