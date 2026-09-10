<template>
  <!-- 显示 loading 或正常登录表单 -->
  <div v-if="showLoading" class="loading-container">
    <!--  <div v-if="true" class="loading-container">-->
    <Spin size="large" class="custom-spin" />
  </div>
  <LoginFormTitle v-show="getShow && !showLoading" class="enter-x" />
  <Form
    class="p-4 enter-x"
    :model="formData"
    :rules="getFormRules"
    ref="formRef"
    v-show="getShow && !showLoading"
    @keypress.enter="handleLogin"
  >
    <FormItem name="account" class="enter-x">
      <Input
        size="large"
        v-model:value="formData.account"
        :placeholder="t('sys.login.userName')"
        class="fix-auto-fill"
        @blur="handleTrimAccount"
      />
    </FormItem>
    <FormItem name="password" class="enter-x">
      <InputPassword
        size="large"
        visibilityToggle
        oncopy="return false"
        onpaste="return false"
        v-model:value="formData.password"
        :placeholder="t('sys.login.password')"
        class="fix-auto-fill"
        @input="handleTrimPassword"
      />
    </FormItem>

    <ARow class="enter-x">
      <ACol :span="12">
        <FormItem>
          <!-- No logic, you need to deal with it yourself -->
          <Checkbox v-model:checked="rememberMe" size="small">
            {{ t('sys.login.rememberMe') }}
          </Checkbox>
        </FormItem>
      </ACol>
      <ACol :span="12">
        <FormItem :style="{ 'text-align': 'right' }">
          <Button type="link" size="small" @click="forgetPassword">
            {{ t('sys.login.forgetPassword') }}
          </Button>
          <Button type="link" size="small" @click="changeLoginStateToRegister">
            {{ t('sys.login.gotoRegister') }}
          </Button>
        </FormItem>
      </ACol>
    </ARow>

    <FormItem class="enter-x">
      <Button type="primary" size="large" block @click="handleLogin" :loading="loading">
        {{ t('sys.login.loginButton') }}
      </Button>
    </FormItem>

    <!--    <Button block @click="setLoginState(LoginStateEnum.REGISTER)">-->
    <!--      {{ t('sys.login.registerButton') }}-->
    <!--    </Button>-->
  </Form>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref, unref, watch } from 'vue';
  import { Button, Checkbox, Col, Form, Input, Row, Spin } from 'ant-design-vue';
  import LoginFormTitle from './LoginFormTitle.vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserStore } from '/@/store/modules/user';
  import { LoginStateEnum, useFormRules, useFormValid, useLoginState } from './useLogin';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { getTokenFromCookie, getUserInfoFromCookie, setUserInfoToCookie } from '/@/utils/dubhe';

  const ACol = Col;
  const ARow = Row;
  const FormItem = Form.Item;
  const InputPassword = Input.Password;
  const { t } = useI18n();
  const { notification, createErrorModal, createMessage } = useMessage();
  const { prefixCls } = useDesign('login');
  const userStore = useUserStore();
  const { setLoginState, getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();

  const formRef = ref();
  const loading = ref(false);
  const rememberMe = ref(false);

  function handleTrimPassword() {
    if (formData.password) {
      // Remove spaces from the password
      formData.password = formData.password.replace(/\s+/g, '');
    }
  }

  const formData = reactive({
    account: '',
    password: '',
  });

  const { validForm } = useFormValid(formRef);

  // 新增的逻辑
  const showLoading = ref(false);

  const getShow = computed(
    () => unref(getLoginState) === LoginStateEnum.LOGIN && !showLoading.value,
  );

  const changeLoginStateToRegister = () => {
    setLoginState(LoginStateEnum.REGISTER);
  };

  onMounted(() => {
    // 通过 window.location.hash 获取 URL 的 hash 部分
    const urlHash = window.location.hash;

    // 使用正则表达式从 hash 中提取查询参数
    const match = urlHash.match(/[\?&]externalLogin=1/);

    //外部登录接口测试逻辑
    if (match) {
      showLoading.value = true; // 显示 loading
      // 自动填充用户名和密码
      formData.account = 'admin';
      formData.password = 'cari1234';
      // 执行登录
      handleLogin();
    }
    //如果之前选择了记住我，应该有用户名和密码保存，读取一下
    if (getUserInfoFromCookie() != null) {
      try {
        const parsedUserInfo = JSON.parse(getUserInfoFromCookie());
        if (parsedUserInfo.account && parsedUserInfo.password) {
          formData.account = parsedUserInfo.account;
          formData.password = parsedUserInfo.password;
          rememberMe.value = true; // 设置"记住我"复选框为选中状态
          // handleLogin();
        }
      } catch (error) {
        console.error('解析保存的登录信息时出错:', error);
      }
    }
  });

  async function handleLogin() {
    const data = await validForm();
    if (!data) return;
    try {
      loading.value = true;
      const user = await userStore.login({
        password: data.password,
        username: data.account,
        mode: 'none', //不要默认的错误提示
      });
      if (user) {
        if (rememberMe.value) {
          setUserInfoToCookie(JSON.stringify({ account: data.account, password: data.password }));
        }
        if (user.roles[0].name === '游客') {
          notification.warn({
            message: t('sys.login.loginSuccessTitle'),
            description: `用户 ${user.username} 您好, ${t('sys.login.visitorNotification')}`,
            duration: null,
          });
        } else {
          notification.success({
            message: t('sys.login.loginSuccessTitle'),
            description: `${t('sys.login.loginSuccessDesc')}: ${user.username}`,
            duration: 3,
          });
        }
      }
    } catch (error) {
      console.error('[ login ]: ', error);
      createErrorModal({
        title: t('sys.api.errorTip'),
        content: (error as unknown as Error).message || t('sys.api.networkExceptionMsg'),
        getContainer: () => document.body.querySelector(`.${prefixCls}`) || document.body,
      });
    } finally {
      loading.value = false;
      showLoading.value = false; // 隐藏 loading
    }
  }

  function forgetPassword() {
    createMessage.warn('请联系管理员修改密码！');
  }

  function handleTrimAccount() {
    if (formData.account) {
      formData.account = formData.account.trim();
    }
  }

  watch(
    () => formData.account,
    (newVal) => {
      const trimmed = newVal.trim();
      if (newVal !== trimmed) {
        formData.account = trimmed;
      }
    },
  );
</script>

<style scoped>
  .loading-container {
    display: flex;
    justify-content: right;
    align-items: center;
    height: 85vh;
  }

  /* 使用更高优先级的选择器 */
  ::v-deep .ant-spin-dot {
    font-size: 120px; /* 增大旋转图标的大小 */
  }

  /* 使用更高优先级的选择器来调整点的大小 */
  ::v-deep .ant-spin-dot i {
    width: 45px !important; /* 增大蓝色点的宽度 */
    height: 45px !important; /* 增大蓝色点的高度 */
    border-radius: 50%; /* 确保点是圆形 */
  }
</style>
