import { FormSchema } from '/@/components/Table';
// 定义列的类型
type Column = {
  title: string; // 列标题
  dataIndex: string; // 数据索引
  width: number; // 列宽度
};

// 声明列数组并初始化
export const computingcolumns: Column[] = [
  {
    title: 'ID', // 第一列的标题
    dataIndex: 'id', // 对应的数据字段
    width: 80, // 列宽度为80像素
  },
  {
    title: '芯片类型', // 第二列的标题
    dataIndex: 'chipType', // 对应的数据字段
    width: 80, // 列宽度为80像素
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '算力id',
    required: false,
    component: 'Input',
    show: false,
  },
  {
    field: 'chipType',
    label: '芯片类型',
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入芯片类型');
          }
          // 如果value中带空格不能过校验
          if (value.includes(' ')) {
            return Promise.reject('类型中不能含有空格');
          }
          if (value.length > 20) {
            return Promise.reject('芯片类型长度应小于20');
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
        await setFieldsValue({ chipType: filteredValue });
      },
    }),
  },
];

export function getcomputingcolumns() {
  return computingcolumns;
}

function setCurrentItem(e): any {
  const fieldNameList = ['chipType'];

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
        options: [{ label: '芯片类型', value: 'chipType' }],
        defaultValue: 'chipType',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'chipType',
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
          await setFieldsValue({ chipType: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
];
