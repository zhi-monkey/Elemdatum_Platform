import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';

export const columns: BasicColumn[] = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 40,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 60,
    sorter: true,
  },
  {
    title: '训练数据集',
    dataIndex: 'dataset',
    width: 80,
  },
  {
    title: '对应算法',
    dataIndex: 'algorithm',
    width: 80,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    align: 'center',
    customRender: ({ record }) => {
      const status = record.status;
      let tagColor = '';
      let tagText = '';

      // 根据状态值设置不同的标签颜色和文本
      switch (status) {
        case 'collecting':
          tagColor = 'blue';
          tagText = '数据收集中';
          break;
        case 'waitingForApproval':
          tagColor = 'orange';
          tagText = '等待审核中';
          break;
        case 'training':
          tagColor = 'green';
          tagText = '模型训练';
          break;
        case 'testing':
          tagColor = 'teal';
          tagText = '模型测试';
          break;
        case 'converting':
          tagColor = 'yellow';
          tagText = '模型转换';
          break;
        case 'waitingToBeIssued':
          tagColor = 'orange';
          tagText = '等待下发';
          break;
        case 'completed':
          tagColor = 'purple';
          tagText = '已完成';
          break;
        default:
          tagColor = 'gray';
          tagText = '未知状态';
          break;
      }

      // 返回渲染的标签
      return h('div', { class: ['flex', 'flex-row', 'justify-center', 'gap-x-1'] }, [
        h(Tag, { color: tagColor }, tagText),
      ]);
    },
  },

  {
    title: '创建时间',
    dataIndex: 'time',
    width: 80,
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

export const searchFormSchema: FormSchema[] = [
  {
    field: 'select',
    label: '筛选项',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        onChange: () => {
          const { resetFields } = formActionType;
          //updateSchema(setCurrentItem(e));
          resetFields();
        },
        options: [
          { label: '名称', value: 'name' },
          { label: '训练数据集', value: 'dataset' },
          { label: '对应算法', value: 'algorithm' },
          { label: '状态', value: 'status' },
        ],
        defaultValue: 'name',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'name',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'dataset',
    label: '',
    component: 'Input',
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'algorithm',
    label: '',
    component: 'Input',
    colProps: { span: 8 },
    ifShow: false,
  },
  {
    field: 'status',
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
        onChange: () => {
          const { resetFields } = formActionType;
          // updateSchema(setCurrentItem(e));
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
