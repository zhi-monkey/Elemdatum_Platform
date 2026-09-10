import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
//定义表格列
export const columns: BasicColumn[] = [
  {
    title: '作业ID',
    dataIndex: 'id',
    width: 50,
    sorter: true,
  },
  {
    title: '算法名称',
    dataIndex: 'modelVersion.model.modelName',
    sorter: true,
    width: 60,
  },
  {
    title: '算法版本',
    dataIndex: 'modelVersion.showName',
    width: 150,
    sorter: true,
  },
  {
    title: '作业类型',
    dataIndex: 'jobType',
    width: 60,
    sorter: true,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.jobType === 1) {
        text = '模型训练';
        color = 'cyan';
      }
      if (record.jobType === 2) {
        text = '模型质检';
        color = 'purple';
      }
      if (record.jobType === 3) {
        text = '模型转换';
        color = 'blue';
      }

      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '作业状态',
    dataIndex: 'status',
    width: 60,
    sorter: true,
    customRender: ({ record }) => {
      let text;
      let color;
      if (record.status === 2) {
        text = '正在进行';
        color = 'cyan';
      }
      if (record.status === 1) {
        text = '执行成功';
        color = 'green';
      }
      if (record.status === -1) {
        text = '执行失败';
        color = 'red';
      }
      if (record.status === -2) {
        text = '作业取消';
        color = 'yellow';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: 'CPU数量',
    dataIndex: 'cpus',
    width: 80,
    customRender: ({ record }) => {
      const cpus = record.cpus;
      let fileSizeString = '';
      if (cpus == '无限制') {
        fileSizeString = cpus;
      } else fileSizeString = cpus + '核';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },
  {
    title: 'GPU显存',
    dataIndex: 'gpus',
    width: 80,
    customRender: ({ record }) => {
      const gpus = record.gpus;
      let fileSizeString = '';
      if (gpus == '未分配') {
        fileSizeString = '未分配';
      } else fileSizeString = gpus + 'GiB';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },
  {
    title: '内存大小',
    dataIndex: 'memory',
    width: 80,
    customRender: ({ record }) => {
      const memory = record.memory;
      let fileSizeString = '';
      if (memory == '无限制') {
        fileSizeString = memory;
      } else fileSizeString = memory + 'B';
      return h(Tag, () => fileSizeString);
    },
    sorter: true,
  },
  {
    title: '数据集',
    dataIndex: 'dataset.name',
    width: 80,
    sorter: true,
  },
  {
    title: '作业信息描述',
    dataIndex: 'description',
    width: 100,
    sorter: true,
  },
  {
    title: '开始时间',
    dataIndex: 'createTime',
    width: 100,
    sorter: true,
  },
];
//定义表格搜索条件
export const searchFormSchema: FormSchema[] = [
  {
    field: 'model',
    label: '算法名称',
    component: 'ApiSelect',
    colProps: { span: 4 },
    //根据模型获取对应的模型版本
    componentProps: ({ formActionType }) => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        api: () =>
          maHttp.get(
            {
              url: 'model/getModel',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          ),
        labelField: 'modelName',
        valueField: 'id',
        immediate: false,
        onChange: (e) => {
          const { updateSchema, resetFields } = formActionType;
          updateSchema({
            field: 'modelVersion',
            label: '算法版本',
            component: 'ApiSelect',
            colProps: { span: 4 },
            componentProps: {
              dropdownAlign: {
                overflow: {
                  adjustY: false, // 关闭下拉框垂直位置自适应
                },
              },
              api: () =>
                maHttp.get(
                  {
                    url: 'modelVersion/getModelVersionsByModel',
                    params: { modelId: e },
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
            },
          });
          resetFields();
        },
      };
    },
  },
  {
    field: 'modelVersion',
    label: '算法版本',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
    },
    colProps: { span: 4 },
  },
  {
    field: 'jobType',
    label: '作业类型',
    component: 'Select',
    componentProps: {
      options: [
        { label: '质检作业', value: 2 },
        { label: '训练作业', value: 1 },
      ],
    },
    colProps: { span: 4 },
  },
  {
    field: 'status',
    label: '作业状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '正在进行', value: 2 },
        { label: '执行成功', value: 1 },
        { label: '执行失败', value: -1 },
        { label: '作业取消', value: -2 },
      ],
    },
    colProps: { span: 4 },
  },
];
//定义表格新增,编辑等操作
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '作业ID',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  // {
  //   field: 'jobType',
  //   label: '作业类型',
  //   component: 'Select',
  //   componentProps: {
  //     options: [
  //       { label: '质检作业', value: 2 },
  //       { label: '训练作业', value: 1 },
  //     ],
  //   },
  //   required: true,
  // },
  // {
  //   field: 'status',
  //   label: '作业状态',
  //   component: 'Select',
  //   componentProps: {
  //     options: [
  //       { label: '正在进行', value: 2 },
  //       { label: '执行成功', value: 1 },
  //       { label: '执行失败', value: -1 },
  //       { label: '取消作业', value: -2 },
  //     ],
  //   },
  //   required: true,
  // },
  {
    field: 'description',
    label: '作业描述',
    component: 'InputTextArea',
    required: false,
  },
];
