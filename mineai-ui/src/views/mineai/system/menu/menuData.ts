import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';

export const columns: BasicColumn[] = [
  {
    title: '',
    dataIndex: 'id',
    ifShow: false,
  },
  {
    title: '菜单名称',
    dataIndex: 'name',
    width: 160,
    align: 'left',
  },
  {
    title: '类型',
    dataIndex: 'type',
    width: 60,
    customRender: ({ record }) => {
      const type = record.type;
      let color;
      let text;
      if (type == 0) {
        color = 'blue';
        text = '目录';
      } else if (type == 1) {
        color = 'green';
        text = '菜单';
      } else {
        color = 'grey';
        text = '按钮';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '路由地址',
    dataIndex: 'path',
    width: 200,
    align: 'left',
    customRender: ({ record }) => {
      const currentText = record.path;
      const currentContent = record.path;
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
    title: '路由名称',
    dataIndex: 'componentName',
    width: 160,
    align: 'left',
    customRender: ({ record }) => {
      const currentText = record.componentName;
      const currentContent = record.componentName;
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
    title: '组件路径',
    dataIndex: 'component',
    width: 300,
    align: 'left',
    customRender: ({ record }) => {
      const currentText = record.component;
      const currentContent = record.component;
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
    title: '布局',
    dataIndex: 'layout',
    width: 200,
    align: 'left',
    ifShow: false,
  },
  {
    title: '隐藏',
    dataIndex: 'hidden',
    width: 50,
    align: 'center',
    customRender: ({ record }) => {
      const hidden = record.hidden;
      let color;
      let text;
      if (hidden) {
        color = 'yellow';
        text = '是';
      } else {
        color = 'blue';
        text = '否';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '页面缓存',
    dataIndex: 'cache',
    width: 65,
    align: 'center',
    customRender: ({ record }) => {
      const cache = record.cache;
      let color;
      let text;
      if (cache) {
        color = 'yellow';
        text = '是';
      } else {
        color = 'blue';
        text = '否';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 50,
    align: 'left',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
    align: 'left',
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

const isMenu = (type: number) => type === 1;

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    show: false,
  },
  {
    field: 'type',
    label: '菜单类型',
    component: 'RadioButtonGroup',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '目录', value: 0 },
        { label: '菜单', value: 1 },
      ],
    },
    colProps: { lg: 24, md: 24 },
  },
  {
    field: 'name',
    label: '菜单名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'icon',
    label: '图标',
    component: 'Input',
    //show: ({ values }) => !isMenu(values.type),
  },
  /*  {
    field: 'pid',
    label: '上级菜单',
    component: 'TreeSelect',
    componentProps: {
      replaceFields: {
        title: 'label',
        key: 'id',
        value: 'id',
      },
      getPopupContainer: () => document.body,
    },
  },*/
  {
    field: 'pid',
    label: '上级菜单',
    component: 'Select',
    show: ({ values }) => isMenu(values.type), // 根据菜单类型动态显示
    componentProps: {
      replaceFields: {
        title: 'label',
        value: 'value',
      },
    },
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    defaultValue: 999,
    required: true,
  },
  {
    field: 'path',
    label: '路由地址',
    component: 'Input',
    required: true,
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ path: filteredValue });
      },
    }),
  },
  {
    field: 'component',
    label: '组件路径',
    component: 'Input',
    //show: ({ values }) => isMenu(values.type),
  },
  {
    field: 'componentName',
    label: '路由名称',
    component: 'Input',
    //show: ({ values }) => isMenu(values.type),
  },
  {
    field: 'cache',
    label: '是否缓存',
    component: 'RadioButtonGroup',
    defaultValue: false,
    componentProps: {
      options: [
        { label: '是', value: true },
        { label: '否', value: false },
      ],
    },
    show: ({ values }) => isMenu(values.type),
  },
  {
    field: 'hidden',
    label: '是否显示',
    component: 'RadioButtonGroup',
    defaultValue: false,
    componentProps: {
      options: [
        { label: '是', value: false },
        { label: '否', value: true },
      ],
    },
  },
  {
    field: 'backTo',
    label: '当前激活菜单',
    component: 'Input',
    show: ({ values }) => values.hidden && isMenu(values.type),
  },
  {
    field: 'permission',
    label: '权限标识',
    component: 'Input',
    show: false,
  },
  {
    field: 'layout',
    label: '布局',
    component: 'Input',
    show: false,
  },
  {
    field: 'extConfig',
    label: '是否外链',
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '否', value: '0' },
        { label: '是', value: '1' },
      ],
    },
    show: false,
  },
];
