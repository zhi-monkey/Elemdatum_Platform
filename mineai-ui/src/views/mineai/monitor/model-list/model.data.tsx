import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

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
  },
  {
    title: '应用场景',
    dataIndex: 'modelClassification',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      const tagList: any[] = [];
      record.modelClassification.forEach((item, index) => {
        const i = index % 4;
        const color = i === 0 ? 'green' : i === 1 ? 'blue' : i === 2 ? 'purple' : 'orange';
        tagList.push(h(Tag, { color: color }, item.classificationName));
      });
      if (tagList.length <= 0) tagList.push(h(Tag, { color: 'gray' }, '无场景'));
      return h('div', { class: ['flex', 'flex-row', 'justify-center', 'gap-x-1'] }, tagList);
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
    title: '算法来源',
    dataIndex: 'source',
    width: 80,
    customRender: ({ record }) => {
      let source = '';
      if (record.source === 1) {
        source = '上传';
        return source;
      }
      if (record.source === 2) {
        source = '标准化发布';
        return source;
      }
      if (record.source === 3) {
        source = '引导式';
        return source;
      }
    },
  },
  {
    title: '发布列表ID',
    dataIndex: 'generationId',
    width: 60,
    customRender: ({ record }) => {
      return record.source === 1 ? '无' : record.generationId;
    },
  },
  {
    title: '发布时间',
    dataIndex: 'releaseTime',
    width: 80,
    customRender: ({ record }) => {
      return record.source === 1 ? '无' : record.releaseTime;
    },
  },
];

export const simpleColumns: BasicColumn[] = [
  {
    title: '算法名称',
    dataIndex: 'modelName',
    width: 100,
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

function setCurrentItem(e): any {
  const fieldNameList = [
    'modelName',
    'modelEnglishName',
    'vagueInfo',
    'monitorType',
    'description',
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
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          { label: '算法英文名称', value: 'modelEnglishName' },
          { label: '算法名称', value: 'modelName' },
          { label: '算法类型', value: 'monitorType' },
          { label: '算法描述', value: 'description' },
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
    colProps: { span: 8 },
  },
  {
    field: 'modelEnglishName',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'monitorType',
    label: '',
    component: 'Select',
    componentProps: {
      options: [
        { label: '可见光', value: 1 },
        { label: '红外', value: 2 },
        { label: '三维', value: 3 },
      ],
    },
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

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    required: true,
    component: 'Input',
  },
  {
    field: 'monitorType',
    label: '监控类型',
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
  },
  {
    field: 'modelClassification',
    label: '应用场景',
    component: 'ApiSelect',
    componentProps: {
      placeholder: '请选择算法应用场景',
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      mode: 'multiple',
      //api接口请求，获取model列表
      api: () =>
        maHttp.get(
          {
            url: 'modelClassifications',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        ),
      labelField: 'classificationName',
      valueField: 'id',
      immediate: true,
    },
    required: true,
  },
  {
    field: 'description',
    label: '算法描述',
    required: true,
    component: 'Input',
  },
  {
    field: 'source',
    label: '算法来源',
    required: true,
    defaultValue: 1,
    component: 'RadioButtonGroup',
    componentProps: {
      options: [{ label: '上传', value: 1 }],
    },
  },
];
