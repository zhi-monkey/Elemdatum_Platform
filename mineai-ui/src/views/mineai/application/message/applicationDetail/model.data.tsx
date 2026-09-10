import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: '应用ID',
    dataIndex: 'id',
    width: 40,
    sorter: true,
    customRender: ({ record }) => {
      const currentText = record.id;
      const currentContent = record.id;
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
    title: '应用名称',
    dataIndex: 'applicationName',
    width: 80,
    customRender: ({ record }) => {
      const currentText = record.applicationName;
      const currentContent = record.applicationName;
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

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'applicationName',
    label: '应用名称',
    required: true,
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入应用名称');
          }
          // 如果名称前后带空格则不能过校验
          if (value.trim() !== value) {
            return Promise.reject('应用名称前后不能带空格');
          }
          if (value.length > 20) {
            return Promise.reject('应用名称长度应小于20');
          }
          return Promise.resolve();
        },
      },
    ],
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ applicationName: filteredValue });
      },
    }),
  },
];
function setCurrentItem(e): any {
  const fieldNameList = [
    'id',
    'applicationName',
    /*'modelName',
    'modelEnglishName',
    'vagueInfo',
    'monitorType',
    'description',*/
  ];

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

export const searchFormSchema: FormSchema[] = [
  {
    field: 'select',
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
          { label: '应用ID', value: 'id' },
          { label: '应用名称', value: 'applicationName' },
          /* { label: '应用场景', value: 'scene' },*/
        ],
        defaultValue: 'applicationName',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'id',
    label: '',
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ id: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
    ifShow: true,
  },
  {
    field: 'applicationName',
    label: '',
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ applicationName: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
    ifShow: false,
  },
];
