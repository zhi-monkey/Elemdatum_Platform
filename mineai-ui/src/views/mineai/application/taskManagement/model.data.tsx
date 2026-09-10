import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h } from 'vue';
import { Tag as ATag, Popover as APopover } from 'ant-design-vue';

// 表格列的定义
export const columns: BasicColumn[] = [
  {
    title: '应用任务ID',
    dataIndex: 'id',
    width: 100,
    sorter: true,
    customRender: ({ record }) => {
      const formattedId = `A${record.id.toString().padStart(8, '0')}`;
      return formattedId;
    },
  },
  {
    title: '应用任务名称',
    dataIndex: 'applicationTaskName',
    width: 120,
  },
  {
    title: '应用名称',
    dataIndex: 'applicationName.applicationName',
    width: 80,
    ifShow: false,
  },
  {
    title: '适用场景',
    dataIndex: 'allApplicableSceneName',
    width: 100,
  },
  {
    title: '设备-版本',
    dataIndex: 'device.deviceAndFirmwareName',
    width: 120,
  },
  {
    title: '绑定算法',
    dataIndex: 'model.modelName',
    width: 100,
  },
  {
    title: '标签信息',
    dataIndex: 'labels',
    width: 100,
    align: 'center',
    customRender: ({ record }) => {
      const labelCountMap = record.labels;
      if (!labelCountMap || labelCountMap.length === 0) {
        return h('span', { class: 'no-tags' }, '-');
      }

      const visibleLabels = labelCountMap.slice(0, 2);
      const hasMoreTags = labelCountMap.length > 2;

      return h(
        'div',
        {
          class: 'tags-container',
          style: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' },
        },
        [
          ...visibleLabels.map((label) =>
            h(
              ATag,
              {
                key: label.name,
                color: label.color,
                class: 'tag-item',
              },
              () => label.name,
            ),
          ),

          hasMoreTags && h('span', { class: 'tags-ellipsis' }, '...'),

          h(
            APopover,
            {
              placement: 'bottomLeft',
              trigger: 'hover',
              overlayClassName: 'tags-popover-custom',
            },
            {
              content: () =>
                h('div', { class: 'popover-tags-content' }, [
                  h('div', { class: 'popover-title' }, '所有标签：'),
                  h('div', { class: 'popover-stats' }, [
                    h('div', { class: 'stat-item' }, [
                      h('span', { class: 'stat-label' }, '标签种类：'),
                      h('span', { class: 'stat-value' }, `${labelCountMap.length} 种`),
                    ]),
                  ]),
                  h(
                    'div',
                    { class: 'popover-tags-list' },
                    labelCountMap.map((label) =>
                      h(
                        'div',
                        {
                          key: label.name,
                          class: 'popover-tag-item-wrapper',
                        },
                        [
                          h(
                            ATag,
                            {
                              color: label.color,
                              class: 'popover-tag-item',
                            },
                            () => label.name,
                          ),
                        ],
                      ),
                    ),
                  ),
                ]),
              default: () =>
                h(
                  'span',
                  {
                    class: 'show-all-tags-trigger',
                    style: { cursor: 'pointer', color: '#1890ff', fontSize: '12px' },
                  },
                  '查看详情',
                ),
            },
          ),
        ],
      );
    },
  },
  {
    title: '发布状态',
    dataIndex: 'isReleased',
    width: 80,
  },
  {
    title: '应用描述',
    dataIndex: 'description',
    width: 80,
    ifShow: false,
  },
];

