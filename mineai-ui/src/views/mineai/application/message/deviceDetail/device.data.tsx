import { BasicColumn, FormSchema } from '/@/components/Table';
import { trainMVList } from '/@/views/mineai/model/algorithmManage/algorithmList/algorithm.data'; // import { h } from 'vue';
// import { h } from 'vue';
// import { Popover } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ElMessage } from 'element-plus'; // 查询所有芯片类型

export const formSchema: FormSchema[] = [
  { field: 'id', label: 'id', component: 'Input', colProps: { span: 8 }, show: false },
  {
    field: 'deviceName',
    label: '设备名称',
    required: true,
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入设备名称');
          }
          // 如果名称前后带空格则不能过校验
          if (value.trim() !== value) {
            return Promise.reject('设备名称前后不能带空格');
          }
          if (value.length > 20) {
            return Promise.reject('设备名称长度应小于20');
          }
          return Promise.resolve();
        },
      },
    ],
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        let filteredValue = e.target.value.replace(/\s+/g, '');
        if (filteredValue.includes('-')) {
          filteredValue = filteredValue.replace(/-/g, '_'); // 替换 - 为 _
          ElMessage.warning('设备名称不能包含"-"，已自动替换为"_"');
        }
        e.target.value = filteredValue;
        await setFieldsValue({ deviceName: filteredValue });
      },
    }),
  },
  {
    field: 'firmwareVersion',
    label: '固件版本',
    required: true,
    rules: [
      {
        required: true,
        validator: (rule, value) => {
          if (!value) {
            return Promise.reject('请输入固件版本');
          }
          // 如果名称前后带空格则不能过校验
          if (value.trim() !== value) {
            return Promise.reject('版本名称前后不能带空格');
          }
          if (value.length > 20) {
            return Promise.reject('固件版本长度应小于20');
          }
          return Promise.resolve();
        },
      },
    ],
    component: 'Input',
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        let filteredValue = e.target.value.replace(/\s+/g, '');
        if (filteredValue.includes('-')) {
          filteredValue = filteredValue.replace(/-/g, '_'); // 替换 - 为 _
          ElMessage.warning('固件版本不能包含"-"，已自动替换为"_"');
        }
        e.target.value = filteredValue;
        await setFieldsValue({ firmwareVersion: filteredValue });
      },
    }),
  },
  {
    field: 'chipType',
    label: '算力芯片类型',
    required: true,
    component: 'ApiSelect',
    componentProps: {
      showSearch: true,
      api: async () => {
        const d = await maHttp.get(
          {
            url: '/chips/findAll',
            method: 'GET',
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
        return d.map((chip) => ({
          label: chip.chipType,
          value: chip.chipType,
        }));
      },
      // placeholder: '请选择芯片类型',
      labelFiled: 'label',
      valueFiled: 'label',
      immediate: true,
      getPopupContainer: (triggerNode) => document.body,
    },
  },
];

export const deviceColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
  },
  {
    title: '设备名称',
    dataIndex: 'deviceName',
  },
  {
    title: '固件版本',
    dataIndex: 'firmwareVersion',
  },
  {
    title: '算力芯片',
    dataIndex: 'chip.chipType',
  },
];

function setCurrentItem(e): any {
  const fieldNameList = ['deviceName', 'firmwareVersion', 'chipType'];

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
          { label: '设备名称', value: 'deviceName' },
          { label: '固件版本', value: 'firmwareVersion' },
          { label: '算力芯片', value: 'chipType' },
        ],
        defaultValue: 'deviceName',
      };
    },
    colProps: { span: 6 },
  },

  {
    field: 'deviceName',
    label: '',
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
          await setFieldsValue({ deviceName: filteredValue });
        },
      };
    },
    // ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'firmwareVersion',
    label: '',
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
          await setFieldsValue({ firmwareVersion: filteredValue });
        },
      };
    },
    ifShow: false,
    colProps: { span: 8 },
  },
  {
    field: 'chipType',
    label: '',
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
          await setFieldsValue({ chipType: filteredValue });
        },
      };
    },
    ifShow: false,
    colProps: { span: 8 },
  },
];
