import { BasicColumn, FormSchema } from '/@/components/Table';
//定义表格列
export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'classificationName',
    sorter: true,
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 85,
    sorter: true,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 85,
    sorter: true,
  },
];
//定义表格搜索条件
// export const searchFormSchema: FormSchema[] = [
//   {
//     field: 'name',
//     label: '名称',
//     component: 'Input',
//     colProps: { span: 4 },
//   },
// ];
//定义表格新增,编辑等操作
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    show: false,
  },
  {
    field: 'classificationName',
    label: '名称',
    component: 'Input',
    required: true,
  },
];
