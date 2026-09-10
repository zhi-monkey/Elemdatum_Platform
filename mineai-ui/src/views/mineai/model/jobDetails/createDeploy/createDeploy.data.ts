// @ts-ignore
import { FormSchema } from '/@/components/Form';
// @ts-ignore
import { maHttp } from '/@/utils/http/axios';
// @ts-ignore
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';

export const deployImageId = ref();
export const deployImage = ref();
// @ts-ignore
export const formSchema: FormSchema[] = [
  {
    field: 'modelVersionId',
    label: '算法版本id',
    component: 'Input',
    show: false,
    componentProps: { readonly: true },
  },
  {
    field: 'image',
    label: '',
    component: 'Input',
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
        immediate: true,
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
    field: 'monitorId',
    label: '摄像头',
    component: 'ApiSelect',
    required: true,
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
              url: 'monitor/monitorList',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'monitorName',
      valueField: 'id',
      immediate: true,
    },
    ifShow: true,
    colProps: { span: 120 },
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
              url: 'job/getJob',
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
