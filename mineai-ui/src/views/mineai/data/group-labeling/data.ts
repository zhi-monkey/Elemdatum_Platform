import { BasicColumn, FormSchema } from '/@/components/Table';
import { formatDateTime } from '/@/utils';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export const columns: BasicColumn[] = [
  {
    title: '任务ID',
    dataIndex: 'id',
    width: 150,
    sorter: true,
  },
  {
    title: '任务名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '标注任务ID',
    dataIndex: 'datasetId',
    width: 150,
    sorter: true,
  },
  {
    title: '来源标注任务',
    dataIndex: 'datasetName',
    width: 150,
  },
  {
    title: '团队名称',
    dataIndex: 'teamName',
    width: 200,
  },
  {
    title: '标注类型',
    dataIndex: 'datasetType',
    width: 150,
    customRender: ({ record }) => {
      // 102：目标检测
      // 103：语义分割
      const annotateType = record.datasetType;
      let color = 'yellow';
      let text = '目标检测';
      if (annotateType === 103) {
        color = 'blue';
        text = '目标分割';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '任务状态',
    dataIndex: 'type',
    width: 200,
    customRender: ({ record }) => {
      const type = record.status;
      let color = 'gray';
      let text = '未标注';
      if (type === 1) {
        color = 'yellow';
        text = '标注中';
      } else if (type === 2) {
        color = 'green';
        text = '已完成';
      } else if (type === 3) {
        color = 'red';
        text = '已终止';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

export const workerColumns: BasicColumn[] = [
  {
    title: '任务ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 150,
    sorter: true,
  },
  {
    title: '任务名称',
    dataIndex: 'name',
    fixed: 'left',
    width: 150,
  },
  {
    title: '数据集名称',
    dataIndex: 'datasetName',
    fixed: 'left',
    width: 150,
  },
  {
    title: '任务接受者',
    dataIndex: 'userName',
    width: 200,
    ifShow: false,
  },
  {
    title: '已标注/待标注',
    dataIndex: 'process',
    width: 200,
  },
  {
    title: '标注进度',
    dataIndex: 'present',
    width: 200,
  },
  {
    title: '标注状态',
    dataIndex: 'status',
    width: 200,
    customRender: ({ record }) => {
      // 0：未标注
      // 1：标注中
      // 2：已提交
      // 3：已终止
      const status = record.status;
      let color = 'gray';
      let text = '未标注';
      switch (status) {
        case 1:
          color = 'yellow';
          text = '标注中';
          break;
        case 2:
          color = 'green';
          text = '已提交';
          break;
        case 3:
          color = 'red';
          text = '已终止';
          break;
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '任务发起人',
    dataIndex: 'createUserName',
    width: 200,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];
export const workerColumns_l: BasicColumn[] = [
  {
    title: '任务ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 150,
    sorter: true,
  },
  {
    title: '任务接受者',
    dataIndex: 'userName',
    width: 200,
  },
  {
    title: '已标注/待标注',
    dataIndex: 'process',
    width: 200,
  },
  {
    title: '标注进度',
    dataIndex: 'present',
    width: 200,
  },
  {
    title: '标注状态',
    dataIndex: 'status',
    width: 200,
    customRender: ({ record }) => {
      // 0：未标注
      // 1：标注中
      // 2：已提交
      // 3：已终止
      const status = record.status;
      let color = 'gray';
      let text = '未标注';
      switch (status) {
        case 1:
          color = 'yellow';
          text = '标注中';
          break;
        case 2:
          color = 'green';
          text = '已提交';
          break;
        case 3:
          color = 'red';
          text = '已终止';
          break;
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];
export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'name',
    label: '任务名称',
    component: 'Input',
    required: true,
    rules: [
      { required: true, message: '请输入任务名称' }, // 保留必填提示
    ],
    componentProps: {
      onChange: (e: any) => {
        // 静默移除所有空格
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue; // 直接修改输入框的值
      },
    },
  },
  {
    field: 'datasetGroup',
    label: '数据集组',
    component: 'ApiSelect',
    componentProps: ({ formActionType }) => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false,
          },
        },
        getPopupContainer: () => document.body,
        dropdownStyle: {
          maxHeight: '300px',
          zIndex: 2000,
        },
        dropdownMatchSelectWidth: false,
        api: async () => {
          return await maHttp.get(
            {
              url: 'datasets/group/getDatasetGroupsWithUnpublishedDatasets',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
        },
        labelField: 'name',
        valueField: 'id',
        immediate: true,
        onChange: async () => {
          // 当数据集组改变时，清空数据集选择
          const { setFieldsValue } = formActionType;
          await setFieldsValue({ dataset: undefined });
        },
      };
    },
    required: true,
  },
  {
    field: 'dataset',
    label: '数据集',
    component: 'ApiSelect',
    componentProps: ({ formModel }) => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        // 将下拉框渲染到body上, 防止被modal遮罩
        getPopupContainer: () => document.body,
        dropdownStyle: {
          maxHeight: '300px',
          zIndex: 2000, // 确保层级高于 modal
        },
        dropdownMatchSelectWidth: false,
        api: async () => {
          const datasetGroupId = formModel.datasetGroup;
          if (!datasetGroupId) {
            return [];
          }
          return await maHttp.get(
            {
              url: 'datasets/getUnpublishedDatasetsByGroupId',
              params: { datasetGroupId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
        },
        labelField: 'name',
        valueField: 'id',
        immediate: false,
      };
    },
    required: true,
    helpMessage: '请先选择数据集组',
  },
  {
    field: 'group',
    label: '标注团队',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      // 将下拉框渲染到body上, 防止被modal遮罩
      getPopupContainer: () => document.body,
      dropdownStyle: {
        maxHeight: '300px',
        zIndex: 2000, // 确保层级高于 modal
      },
      dropdownMatchSelectWidth: false,
      api: async () => {
        const v = await maHttp.get(
          {
            url: 'datasets/team',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        return v.result;
      },
      labelField: 'name',
      valueField: 'id',
      immediate: false,
    },
    required: true,
  },
  {
    field: 'otherAction',
    label: '其他操作',
    component: 'Input',
    slot: 'otherAction',
  },
];
