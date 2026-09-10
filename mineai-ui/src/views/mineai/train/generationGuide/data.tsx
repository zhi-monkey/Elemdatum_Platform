import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';
import { selectedArch } from '/@/views/mineai/monitor/model-list/DeployMent.data';

const generationName = ref('');
let modelList: any = [];
let monitorList: any[] = [];
export const hyperParamPrefix = 'hyperParamPrefix';

export const selectedJobType = ref();
export const gpuRemained = ref();
export const selectedMonitor = ref();
export const selectedModel = ref();
//定义表格新增,编辑等操作
export const step1Schemas: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 20 },
  },
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
    defaultValue: 0,
    componentProps: ({ formActionType, formModel }) => {
      return {
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
        onChange: async () => {
          generationName.value = formModel.name;
          const { resetFields, setFieldsValue } = formActionType;
          await resetFields();
          await setFieldsValue({ name: generationName.value });
        },
      };
    },
    show: false,
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formActionType }) => {
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
                url: 'model/getModel',
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
        onChange: (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          const { setFieldsValue } = formActionType;
          setFieldsValue({ ...model });
          selectedModel.value = model;
        },
      };
    },
    colProps: { span: 25 },
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
    colProps: { span: 25 },
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
    colProps: { span: 18 },
  },
];

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
          if (e === 1) updateSchema({ field: 'weightPath', ifShow: false });
          if (e === 2) updateSchema({ field: 'weightPath', ifShow: true });
        },
      };
    },
    required: true,
    show: false,
  },
  // {
  //   field: 'dataset',
  //   label: '数据集',
  //   component: 'ApiSelect',
  //   componentProps: {
  //     dropdownAlign: {
  //       overflow: {
  //         adjustY: false, // 关闭下拉框垂直位置自适应
  //       },
  //     },
  //     api: () =>
  //       maHttp
  //         .get(
  //           {
  //             url: 'modelDataset/findDatasetByDataType',
  //             params: {},
  //             headers: {
  //               // @ts-ignore
  //               ignoreCancelToken: true,
  //             },
  //           },
  //           { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //         )
  //         .then((v) => {
  //           return v;
  //         }),
  //     labelField: 'name',
  //     valueField: 'id',
  //     immediate: false,
  //     placeholder: '请选择作业需要的数据集',
  //   },
  //   required: true,
  // },
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
            /* eslint-disable-next-line */
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

export const bindSchemas: FormSchema[] = [
  {
    field: 'modelVersionIds',
    component: 'Input',
    label: '算法版本Id',
    required: true,
    show: false,
  },
  {
    field: 'modelVersionShowName',
    component: 'Input',
    label: '算法版本',
    required: true,
  },
  {
    field: 'mineServiceId',
    component: 'ApiSelect',
    label: '服务',
    required: true,
    componentProps: {
      placeholder: '请选择服务',
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'mineService/mineServiceList',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'name',
      valueField: 'id',
      immediate: false,
    },
  },
  {
    field: 'monitorId',
    component: 'ApiSelect',
    label: '监控设备',
    required: true,
    componentProps: {
      placeholder: '请选择监控设备',
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'monitor/getMonitorList',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then((v) => {
            monitorList = v;
            return v;
          }),
      onChange: (e) => {
        selectedMonitor.value = monitorList.filter((item) => item.id === e)[0];
      },
      labelField: 'monitorName',
      valueField: 'id',
      immediate: false,
    },
  },
];

export const DeploySchemas: FormSchema[] = [
  {
    field: 'modelVersionId',
    label: '算法版本id',
    component: 'Input',
    required: true,
    show: false,
    componentProps: { readonly: true },
  },
  {
    field: 'image',
    label: '镜像URL',
    component: 'Input',
    show: false,
    componentProps: { readonly: true },
  },
  {
    field: 'monitorId',
    label: '监控设备Id',
    component: 'Input',
    required: true,
    show: false,
    componentProps: { readonly: true },
  },
  {
    field: 'showName',
    label: '镜像Tag',
    component: 'Input',
    show: true,
    componentProps: { readonly: true },
  },
  {
    field: 'monitorName',
    label: '监控设备',
    component: 'Input',
    show: true,
    componentProps: { readonly: true },
  },
  {
    field: 'architecture',
    label: '控制器架构',
    component: 'RadioButtonGroup',
    componentProps: ({ formActionType }) => {
      return {
        options: [
          { label: 'amd64', value: 'amd64' },
          { label: 'arm64', value: 'arm64' },
        ],
        onChange: (e) => {
          const { updateSchema } = formActionType;
          selectedArch.value = e;
          if (e === 'arm64')
            updateSchema([
              { field: 'memory', show: false },
              { field: 'cpus', show: false },
              {
                field: 'gpus',
                show: false,
                rules: [
                  {
                    required: false,
                  },
                ],
              },
              { field: 'gpu', show: false },
            ]);
          if (e === 'amd64')
            updateSchema([
              { field: 'memory', show: true },
              { field: 'cpus', show: true },
              {
                field: 'gpus',
                show: true,
                rules: [
                  {
                    required: true,
                    // @ts-ignore
                    validator: async (rule, value) => {
                      const regGpus = /^[1-9]\d*$/;
                      if (!regGpus.test(value)) {
                        /* eslint-disable-next-line */
                        return Promise.reject('输入格式错误，请输入正整数');
                      }
                      return Promise.resolve();
                    },
                    trigger: 'change',
                  },
                ],
              },
              { field: 'gpu', show: true },
            ]);
        },
      };
    },
    required: true,
  },
  {
    field: 'controllerId',
    label: '选择控制器',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formActionType }) => {
      const { setFieldsValue } = formActionType;
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
                url: 'controller/findAllController',
                params: {},
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
            )
            .then((v) => {
              return v;
            }),
        labelField: 'name',
        valueField: 'id',
        immediate: false,
        onChange: (e) => {
          maHttp
            .get(
              {
                url: 'deploy/getGpu',
                params: { controllerId: e },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              setFieldsValue({ gpu: v.gpuRemained + '(GiB)' + ' / ' + v.gpuNum + '(GiB)' });
            });
        },
      };
    },
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
            /* eslint-disable-next-line */
            return Promise.reject('输入格式错误，请输入正整数');
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
    componentProps: {
      placeholder: '待选择控制器',
      readonly: true,
    },
  },
];
export const convertSchems: FormSchema[] = [
  {
    field: 'type',
    label: '设备类型',
    component: 'Select',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      options: [
        { value: 'rk1806', label: 'rk1806' },
        { value: 'rk1808', label: 'rk1808' },
        { value: 'rk3399pro', label: 'rk3399pro' },
        { value: 'rv1126', label: 'rv1126' },
        { value: 'rv1109', label: 'rv1109' },
      ],
    },
    required: true,
  },
  {
    field: 'datasetPath',
    label: '数据集',
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
              url: `datarepos/queryAllDataRepos`,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'name',
      valueField: 'uri',
      immediate: false,
      placeholder: '请选择数据集',
    },
    required: true,
  },
];
