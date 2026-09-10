import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export const hyperParamTrainPrefix = 'hyperParamTrainPrefix';
export const hyperParamInspectPrefix = 'hyperParamInspectPrefix';
let modelList: any = [];
export const dataSchemas: FormSchema[] = [
  {
    field: 'id',
    label: 'Id',
    component: 'Input',
    required: true,
    show: false,
  },
];
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
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    colProps: { span: 18 },
    ifShow: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[a-z0-9]+$/;
          if (!value) {
            return Promise.resolve();
          }
          if (!regCpus.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('输入格式错误，请输入小写英文');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    required: true,
    component: 'Input',
    colProps: { span: 18 },
    ifShow: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'modelDescription',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    ifShow: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formActionType }): {} => {
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
        labelField: 'modelNickName',
        valueField: 'id',
        immediate: false,
        colProps: { span: 18 },
        onChange: (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          const { setFieldsValue } = formActionType;
          setFieldsValue({
            modelName: model.modelName,
            modelEnglishName: model.modelEnglishName,
          });
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
    field: 'preTrainingWeight',
    label: '是否加载预训练权重',
    component: 'RadioGroup',
    defaultValue: 0,
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
    required: false,
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
    rules: [
      // {
      //   required: false,
      //   // @ts-ignore
      //   validator: async (rule, value) => {
      //     const regCpus = /(0\.\d+|1(\.0+)?)$/;
      //     if (!value) {
      //       return Promise.resolve();
      //     }
      //     if (!regCpus.test(value)) {
      //       /* eslint-disable-next-line */
      //       return Promise.reject('输入格式错误，请输入0到1的小数');
      //     }
      //     return Promise.resolve();
      //   },
      //   trigger: 'change',
      // },
    ],
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
  {
    field: 'memory',
    label: '选择内存大小',
    component: 'Input',
    ifShow: false,
    componentProps: { placeholder: '内存单位请输入Gi或Mi (默认无限制)' },
    colProps: { span: 18 },
  },
];


export const trainTimeSchemas: FormSchema[] = [
  {
    field: 'delayTrainTime',
    label: '延迟训练时间(分钟)',
    component: 'Input',
    componentProps: { placeholder: '时间单位默认为分钟' },
    defaultValue: 0,
    rules: [
      {
       required: true,
        //required: false,
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
    colProps: { span: 18 },
  },
  {
    field: 'maxTrainTime',
    label: '最大训练时长(分钟)',
    component: 'Input',
    componentProps: { placeholder: '时间单位默认为分钟' },
    defaultValue: 0,
    rules: [
      {
        required: true,
        //required: false,
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

export const inspectSchemas: FormSchema[] = [];
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
