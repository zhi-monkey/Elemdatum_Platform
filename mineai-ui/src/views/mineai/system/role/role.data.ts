import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';
import { Rule } from '/@/components/Form';

export const columns: BasicColumn[] = [
  {
    title: '角色ID',
    dataIndex: 'id',
    sorter: true,
    width: 50,
  },
  {
    title: '角色名称',
    dataIndex: 'name',
    width: 50,
    ellipsis: true,
    customRender: ({ record }) => {
      return h(Tag, { color: 'cyan' }, () => record.name);
    },
  },
  {
    title: '角色备注信息',
    dataIndex: 'remark',
    width: 50,
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
    width: 50,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'blurry',
    label: '角色名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ blurry: filteredValue });
      },
    }),
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '角色ID',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'name',
    label: '角色名称',
    component: 'Input',
    required: true,
    rules: [
      {
        required: true,
        validator: (_rule: Rule, value: number) => {
          return new Promise((resolve, reject) => {
            if (!value) {
              reject('不能为空'); // 必填校验
              return;
            }
            if (value.length <= 10) {
              resolve(); // 验证通过
            } else {
              reject('输入内容长度不能超过10个字符');
            }
          });
        },
      },
    ],
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ name: filteredValue });
      },
    }),
  },
  {
    field: 'remark',
    label: '角色备注信息',
    component: 'InputTextArea',
    rules: [
      {
        validator: async (_rule, value) => {
          if (value && value.length > 45) {
            return Promise.reject('备注长度不能超过45位');
          }
          if (!value) {
            return Promise.resolve();
          }
        },
      },
    ],
  },
];
