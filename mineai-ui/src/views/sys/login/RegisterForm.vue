<template>
  <template v-if="getShow">
    <LoginFormTitle class="enter-x" />
    <BasicForm class="enter-x" @register="registerForm" @submit="handleSubmit" />
    <Button size="large" block class="mt-4 enter-x" @click="handleBackLogin"> 返回登录 </Button>
  </template>
</template>
<script lang="ts" setup>
  import { unref, computed, h } from 'vue';
  import LoginFormTitle from './LoginFormTitle.vue';
  import { Button } from 'ant-design-vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form/index';
  import { useLoginState, LoginStateEnum } from './useLogin';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { notification } from 'ant-design-vue/es';
  const formSchema: FormSchema[] = [
    {
      field: 'username',
      label: '用户名',
      component: 'Input',
      show: (params) => {
        return params.model.createTime === undefined;
      },
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
      field: 'name',
      label: '昵称',
      component: 'Input',
      componentProps: {
        autoComplete: 'off',
      },
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
      field: 'password',
      label: '密码',
      component: 'StrengthMeter',
      show: (params) => {
        return params.model.createTime === undefined;
      },
      componentProps: {
        placeholder: '密码',
        autocomplete: 'new-password',
      },
      rules: [
        {
          required: true,
          message: '请输入密码',
        },
      ],
    },
    {
      field: 'confirmPassword',
      label: '确认密码',
      component: 'InputPassword',
      ifShow: (params) => {
        // console.log(params);
        return params.model.createTime === undefined;
      },
      componentProps: {
        placeholder: '请再次输入密码',
        autoComplete: 'off',
      },
      dynamicRules: ({ values }) => {
        return [
          {
            required: true,
            validator: (_, value) => {
              if (!value) {
                return Promise.reject('密码不能为空');
              }
              if (value !== values.password) {
                return Promise.reject('两次输入的密码不一致!');
              }
              return Promise.resolve();
            },
          },
        ];
      },
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
  ];

  const { handleBackLogin, getLoginState } = useLoginState();
  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.REGISTER);
  const { createMessage } = useMessage();
  const [registerForm, { validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    size: 'large',
    actionColOptions: { span: 24 },
    submitButtonOptions: {
      text: '提交',
    },
    labelAlign: 'left',
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      values.status = 2;
      values.role = { id: -1 };
      values.department = { id: -2 };
      await maHttp.post(
        {
          url: 'auth/register',
          params: values,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
      );

      //提示
      notification.open({
        message: '提示',
        description: '注册成功，请联系管理员分配权限',
        onClose: close,
        duration: 0,
      });
      //返回登录页
      handleBackLogin();

      //提示注册成功
      // createMessage.success('注册成功，请联系管理员分配权限');
    } catch (e) {
      console.log('e', e);
    }
  }
</script>
<style>
  [data-theme='dark'] .ant-input-affix-wrapper {
    border: 1px solid #232a3b;
    background-color: #232a3b;
  }

  [data-theme='dark'] .ant-radio-button-wrapper {
    color: #c9d1d9;
    background: #232a3b;
    border: 1px solid #232a3b;
  }
</style>
