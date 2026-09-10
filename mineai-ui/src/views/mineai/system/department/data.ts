import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h } from 'vue';
import { Progress } from 'ant-design-vue';
import { Rule } from '/@/components/Form';
import { RESOURCE_LEVEL_OPTIONS } from '/@/enums/resourceConfig';

export const columns: BasicColumn[] = [
  {
    title: '部门ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '部门名称',
    dataIndex: 'name',
    width: 120,
  },
  {
    title: '部门描述',
    dataIndex: 'description',
    width: 150,
  },
  {
    title: '用户数量',
    dataIndex: 'userCount',
    width: 50,
  },
  {
    title: '部门内存总量/GB',
    dataIndex: 'memoryUsed',
    width: 150,
    customRender: ({ record }) => {
      const usedRatio = record.memoryUsed / record.memoryLimit;
      const percent = Math.round(usedRatio * 100);

      let strokeColor;
      if (usedRatio < 0.5) {
        strokeColor = {
          '0%': '#8cf689',
          '100%': '#cde6a7',
        };
      } else if (usedRatio < 0.7) {
        strokeColor = {
          '0%': '#fff700',
          '100%': '#ffcc00',
        };
      } else {
        strokeColor = {
          '0%': '#ff0000',
          '100%': '#ff6666',
        };
      }

      return h(Progress, {
        percent: percent,
        style: { width: '50%', margin: '0 auto' },
        format: () => `${record.memoryUsed}/${record.memoryLimit}`,
        strokeColor: strokeColor,
      });
    },
  },
  {
    title: '部门CPU总量/核',
    dataIndex: 'cpuUsed',
    width: 150,
    customRender: ({ record }) => {
      const usedRatio = record.cpuUsed / record.cpuLimit;
      const percent = Math.round(usedRatio * 100);

      let strokeColor;
      if (usedRatio < 0.5) {
        strokeColor = {
          '0%': '#8cf689',
          '100%': '#cde6a7',
        };
      } else if (usedRatio < 0.7) {
        strokeColor = {
          '0%': '#fff700',
          '100%': '#ffcc00',
        };
      } else {
        strokeColor = {
          '0%': '#ff0000',
          '100%': '#ff6666',
        };
      }

      return h(Progress, {
        percent: percent,
        style: { width: '50%', margin: '0 auto' },
        format: () => `${record.cpuUsed}/${record.cpuLimit}`,
        strokeColor: strokeColor,
      });
    },
  },
  {
    title: '部门GPU显存总量/GB',
    dataIndex: 'gpuMemoryUsed',
    width: 150,
    customRender: ({ record }) => {
      const usedRatio = record.gpuMemoryUsed / record.gpuMemoryLimit;
      const percent = Math.round(usedRatio * 100);

      let strokeColor;
      if (usedRatio < 0.5) {
        strokeColor = {
          '0%': '#8cf689',
          '100%': '#cde6a7',
        };
      } else if (usedRatio < 0.7) {
        strokeColor = {
          '0%': '#fff700',
          '100%': '#ffcc00',
        };
      } else {
        strokeColor = {
          '0%': '#ff0000',
          '100%': '#ff6666',
        };
      }

      return h(Progress, {
        percent: percent,
        style: { width: '50%', margin: '0 auto' },
        format: () => `${record.gpuMemoryUsed}/${record.gpuMemoryLimit}`,
        strokeColor: strokeColor,
      });
    },
  },
];

const validateNonNegativeInteger = (_, value) => {
  if (!value) {
    return Promise.reject('该字段不能为空');
  }
  if (!/^[1-9]\d*$/.test(value)) {
    return Promise.reject('请输入大于 0 的整数');
  }
  return Promise.resolve();
};

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ name: filteredValue });
      },
    }),
  },
];

export const memberColumns: BasicColumn[] = [
  {
    title: '成员ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 150,
    sorter: true,
  },
  {
    title: '成员姓名',
    dataIndex: 'username',
    width: 200,
  },
  {
    title: '内存使用量/GB',
    dataIndex: 'memoryUsed',
    width: 200,
  },
  {
    title: 'CPU使用量/核',
    dataIndex: 'cpuUsed',
    width: 200,
  },
  {
    title: 'GPU显存使用量/GB',
    dataIndex: 'gpuMemoryUsed',
    width: 200,
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'id',
    component: 'Input',
    show: false,
  },
  {
    field: 'name',
    label: '部门名称',
    component: 'Input',
    required: true,
    rules: [
      {
        required: true,
        validator: (_rule: Rule, value: number) => {
          return new Promise((resolve, reject) => {
            if (!value) {
              reject('不能为空');
              return;
            }
            if (value.length <= 10) {
              resolve();
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
    field: 'description',
    label: '部门描述',
    component: 'Input',
    required: true,
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
  {
    field: 'resourceLevel',
    label: '资源配置',
    component: 'Select',
    required: true,
    componentProps: {
      placeholder: '请选择资源配置等级',
      options: RESOURCE_LEVEL_OPTIONS, // 使用配置文件中的选项
      dropdownAlign: {
        overflow: {
          adjustY: false,
        },
      },
      allowClear: false,
      showArrow: true,
    },
    rules: [
      {
        required: true,
        validator: (_, value) => {
          if (!value) {
            return Promise.reject('请选择资源配置等级');
          }
          return Promise.resolve();
        },
      },
    ],
  },
];