// 搜索表单字段定义
export const searchFormSchema: FormSchema[] = [
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
          { label: '应用任务名称', value: 'applicationTaskName' },
          { label: '适用场景', value: 'scenes' },
          { label: '设备固件', value: 'deviceFirmWare' },
          { label: '绑定算法', value: 'modelId' },
          //{ label: '模糊查询', value: 'vagueInfo' },
        ],
        defaultValue: 'applicationTaskName',
      };
    },
    colProps: { span: 6 },
  },
  {
    field: 'applicationTaskName',
    label: '',
    ifShow: true,
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ applicationTaskName: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
  {
    field: 'scenes',
    label: '',
    ifShow: false,
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ scenes: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
  {
    field: 'deviceFirmWare',
    label: '',
    ifShow: false,
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ deviceFirmWare: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
  {
    field: 'modelId',
    label: '',
    ifShow: false,
    component: 'Input',
    componentProps: ({ formActionType }) => {
      return {
        onChange: async (e) => {
          const { setFieldsValue } = formActionType;

          // 获取当前输入框的值
          const currentValue = e.target.value;

          // 过滤掉所有空格
          const filteredValue = currentValue.replace(/\s+/g, '');

          // 更新输入框的值（直接操作 DOM）
          e.target.value = filteredValue;

          // 同步更新表单字段的值
          await setFieldsValue({ modelId: filteredValue });
        },
      };
    },
    colProps: { span: 8 },
  },
  // {
  //   field: 'vagueInfo',
  //   label: '',
  //   component: 'Input',
  //   ifShow: false,
  //   colProps: { span: 8 },
  // },
];

function setCurrentItem(e): any {
  const fieldNameList = [
    'applicationTaskName',
    'scenes',
    'deviceFirmWare',
    'modelId' /*'vagueInfo'*/,
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

// 表单字段定义
export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'applicationTaskName',
    label: '任务名称',
    required: true,
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入任务名称');
          }
          // 如果名称前后带空格则不能过校验
          if (value.trim() !== value) {
            return Promise.reject('任务名称前后不能带空格');
          }
          if (value.length > 20) {
            return Promise.reject('任务名称长度应小于20');
          }
          return Promise.resolve();
        },
      },
    ],
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ applicationTaskName: filteredValue });
      },
    }),
  },
  {
    field: 'applicationNameId',
    label: '应用名称',
    required: true,
    component: 'ApiSelect',
    componentProps: () => ({
      dropdownAlign: {
        overflow: {
          adjustY: true, // 关闭下拉框垂直位置自适应
        },
      },
      api: async () => {
        const url = `applicationName/findAll`;
        return await maHttp.get(
          {
            url,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'applicationName',
      valueField: 'id',
      immediate: true,
    }),
  },
  {
    field: 'description',
    label: '应用描述',
    required: true,
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入应用描述');
          }
          // 如果名称前后带空格则不能过校验
          if (value.trim() !== value) {
            return Promise.reject('应用描述前后不能带空格');
          }
          if (value.length > 30) {
            return Promise.reject('应用描述长度应小于30');
          }
          return Promise.resolve();
        },
      },
    ],
    component: 'Input',
  },
  {
    field: 'scenes',
    label: '适用场景',
    required: true,
    component: 'ApiSelect',
    componentProps: () => ({
      dropdownAlign: {
        overflow: {
          adjustY: true,
        },
      },
      mode: 'multiple',
      api: async () => {
        const url = `scene/findAllActive`;
        return await maHttp.get(
          {
            url,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'name',
      valueField: 'id',
      immediate: true,
    }),
  },
  {
    field: 'deviceFirmWare',
    component: 'InputGroup',
    label: '设备固件',
    slot: 'deviceFirmWare',
    required: true,
  },

  {
    field: 'modelId',
    label: '绑定算法',
    required: true,
    component: 'ApiSelect',
    componentProps: () => ({
      dropdownAlign: {
        overflow: {
          adjustY: true,
        },
      },
      api: async () => {
        const url = `model/getAllModelInfo`;
        return await maHttp.get(
          {
            url,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
      },
      labelField: 'modelName',
      valueField: 'id',
      immediate: true,
    }),
  },
];

export const LabelInfoColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 100,
    sorter: (a, b) => a.id - b.id,
    defaultSortOrder: 'ascend',
  },
  {
    title: '标签名称',
    dataIndex: 'name',
    key: 'name',
    ellipsis: true,
  },
  {
    title: '颜色标识',
    key: 'color',
    width: 200,
  },
];
