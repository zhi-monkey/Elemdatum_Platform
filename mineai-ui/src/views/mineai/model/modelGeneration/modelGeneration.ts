import {BasicColumn, FormSchema} from '/@/components/Table';
import {h, ref} from 'vue';
import {Popover, Tag} from 'ant-design-vue';
import {maHttp} from '/@/utils/http/axios';
import {MaBackendUrlEnum} from '/@/enums/mineaiEnum';

const generationName = ref('');

// 定义表格列
export const columns: BasicColumn[] = [
  {
    title: '任务名称',
    dataIndex: 'name',
    width: 80,
  },
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
    ifShow: false,
  },
  // {
  //   title: '训练数据集',
  //   dataIndex: 'trainDataset',
  //   width: 80,
  //   customRender: ({record}) => {
  //     let text;
  //     if (record.trainDataset) {
  //       var versionNameTmp = record.trainDataset.versionName;
  //       if(versionNameTmp.startsWith("V")|| versionNameTmp.startsWith("v")){
  //         text = record.trainDataset.name + '-' + record.trainDataset.versionName;
  //       }else{
  //         text = record.trainDataset.name + '-' + record.trainDataset.versionSource;
  //       }
  //     } else {
  //       text = '-';
  //     }
  //     return {children: text};
  //   },
  // },
  // {
  //   title: '质检数据集',
  //   dataIndex: 'testDataset',
  //   width: 80,
  //   customRender: ({record}) => {
  //     let text;
  //     if (record.testDataset) {
  //       var versionNameTmp = record.testDataset.versionName;
  //       if(versionNameTmp.startsWith("V")|| versionNameTmp.startsWith("v")){
  //         text = record.testDataset.name + '-' + record.testDataset.versionName;
  //       }else{
  //         text = record.testDataset.name + '-' + record.testDataset.versionSource;
  //       }
  //     } else {
  //       text = '-';
  //     }
  //     return {children: text};
  //   },
  // },
  {
    title: '算法名称',
    dataIndex: 'model',
    width: 90,
    customRender: ({record}) => {
      let text;
      if (record.model) {
        text = record.model.modelName;
      } else {
        text = '-';
      }
      return {children: text};
    },
  },
  // {
  //   title: '训练镜像',
  //   dataIndex: 'trainModelVersion',
  //   width: 50,
  //   sorter: true,
  //   customRender: ({record}) => {
  //     let text;
  //     let color;
  //     if (!record.trainModelVersion) {
  //       text = '未上传';
  //       color = 'yellow';
  //     } else {
  //       text = '已上传';
  //       color = 'green';
  //     }
  //     return h(Tag, {color: color}, () => text);
  //   },
  // },
  // {
  //   title: '质检镜像',
  //   dataIndex: 'testModelVersion',
  //   width: 50,
  //   sorter: true,
  //   customRender: ({record}) => {
  //     let text;
  //     let color;
  //     if (!record.testModelVersion) {
  //       text = '未上传';
  //       color = 'yellow';
  //     } else {
  //       text = '已上传';
  //       color = 'green';
  //     }
  //     return h(Tag, {color: color}, () => text);
  //   },
  // },
  // {
  //   title: '部署镜像',
  //   dataIndex: 'deployModelVersion',
  //   width: 50,
  //   sorter: true,
  //   customRender: ({record}) => {
  //     let text;
  //     let color;
  //     if (!record.deployModelVersion) {
  //       text = '未上传';
  //       color = 'yellow';
  //     } else {
  //       text = '已上传';
  //       color = 'green';
  //     }
  //     return h(Tag, {color: color}, () => text);
  //   },
  // },
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
    width: 80,
    customRender: ({record}) => {
      let text;
      let color;

      if (record.status === 1) {  // EXECUTING
        text = '执行中';
        color = 'blue';
      } else if (record.status === 2) {  // PAUSED
        text = '暂停';
        color = 'yellow';
      } else if (record.status === 3) {  // NOT_ACTIVE
        text = '未激活';
        color = 'gray';
      } else {
        text = '未知状态';
        color = 'red';
      }

      return h(Tag, {color: color}, () => text);
    },

  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 100,
    customRender: ({record}) => {
      let currentText: string = record.description;
      const currentContent: string = record.description;
      if (currentText?.length > 10) {
        currentText = currentText.substring(0, 10) + '...';
      }
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '训练时间上限',
    dataIndex: 'maxTrainTime',
    width: 85,
    customRender: ({record}) => {
      const maxTrainTimeWithUnit =
        record.maxTrainTime === 0 ? '无限制' : record.maxTrainTime + ' 分钟';

      return h(() => maxTrainTimeWithUnit);
    },
  },
  {
    title: '创建人',
    dataIndex: 'userName',
    width: 85,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 85,
    sorter: true,
  },
];

