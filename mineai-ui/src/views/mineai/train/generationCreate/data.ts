import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

import { emit } from '/@/views/mineai/model/generationCreate/modelCreateEventBus';
import { useUserStore } from '/@/store/modules/user';
import { ref } from 'vue';

export const datasetSource = ref(1); // 默认为 1，表示"是"
export const MODEL_SOURCE_EXPLORE = 'EXPLORE';
export const MODEL_SOURCE_BASE = 'BASE';

export const hyperParamTrainPrefix = 'hyperParamTrainPrefix';
export const hyperParamInspectPrefix = 'hyperParamInspectPrefix';
let modelList: any = [];
const userStore = useUserStore();
const userData = userStore.getUserInfo;

export const generationSchemas: FormSchema[] = [
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
  {
    field: 'modelSource',
    label: '算法来源',
    component: 'RadioGroup',
    defaultValue: MODEL_SOURCE_EXPLORE,
    required: true,
    componentProps: ({ formActionType }) => {
      return {
        options: [
          { label: '算法列表', value: MODEL_SOURCE_EXPLORE },
          { label: '基础算法库', value: MODEL_SOURCE_BASE },
        ],
        onChange: async () => {
          const { setFieldsValue } = formActionType;
          await setFieldsValue({ model: undefined });
          modelList = [];
          emit('modelChanged', null);
        },
      };
    },
    colProps: { span: 21 },
    labelWidth: '30%',
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formModel }): {} => {
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
            url:
                  formModel.modelSource === MODEL_SOURCE_BASE
                    ? 'model/getModel'
                    : 'modelExplore/getUnpublishedModel',
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
          if (model) {
            model.modelSource = formModel.modelSource || MODEL_SOURCE_EXPLORE;
          }
          emit('modelChanged', model);
        },
      };
    },
    colProps: { span: 21 },
    labelWidth: '30%',
  },
  // Removed: 数据集划分占比字段已移动到 DatasetSelectCard 组件中
  // {
  //   field: 'splitSize',
  //   label: '数据集划分占比',
  //   component: 'Slider',
  //   slot: 'splitSizeSlot',
  //   colProps: { span: 21 },
  //   labelWidth: '30%',
  //   componentProps: {
  //     defaultValue: [20, 50],
  //   },
  // },
];

export const trainSchemas: FormSchema[] = [
  //占位用，防止渲染失败错误，不要更改
  {
    field: 'memory',
    label: '内存大小',
    component: 'Input',
    ifShow: false,
    componentProps: { placeholder: '内存单位请输入Gi或Mi' },
    colProps: { span: 18 },
  },
];

/**
 * 配置参数表单结构定义
 * 包含资源配置的标题、描述、CPU、内存、显存等字段
 */
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
        trigger: 'blur',
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
        trigger: 'blur',
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
        trigger: 'blur',
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

/**
 * 资源配置选择表单结构定义
 * 用于选择已有的资源配置
 */
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

export const ManagementSchemas: FormSchema[] = [
  {
    field: 'configurations',
    label: '现有配置',
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
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        ),
      labelField: 'title',
      valueField: 'id',
      mode: 'multiple',
      showSearch: true, // 开启搜索功能
      filterOption: (input: string, option: any) => {
        // 模糊搜索功能
        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
      },
      dropdownStyle: {
        maxHeight: '400px', // 限制下拉框最大高度
        overflow: 'auto', // 添加滚动条
      },
    },
  },
];
