import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';
import { selectedArch } from '/@/views/mineai/monitor/model-list/DeployMent.data';

let modelList: any = [];
let monitorList: any[] = [];
export const hyperParamPrefix = 'hyperParamPrefix';

export const selectedJobType = ref();
export const gpuRemained = ref();
export const selectedMonitor = ref();
export const step1Schemas: FormSchema[] = [
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
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
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    show: ({ values }) => {
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
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'monitorType',
    label: '算法类型',
    required: true,
    component: 'RadioButtonGroup',
    defaultValue: 1,
    componentProps: {
      options: [
        { label: '可见光', value: 1 },
        { label: '红外', value: 2, disabled: true },
        { label: '三维', value: 3, disabled: true },
      ],
    },
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'datasetType',
    label: '数据类型',
    required: true,
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '图像', value: '图像' },
        { label: '视频', value: '视频' },
        { label: '音频', value: '音频' },
        { label: '点云', value: '点云' },
      ],
    },
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'description',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
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
        },
      };
    },
    ifShow: ({ values }) => {
      return values.isAddModel === 0;
    },
    colProps: { span: 120 },
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'monitorType',
    label: '算法类型',
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '可见光', value: 1 },
        { label: '红外', value: 2 },
        { label: '三维', value: 3 },
      ],
    },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'datasetType',
    label: '数据类型',
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '图像', value: '图像' },
        { label: '视频', value: '视频' },
        { label: '音频', value: '音频' },
        { label: '点云', value: '点云' },
      ],
    },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
];

export const jobSchemas: FormSchema[] = [
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    componentProps: { readonly: true },
  },
  {
    field: 'modelVersionShowName',
    label: '算法版本',
    component: 'Input',
    componentProps: { readonly: true },
  },
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
              url: 'modelDataset/findDatasetByDataType',
              params: {},
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
      labelField: 'name',
      valueField: 'id',
      immediate: false,
      placeholder: '请选择作业需要的数据集',
    },
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
    component: 'ApiSelect',
    componentProps: {
      placeholder: '请选择权重文件',
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: 'modelJob/getJobByModelVersionId',
              params: {},
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
      labelField: 'description',
      valueField: 'name',
      immediate: false,
    },
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