// 定义表格搜索条件
export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '任务名称',
    component: 'Input',
    colProps: {span: 10},
  },
];
//定义表格新增,编辑等操作
let modelList: any = [];
export const addFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: {span: 20},
  },
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
    defaultValue: 1,
    componentProps: ({formActionType, formModel}) => {
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
          const {resetFields, setFieldsValue} = formActionType;
          await resetFields();
          await setFieldsValue({name: generationName.value});
        },
      };
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    show: ({values}) => {
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
    show: ({values}) => {
      return values.isAddModel === 1;
    },
  },

  {
    field: 'description',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    show: ({values}) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({formActionType}) => {
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
                url: 'model/getUnpublishedModel',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              {urlPrefix: MaBackendUrlEnum.MODEL_MANAGER},
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
          const {setFieldsValue} = formActionType;
          setFieldsValue({...model});
        },
      };
    },
    ifShow: ({values}) => {
      return values.isAddModel === 0;
    },
    colProps: {span: 25},
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    required: true,
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    component: 'Input',
    required: true,
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
  // {
  //   field: 'imageCheck',
  //   label: '镜像选择',
  //   component: 'CheckboxGroup',
  //   defaultValue: [1],
  //   required: true,
  //   componentProps: () => {
  //     return {
  //       options: [
  //         { label: '训练', value: 1 },
  //         { label: '质检', value: 2 },
  //         { label: '推理', value: 3 },
  //       ],
  //     };
  //   },
  //   show: ({ values }) => {
  //     return values.isAddModel === 1;
  //   },
  // },
  {
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
  // {
  //   field: 'trainImageDescription',
  //   label: '训练镜像功能描述',
  //   component: 'Input',
  //   required: true,
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regLength = /^.{0,10}$/;
  //         if (!value) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('请输入内容');
  //         }
  //         if (!regLength.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入的长度必须小于或等于10');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(1);
  //   },
  // },
  // {
  //   field: 'trainImageUrl',
  //   label: '训练镜像Url地址填写',
  //   component: 'Input',
  //   required: true,
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(1);
  //   },
  // },
  // {
  //   field: 'testImageDescription',
  //   label: '质检镜像功能描述',
  //   required: true,
  //   component: 'Input',
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regLength = /^.{0,10}$/;
  //         if (!value) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('请输入内容');
  //         }
  //         if (!regLength.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入的长度必须小于或等于10');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(2);
  //   },
  // },
  // {
  //   field: 'testTrainImageUrl',
  //   label: '质检镜像Url地址填写',
  //   required: true,
  //   component: 'Input',
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(2);
  //   },
  // },
  // {
  //   field: 'deployImageDescription',
  //   label: '推理镜像功能描述',
  //   required: true,
  //   component: 'Input',
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regLength = /^.{0,10}$/;
  //         if (!value) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('请输入内容');
  //         }
  //         if (!regLength.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入的长度必须小于或等于10');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(3);
  //   },
  // },
  // {
  //   field: 'deployImageUrl',
  //   label: '推理镜像Url地址填写',
  //   required: true,
  //   component: 'Input',
  //   show: ({ values }) => {
  //     return values.isAddModel === 1 && values.imageCheck.includes(3);
  //   },
  // },
  // {
  //   field: 'trainDataset',
  //   label: '训练数据集',
  //   component: 'ApiSelect',
  //   componentProps: {
  //     api: () =>
  //       maHttp
  //         .get(
  //           {
  //             url: 'modelDataset/findAllDatasets',
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
  //     immediate: true,
  //     placeholder: '请选择数据集',
  //   },
  //   colProps: { span: 18 },
  //   required: true,
  // },
  // {
  //   field: 'datasetSource',
  //   label: '质检集来自训练集',
  //   component: 'RadioGroup',
  //   componentProps: {
  //     options: [
  //       {
  //         label: '是',
  //         value: 1,
  //       },
  //       {
  //         label: '否',
  //         value: 0,
  //       },
  //     ],
  //   },
  //   required: true,
  //   colProps: { span: 18 },
  // },
  // {
  //   field: 'trainSize',
  //   label: '训练用数据集占比',
  //   component: 'InputNumber',
  //   componentProps: {
  //     controls: false,
  //     min: 0,
  //     max: 1,
  //   },
  //   colProps: { span: 12 },
  // },
  // {
  //   field: 'testDataset',
  //   label: '质检数据集',
  //   component: 'ApiSelect',
  //   componentProps: {
  //     api: () =>
  //       maHttp
  //         .get(
  //           {
  //             url: 'modelDataset/findAllDatasets',
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
  //     immediate: true,
  //     placeholder: '请选择数据集',
  //   },
  //   colProps: { span: 18 },
  // },
  // {
  //   field: 'resource',
  //   label: '选择资源配置',
  //   component: 'ApiSelect',
  //   componentProps: {
  //     api: () =>
  //       maHttp.get(
  //         {
  //           url: 'modelGeneration/findAllHardwareParams',
  //           headers: {
  //             // @ts-ignore
  //             ignoreCancelToken: true,
  //           },
  //         },
  //         { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //       ),
  //     labelField: 'name',
  //     valueField: 'id',
  //     immediate: true,
  //     placeholder: '请选择资源配置',
  //   },
  //   colProps: { span: 18 },
  // },
];

