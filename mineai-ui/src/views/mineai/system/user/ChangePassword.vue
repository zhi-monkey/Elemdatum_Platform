<template>
  <PageWrapper
    :title="`修改用户` + username + `的密码`"
    @back="goBack"
    style="margin: 0 16px 0 16px"
  >
    <div class="py-8 bg-white flex flex-col justify-center items-center">
      <BasicForm @register="register" />
      <div class="flex justify-center">
        <a-button @click="resetFields"> 重置</a-button>
        <a-button class="!ml-4" type="primary" @click="handleSubmit"> 确认</a-button>
      </div>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { defineComponent, ref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useRoute, useRouter } from 'vue-router';

  import { formSchema } from './pwd.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton } from 'ant-design-vue';
  import { updatePass } from '/@/views/mineai/system/api/user';
  import { encrypt } from '/@/utils/dubhe/rsaEncrypt';

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  const { success } = createMessage;

  // 此处可以得到用户名
  const username = ref(route.params?.username);
  const [register, { validate, resetFields }] = useForm({
    size: 'large',
    labelWidth: 100,
    showActionButtonGroup: false,
    schemas: formSchema,
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      values.username = route.params?.username;
      await updatePass({ newPass: encrypt(values.newPass), oldPass: encrypt(values.oldPass) });
      success('修改成功');
      router.go(-1);
    } catch (error) {}
  }

  function goBack() {
    router.go(-1);
  }
</script>
<style></style>
