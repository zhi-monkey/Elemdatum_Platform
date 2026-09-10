import { BasicColumn, FormSchema } from '/@/components/Table';
import { h, ref } from 'vue';
import { Popover, Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { useUserStore } from '/@/store/modules/user';

interface data {
  label: string;
  value: string;
}

export const trainMVList = ref<data[]>([]);
export const deployMVList = ref<data[]>([]);
export const convertMVList = ref<data[]>([]);
export const columns: BasicColumn[] = [
  {
    title: '算法ID',
    dataIndex: 'id',
    width: 40,
    sorter: true,
  },
  {
    title: '算法名称',
    dataIndex: 'modelName',
    width: 80,
    customRender: ({ record }) => {
      const currentText = record.modelName;
      const currentContent = record.modelName;
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
    title: '算法别称',
    dataIndex: 'modelNickName',
    width: 80,
    customRender: ({ record }) => {
      const currentText = record.modelNickName;
      const currentContent = record.modelNickName;
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
    title: '算法描述',
    dataIndex: 'description',
    width: 100,
    customRender: ({ record }) => {
      const currentText = record.description;
      const currentContent = record.description;
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
    title: '算法用途',
    dataIndex: 'modelFunction',
    width: 80,
    align: 'center',
    customRender: ({ record }) => {
      const isTrainable = record.trainModelVersion;
      const isDeployable = record.deployModelVersion;
      const isConvertable = record.convertModelVersion;
      const isAutoLabel = record.trainModelVersion?.autoLabel;
      const tagList: any[] = [];
      if (isTrainable) tagList.push(h(Tag, { color: 'green' }, '训练'));
      if (isDeployable) tagList.push(h(Tag, { color: 'purple' }, '推理'));
      if (isConvertable) tagList.push(h(Tag, { color: 'blue' }, '转换'));
      if (isAutoLabel) tagList.push(h(Tag, { color: 'orange' }, '自动标注'));
      if (!(isTrainable || isDeployable || isConvertable || isAutoLabel))
        tagList.push(h(Tag, { color: 'gray' }, '无用途'));
      return h('div', { class: ['flex', 'flex-row', 'justify-center', 'gap-x-1'] }, tagList);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 80,
  },
  {
    title: '创建人',
    dataIndex: 'creator',
    width: 50,
    customRender: ({ record }) => {
      return record.creator.username;
    },
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '算法名称',
    dataIndex: 'modelName',
    width: 100,
    customRender: ({ record }) => {
      const currentText = record.modelName;
      const currentContent = record.modelName;
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
    title: '算法描述',
    dataIndex: 'description',
    width: 200,
    customRender: ({ record }) => {
      const currentText = record.description;
      const currentContent = record.description;
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
];
let modelList: any = [];

function setCurrentItem(e): any {
  const fieldNameList = [
    'modelName',
    // 'modelEnglishName',
    'modelNickName',
    'vagueInfo',
    //'monitorType',
    'description',
    'userName',
  ];

  const result: any[] = [];

  for (const item in fieldNameList) {
    if (e === fieldNameList[item]) {
      result.push({ field: fieldNameList[item], ifShow: true });
    } else {
      result.push({ field: fieldNameList[item], ifShow: false });
    }
  }

  return result;
}

export const searchFormSchema: FormSchema[] = [
  {
    field: 'select',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        defaultValue: 'vagueInfo',
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          // { label: '算法英文名称', value: 'modelEnglishName' },
          { label: '模糊查询', value: 'vagueInfo' },
          { label: '算法名称', value: 'modelName' },
          { label: '算法别称', value: 'modelNickName' },
          // { label: '算法类型', value: 'monitorType' },
          { label: '算法描述', value: 'description' },
          { label: '用户名', value: 'userName' },
        ],
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'vagueInfo',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ vagueInfo: filteredValue });
      },
    }),
  },
  {
    field: 'modelName',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ modelName: filteredValue });
      },
    }),
  },
  {
    field: 'modelNickName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ modelNickName: filteredValue });
      },
    }),
  },
  // {
  //   field: 'modelEnglishName',
  //   label: '',
  //   component: 'Input',
  //   ifShow: false,
  //   colProps: { span: 8 },
  // },
  // {
  //   field: 'monitorType',
  //   label: '',
  //   component: 'Select',
  //   componentProps: {
  //     options: [
  //       { label: '可见光', value: 1 },
  //       { label: '红外', value: 2 },
  //       { label: '三维', value: 3 },
  //     ],
  //   },
  //   ifShow: false,
  //   colProps: { span: 8 },
  // },
  {
    field: 'description',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ description: filteredValue });
      },
    }),
  },
  {
    field: 'userName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ userName: filteredValue });
      },
    }),
  },
];