export const reuseFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: {span: 20},
  },
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
    show: false,
    componentProps: ({formActionType, formModel}) => {
      return {
        options: [
          {
            label: '否',
            value: 0,
          },
        ],
        onChange: async () => {
          generationName.value = formModel.name;
          const {resetFields, setFieldsValue} = formActionType;
          await resetFields();
          await setFieldsValue({name: generationName.value});
        },
      };
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    show: ({values}) => {
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
    show: ({values}) => {
      return values.isAddModel === 1;
    },
  },

  {
    field: 'description',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    show: ({values}) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({formActionType}) => {
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
                url: 'model/getUnpublishedModel',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              {urlPrefix: MaBackendUrlEnum.MODEL_MANAGER},
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
          const {setFieldsValue} = formActionType;
          setFieldsValue({...model});
        },
      };
    },
    ifShow: ({values}) => {
      return values.isAddModel === 0;
    },
    colProps: {span: 25},
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    required: true,
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    component: 'Input',
    required: true,
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: {readonly: true},
    show: ({values}) => {
      return values.isAddModel === 0;
    },
  },
];

export const updateFormSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'Id',
    component: 'Input',
    required: true,
    show: false,
  },
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: {span: 20},
  },
  // {
  //   field: 'modelName',
  //   label: '算法名称',
  //   required: true,
  //   component: 'Input',
  // },
  // {
  //   field: 'modelEnglishName',
  //   label: '算法英文名称',
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regCpus = /^[a-z0-9]+$/;
  //         if (!value) {
  //           return Promise.resolve();
  //         }
  //         if (!regCpus.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('输入格式错误，请输入小写英文');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   required: true,
  //   component: 'Input',
  // },
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
              url: 'modelDataset/findAllDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            {urlPrefix: MaBackendUrlEnum.MODEL_MANAGER},
          )
          .then((v) => {
            return v;
          }),
      labelField: 'fullName',
      valueField: 'id',
      immediate: false,
    },
    required: true,
  },
  // {
  //   field: 'monitorType',
  //   label: '算法类型',
  //   required: true,
  //   component: 'RadioButtonGroup',
  //   componentProps: {
  //     options: [
  //       { label: '可见光', value: 1 },
  //       { label: '红外', value: 2, disabled: true },
  //       { label: '三维', value: 3, disabled: true },
  //     ],
  //   },
  // },
  // {
  //   field: 'datasetType',
  //   label: '数据类型',
  //   required: true,
  //   component: 'RadioButtonGroup',
  //   componentProps: {
  //     options: [
  //       { label: '图像', value: '图像' },
  //       { label: '视频', value: '视频' },
  //       { label: '音频', value: '音频' },
  //       { label: '点云', value: '点云' },
  //     ],
  //   },
  // },
  {
    field: 'trainSize',
    label: '训练用数据集占比',
    component: 'InputNumber',
    componentProps: {
      controls: false,
      min: 0,
      max: 1,
    },
    required: true,
  },
  // {
  //   field: 'description',
  //   label: '算法功能描述',
  //   component: 'Input',
  // },
];

export const datasetFormSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'Id',
    component: 'Input',
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
              url: 'modelDataset/findAllDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            {urlPrefix: MaBackendUrlEnum.MODEL_MANAGER},
          )
          .then((v) => {
            return v;
          }),
      labelField: 'fullName',
      valueField: 'id',
      immediate: false,
      placeholder: '请选择数据集',
    },
    required: true,
  },
  {
    field: 'trainSize',
    label: '训练用数据集占比',
    component: 'InputNumber',
    componentProps: {
      controls: false,
      min: 0,
      max: 1,
    },
    required: true,
  },
];

export const trainJobTimeSchema: FormSchema[] = [
  {
    field: 'trainTime',
    label: '训练等待时间(单位为分钟)',
    component: 'Input',
    required: false,
    defaultValue: 0,
    colProps: {span: 150},
  },
  {
    field: 'stopTime',
    label: '训练超时时间(单位为分钟)',
    component: 'Input',
    required: false,
    defaultValue: 0,
    colProps: {span: 100},
  },
];

export const columnsTwo: BasicColumn[] = [
  {
    title: 'Id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '创造时间',
    dataIndex: 'creatTime',
    width: 80,
  },
  {
    title: '结束时间',
    dataIndex: 'endTime',
    width: 80,
  },
  {
    title: '模型',
    dataIndex: 'creatTime',
    width: 80,
  },
];

export const subColumns: BasicColumn[] = [
  {
    title: '子列表ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '训练开始时间',
    dataIndex: 'startTime',
    width: 80,
  },
  {
    title: '训练完成时间',
    dataIndex: 'endTime',
    width: 80,
  },
  {
    title: '快照',
    dataIndex: 'photo',
    width: 0,
  },
];
