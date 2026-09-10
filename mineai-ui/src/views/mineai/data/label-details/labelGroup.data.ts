import { BasicColumn, FormSchema } from '/@/components/Table';
import { h, reactive, ref } from 'vue';
import { formatDateTime } from '/@/utils';
import { Popover } from 'ant-design-vue';

// @ts-ignore
export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 200,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    sorter: true,
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
    title: '标签数量',
    dataIndex: 'count',
    width: 120,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    sorter: true,
    width: 150,
    customRender: ({ record }) => {
      return formatDateTime(record.updateTime);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    sorter: true,
    width: 200,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
  {
    title: '标签组描述',
    dataIndex: 'remark',
    sorter: true,
    width: 150,
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
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 8 },
  },
  // {
  //   field: 'createTime',
  //   component: 'RangePicker',
  //   componentProps: {
  //     valueFormat: 'x',
  //     showTime: {
  //       format: 'YYYY-MM-DD HH:mm:ss',
  //       showTime: true,
  //     },
  //   },
  //   label: '创建时间',
  //   colProps: {
  //     xl: 12,
  //     xxl: 6,
  //   },
  // },
  // {
  //   field: 'updateTime',
  //   component: 'RangePicker',
  //   componentProps: {
  //     valueFormat: 'x',
  //     showTime: {
  //       format: 'YYYY-MM-DD HH:mm:ss',
  //       showTime: true,
  //     },
  //   },
  //   label: '更新时间',
  //   colProps: {
  //     xl: 12,
  //     xxl: 6,
  //   },
  // },
];

//记录label的index
export const labelIndex = ref(1);
//记录label的field
export const labelItemList = ref<string[]>(['label1']);

//记录每个label的颜色
export const defaultColor = '#409EFF';
export const labelsColor = reactive<{ [field: string]: string }>({
  label1: defaultColor,
});

// @ts-ignore
export const formSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'labelGroupType',
    label: '类型',
    component: 'Select',
    componentProps: {
      options: [
        { label: '视觉', value: 0 },
        { label: '文本', value: 1 },
      ],
    },
    required: true,
  },
  {
    field: 'remark',
    label: '描述',
    component: 'Input',
  },
  {
    field: 'type',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
  {
    field: 'label1',
    label: '标签1',
    component: 'Input',
    slot: 'add',
    required: true,
    rules: [
      {
        required: true,
        message: '标签名称不能为空',
      },
      {
        max: 30,
        message: '标签名称不能超过30个字符',
      },
      {
        validator: async (_: any, value: string) => {
          // 自定义验证规则，检查标签组名称是否包含空格
          if (/\s/.test(value)) {
            return Promise.reject('标签名称不能包含空格');
          }
          return Promise.resolve();
        },
      },
    ],
  },
  { field: 'operateType', label: '', component: 'Input', defaultValue: 1, show: false },
];