export const simpleSearchFormSchema: FormSchema[] = [
  {
    field: '',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          { label: '算法名称', value: 'modelName' },
          {
            label: '算法描述',
            value: 'description',
          },
          { label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'modelName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'modelName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'description',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'vagueInfo',
    label: '',
    ifShow: false,
    component: 'Input',
    colProps: { span: 8 },
  },
];

/**
 *   负责新建/更新算法的 form表单
 * */
export const formSchema: FormSchema[] = [
  // {
  //   field: 'isAddModel',
  //   component: 'RadioButtonGroup',
  //   label: '新建算法',
  //   required: true,
  //   defaultValue: 1,
  //   colProps: { span: 18 },
  //   componentProps: ({ formActionType }) => {
  //     return {
  //       options: [
  //         {
  //           label: '是',
  //           value: 1,
  //         },
  //         {
  //           label: '否',
  //           value: 0,
  //         },
  //       ],
  //       onChange: async (e) => {
  //         const { updateSchema } = formActionType;
  //         await updateSchema({
  //           field: 'modelName',
  //           component: 'Input',
  //           label: '算法名称',
  //           //componentProps: { disabled: e === 0 },
  //         });
  //       },
  //     };
  //   },
  // },
  // 新增隐藏字段放是否是编辑弹窗
  {
    field: 'isUpdate',
    label: '',
    component: 'Input',
    show: false, // 永久隐藏
    defaultValue: false, // 默认新增模式
  },
  {
    field: 'model',
    component: 'ApiSelect',
    label: '选择已有算法',
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
            .then((result) => {
              modelList = result;
              return result;
            }),

        labelField: 'modelName',
        valueField: 'id',
        immediate: false,
        onChange: async (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          const { setFieldsValue } = formActionType;
          const imageList = [1];
          if (model.deployModelVersion != null) {
            await setFieldsValue({ deployImage: model.deployModelVersion.id });
            imageList.push(2);
          }
          if (model.convertModelVersion != null) {
            imageList.push(3);
            await setFieldsValue({ convertImage: model.convertModelVersion.id });
          }
          await setFieldsValue({
            ...model,
            imageCheck: imageList,
            trainImage: model.trainModelVersion?.id ?? null,
          });
        },
      };
    },
    required: true,
    ifShow: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ modelName: filteredValue });
      },
    }),
  },
  {
    field: 'modelNickName',
    label: '算法别称',
    required: true,
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ modelNickName: filteredValue });
      },
    }),
  },
  {
    field: 'description',
    label: '算法描述',
    required: true,
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ description: filteredValue });
      },
    }),
  },
  {
    field: 'imageCheck',
    label: '配置镜像',
    component: 'CheckboxGroup',
    defaultValue: [1],
    required: true,
    dynamicDisabled: ({ values }) => {
      return values.isUpdate;
    },
    componentProps: () => {
      return {
        options: [
          { label: '训练', value: 1, disabled: true },
          // { label: '推理', value: 2 },
          { label: '转换', value: 3 },
        ],
      };
    },
    // ifShow: ({ values }) => {
    //   return values.isAddModel === 1;
    // },
  },
  {
    field: 'trainImage',
    label: '训练镜像',
    required: true,
    component: 'Select',
    dynamicDisabled: ({ values }) => {
      return values.isUpdate;
    },
    componentProps: () => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        showSearch: true,
        options: trainMVList.value, // 使用ref类型变量动态赋值options属性时，必需使用箭头函数的形式返回配置信息
        colProps: { span: 4 },
        getPopupContainer: (triggerNode) => document.body,
      };
    },
  },
  {
    field: 'deployImage',
    label: '推理镜像',
    required: true,
    component: 'Select',
    dynamicDisabled: ({ values }) => {
      return values.isUpdate;
    },
    componentProps: () => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        showSearch: true,
        options: deployMVList.value, // 使用ref类型变量动态赋值options属性时，必需使用箭头函数的形式返回配置信息
        colProps: { span: 4 },
      };
    },
    ifShow: ({ values }) => {
      return values.imageCheck.includes(2);
    },
  },
  {
    field: 'convertImage',
    label: '转换镜像',
    required: true,
    component: 'Select',
    dynamicDisabled: ({ values }) => {
      return values.isUpdate;
    },
    componentProps: () => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        showSearch: true,
        options: convertMVList.value,
        colProps: { span: 4 },
      };
    },
    ifShow: ({ values }) => {
      return values.imageCheck.includes(3);
    },
  },
];
const userStore = useUserStore();
const userData = userStore.getUserInfo;

export async function getTrainMVList() {
  await maHttp
    .get(
      {
        url: 'modelVersion/modelversions/train?userId=' + userData.id,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((trainList) => {
      const trainModelVersionList: data[] = [];

      trainList.forEach((v) => {
        trainModelVersionList.push({ label: v.name, value: v.id });
      });
      trainMVList.value = trainModelVersionList;
    });
}

export async function getDeployMVList() {
  await maHttp
    .get(
      {
        url: 'modelVersion/modelversions/deploy?userId=' + userData.id,
        params: {},
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((deployList) => {
      const deployModelVersionList: data[] = [];

      deployList.forEach((v) => {
        deployModelVersionList.push({ label: v.name, value: v.id });
      });
      deployMVList.value = deployModelVersionList;
    });
}

export async function getConvertMVList() {
  await maHttp
    .get(
      {
        url: 'modelVersion/modelversions/conversion?userId=' + userData.id,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((convertList) => {
      const convertModelVersionList: data[] = [];

      convertList.forEach((v) => {
        convertModelVersionList.push({ label: v.name, value: v.id });
      });
      convertMVList.value = convertModelVersionList;
    });
}
