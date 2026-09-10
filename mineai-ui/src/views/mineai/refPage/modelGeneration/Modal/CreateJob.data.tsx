import { FormSchema } from '/@/components/Form';
import { BasicColumn } from '/@/components/Table';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

// 新建作业表单 form
export const datasetId = ref(0);
export const hyperParamPrefix = 'hyperParamPrefix';
export const selectedJobType = ref();
export const gpuRemained = ref();
export const jobSchemas: FormSchema[] = [
  {
    field: 'modelVersionId',
    label: '算法版本',
    component: 'Input',
    componentProps: { readonly: true },
    required: true,
    show: false,
  },
  {
    field: 'jobType',
    label: '作业类型',
    component: 'RadioButtonGroup',
    componentProps: ({ formActionType }) => {
      return {
        options: [],
        onChange: (e) => {
          const { updateSchema } = formActionType;
          selectedJobType.value = e;
          if (e === 1) updateSchema({ field: 'weightPath', show: false });
          if (e === 2) updateSchema({ field: 'weightPath', show: true });
        },
      };
    },
    required: true,
    show: false,
  },
  {
    field: 'dataset',
    label: '数据集',
    component: 'Input',
    componentProps: { readonly: true },
    required: true,
  },
  {
    field: 'memory',
    label: '选择内存大小',
    rules: [
      {
        required: false,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[1-9].*?(Gi|Mi)$/g;
          if (!value) {
            return Promise.resolve();
          }
          if (!regCpus.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('格式错误，请输入例如：4Gi 的格式');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    component: 'Input',
    componentProps: { placeholder: '内存单位请输入Gi或Mi (默认无限制)' },
  },
  {
    field: 'cpus',
    label: '选择CPU数量',
    rules: [
      {
        required: false,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[1-9]\d*$/;
          if (!value) {
            return Promise.resolve();
          }
          if (!regCpus.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('输入格式错误，请输入正整数');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    component: 'Input',
    componentProps: {
      placeholder: '请选择作业需要的CPU核数 (默认无限制)',
    },
  },
  {
    field: 'gpus',
    label: '设置GPU使用上限',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regGpus = /^[1-9]\d*$/;
          if (!regGpus.test(value)) {
            return Promise.reject('输入格式错误，请输入正整数');
          }
          if (value > gpuRemained.value) {
            return Promise.reject('输入不能大于GPU剩余资源');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    component: 'Input',
    componentProps: { placeholder: '请输入正整数(单位GiB) (默认无限制)' },
  },
  {
    field: 'gpu',
    label: 'GPU剩余资源/节点总资源',
    component: 'Input',
  },
  {
    field: 'weightPath',
    label: '选择权重文件',
    component: 'Input',
    componentProps: { readonly: true },
    required: true,
  },
  {
    field: 'description',
    label: '作业信息描述',
    component: 'InputTextArea',
    componentProps: { placeholder: '请输入此次作业的介绍' },
  },
];

export const columns: BasicColumn[] = [
  {
    title: 'id',
    dataIndex: 'id',
    sorter: true,
    width: 80,
  },
  {
    title: '算法名称',
    dataIndex: 'modelVersion.model.modelName',
    sorter: true,
    width: 200,
  },
  {
    title: '算法版本',
    dataIndex: 'modelVersion.name',
    width: 200,
    sorter: true,
  },
  {
    title: '作业类型',
    dataIndex: 'jobType',
    width: 120,
    sorter: true,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.jobType === 1) {
        text = '模型训练';
        color = 'cyan';
      }
      if (record.jobType === 2) {
        text = '模型质检';
        color = 'purple';
      }
      if (record.jobType === 3) {
        text = '模型转换';
        color = 'blue';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '数据集',
    dataIndex: 'dataset.name',
    width: 100,
    sorter: true,
  },
  {
    title: '作业状态',
    dataIndex: 'status',
    width: 100,
    sorter: true,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.status === 2) {
        text = '正在进行';
        color = 'cyan';
      } else if (record.status === 1) {
        text = '执行成功';
        color = 'green';
      } else if (record.status === -1) {
        text = '执行失败';
        color = 'red';
      } else {
        text = '作业取消';
        color = 'yellow';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: 'CPU数量',
    dataIndex: 'cpus',
    width: 100,
    customRender: ({ record }) => {
      const cpus = record.cpus;
      let fileSizeString = '';
      if (cpus == '无限制') {
        fileSizeString = cpus;
      } else fileSizeString = cpus + '核';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },

  {
    title: 'GPU显存',
    dataIndex: 'gpus',
    width: 100,
    customRender: ({ record }) => {
      const gpus = record.gpus;
      let fileSizeString = '';
      if (gpus == '无限制') {
        fileSizeString = '无限制';
      } else fileSizeString = gpus + 'GiB';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },
  {
    title: '内存大小',
    dataIndex: 'memory',
    width: 100,
    customRender: ({ record }) => {
      const memory = record.memory;
      let fileSizeString = '';
      if (memory == '无限制') {
        fileSizeString = memory;
      } else fileSizeString = memory + 'B';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },

  {
    title: '作业信息描述',
    dataIndex: 'description',
    width: 200,
  },
  {
    title: '开始时间',
    dataIndex: 'createTime',
    width: 200,
    sorter: true,
  },
  // {
  //   title: '结束时间',
  //   dataIndex: 'lastJobTime',
  //   width: 70,
  //   sorter: true,
  // },
];
