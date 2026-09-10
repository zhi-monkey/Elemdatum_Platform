import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export const formSchema: FormSchema[] = [
  {
    field: 'image',
    label: '镜像Tag',
    component: 'Input',
    show: true,
    componentProps: { readonly: true },
  },
  {
    field: 'convertType',
    label: '转换类型',
    component: 'Select',
    required: true,
    componentProps: {
      placeholder: '请选择转换类型',
      options: [
        {
          label: '.pt转.rknn',
          value: 'rknn',
          key: '1',
        },
        {
          label: '.pt转.engine',
          value: 'engine',
          key: '2',
        },
      ],
    },
  },
  {
    field: 'architecture',
    label: '控制器架构',
    component: 'Select',
    required: true,
    componentProps: {
      placeholder: '请选择控制器架构',
      options: [
        {
          label: 'arm64',
          value: 'arm64',
        },
        {
          label: 'amd64',
          value: 'amd64',
        },
      ],
    },
  },
  {
    field: 'controllerId',
    label: '选择控制器',
    component: 'ApiSelect',
    required: true,
    componentProps: () => {
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
      };
    },
  },
  {
    field: 'job',
    label: '选择权重文件',
    component: 'ApiSelect',
    required: true,
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
              url: 'modelJob/getJob',
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
      valueField: 'name',
      immediate: false,
    },
  },
  {
    field: 'weightName',
    label: '请输入权重文件名称',
    required: true,
    component: 'Input',
    componentProps: {
      placeholder: '请输入权重文件名称（不包含后缀）',
    },
  },
  {
    field: 'labels',
    label: '请输入模型标签',
    required: false,
    component: 'Input',
    componentProps: {
      placeholder: '请输入模型标签，每个标签用“,”来分割',
    },
  },
  // {
  //   field: 'memory',
  //   label: '选择内存大小',
  //   rules: [
  //     {
  //       required: false,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regCpus = /^[1-9].*?(Gi|Mi)$/g;
  //         if (!value) {
  //           return Promise.resolve();
  //         }
  //         if (!regCpus.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('格式错误，请输入例如：4Gi 的格式');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   component: 'Input',
  //   componentProps: { placeholder: '内存单位请输入Gi或Mi ()' },
  // },
  // {
  //   field: 'cpus',
  //   label: '选择CPU数量',
  //   rules: [
  //     {
  //       required: false,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regCpus = /^[1-9]\d*$/;
  //         if (!value) {
  //           return Promise.resolve();
  //         }
  //         if (!regCpus.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入格式错误，请输入正整数');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   component: 'Input',
  //   componentProps: {
  //     placeholder: '请选择作业需要的CPU核数 ()',
  //   },
  // },
  // {
  //   field: 'gpus',
  //   label: '设置GPU使用上限',
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regGpus = /^[1-9]\d*$/;
  //
  //         if (!regGpus.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入格式错误，请输入正整数');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   component: 'Input',
  //   componentProps: { placeholder: '请输入正整数(单位GiB) ()' },
  // },
  {
    field: 'description',
    label: '转换作业描述',
    required: false,
    component: 'Input',
    componentProps: {
      placeholder: '请输入此次转换作业的描述',
    },
  },
];
