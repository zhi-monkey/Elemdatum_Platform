<template>
  <CollapseContainer title="个人信息" :canExpan="false">
    <a-row :gutter="24">
      <a-col :span="14">
        <BasicForm @register="register" />
      </a-col>
    </a-row>
    <div>
      <a-button type="primary" @click="handleSubmit" style="margin-left: 120px">更新信息</a-button>
      <a-button color="warning" @click="handlePassword" style="margin-left: 20px">
        修改密码
      </a-button>
    </div>
  </CollapseContainer>
</template>
<script lang="ts" setup>
  import { Row as ARow, Col as ACol, Button as AButton } from 'ant-design-vue';
  import { onMounted } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { CollapseContainer } from '/@/components/Container';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { userschemas } from './data';
  import { useUserStore } from '/@/store/modules/user';
  import { useGo } from '/@/hooks/web/usePage';
  import { getUserInfo, updateUserInfo } from '/@/views/mineai/system/api/user';

  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const go = useGo();

  const [register, { setFieldsValue, validate, getFieldsValue }] = useForm({
    labelWidth: 120,
    schemas: userschemas,
    showActionButtonGroup: false,
  });

  onMounted(async () => {
    const newUserInfo = await getUserInfo();
    userStore.userInfo = newUserInfo;
    const data = userStore.getUserInfo;
    await setFieldsValue(data);
  });

  function handlePassword() {
    const values = getFieldsValue();
    go('/maUser/change_password/' + values.username);
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      await updateUserInfo(values);
      userStore.userInfo = await getUserInfo();
      createMessage.success('更新成功！');
    } catch (e) {
      console.log(e);
    }
  }
</script>
