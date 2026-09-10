import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';

interface data {
  label: string;
  value: string;
}

export const showInspectDataset = ref(false);
export const hyperParamTrainPrefix = 'hyperParamTrainPrefix';
export const hyperParamInspectPrefix = 'hyperParamInspectPrefix';
export const datasetData = ref<data[]>([]);

export async function getDatasetList() {
  //获取数据集Name列表
  maHttp
    .get(
      {
        url: 'modelDataset/findAllDatasetName',
        params: {},
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((datasetList) => {
      const datasetNameList: data[] = [];

      datasetList.forEach((v) => {
        datasetNameList.push({ label: v, value: v });
      });
      datasetData.value = datasetNameList;
    });
}

export const generationSchemas: FormSchema[] = [
  {
    field: 'id',
    label: 'Id',
    component: 'Input',
    required: true,
    show: false,
  },
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 21 },
    labelWidth: '30%',
  },
  {
    field: 'description',
    label: '训练任务描述',
    component: 'InputTextArea',
    required: false,
    colProps: { span: 21 },
    labelWidth: '30%',
  },
];
export const trainSchemas: FormSchema[] = [
  {
    field: 'memory',
    label: '内存大小',
    component: 'Input',
    ifShow: false,
    componentProps: { placeholder: '内存单位请输入Gi或Mi' },
    colProps: { span: 18 },
  },
];
export const ConfigurationSchemas: FormSchema[] = [
  {
    field: 'title',
    label: '配置标题',
    rules: [
      {
        required: true,
        message: '请填写配置标题',
        trigger: 'blur',
      },
      {
        max: 50,
        message: '标题长度不能超过50个字符',
        trigger: 'blur',
      },
    ],
    component: 'Input',
    componentProps: {
      placeholder: '请输入配置标题',
    },
  },
  {
    field: 'description',
    label: '配置描述',
    rules: [
      {
        max: 200,
        message: '描述长度不能超过200个字符',
        trigger: 'blur',
      },
    ],
    component: 'Input',
    componentProps: {
      placeholder: '请描述配置适用情况',
    },
  },
  {
    field: 'cpus',
    label: 'CPU数量',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[1-9]\d*$/;
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
      placeholder: '请选择作业需要的CPU核数',
    },
  },
  {
    field: 'memory',
    label: '内存大小',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[1-9]\d*$/;
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
    componentProps: { placeholder: '内存单位为Gi' },
  },
  {
    field: 'gpuMemory',
    label: '显存大小',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[1-9]\d*$/;
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
      placeholder: '显存单位为Gi',
    },
  },
  //是否为默认
  {
    field: 'isDefault',
    label: '是否为默认配置',
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
    required: true,
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
    required: true,
    colProps: { span: 21 },
    labelWidth: '30%',
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

export const getModelGenerationBoundVersionsWithoutLabelFilter = async (
  modelGenerationId: Number,
) => {
  return maHttp.get(
    {
      url: 'modelGeneration/getModelGenerationBoundVersionsWithoutLabelFilter',
      params: {
        modelGenerationId,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

/**
 * 分页查询数据集组
 */
export const getPrivateAndPublicDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPrivateAndPublicDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 分页查询公开数据集组
 */
export const getPublicDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPublicDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 分页查询私有数据集组
 */
export const getPrivateDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPrivateDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 按名称搜索数据集组（分页）
 */
export const searchPrivateAndPublicDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPrivateAndPublicDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 按名称搜索公开数据集组（分页）
 */
export const searchPublicDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPublicDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 按名称搜索私有数据集组（分页）
 */
export const searchPrivateDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPrivateDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 根据数据集组ID获取数据集列表
 */
export const getDatasetsByDatasetGroupId = async (datasetGroupId: number) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getDatasetsByDatasetGroupId',
      params: { datasetGroupId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 根据数据集组ID和标注类型获取数据集列表
 */
export const getDatasetsByDatasetGroupIdAndAnnotateType = async (
  datasetGroupId: number,
  annotateType: number,
) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getDatasetsByDatasetGroupIdAndAnnotateType',
      params: { datasetGroupId, annotateType },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 获取数据集版本列表
 */
export const getDatasetVersions = async (params: {
  datasetId: number;
  page?: number;
  pageSize?: number;
  current?: number;
  size?: number;
  format?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/versions/versionsDetailList',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
