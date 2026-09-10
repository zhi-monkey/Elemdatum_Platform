import { FormSchema } from '/@/components/Form';

// 个人信息 form
export const userschemas: FormSchema[] = [
  {
    field: 'id',
    label: '用户ID',
    component: 'Input',
    colProps: { span: 15 },
    dynamicDisabled: true,
    componentProps: {
      readonly: true,
    },
  },
  {
    field: 'username',
    label: '用户名',
    component: 'Input',
    colProps: { span: 15 },
    dynamicDisabled: true,
    componentProps: {
      readonly: true,
    },
  },
  {
    field: 'nickName',
    component: 'Input',
    label: '昵称',
    colProps: { span: 15 },
    required: true,
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
    field: 'phone',
    label: '电话',
    colProps: { span: 15 },
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
  // {
  //   field: 'email',
  //   label: '邮箱',
  //   rules: [
  //     {
  //       required: true,
  //       // @ts-ignore
  //       validator: async (rule, value) => {
  //         const regEmails = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
  //         if (!value) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('请输入邮箱');
  //         }
  //         if (!regEmails.test(value)) {
  //           /* eslint-disable-next-line */
  //           return Promise.reject('邮箱格式错误');
  //         }
  //         return Promise.resolve();
  //       },
  //       trigger: 'change',
  //     },
  //   ],
  //   component: 'Input',
  // },
];
