import { BasicColumn, FormSchema } from '/@/components/Table';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

let modelList: any = [];
const generationName = ref('');
export const columns: BasicColumn[] = [
  {
    title: '名称',
    dataIndex: 'name',
    sorter: true,
    width: 60,
  },
  {
    title: '训练数据集',
    dataIndex: 'trainDataset',
    width: 100,
    customRender: ({ record }) => {
      let text;
      if (record.trainDataset) {
        const versionNameTmp = record.trainDataset.versionName;
        if (versionNameTmp == null) {
          text = '-';
        } else {
          if (versionNameTmp.startsWith('V') || versionNameTmp.startsWith('v')) {
            text = record.trainDataset.name + '-' + record.trainDataset.versionName;
          } else {
            text = record.trainDataset.name + '-' + record.trainDataset.versionSource;
          }
        }
      }
      return { children: text };
    },
  },
  {
    title: '对应算法',
    dataIndex: 'model',
    width: 100,
    customRender: ({ record }) => {
      let text;
      if (record.modelStore) {
        text = record.modelStore.modelName;
      } else {
        text = '-';
      }
      return { children: text };
    },
  },

  // {
  //   title: '训练状态',
  //   dataIndex: 'trainJob',
  //   width: 100,
  //   sorter: true,
  //   customRender: ({ record }) => {
  //     let text;
  //     let color;
  //     if (!record.trainJob) {
  //       text = '未训练';
  //       color = 'gray';
  //     } else if (record.trainJob.status === 2) {
  //       text = '训练中';
  //       color = 'cyan';
  //     } else if (record.trainJob.status === 1) {
  //       text = '训练成功';
  //       color = 'green';
  //     } else if (record.trainJob.status === -1) {
  //       text = '训练失败';
  //       color = 'red';
  //     } else if (record.trainJob.status === -2) {
  //       text = '训练取消';
  //       color = 'yellow';
  //     }
  //     return h(Tag, { color: color }, () => text);
  //   },
  // },
  // {
  //   title: '质检状态',
  //   dataIndex: 'testJob',
  //   width: 100,
  //   sorter: true,
  //   customRender: ({ record }) => {
  //     let text;
  //     let color;
  //     if (!record.testJob) {
  //       text = '未质检';
  //       color = 'gray';
  //     } else if (record.testJob.status === 2) {
  //       text = '质检中';
  //       color = 'cyan';
  //     } else if (record.testJob.status === 1) {
  //       text = '质检成功';
  //       color = 'green';
  //     } else if (record.testJob.status === -1) {
  //       text = '质检失败';
  //       color = 'red';
  //     } else if (record.testJob.status === -2) {
  //       text = '质检取消';
  //       color = 'yellow';
  //     }
  //     return h(Tag, { color: color }, () => text);
  //   },
  // },
  // {
  //   title: '发布状态',
  //   dataIndex: 'isRelease',
  //   width: 80,
  //   customRender: ({ record }) => {
  //     let text;
  //     let color;
  //     if (record.isRelease) {
  //       text = '已发布';
  //       color = 'blue';
  //     } else {
  //       text = '未发布';
  //       color = 'gray';
  //     }
  //     return h(Tag, { color: color }, () => text);
  //   },
  // },
  {
    title: '状态',
    dataIndex: 'status',
    width: 40,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.status === 0) {
        text = '创建成功';
        color = 'green';
      } else if (record.status === 1) {
        text = '训练中';
        color = 'gray';
      } else if (record.status === 2) {
        text = '质检中';
        color = 'gray';
      } else if (record.status === 3) {
        text = '已停止';
        color = 'yellow';
      } else if (record.status === 4) {
        text = '训练失败';
        color = 'red';
      } else if (record.status === 5) {
        text = '质检失败';
        color = 'red';
      } else if (record.status === 6) {
        text = '训练完成';
        color = 'green';
      } else if (record.status === 7) {
        text = '质检完成';
        color = 'green';
      } else if (record.status === 8) {
        text = '作业完成';
        color = 'blue';
      } else if (record.status === 9) {
        text = '取消';
        color = 'yellow';
      } else if (record.status === 10) {
        text = '发布中';
        color = 'blue';
      } else if (record.status === 11) {
        text = '被打回';
        color = 'red';
      } else if (record.status === 14) {
        text = '发布完成';
        color = 'green';
      } else if (record.status === 15) {
        text = '转换完成';
        color = 'green';
      } else if (record.status === 16) {
        text = '转换失败';
        color = 'green';
      } else if (record.status === 17) {
        text = '转换中';
        color = 'blue';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 80,
    sorter: true,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { span: 5 },
  },
];
export const addFormSchema: FormSchema[] = [
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
    defaultValue: 1,
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
    colProps: { span: 25 },
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
  // {
  //   field: 'monitorType',
  //   label: '算法类型',
  //   component: 'Input',
  //   render: ({ model, field }) => {
  //     console.log(model[field]);
  //     let text;
  //     switch (model[field]) {
  //       case 1:
  //         text = '可见光';
  //         break;
  //       case 2:
  //         text = '红外';
  //         break;
  //       case 3:
  //         text = '三维';
  //         break;
  //       default:
  //         text = '无';
  //     }
  //     return h(Tag, { color: 'blue' }, () => text);
  //   },
  //   show: ({ values }) => {
  //     return values.isAddModel === 0;
  //   },
  // },
  // {
  //   field: 'datasetType',
  //   label: '数据类型',
  //   component: 'Input',
  //   render: ({ model, field }) => {
  //     return h(Tag, { color: 'blue' }, () => model[field] ?? '无');
  //   },
  //   show: ({ values }) => {
  //     return values.isAddModel === 0;
  //   },
  // },
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

export const reuseFormSchema: FormSchema[] = [
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
    show: false,
    componentProps: ({ formActionType, formModel }) => {
      return {
        options: [
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
    colProps: { span: 25 },
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
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
];
