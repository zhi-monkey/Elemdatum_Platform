import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

import { emit } from '/@/views/mineai/model/generationCreate/modelCreateEventBus';

export const hyperParamTrainPrefix = 'hyperParamTrainPrefix';
export const hyperParamInspectPrefix = 'hyperParamInspectPrefix';
let modelList: any = [];

export const generationSchemas: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 18 },
  },
  {
    field: 'description',
    label: '训练任务描述',
    component: 'InputTextArea',
    required: false,
    colProps: { span: 18 },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: (): {} => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        api: () =>
          maHttp
            .get(
              {
                url: 'modelExplore/getUnpublishedModel',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              modelList = v;
              return v;
            }),
        labelField: 'modelName',
        valueField: 'id',
        immediate: false,
        colProps: { span: 18 },
        onChange: (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          emit('modelChanged', model);
        },
      };
    },
    colProps: { span: 18 },
  },
  {
    field: 'datasetSource',
    label: '是否采用数据集划分',
    component: 'RadioGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        {
          label: '是',
          value: 1,
        },
        {
          label: '否',
          value: 0,
        },
      ],
    },
    required: true,
    colProps: { span: 18 },
  },
  {
    field: 'splitSize',
    label: '数据集划分占比',
    component: 'Input',
    componentProps: {
      controls: false,
      // min: 0,
      // max: 1,
      placeholder: '依次填写训练-质检-验证数据集占比',
    },
    colProps: { span: 18 },
    ifShow: ({ values }) => {
      return values.datasetSource === 1;
    },
  },
  {
    field: 'trainDataset',
    label: '训练数据集',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'modelDataset/findAllDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'fullName',
      valueField: 'id',
      immediate: true,
      placeholder: '请选择训练数据集',
    },
    colProps: { span: 18 },
    required: true,
  },
  {
    field: 'testDataset',
    label: '质检数据集',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'modelDataset/findAllDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'fullName',
      valueField: 'id',
      immediate: true,
      placeholder: '请选择质检数据集',
    },
    required: true,
    colProps: { span: 18 },
    ifShow: ({ values }) => {
      return values.datasetSource != 1;
    },
  },
  {
    field: 'valDataset',
    label: '验证数据集',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'modelDataset/findAllDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'fullName',
      valueField: 'id',
      immediate: true,
      placeholder: '请选择验证数据集',
    },
    colProps: { span: 18 },
    required: true,
    ifShow: ({ values }) => {
      return values.datasetSource != 1;
    },
  },
];

export const trainSchemas: FormSchema[] = [
  //占位用，防止渲染失败错误，不要更改
  {
    field: 'memory',
    label: '选择内存大小',
    component: 'Input',
    ifShow: false,
    componentProps: { placeholder: '内存单位请输入Gi或Mi (默认无限制)' },
    colProps: { span: 18 },
  },
];

export const ConfigurationSchemas: FormSchema[] = [
  {
    field: 'memory',
    label: '选择内存大小',
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
    componentProps: { placeholder: '内存单位为Gi (默认无限制)' },
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
];

export const ResourceSchemas: FormSchema[] = [
  {
    field: 'resource',
    label: '选择资源配置',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp.get(
          {
            url: 'modelGeneration/findAllHardwareParams',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        ),
      labelField: 'name',
      valueField: 'id',
      immediate: true,
      placeholder: '请选择资源配置',
    },
    colProps: { span: 26 },
  },
];
