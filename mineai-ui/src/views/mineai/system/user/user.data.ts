import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Popover, Tag } from 'ant-design-vue';
import { getAll } from '/@/views/mineai/system/api/role';
import { formatDateTime } from '/@/utils';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
import { maHttp } from '/@/utils/http/axios';

export const columns: BasicColumn[] = [
  {
    title: '用户ID',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '用户名',
    dataIndex: 'username',
    width: 160,
    ellipsis: true,
    customRender: ({ record }) => {
      const currentText = record.username;
      const currentContent = record.username;
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
    title: '昵称',
    dataIndex: 'nickName',
    width: 100,
    ellipsis: true,
    customRender: ({ record }) => {
      const currentText = record.nickName;
      const currentContent = record.nickName;
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
    title: '电话',
    dataIndex: 'phone',
    width: 120,
    customRender: ({ record }) => {
      const currentText = record.phone;
      const currentContent = record.phone;
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
    title: '邮箱',
    dataIndex: 'email',
    width: 180,
    customRender: ({ record }) => {
      const currentText = record.email;
      const currentContent = record.email;
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
    title: '性别',
    dataIndex: 'sex',
    width: 60,
    customRender: ({ record }) => {
      const color = record.sex === '男' ? 'blue' : 'purple';
      return h(
        Tag,
        { color: color },
        {
          default: () => record.sex,
        },
      );
    },
  },
  {
    title: '状态',
    dataIndex: 'enabled',
    width: 60,
    customRender: ({ record }) => {
      const enable = record.enabled;
      const color = enable ? 'green' : 'red';
      const text = enable ? '启用' : '停用';
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '用户备注信息',
    dataIndex: 'remark',
    width: 200,
    ellipsis: true,
    customRender: ({ record }) => {
      const currentText = record.remark;
      const currentContent = record.remark;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
          pointAtCenter: true,
          overlayStyle: { width: 'auto', minWidth: '100px' },
        },
        () => currentText,
      );
    },
  },
  {
    title: '角色',
    dataIndex: 'roles',
    width: 160,
    ellipsis: true,
    customRender: ({ record }) => {
      const roleName = record.roles.length > 0 ? record.roles[0].name : '无';
      return h(Tag, { color: 'cyan' }, () => roleName);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'blurry',
    label: '用户名',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ blurry: filteredValue });
      },
    }),
  },
  {
    field: 'enabled',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '启用', value: 'true' },
        { label: '停用', value: 'false' },
      ],
    },
    colProps: { xl: 12, xxl: 4 },
  },
  {
    field: 'roleId',
    label: '角色',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        getAll().then((v) => {
          return v;
        }),
      labelField: 'name',
      valueField: 'id',
      immediate: true,
    },
    colProps: { xl: 12, xxl: 6 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '用户ID',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'username',
    label: '用户名',
    component: 'Input',
    rules: [
      {
        required: true,
        validator: async (_rule, value) => {
          // 空值校验（包括null/undefined/空字符串）
          if (!value) {
            return Promise.reject('请输入用户名');
          }

          // 去除首尾空格（处理多个空格情况）
          const trimmedValue = value.trim();

          // 纯空格校验（处理任意数量空格的情况）
          if (trimmedValue === '') {
            return Promise.reject('用户名不能全为空格');
          }

          // 前后空格校验（处理包含有效字符但带空格的情况）
          if (value !== trimmedValue) {
            return Promise.reject('用户名前后不能包含空格');
          }

          // 有效长度校验（基于去除空格后的实际内容）
          if (trimmedValue.length > 20) {
            return Promise.reject('用户名长度不能超过20位');
          }

          return Promise.resolve();
        },
      },
    ],
  },
  {
    field: 'nickName',
    label: '昵称',
    component: 'Input',
    rules: [
      {
        required: true,
        validator: async (_rule, value) => {
          if (!value) {
            /* eslint-disable-next-line */
            return Promise.reject('请输入昵称');
          }
          if (value.length > 20) {
            return Promise.reject('昵称长度不能超过20位');
          }
          return Promise.resolve();
        },
      },
    ],
  },
  {
    field: 'phone',
    label: '电话',
    rules: [
      {
        required: true,
        validator: async (_rule, value) => {
          const regEmails =
            /^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\d{8}$/;
          if (!value) {
            /* eslint-disable-next-line */
            return Promise.reject('请输入手机号');
          }
          if (!regEmails.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('手机号格式错误');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    component: 'Input',
  },
  {
    field: 'email',
    label: '邮箱',
    rules: [
      {
        required: true,
        validator: async (_rule, value) => {
          const regEmails = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
          if (!value) {
            /* eslint-disable-next-line */
            return Promise.reject('请输入邮箱');
          }
          if (!regEmails.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('邮箱格式错误');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    component: 'Input',
  },
  {
    field: 'sex',
    label: '性别',
    component: 'RadioButtonGroup',
    defaultValue: '男',
    required: true,
    componentProps: {
      options: [
        { label: '男', value: '男' },
        { label: '女', value: '女' },
      ],
    },
  },
  {
    field: 'enabled',
    label: '状态',
    component: 'RadioButtonGroup',
    defaultValue: true,
    required: true,
    componentProps: {
      options: [
        { label: '启用', value: true },
        { label: '停用', value: false },
      ],
    },
  },
  {
    label: '备注',
    field: 'remark',
    component: 'InputTextArea',
    rules: [
      {
        validator: async (_rule, value) => {
          if (value && value.length > 45) {
            return Promise.reject('备注长度不能超过45位');
          }
          if (!value) {
            return Promise.resolve();
          }
        },
        trigger: 'change',
      },
    ],
  },
  {
    field: 'roles',
    label: '角色',
    component: 'Input',
    slot: 'roles',
    required: true,
  },
  {
    field: 'departmentId',
    label: '部门',
    component: 'ApiSelect',
    componentProps: () => ({
      dropdownAlign: {
        overflow: {
          adjustY: true, // 关闭下拉框垂直位置自适应
        },
      },
      mode: 'single',
      api: async () => {
        // const department = formModel.department; // 从表单模型中获取部门ID
        // const isUpdate = !!department; // 判断是否为编辑模式
        // const url = isUpdate
        //   ? `departments/${departmentId}/users` // 编辑模式：查询当前部门的和未分配的用户
        //   : 'departments/unassigned-users'; // 新增模式：查询未分配部门的用户
        const url = `departments/all`;
        return await maHttp.get(
          {
            url,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
        );
      },
      labelField: 'name',
      valueField: 'id',
      immediate: true,
    }),
  },
];
